package com.cleanspace.data.repository

import android.content.IntentSender

/**
 * Thrown when deleting a MediaStore item requires user confirmation.
 */
class DeleteRequestException(
    val intentSender: IntentSender,
) : Exception("Delete requires user confirmation")

