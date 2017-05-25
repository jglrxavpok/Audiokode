package org.jglrxavpok.audiokode.decoders

import org.jglrxavpok.audiokode.StreamingBufferSize
import org.jglrxavpok.audiokode.StreamingInfos
import org.lwjgl.openal.AL10
import org.lwjgl.openal.AL10.alBufferData
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioSystem

object StreamingWaveDecoder: StreamingDecoder {
    override fun prepare(input: InputStream): StreamingInfos {
        val ais = AudioSystem.getAudioInputStream(input)
        //get format of data
        val audioFormat = ais.format

        // get channels
        val channels: Int
        if (audioFormat.channels == 1) {
            if (audioFormat.sampleSizeInBits == 8) {
                channels = AL10.AL_FORMAT_MONO8
            } else if (audioFormat.sampleSizeInBits == 16) {
                channels = AL10.AL_FORMAT_MONO16
            } else {
                throw IOException("Illegal sample size ${audioFormat.sampleSizeInBits}")
            }
        } else if (audioFormat.channels == 2) {
            if (audioFormat.sampleSizeInBits == 8) {
                channels = AL10.AL_FORMAT_STEREO8
            } else if (audioFormat.sampleSizeInBits == 16) {
                channels = AL10.AL_FORMAT_STEREO16
            } else {
                throw IOException("Illegal sample size ${audioFormat.sampleSizeInBits}")
            }
        } else {
            throw IOException("Only mono or stereo is supported, found ${audioFormat.channels} channels")
        }

        return StreamingInfos(this, channels, audioFormat.sampleRate.toInt(), audioFormat.sampleSizeInBits, audioFormat.isBigEndian, ais.frameLength, audioFormat.channels, input)
    }

    override fun loadNextChunk(bufferID: Int, infos: StreamingInfos): Boolean {
        val buffer: ByteBuffer
            //available = infos.channels * infos.frameLength.toInt() * infos.sampleSizeInBits / 8
        val buf = ByteArray(StreamingBufferSize)
        var read: Int
        var total = 0
        var eof = false
        do {
            read = infos.input.read(buf, total, buf.size - total)
            if(read != -1 && total < buf.size)
                total += read

            if(read == -1) {
                eof = true
            }
        } while(read != -1 && total < buf.size)
        buffer = DirectWaveDecoder.convertAudioBytes(buf, infos.sampleSizeInBits == 16, if (infos.isBigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN)

        alBufferData(bufferID, infos.format, buffer, infos.frequency)
        return eof
    }
}