import org.jglrxavpok.audiokode.SoundEngine
import org.lwjgl.openal.AL10
import kotlin.system.measureNanoTime

object TestStreamingVorbis {

    @JvmStatic fun main(args: Array<String>) {
        val engine = SoundEngine()
        engine.initWithDefaultOpenAL()
        val source = engine.backgroundMusic("TestVorbis", false)
        source.play()
        source.gain = 0.25f
        while(source.isPlaying()) {
            engine.update()
        }
        println("End!")
    }
}