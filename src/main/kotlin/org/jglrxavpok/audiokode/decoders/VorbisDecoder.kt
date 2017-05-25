package org.jglrxavpok.audiokode.decoders

import org.jglrxavpok.audiokode.Buffer
import org.jglrxavpok.audiokode.SoundEngine
import org.lwjgl.BufferUtils

import org.lwjgl.openal.AL10.AL_FORMAT_STEREO16
import org.lwjgl.openal.AL10.AL_FORMAT_MONO16
import org.lwjgl.stb.STBVorbis.stb_vorbis_decode_memory
import org.lwjgl.system.MemoryStack.stackPop
import org.lwjgl.system.MemoryStack.stackMallocInt
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.libc.LibCStdlib.free


object VorbisDecoder: AudioDecoder {
    override val extension: String = "ogg"

    override fun decode(raw: ByteArray, engine: SoundEngine): Buffer {
        stackPush()
        val channelsBuffer = stackMallocInt(1)
        stackPush()
        val sampleRateBuffer = stackMallocInt(1)

        val rawBuffer = BufferUtils.createByteBuffer(raw.size)
        rawBuffer.put(raw)
        rawBuffer.flip()

        val rawAudioBuffer = stb_vorbis_decode_memory(rawBuffer, channelsBuffer, sampleRateBuffer)

//Retreive the extra information that was stored in the buffers by the function
        val channels = channelsBuffer.get()
        val sampleRate = sampleRateBuffer.get()
//Free the space we allocated earlier
        stackPop()
        stackPop()

//Find the correct OpenAL format
        var format = -1
        if (channels == 1) {
            format = AL_FORMAT_MONO16
        } else if (channels == 2) {
            format = AL_FORMAT_STEREO16
        }

        val result = engine.newBuffer()
        result.frequency = sampleRate
        result.format = format
        engine.upload(result, rawAudioBuffer)

        free(rawAudioBuffer)
        return result
    }
}