package org.jglrxavpok.audiokode

import org.jglrxavpok.audiokode.decoders.StreamingDecoder
import org.jglrxavpok.audiokode.decoders.StreamingWaveDecoder
import org.jglrxavpok.audiokode.finders.AUDIO_NOT_FOUND
import org.jglrxavpok.audiokode.finders.AudioInfo
import org.lwjgl.openal.AL10.*
import org.lwjgl.system.MemoryStack
import java.io.InputStream

class StreamingSource(engine: SoundEngine): Source(engine) {

    var infos: StreamingInfos? = null
    private var eof = false

    fun updateStream() {
        if(eof)
            return
        val processed = alGetSourcei(alID, AL_BUFFERS_PROCESSED)
        MemoryStack.stackPush()
        if(processed > 0) {
            val id = alSourceUnqueueBuffers(alID)
            if( ! loadNext(id)) {
                eof = true
                println("eof!!!")
                return
            }
            alSourceQueueBuffers(alID, id)
        }
        MemoryStack.stackPop()
    }

    private fun loadNext(bufferID: Int): Boolean {
        val eof = infos?.decoder?.loadNextChunk(bufferID, infos!!) ?: true
        // TODO: handle looping
        return ! eof
    }

    fun prepareRotatingBuffers() {
        alSourcei(alID, AL_BUFFER, 0)
        for (i in 1..4) {
            val buffer = alGenBuffers()
            infos!!.decoder.loadNextChunk(buffer, infos!!)
            alSourceQueueBuffers(alID, buffer)
        }
    }
}

// TODO: Cleanup or simplify
data class StreamingInfos(val decoder: StreamingDecoder, val format: Int, val frequency: Int, val channels: Int, val input: InputStream) {
    val payload = hashMapOf<String, Any>()
}