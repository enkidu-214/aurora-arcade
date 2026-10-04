package com.aurora.arcade.core

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption.*
import java.util.concurrent.Executors

/** All instances share one ordered I/O queue, including reads after Activity recreation. */
class GameFileStore(private val file: File) {
    fun save(save: EngineSave) {
        val bytes=SaveCodec.encode(save)
        writer.execute {
            runCatching {
                file.parentFile?.mkdirs()
                val temp=File(file.parentFile,"${file.name}.pending")
                FileOutputStream(temp).use { it.write(bytes); it.fd.sync() }
                Files.move(temp.toPath(),file.toPath(),ATOMIC_MOVE,REPLACE_EXISTING)
            }
        }
    }
    fun load(): EngineSave? = writer.submit<EngineSave?> {
        runCatching { SaveCodec.decode(file.readBytes()) }.getOrNull()
    }.get()
    companion object {
        private val writer=Executors.newSingleThreadExecutor { task -> Thread(task,"aurora-save").apply { isDaemon=true } }
    }
}
