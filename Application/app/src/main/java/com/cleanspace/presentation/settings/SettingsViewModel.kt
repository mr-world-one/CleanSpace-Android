package com.cleanspace.presentation.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cleanspace.data.local.database.dao.WhitelistDao
import com.cleanspace.data.local.database.entities.WhitelistEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val whitelistDao: WhitelistDao,
) : ViewModel() {

    val whitelistDirectories: StateFlow<List<WhitelistEntity>> =
        whitelistDao.getAllDirectories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addSafTree(treeUri: String) {
        viewModelScope.launch {
            whitelistDao.insert(WhitelistEntity(path = treeUri, isEnabled = true))
        }
    }

    fun toggle(entry: WhitelistEntity) {
        viewModelScope.launch {
            whitelistDao.insert(entry.copy(isEnabled = !entry.isEnabled))
        }
    }

    fun remove(entry: WhitelistEntity) {
        viewModelScope.launch {
            whitelistDao.delete(entry)
            revokeSafPermissionIfAny(entry.path)
        }
    }

    private fun revokeSafPermissionIfAny(path: String) {
        if (!path.startsWith("content://")) return
        val uri = runCatching { Uri.parse(path) }.getOrNull() ?: return
        val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            context.contentResolver.releasePersistableUriPermission(uri, flags)
        }
    }
}
