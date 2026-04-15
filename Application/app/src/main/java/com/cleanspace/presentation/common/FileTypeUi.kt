package com.cleanspace.presentation.common

import android.content.Context
import com.cleanspace.R
import com.cleanspace.data.models.FileType

object FileTypeUi {

    fun displayName(context: Context, type: FileType): String {
        return context.getString(
            when (type) {
                FileType.CACHE -> R.string.cache_files
                FileType.DUPLICATE -> R.string.duplicates
                FileType.TEMPORARY -> R.string.temporary_files
                FileType.LARGE -> R.string.large_files
                FileType.SYSTEM -> R.string.system_files
                FileType.UNKNOWN -> R.string.unknown_files

                // Media categories
                FileType.SCREENSHOTS -> R.string.category_screenshots
                FileType.DOWNLOADS -> R.string.category_downloads
                FileType.MESSENGERS -> R.string.category_messengers
                FileType.MEDIA_IMAGES -> R.string.category_images
                FileType.MEDIA_VIDEO -> R.string.category_videos
                FileType.MEDIA_AUDIO -> R.string.category_audio
            },
        )
    }
}

