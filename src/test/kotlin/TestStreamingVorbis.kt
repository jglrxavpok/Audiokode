import org.jglrxavpok.audiokode.SoundEngine
import org.lwjgl.openal.AL10

object TestStreamingVorbis {

    @JvmStatic fun main(args: Array<String>) {
        val engine = SoundEngine()
        engine.initWithDefaultOpenAL()
        val source = engine.backgroundMusic("TestVorbis2", false)
        source.play()
        while(source.isPlaying()) {
            engine.update()
            Thread.sleep(10)
        }
        println("End!")
    }
}