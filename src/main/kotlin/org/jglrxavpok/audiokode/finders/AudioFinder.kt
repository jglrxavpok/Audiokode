package org.jglrxavpok.audiokode.finders

import org.jglrxavpok.audiokode.decoders.AudioDecoder
import org.jglrxavpok.audiokode.decoders.WaveDecoder
import java.io.ByteArrayInputStream
import java.io.InputStream

interface AudioFinder {
    fun findAudio(identifier: String): AudioInfo
}

data class AudioInfo(val input: InputStream, val decoder: AudioDecoder)

val AUDIO_NOT_FOUND = AudioInfo(ByteArrayInputStream(byteArrayOf(0)), WaveDecoder)