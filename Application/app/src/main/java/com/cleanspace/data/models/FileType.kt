package com.cleanspace.data.models

enum class FileType {
    CACHE,
    DUPLICATE,
    TEMPORARY,
    LARGE,

    // MediaStore categories (MVP)
    MEDIA_IMAGES,
    MEDIA_VIDEO,
    MEDIA_AUDIO,
    SCREENSHOTS,
    MESSENGERS,
    DOWNLOADS,

    SYSTEM,
    UNKNOWN
}
