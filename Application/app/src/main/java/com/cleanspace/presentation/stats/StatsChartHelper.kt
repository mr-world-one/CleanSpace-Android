package com.cleanspace.presentation.stats

import com.cleanspace.data.local.database.entities.CleaningRecordEntity
import com.cleanspace.data.models.FileType
import com.cleanspace.data.models.ScanResult
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieEntry
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StatsChartHelper {

    data class LineSeries(
        val entries: List<Entry>,
        val xLabels: List<String>,
    )

    enum class DashboardBucket(val label: String) {
        PHOTO("Фото"),
        DUPLICATES("Дублікати"),
        AUDIO_VIDEO("Аудіо/Відео"),
        PHONE_CACHE("Кеш телефону"),
    }

    /**
     * Builds 7-day chart data (last 7 days, including today). Values are freedBytes per day.
     */
    fun buildWeeklyFreedSeries(nowMs: Long, records: List<CleaningRecordEntity>): LineSeries {
        val dayMs = 24L * 60L * 60L * 1000L
        val start = normalizeToDayStart(nowMs - 6 * dayMs)

        val totalsByDayIndex = LongArray(7)
        records.forEach { r ->
            val dayStart = normalizeToDayStart(r.date)
            val index = ((dayStart - start) / dayMs).toInt()
            if (index in 0..6) totalsByDayIndex[index] += r.freedBytes
        }

        val df = SimpleDateFormat("dd.MM", Locale("uk"))
        val entries = (0..6).map { i ->
            Entry(i.toFloat(), (totalsByDayIndex[i] / (1024f * 1024f)).coerceAtLeast(0f)) // MB
        }
        val labels = (0..6).map { i ->
            df.format(Date(start + i * dayMs))
        }

        return LineSeries(entries, labels)
    }

    /**
     * Pie chart grouped exactly like dashboard cards.
     */
    fun buildCategoryPie(records: List<CleaningRecordEntity>): List<PieEntry> {
        val totals = emptyDashboardTotals()

        records.forEach { r ->
            val buckets = parseCategories(r.categories)
                .mapNotNull { mapToDashboardBucket(it) }
                .distinct()

            if (buckets.isEmpty()) return@forEach

            val part = r.freedBytes / buckets.size
            buckets.forEach { bucket ->
                totals[bucket] = (totals[bucket] ?: 0L) + part
            }
        }

        return toPieEntries(totals)
    }

    /**
     * Pie chart from the latest scan result (current state on device).
     */
    fun buildCategoryPieFromScan(scan: ScanResult?): List<PieEntry> {
        if (scan == null) return emptyList()

        val totals = emptyDashboardTotals()
        scan.categoriesSummary.forEach { (type, bytes) ->
            val bucket = mapToDashboardBucket(type.name) ?: return@forEach
            totals[bucket] = (totals[bucket] ?: 0L) + bytes.coerceAtLeast(0L)
        }

        // Duplicates in dashboard/scan are represented by groups, not always by categoriesSummary.
        val duplicatesBytes = scan.duplicatesGroups.sumOf { group ->
            group.sortedBy { it.lastModified }.drop(1).sumOf { it.sizeBytes }
        }
        if (duplicatesBytes > 0L) {
            totals[DashboardBucket.DUPLICATES] = duplicatesBytes
        }

        return toPieEntries(totals)
    }

    private fun toPieEntries(totals: Map<DashboardBucket, Long>): List<PieEntry> =
        totals
            .map { (bucket, bytes) -> bucket to bytes.coerceAtLeast(0L) }
            .filter { (_, bytes) -> bytes > 0L }
            .map { (bucket, bytes) -> PieEntry(bytes.toFloat(), bucket.label) }

    private fun emptyDashboardTotals() = linkedMapOf(
        DashboardBucket.PHOTO to 0L,
        DashboardBucket.DUPLICATES to 0L,
        DashboardBucket.AUDIO_VIDEO to 0L,
        DashboardBucket.PHONE_CACHE to 0L,
    )

    private fun mapToDashboardBucket(raw: String): DashboardBucket? {
        val normalized = raw.trim().uppercase(Locale.ROOT)
        val type = runCatching { FileType.valueOf(normalized) }.getOrNull()

        if (type != null) {
            return when (type) {
                FileType.DUPLICATE -> DashboardBucket.DUPLICATES
                FileType.MEDIA_IMAGES, FileType.SCREENSHOTS -> DashboardBucket.PHOTO
                FileType.MEDIA_AUDIO, FileType.MEDIA_VIDEO -> DashboardBucket.AUDIO_VIDEO
                FileType.CACHE,
                FileType.TEMPORARY,
                FileType.LARGE,
                FileType.MESSENGERS,
                FileType.DOWNLOADS,
                FileType.SYSTEM,
                FileType.UNKNOWN -> DashboardBucket.PHONE_CACHE
            }
        }

        return when (normalized) {
            "PHOTO", "PHOTOS", "IMAGES", "MEDIA_IMAGES", "SCREENSHOTS" -> DashboardBucket.PHOTO
            "DUPLICATE", "DUPLICATES" -> DashboardBucket.DUPLICATES
            "AUDIO", "VIDEO", "AUDIO_VIDEO", "MEDIA_AUDIO", "MEDIA_VIDEO" -> DashboardBucket.AUDIO_VIDEO
            "CACHE", "TEMPORARY", "LARGE" -> DashboardBucket.PHONE_CACHE
            else -> null
        }
    }

    private fun parseCategories(json: String): List<String> = try {
        val arr = JSONArray(json)
        buildList {
            for (i in 0 until arr.length()) {
                val v = arr.optString(i).trim()
                if (v.isNotEmpty()) add(v)
            }
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun normalizeToDayStart(timeMs: Long): Long {
        val dayMs = 24L * 60L * 60L * 1000L
        return timeMs - (timeMs % dayMs)
    }
}
