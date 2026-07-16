package com.followupnadlan.profile

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/**
 * Writes the vCard to a private cache file and returns a shareable `content://` [Uri] via
 * [FileProvider]. Each write uses a fresh UUID name so a double-tap produces two distinct files
 * rather than overwriting one WhatsApp may still be reading. Cleanup is age-based (not a folder
 * wipe): only files older than [MAX_AGE_MS] or empty/corrupt ones are removed, so a fresh card
 * held open in a composer is never deleted out from under it.
 */
class ContactCardFileWriter(private val context: Context) {

    /** @throws java.io.IOException if the directory or file cannot be created/written. */
    fun write(vCard: String): Uri {
        val dir = File(context.cacheDir, DIR_NAME).apply { mkdirs() }
        cleanupOldFiles(dir)
        val file = File(dir, "contact-${UUID.randomUUID()}.vcf")
        file.writeText(vCard, Charsets.UTF_8)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun cleanupOldFiles(dir: File) {
        val now = System.currentTimeMillis()
        dir.listFiles()?.forEach { file ->
            if (!file.isFile) return@forEach
            // Cleanup runs before this write creates its own file, and all writes are on the
            // main thread, so removing empty leftovers here can't race a card being shared now.
            val stale = now - file.lastModified() > MAX_AGE_MS
            val corrupt = file.length() == 0L
            if (stale || corrupt) file.delete()
        }
    }

    private companion object {
        const val DIR_NAME = "contact_cards"
        const val MAX_AGE_MS = 24L * 60 * 60 * 1000
    }
}
