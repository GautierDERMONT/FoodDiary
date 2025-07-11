// FoodDiaryApplication.kt
package com.fooddiary

import android.app.Application
import com.fooddiary.data.AppDatabase
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache

class FoodDiaryApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }

    val imageLoader by lazy {
        ImageLoader.Builder(this)
            .crossfade(true)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // 50MB
                    .build()
            }
            .build()
    }
}