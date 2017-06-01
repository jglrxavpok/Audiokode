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

    override fun update() {
        updateStream()
        super.update()
    }

    fun updateStream() {
        val processed = alGetSourcei(alID, AL_BUFFERS_PROCESSED)
        if (processed > 0) {
            val buffers = IntArray(processed)
            alSourceUnqueueBuffers(alID, buffers)
            if (!eof) {
                for (id in buffers) {
                    if (!loadNext(id)) {
                        eof = true
                        break
                    }
                }
                alSourceQueueBuffers(alID, buffers)
            }
        }
    }

    private fun loadNext(bufferID: Int): Boolean {
        val eof = infos?.decoder?.loadNextChunk(bufferID, infos!!, engine) ?: true
        // TODO: handle looping
        return ! eof
    }

    fun prepareRotatingBuffers() {
        alSourcei(alID, AL_BUFFER, 0)
        val buffers = IntArray(8)
        alGenBuffers(buffers)
        for(id in buffers)
            loadNext(id)
        alSourceQueueBuffers(alID, buffers)
    }
}

// TODO: Cleanup or simplify
data class StreamingInfos(val decoder: StreamingDecoder, val format: Int, val frequency: Int, val channels: Int, val input: InputStream, val filter: AudioFilter) {
    val payload = hashMapOf<String, Any>()
}