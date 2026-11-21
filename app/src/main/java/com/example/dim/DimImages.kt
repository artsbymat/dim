package com.example.dim

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.net.Uri
import android.provider.MediaStore
import androidx.annotation.RequiresApi

@RequiresApi(29)
internal fun saveScreenshot(context: Context, bitmap: Bitmap): Boolean {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "Dim-${System.currentTimeMillis()}.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Dim")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    var saved = false
    try {
        if (resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } != true) {
            return false
        }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        saved = resolver.update(uri, values, null, null) > 0
        return saved
    } finally {
        if (!saved) resolver.delete(uri, null, null)
    }
}

internal fun restoreScreenshot(resolver: ContentResolver, source: Uri, destination: Uri, opacity: Int): Boolean {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
    if (options.outWidth <= 0 || options.outHeight <= 0) return false

    // shortcut: downsample very large inputs to bound memory, use tiled decoding to preserve their resolution.
    options.inSampleSize = 1
    while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > 4096) {
        options.inSampleSize *= 2
    }
    options.inJustDecodeBounds = false
    options.inMutable = true
    options.inPreferredConfig = Bitmap.Config.ARGB_8888
    options.inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
    val bitmap = resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
        ?: return false
    try {
        val channels = IntArray(256) { restoreChannel(it, opacity) }
        val row = IntArray(bitmap.width)
        for (y in 0 until bitmap.height) {
            bitmap.getPixels(row, 0, row.size, 0, y, row.size, 1)
            for (x in row.indices) {
                val pixel = row[x]
                row[x] = (pixel and 0xff000000.toInt()) or
                    (channels[(pixel shr 16) and 255] shl 16) or
                    (channels[(pixel shr 8) and 255] shl 8) or channels[pixel and 255]
            }
            bitmap.setPixels(row, 0, row.size, 0, y, row.size, 1)
        }
        return resolver.openOutputStream(destination)?.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        } == true
    } finally {
        bitmap.recycle()
    }
}
