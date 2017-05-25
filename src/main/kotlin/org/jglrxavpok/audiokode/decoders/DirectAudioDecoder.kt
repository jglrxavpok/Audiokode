package org.jglrxavpok.audiokode.decoders

import org.jglrxavpok.audiokode.Buffer
import org.jglrxavpok.audiokode.SoundEngine

interface AudioDecoder {

    val streamingVariant: StreamingDecoder
    val extension: String
    /**
     * Decodes a raw byte stream (from a sound file, for instance) and converts it to a format that is usable by OpenAL
     */
    fun decode(raw: ByteArray, engine: SoundEngine): Buffer
}

val Decoders = mutableListOf<AudioDecoder>()