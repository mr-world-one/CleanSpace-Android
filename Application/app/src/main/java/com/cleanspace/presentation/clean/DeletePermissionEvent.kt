package com.cleanspace.presentation.clean

import android.content.IntentSender

sealed interface DeletePermissionEvent {
    data class Request(val intentSender: IntentSender) : DeletePermissionEvent
}

