package org.jglrxavpok.audiokode

import org.jglrxavpok.audiokode.decoders.StreamingDecoder
import org.jglrxavpok.audiokode.filters.AudioFilter
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL10.*
import org.lwjgl.system.MemoryStack
import java.io.InputStream

class StreamingSource(engine: SoundEngine): Source(engine) {

    var infos: StreamingInfos? = null
    private var eof = false

    fun updateStream() {
        if(eof) {
            println("end of file StreamingSource") // FIXME
            return
        }
        val processed = alGetSourcei(alID, AL_BUFFERS_PROCESSED)
        if (processed > 0) {
            MemoryStack.stackPush()
            val buffers = MemoryStack.stackMallocInt(processed)
            alSourceUnqueueBuffers(alID, buffers)
            buffers.rewind()
            while(buffers.hasRemaining()) {
                if (!loadNext(buffers.get())) {
                    eof = true
                    break
                }
            }
            buffers.rewind()
            alSourceQueueBuffers(alID, buffers)
            MemoryStack.stackPop()
        }
    }

    private fun loadNext(bufferID: Int): Boolean {
        val eof = infos?.decoder?.loadNextChunk(bufferID, infos!!, engine) ?: true
        // TODO: handle looping
        return ! eof
    }

    fun prepareRotatingBuffers() {
        alSourcei(alID, AL_BUFFER, 0)
        val buffers = BufferUtils.createIntBuffer(8)
        alGenBuffers(buffers)
        buffers.rewind()
        while(buffers.hasRemaining())
            loadNext(buffers.get())
        buffers.rewind()
        alSourceQueueBuffers(alID, buffers)
    }
}

// TODO: Cleanup or simplify
data class StreamingInfos(val decoder: StreamingDecoder, val format: Int, val frequency: Int, val channels: Int, val input: InputStream, val filter: AudioFilter) {
    val payload = hashMapOf<String, Any>()
}