package org.jglrxavpok.audiokode.decoders

import java.nio.ByteBuffer
import org.jglrxavpok.audiokode.Buffer
import org.jglrxavpok.audiokode.SoundEngine
import org.lwjgl.openal.AL10
import javax.sound.sampled.AudioSystem
import java.io.ByteArrayInputStream
import java.nio.ByteOrder


object WaveDecoder: AudioDecoder {
    override val extension: String = "wav"
    // FIXME FROM LWJGL2 WaveData

    override fun decode(raw: ByteArray, engine: SoundEngine): Buffer {
        val input = ByteArrayInputStream(raw)
        val ais = AudioSystem.getAudioInputStream(input)
        //get format of data
        val audioformat = ais.getFormat()

        // get channels
        var channels = 0
        if (audioformat.getChannels() == 1) {
            if (audioformat.getSampleSizeInBits() == 8) {
                channels = AL10.AL_FORMAT_MONO8
            } else if (audioformat.getSampleSizeInBits() == 16) {
                channels = AL10.AL_FORMAT_MONO16
            } else {
                assert(false) { "Illegal sample size" }
            }
        } else if (audioformat.getChannels() == 2) {
            if (audioformat.getSampleSizeInBits() == 8) {
                channels = AL10.AL_FORMAT_STEREO8
            } else if (audioformat.getSampleSizeInBits() == 16) {
                channels = AL10.AL_FORMAT_STEREO16
            } else {
                assert(false) { "Illegal sample size" }
            }
        } else {
            assert(false) { "Only mono or stereo is supported" }
        }

        //read data into buffer
        var buffer: ByteBuffer? = null
        var available = ais.available()
        if (available <= 0) {
            available = ais.getFormat().getChannels() * ais.getFrameLength() as Int * ais.getFormat().getSampleSizeInBits() / 8
        }
        val buf = ByteArray(ais.available())
        var read = 0
        var total = 0
        do {
            read = ais.read(buf, total, buf.size - total)
            if(read != -1 && total < buf.size)
                total += read
        } while(read != -1 && total < buf.size)
        buffer = convertAudioBytes(buf, audioformat.getSampleSizeInBits() == 16, if (audioformat.isBigEndian()) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN)


        val result = engine.newBuffer()
        result.format = channels
        result.frequency = audioformat.sampleRate.toInt()
        engine.upload(result, buffer)
        return result
    }

    private fun convertAudioBytes(audio_bytes: ByteArray, two_bytes_data: Boolean, order: ByteOrder): ByteBuffer {
        val dest = ByteBuffer.allocateDirect(audio_bytes.size)
        dest.order(ByteOrder.nativeOrder())
        val src = ByteBuffer.wrap(audio_bytes)
        src.order(order)
        if (two_bytes_data) {
            val dest_short = dest.asShortBuffer()
            val src_short = src.asShortBuffer()
            while (src_short.hasRemaining())
                dest_short.put(src_short.get())
        } else {
            while (src.hasRemaining())
                dest.put(src.get())
        }
        dest.rewind()
        return dest
    }


}
