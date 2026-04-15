package com.cleanspace.data.repository

import android.annotation.SuppressLint
import android.app.RecoverableSecurityException
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.cleanspace.data.local.database.dao.WhitelistDao
import com.cleanspace.data.local.preferences.SettingsDataStore
import com.cleanspace.data.models.FileItem
import com.cleanspace.data.models.FileSource
import com.cleanspace.data.models.FileType
import com.cleanspace.data.models.ScanProgress
import com.cleanspace.data.models.ScanResult
import com.cleanspace.data.storage.MediaStoreScanner
import com.cleanspace.data.storage.SafAccessManager
import com.cleanspace.domain.usecases.FindDuplicatesUseCase
import com.cleanspace.domain.utils.FileClassifier
import com.cleanspace.domain.utils.SafFileClassifier
import com.cleanspace.utils.AppLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val whitelistDao: WhitelistDao,
    private val mediaStoreScanner: MediaStoreScanner,
    private val findDuplicatesUseCase: FindDuplicatesUseCase,
) : ScanRepository {

    private val cancelled = AtomicBoolean(false)
    private val scanning = AtomicBoolean(false)

    override fun cancelScan() {
        cancelled.set(true)
    }

    override fun isScanning(): Boolean = scanning.get()

    override suspend fun scanFiles(onProgress: suspend (ScanProgress) -> Unit): ScanResult =
        withContext(Dispatchers.IO) {
            scanning.set(true)
            cancelled.set(false)
            val start = System.currentTimeMillis()

            val largeThreshold = settingsDataStore.largeFileThreshold.first()
            val maxDepth = 10

            val enabledWhitelist = whitelistDao.getEnabledDirectories().first().map { it.path }
            val safTrees = enabledWhitelist
                .filter { it.startsWith("content://") }
                .mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }
            val hasSafScope = safTrees.isNotEmpty()

            val items = ArrayList<FileItem>(1024)
            var filesFound = 0
            var totalSize = 0L

            // 0) MediaStore scan (only when SAF scope is enabled)
            if (hasSafScope) {
                runCatching {
                    val msItems = mediaStoreScanner.scanAll(largeThreshold, maxItems = 5000)
                    AppLog.d("MediaStore items: ${msItems.size}")
                    items.addAll(msItems)
                    filesFound += msItems.size
                    totalSize += msItems.sumOf { it.sizeBytes }

                    onProgress(
                        ScanProgress(
                            percent = if (msItems.isEmpty()) 0 else 5,
                            currentDirectory = "MediaStore",
                            filesFound = filesFound,
                            totalSizeFound = totalSize,
                            isCancelled = false,
                            currentDepth = 0,
                        ),
                    )
                }.onFailure { e ->
                    AppLog.e("MediaStore scan failed", e)
                }
            }

            // 0.5) Accessible cache scan (only what Android allows this app to access)
            runCatching {
                val cacheItems = scanAccessibleCacheFiles(maxItems = 1200)
                if (cacheItems.isNotEmpty()) {
                    items.addAll(cacheItems)
                    filesFound += cacheItems.size
                    totalSize += cacheItems.sumOf { it.sizeBytes }

                    onProgress(
                        ScanProgress(
                            percent = 8,
                            currentDirectory = "Кеш телефону (доступний)",
                            filesFound = filesFound,
                            totalSizeFound = totalSize,
                            isCancelled = false,
                            currentDepth = 0,
                        ),
                    )
                }
            }.onFailure { e ->
                AppLog.e("Accessible cache scan failed", e)
            }

            // SAF trees
            // (already resolved above)

            // Legacy roots (only when SAF scope is enabled)
            val roots = if (hasSafScope) {
                buildList { add(Environment.getExternalStorageDirectory()) }
                    .distinct()
                    .filter { it.exists() && it.isDirectory }
            } else {
                emptyList()
            }

            // Rough progress accounting / estimates
            val mediaStoreEstimate = items.size
            val safEstimate = safTrees.sumOf { treeUri ->
                runCatching { countSafFiles(treeUri, maxDepth) }.getOrDefault(0)
            }.coerceAtLeast(0)
            val legacyEstimate = roots.sumOf { root ->
                runCatching { countLegacyFiles(root, maxDepth, limit = 10_000) }.getOrDefault(0)
            }.coerceAtLeast(0)

            val totalEstimate = (mediaStoreEstimate + safEstimate + legacyEstimate).coerceAtLeast(1)
            var processedTotal = mediaStoreEstimate

            suspend fun emitProgress(currentDir: String, depth: Int, force: Boolean = false) {
                if (!force && processedTotal % 200 != 0) return
                val percent = ((processedTotal * 100) / totalEstimate).coerceIn(0, 100)
                onProgress(
                    ScanProgress(
                        percent = percent,
                        currentDirectory = currentDir,
                        filesFound = filesFound,
                        totalSizeFound = totalSize,
                        isCancelled = false,
                        currentDepth = depth,
                    ),
                )
            }

            AppLog.d("Enabled whitelist entries: ${enabledWhitelist.size} (SAF trees: ${safTrees.size})")

            if (!hasSafScope) {
                AppLog.d("SAF scope disabled: skipping global scans, showing only accessible cache")
            }

            // 1) SAF scan
            for (treeUri in safTrees) {
                ensureActive()
                if (cancelled.get()) throw kotlinx.coroutines.CancellationException("Scan cancelled")

                scanSafTree(
                    treeUri = treeUri,
                    maxDepth = maxDepth,
                    largeThreshold = largeThreshold,
                    items = items,
                    onEachFile = { dir, depth, itemSize ->
                        filesFound++
                        processedTotal++
                        totalSize += itemSize
                        emitProgress(dir, depth)
                    },
                )
            }

            // 2) Legacy File scan
            val queue: ArrayDeque<Pair<File, Int>> = ArrayDeque()
            roots.forEach { queue.add(it to 0) }

            while (queue.isNotEmpty()) {
                ensureActive()
                if (cancelled.get()) throw kotlinx.coroutines.CancellationException("Scan cancelled")

                val (dir, depth) = queue.removeFirst()
                if (depth > maxDepth) continue

                val dirPath = dir.absolutePath
                if (isSystemDir(dirPath)) continue
                if (enabledWhitelist.any { it.startsWith("/") && dirPath.startsWith(it) }) continue

                val children = dir.listFiles() ?: continue
                for (child in children) {
                    ensureActive()
                    if (cancelled.get()) throw kotlinx.coroutines.CancellationException("Scan cancelled")

                    if (child.isDirectory) {
                        queue.add(child to (depth + 1))
                    } else {
                        val type = FileClassifier.classify(child, largeThreshold)
                        items.add(
                            FileItem(
                                path = child.absolutePath,
                                uri = null,
                                source = FileSource.FILE_PATH,
                                name = child.name,
                                sizeBytes = child.length(),
                                type = type,
                                lastModified = child.lastModified(),
                                isDeletable = type != FileType.SYSTEM,
                                parentDirectory = child.parentFile?.absolutePath ?: "",
                            )
                        )
                        filesFound++
                        processedTotal++
                        totalSize += child.length()
                        emitProgress(dirPath, depth)
                    }
                }
            }

            // End progress
            processedTotal = totalEstimate
            emitProgress(currentDir = "Сканування завершено", depth = 0, force = true)

            AppLog.d("Total items after scans: ${items.size}")

            // Duplicates are heavy: compute on-demand from UI instead of during scanning.
            val duplicateGroups: List<List<FileItem>> = emptyList()
            val categoriesSummary = items.groupBy { it.type }.mapValues { (_, v) -> v.sumOf { it.sizeBytes } }
            val countByType = items.groupBy { it.type }.mapValues { (_, v) -> v.size }

            AppLog.d("Count by type: $countByType")

            val duration = System.currentTimeMillis() - start
            scanning.set(false)

            ScanResult(
                items = items.take(5000),
                totalSize = totalSize,
                scanDuration = duration,
                categoriesSummary = categoriesSummary,
                duplicatesGroups = duplicateGroups,
                filesCountByType = countByType,
            )
        }

    @SuppressLint("ObsoleteSdkInt")
    override suspend fun deleteFiles(
        files: List<FileItem>,
        onProgress: suspend (Int) -> Unit,
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            var freed = 0L

            AppLog.d("Delete requested: total=${'$'}{files.size}")

            // If any MediaStore items require confirmation, request it in one batch.
            val mediaStoreUris = files
                .asSequence()
                .filter { it.source == FileSource.MEDIA_STORE && it.isDeletable }
                .mapNotNull { it.uri }
                .mapNotNull { s -> runCatching { Uri.parse(s) }.getOrNull() }
                .toList()

            AppLog.d("Delete MediaStore candidates: ${'$'}{mediaStoreUris.size}")

            // On Android 11+ we can request user confirmation upfront for a list of URIs.
            if (mediaStoreUris.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // IMPORTANT: this is not an error; it is the normal Android flow.
                val sender = MediaStore.createDeleteRequest(context.contentResolver, mediaStoreUris).intentSender
                AppLog.d("Emitting DeleteRequestException for ${'$'}{mediaStoreUris.size} uris")
                throw DeleteRequestException(sender)
            }

            files.forEachIndexed { index, fileItem ->
                try {
                    when (fileItem.source) {
                        FileSource.MEDIA_STORE -> {
                            val u = fileItem.uri?.let { Uri.parse(it) }
                            if (fileItem.isDeletable && u != null) {
                                val rows = context.contentResolver.delete(u, null, null)
                                AppLog.d("MediaStore delete rows=${'$'}rows uri=${'$'}u")
                                if (rows > 0) freed += fileItem.sizeBytes
                            }
                        }

                        FileSource.SAF_TREE -> {
                            val u = fileItem.uri?.let { Uri.parse(it) }
                            if (fileItem.isDeletable && u != null) {
                                val ok = SafAccessManager.deleteDocument(context.contentResolver, u)
                                AppLog.d("SAF delete ok=${'$'}ok uri=${'$'}u")
                                if (ok) freed += fileItem.sizeBytes
                            }
                        }

                        else -> {
                            val f = File(fileItem.path)
                            if (fileItem.isDeletable && f.exists() && f.isFile) {
                                val len = f.length()
                                val ok = f.delete()
                                AppLog.d("File delete ok=${'$'}ok path=${'$'}{f.absolutePath}")
                                if (ok) freed += len
                            }
                        }
                    }
                } catch (e: SecurityException) {
                    AppLog.e("Delete SecurityException source=${'$'}{fileItem.source} uri=${'$'}{fileItem.uri}", e)

                    // Android 10 (Q) RecoverableSecurityException
                    val sender = when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException -> {
                            e.userAction.actionIntent.intentSender
                        }

                        else -> null
                    }

                    if (sender != null) throw DeleteRequestException(sender)
                    else throw e
                }

                onProgress(((index + 1) * 100) / (files.size.coerceAtLeast(1)))
            }

            AppLog.d("Delete completed freed=${'$'}freed")
            freed
        }
    }

    private fun isSystemDir(path: String): Boolean {
        val p = path.lowercase()
        return p.startsWith("/system") || p.startsWith("/proc") || p.startsWith("/dev") || p.startsWith("/sys")
    }

    private suspend fun scanSafTree(
        treeUri: Uri,
        maxDepth: Int,
        largeThreshold: Long,
        items: MutableList<FileItem>,
        onEachFile: suspend (currentDir: String, depth: Int, size: Long) -> Unit,
    ) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return

        val queue: ArrayDeque<Pair<DocumentFile, Int>> = ArrayDeque()
        queue.add(root to 0)

        while (queue.isNotEmpty()) {
            if (cancelled.get()) throw kotlinx.coroutines.CancellationException("Scan cancelled")

            val (dir, depth) = queue.removeFirst()
            if (depth > maxDepth) continue

            val dirName = dir.name ?: treeUri.toString()

            dir.listFiles().forEach { child ->
                if (cancelled.get()) throw kotlinx.coroutines.CancellationException("Scan cancelled")

                if (child.isDirectory) {
                    queue.add(child to (depth + 1))
                } else {
                    val name = child.name ?: "(unknown)"
                    val size = child.length()
                    val lastModified = child.lastModified()
                    val type = SafFileClassifier.classify(
                        documentName = name,
                        parentDirectory = dirName,
                        sizeBytes = size,
                        largeThreshold = largeThreshold,
                    )

                    items.add(
                        FileItem(
                            path = "SAF:${'$'}treeUri",
                            uri = child.uri.toString(),
                            source = FileSource.SAF_TREE,
                            name = name,
                            sizeBytes = size,
                            type = type,
                            lastModified = lastModified,
                            isDeletable = type != FileType.SYSTEM,
                            parentDirectory = dirName,
                        ),
                    )

                    onEachFile(dirName, depth, size)
                }
            }
        }
    }

    private fun countSafFiles(treeUri: Uri, maxDepth: Int): Int {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return 0

        val queue: ArrayDeque<Pair<DocumentFile, Int>> = ArrayDeque()
        queue.add(root to 0)

        var fileCount = 0

        while (queue.isNotEmpty()) {
            if (cancelled.get()) throw kotlinx.coroutines.CancellationException("Scan cancelled")

            val (dir, depth) = queue.removeFirst()
            if (depth > maxDepth) continue

            dir.listFiles().forEach { child ->
                if (cancelled.get()) throw kotlinx.coroutines.CancellationException("Scan cancelled")

                if (child.isDirectory) {
                    queue.add(child to (depth + 1))
                } else {
                    fileCount++
                }
            }
        }

        return fileCount
    }

    private fun countLegacyFiles(root: File, maxDepth: Int, limit: Int): Int {
        val queue: ArrayDeque<Pair<File, Int>> = ArrayDeque()
        queue.add(root to 0)
        var count = 0

        while (queue.isNotEmpty()) {
            val (dir, depth) = queue.removeFirst()
            if (depth > maxDepth) continue

            val children = dir.listFiles() ?: continue
            for (child in children) {
                if (child.isDirectory) {
                    queue.add(child to (depth + 1))
                } else {
                    count++
                    if (count >= limit) return count
                }
            }
        }
        return count
    }

    private fun scanAccessibleCacheFiles(maxItems: Int): List<FileItem> {
        val roots = buildList {
            context.cacheDir?.let { add(it) }
            context.externalCacheDirs?.filterNotNull()?.forEach { add(it) }
        }
            .distinctBy { it.absolutePath }
            .filter { it.exists() && it.isDirectory }

        if (roots.isEmpty()) return emptyList()

        val out = ArrayList<FileItem>(256)
        val queue: ArrayDeque<File> = ArrayDeque(roots)

        while (queue.isNotEmpty() && out.size < maxItems) {
            val dir = queue.removeFirst()
            val children = dir.listFiles() ?: continue

            for (child in children) {
                if (out.size >= maxItems) break

                if (child.isDirectory) {
                    queue.add(child)
                } else if (child.isFile) {
                    out.add(
                        FileItem(
                            path = child.absolutePath,
                            uri = null,
                            source = FileSource.FILE_PATH,
                            name = child.name,
                            sizeBytes = child.length().coerceAtLeast(0L),
                            type = FileType.CACHE,
                            lastModified = child.lastModified(),
                            isDeletable = true,
                            parentDirectory = child.parentFile?.absolutePath ?: "",
                        ),
                    )
                }
            }
        }

        return out
    }
}
