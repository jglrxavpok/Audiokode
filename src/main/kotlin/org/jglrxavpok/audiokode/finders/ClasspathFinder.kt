package org.jglrxavpok.audiokode.finders

import org.jglrxavpok.audiokode.decoders.Decoders

object ClasspathFinder: AudioFinder {
    override fun findAudio(identifier: String): AudioInfo {
        for((extension, decoder, streamingDecoder) in Decoders.map { Triple(it.extension, it, it.streamingVariant) }) {
            val input = javaClass.getResourceAsStream("/$identifier.$extension")
            if (input != null) {
                return AudioInfo(input.buffered(), decoder, streamingDecoder)
            }
        }
        return AUDIO_NOT_FOUND
    }
}