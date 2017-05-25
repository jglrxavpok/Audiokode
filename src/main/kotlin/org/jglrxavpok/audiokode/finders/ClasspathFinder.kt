package org.jglrxavpok.audiokode.finders

import org.jglrxavpok.audiokode.decoders.Decoders

object ClasspathFinder: AudioFinder {
    override fun findAudio(identifier: String): AudioInfo {
        for((extension, decoder) in Decoders.map { Pair(it.extension, it) }) {
            val input = javaClass.getResourceAsStream("/$identifier.$extension")
            if (input != null) {
                return AudioInfo(input, decoder)
            }
        }
        return AUDIO_NOT_FOUND
    }
}