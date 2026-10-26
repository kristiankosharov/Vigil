package app.vigil.feature.documents.data.local

import java.io.File
import java.io.FileNotFoundException

/**
 * The encrypted pages on disk: `<root>/<document id>/<page>.bin`. [root] is in `noBackupFilesDir`:
 * the pages can't be decrypted on another device anyway (the RSA key stays in this Keystore).
 */
internal class DocumentFiles(private val root: File) {

    /** Writes to a temporary file first, so a crash never leaves half a page. */
    fun write(id: String, index: Int, bytes: ByteArray) {
        val file = page(id, index)
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeBytes(bytes)
        if (!temp.renameTo(file)) throw java.io.IOException("Cannot save page $index of $id")
    }

    /** Throws [FileNotFoundException] for an unknown document or page. */
    fun read(id: String, index: Int): ByteArray = page(id, index).readBytes()

    fun delete(id: String) {
        File(root, id).deleteRecursively()
    }

    fun deleteAll() {
        root.deleteRecursively()
    }

    private fun page(id: String, index: Int) = File(File(root, id), "$index.bin")
}
