package org.jglrxavpok.audiokode.decoders

import org.jglrxavpok.audiokode.SoundEngine
import org.jglrxavpok.audiokode.StreamingInfos
import org.jglrxavpok.audiokode.filters.AudioFilter
import java.io.InputStream

interface StreamingDecoder {

    fun prepare(input: InputStream, filter: AudioFilter): StreamingInfos
    fun loadNextChunk(bufferID: Int, infos: StreamingInfos, engine: SoundEngine): Boolean
}