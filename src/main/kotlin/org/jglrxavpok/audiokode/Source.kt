package org.jglrxavpok.audiokode

import org.lwjgl.openal.AL10

open class Source(protected val engine: SoundEngine): Disposable {
    var position: Vector3D = NullVector
        set(value) {
            field = value
            engine.sourcePosition(this, value)
        }
    var velocity: Vector3D = NullVector
        set(value) {
            field = value
            engine.sourceVelocity(this, value)
        }
    var gain: Float = 1f
        set(value) {
            field = value
            engine.sourceGain(this, value)
        }
    var pitch: Float = 1f
        set(value) {
            field = value
            engine.sourcePitch(this, value)
        }
    var looping = false
        set(value) {
            field = value
            engine.sourceLooping(this, value)
        }
    var identifier: String = ""
        internal set
    internal var alID: Int = -1

    fun bindBuffer(buffer: Buffer) {
        engine.bindSourceBuffer(this, buffer)
    }

    override fun dispose() {
        engine.disposeSource(this)
    }

    fun isPlaying(): Boolean {
        return AL10.alGetSourcei(alID, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING
    }

    fun play() {
        AL10.alSourcePlay(alID)
    }

    fun pause() {
        AL10.alSourcePause(alID)
    }

    fun resume() {
        play()
    }

    fun stop() {
        AL10.alSourceStop(alID)
    }

}