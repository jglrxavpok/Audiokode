package org.jglrxavpok.audiokode.decoders

import org.jglrxavpok.audiokode.Buffer
import org.jglrxavpok.audiokode.StreamingInfos
import java.io.InputStream

interface StreamingDecoder {

    fun prepare(input: InputStream): StreamingInfos
    fun loadNextChunk(bufferID: Int, infos: StreamingInfos): Boolean
}