package org.jglrxavpok.audiokode

import org.jglrxavpok.audiokode.decoders.Decoders
import org.jglrxavpok.audiokode.decoders.DirectVorbisDecoder
import org.jglrxavpok.audiokode.decoders.DirectWaveDecoder
import org.jglrxavpok.audiokode.finders.*
import org.lwjgl.openal.AL
import org.lwjgl.openal.AL10.*
import org.lwjgl.openal.ALC
import org.lwjgl.openal.ALC10.*
import org.lwjgl.openal.ALC10.ALC_DEFAULT_DEVICE_SPECIFIER
import org.lwjgl.openal.ALC10.alcGetString
import org.lwjgl.openal.ALC10.alcMakeContextCurrent
import java.nio.ByteBuffer
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ShortBuffer

open class SoundEngine: Disposable {

    val listener = Listener()
    private val finders = mutableListOf<AudioFinder>()
    private val sourcePool = Pool { createNewSource() }
    private val streamingSourcePool = Pool { createNewStreamingSource() }
    private val bufferPool = Pool { createNewBuffer() }
    private val autoDispose = mutableListOf<Source>()
    private val streamingSources = mutableListOf<StreamingSource>()
    private val createdBuffers = hashSetOf<Buffer>()
    private val createdSources = hashSetOf<Source>()

    fun initWithDefaultOpenAL() {
        initWithOpenAL(alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER))
    }

    fun initWithOpenAL(deviceName: String) {
        val device = alcOpenDevice(deviceName)
        val attributes = intArrayOf(0)
        val context = alcCreateContext(device, attributes)
        alcMakeContextCurrent(context)
        val alcCapabilities = ALC.createCapabilities(device)
        AL.createCapabilities(alcCapabilities)

        checkErrors("post AL init")

        init()
    }

    open fun init() {
        if( ! Decoders.contains(DirectWaveDecoder))
            Decoders.add(DirectWaveDecoder)
        if( ! Decoders.contains(DirectVorbisDecoder))
            Decoders.add(DirectVorbisDecoder)

        addFinder(ClasspathFinder)
        addFinder(DiskRelativeFinder)
        addFinder(DiskAbsoluteFinder)
    }

    fun addFinder(finder: AudioFinder) {
        finders += finder
    }

    /**
     * Prepares a source ready to play a background sound
     */
    fun backgroundSound(identifier: String, looping: Boolean): Source {
        val source = prepareDirectSource(identifier, looping)
        alSourcei(source.alID, AL_SOURCE_RELATIVE, AL_TRUE) // the source will play exactly where the listener is
        return source
    }

    private fun prepareDirectSource(identifier: String, looping: Boolean): Source {
        val source = newSource()
        source.identifier = identifier
        source.looping = looping
        checkErrors("post generation")

        val buffer = decodeDirect(identifier)
        checkErrors("post decode")

        source.bindBuffer(buffer)
        checkErrors("post bind")
        return source
    }

    fun sound(identifier: String, looping: Boolean): Source {
        val source = prepareDirectSource(identifier, looping)
        alSourcei(source.alID, AL_SOURCE_RELATIVE, AL_FALSE)
        return source
    }

    fun backgroundMusic(identifier: String, looping: Boolean): Source {
        val source = prepareStreamingSource(identifier, looping)
        alSourcei(source.alID, AL_SOURCE_RELATIVE, AL_TRUE) // the source will play exactly where the listener is
        return source
    }

    fun music(identifier: String, looping: Boolean): Source {
        val source = prepareStreamingSource(identifier, looping)
        alSourcei(source.alID, AL_SOURCE_RELATIVE, AL_FALSE)
        return source
    }

    private fun prepareStreamingSource(identifier: String, looping: Boolean): Source {
        val infos = prepareStreaming(identifier)

        val source = newStreamingSource()
        source.infos = infos

        source.identifier = identifier
        // TODO source.looping = looping

        source.prepareRotatingBuffers()
        streamingSources += source

        return source
    }

    /**
     * Plays a background sound immediately and set its resources up to be disposed after being played
     */
    fun quickplayBackgroundSound(identifier: String) {
        val source = backgroundSound(identifier, false)
        autoDispose += source
        source.play()
    }

    fun quickplayMusic(identifier: String) {
        val source = music(identifier, false)
        autoDispose += source
        source.play()
    }

    fun quickplaySound(identifier: String) {
        val source = sound(identifier, false)
        autoDispose += source
        source.play()
    }

    fun quickplayBackgroundMusic(identifier: String) {
        val source = backgroundMusic(identifier, false)
        autoDispose += source
        source.play()
    }

    private fun prepareStreaming(identifier: String): StreamingInfos {
        finders.reversed()
                .map { it.findAudio(identifier) }
                .filter { it != AUDIO_NOT_FOUND }
                .forEach { return it.streamDecoder.prepare(it.input) } // remember: this 'return' returns from decodeDirect!
        throw IOException("Could not find audio file with identifier $identifier")
    }

    private fun decodeDirect(identifier: String): Buffer {
        finders.reversed()
                .map { it.findAudio(identifier) }
                .filter { it != AUDIO_NOT_FOUND }
                .forEach { return it.decoder.decode(readData(it.input), this) } // remember: this 'return' returns from decodeDirect!
        throw IOException("Could not find audio file with identifier $identifier")
    }

    private fun readData(input: InputStream): ByteArray {

        val buffer = ByteArrayOutputStream()
        var nRead: Int
        val data = ByteArray(1024)
        do {
            nRead = input.read(data, 0, data.size)
            if(nRead != -1)
                buffer.write(data, 0, nRead)
        } while (nRead != -1)

        buffer.flush()
        val byteArray = buffer.toByteArray()
        return byteArray
    }

    private fun createNewSource(): Source {
        val source = Source(this)
        val id = alGenSources()
        source.alID = id
        source.gain = 1f
        source.position = NullVector
        source.velocity = NullVector
        source.pitch = 1f

        createdSources += source
        return source
    }

    private fun createNewStreamingSource(): StreamingSource {
        val source = StreamingSource(this)
        val id = alGenSources()
        source.alID = id
        source.gain = 1f
        source.position = NullVector
        source.velocity = NullVector
        source.pitch = 1f

        createdSources += source
        return source
    }

    fun update() {
        updateListener()

        autoDispose.filterNot { it.isPlaying() }.forEach(Source::dispose)
        autoDispose.clear()
        streamingSources.forEach(StreamingSource::updateStream)
    }

    fun updateListener() {
        alListener3f(AL_POSITION, listener.position.x, listener.position.y, listener.position.z)
        alListener3f(AL_VELOCITY, listener.velocity.x, listener.velocity.y, listener.velocity.z)
        alListenerfv(AL_POSITION, floatArrayOf(
                listener.lookDirection.x, listener.lookDirection.y, listener.lookDirection.z,
                listener.up.x, listener.up.y, listener.up.z
        ))
    }

    internal fun sourcePosition(source: Source, value: Vector3D) {
        alSource3f(source.alID, AL_POSITION, value.x, value.y, value.z)
    }

    internal fun sourceVelocity(source: Source, value: Vector3D) {
        alSource3f(source.alID, AL_VELOCITY, value.x, value.y, value.z)
    }

    internal fun sourceGain(source: Source, value: Float) {
        alSourcef(source.alID, AL_GAIN, value)
    }

    internal fun sourcePitch(source: Source, value: Float) {
        alSourcef(source.alID, AL_PITCH, value)
    }

    internal fun sourceLooping(source: Source, value: Boolean) {
        alSourcei(source.alID, AL_LOOPING, if(value) AL_TRUE else AL_FALSE)
    }

    internal fun bindSourceBuffer(source: Source, buffer: Buffer) {
        alSourcei(source.alID, AL_BUFFER, buffer.alID)
    }

    internal fun upload(buffer: Buffer, raw: ByteArray) {
        val dataBuffer = ByteBuffer.allocateDirect(raw.size)
        dataBuffer.put(raw)
        dataBuffer.flip()
        upload(buffer, dataBuffer)
    }

    internal fun upload(buffer: Buffer, raw: ShortBuffer) {
        val format = buffer.format
        val frequency = buffer.frequency
        alBufferData(buffer.alID, format, raw, frequency)
    }

    internal fun upload(buffer: Buffer, raw: ByteBuffer) {
        val format = buffer.format
        val frequency = buffer.frequency
        alBufferData(buffer.alID, format, raw, frequency)
    }

    fun createNewBuffer(): Buffer {
        val buffer = Buffer(alGenBuffers(), this)
        createdBuffers += buffer
        return buffer
    }

    fun newStreamingSource(): StreamingSource {
        return streamingSourcePool.get()
    }

    fun newSource(): Source {
        return sourcePool.get()
    }

    fun newBuffer(): Buffer {
        return bufferPool.get()
    }

    internal fun disposeSource(source: Source) {
        sourcePool.add(source)
        alSourcei(source.alID, AL_BUFFER, 0)
    }

    internal fun disposeBuffer(buffer: Buffer) {
        bufferPool.add(buffer)
    }

    override fun dispose() {
        alDeleteSources(createdSources.map { it.alID }.toIntArray())
        alDeleteBuffers(createdBuffers.map { it.alID }.toIntArray())
    }

    fun isSomethingPlaying(): Boolean {
        return createdSources.any { it.isPlaying() }
    }
}
