package org.jglrxavpok.audiokode.finders

import org.jglrxavpok.audiokode.decoders.Decoders
import java.io.File
import java.io.FileInputStream

object DiskAbsoluteFinder: AudioFinder {
    override fun findAudio(identifier: String): AudioInfo {
        for((extension, decoder) in Decoders.map { Pair(it.extension, it) }) {
            val file = File("$identifier.$extension")
            if(file.exists()) {
                val input = FileInputStream(file)
                return AudioInfo(input, decoder)
            }
        }
        return AUDIO_NOT_FOUND
    }
}