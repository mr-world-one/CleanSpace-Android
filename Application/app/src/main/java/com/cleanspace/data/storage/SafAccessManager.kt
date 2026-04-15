package com.cleanspace.data.storage

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile

object SafAccessManager {

    fun takePersistablePermission(context: Context, treeUri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(treeUri, flags)
    }

    fun hasReadWritePermission(context: Context, treeUri: Uri): Boolean {
        val resolver = context.contentResolver
        return resolver.persistedUriPermissions.any { p ->
            p.uri == treeUri && p.isReadPermission && p.isWritePermission
        }
    }

    fun listFiles(context: Context, treeUri: Uri): Sequence<DocumentFile> {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptySequence()
        return root.listFiles().asSequence()
    }

    fun deleteDocument(resolver: ContentResolver, documentUri: Uri): Boolean {
        return try {
            DocumentsContract.deleteDocument(resolver, documentUri)
        } catch (_: Exception) {
            false
        }
    }
}

