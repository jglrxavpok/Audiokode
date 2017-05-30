package org.jglrxavpok.audiokode.decoders

import org.jglrxavpok.audiokode.SoundEngine
import org.jglrxavpok.audiokode.StreamingBufferSize
import org.jglrxavpok.audiokode.StreamingInfos
import org.jglrxavpok.audiokode.filters.AudioFilter
import org.lwjgl.openal.AL10
import org.lwjgl.stb.STBVorbis.*
import org.lwjgl.stb.STBVorbisInfo
import org.lwjgl.system.MemoryStack.*
import java.io.InputStream
import java.nio.ByteBuffer
import org.lwjgl.BufferUtils

/**
 * Still reads all the file contents to the memory. OpenAL has only small buffers streamed to it though.
 */
object StreamingVorbisDecoderPushdata: StreamingDecoder {

    private val STBDecoderKey = "stb decoder"
    private val AudioStreamKey = "audio stream"

    override fun prepare(input: InputStream, filter: AudioFilter): StreamingInfos {
        val stream = AudioStream(input)
        stream.refill()
        val header = stream.extractContent()
        stackPush()
        val data = ByteBuffer.allocateDirect(header.size)
        data.put(header)
        data.flip()

        val error = stackMallocInt(1)
        val consumed = stackMallocInt(1)

        val decoderInstance = stb_vorbis_open_pushdata(data, consumed, error, null)
        stream.consume(consumed[0])
        println("error is ${error[0]}, consumed is ${consumed[0]}")

        val infos = stb_vorbis_get_info(decoderInstance, STBVorbisInfo.malloc())
        val format = when(infos.channels()) {
            1 -> AL10.AL_FORMAT_MONO16
            2 -> AL10.AL_FORMAT_STEREO16
            else -> kotlin.error("Unknown channel count ${infos.channels()}")
        }
        val result = StreamingInfos(this, format, infos.sample_rate(), infos.channels(), input.buffered(), filter)
        result.payload[STBDecoderKey] = decoderInstance
        result.payload[AudioStreamKey] = stream
        stackPop()

        return result
    }

    override fun loadNextChunk(bufferID: Int, infos: StreamingInfos, engine: SoundEngine): Boolean {
        val stream = infos.payload[AudioStreamKey] as AudioStream
        val decoder = infos.payload[STBDecoderKey] as Long

        stackPush()
        val channelsOut = stackMallocInt(1)
        val samplesOut = stackMallocInt(1)
        val samplesPointerPointer = stackMallocPointer(1)

        var eof: Boolean
        do {
            eof = stream.refill()
            val data = stream.extractContent()
            val buffer = BufferUtils.createByteBuffer(data.size)
            buffer.put(data)
            buffer.flip()

            val consumed = stb_vorbis_decode_frame_pushdata(decoder, buffer, channelsOut, samplesPointerPointer, samplesOut)
            stream.consume(consumed)
            val samples = samplesOut.get(0)

            channelsOut.rewind()
            samplesOut.rewind()
        } while(samples == 0)

        val channels = channelsOut.get(0)
        val samples = samplesOut.get(0)

        val output_pp = samplesPointerPointer.getPointerBuffer(channels) // float**
        val finalOutput = stackMallocShort(samples*channels)
        for (c in 0..channels - 1) {
            val channel = output_pp.getFloatBuffer(c, samples) // float*
            for (s in 0..samples - 1) {
                val sample = channel.get(s)
                val shortSamples = (sample*Short.MAX_VALUE).toShort()
                finalOutput.put(c+s*channels, shortSamples)
            }
        }

        finalOutput.rewind()

        engine.bufferData(bufferID, infos.format, finalOutput, infos.frequency)
        stackPop()

        return eof
    }

}

private class AudioStream(val input: InputStream) {
    private val buffer = ByteArray(StreamingBufferSize)
    var length = 0
        private set

    fun extractContent(): ByteArray {
        val extracted = ByteArray(length)
        /*for (i in 0 until length) {
            extracted[i] = buffer[i]
        }*/
        System.arraycopy(buffer, 0, extracted, 0, length)
        return extracted
    }

    fun consume(n: Int) {
        //for (i in 0 until length)
        //    buffer[i] = if(i+n < buffer.size) buffer[i+n] else 0
        System.arraycopy(buffer, n, buffer, 0, if(n+length > buffer.size) buffer.size-n else length) // shift

        length -= n
        if(length < 0)
            length = 0
    }

    fun refill(): Boolean {
        val tail = ByteArray(buffer.size-length)
        val eof = readChunk(input, tail)
        System.arraycopy(tail, 0, buffer, length, tail.size)

        length += tail.size

        return eof
    }

    private fun readChunk(input: InputStream, chunk: ByteArray): Boolean {
        var eof = false
        var total = 0
        do {
            val read = input.read(chunk, total, chunk.size-total)
            if(read != -1) {
                total += read
            }
            if(read == -1)
                eof = true
        } while(read != -1 && total < chunk.size)
        return eof
    }
}