import org.jglrxavpok.audiokode.SoundEngine

object TestStreamingVorbis {

    @JvmStatic fun main(args: Array<String>) {
        val engine = SoundEngine()
        engine.initWithDefaultOpenAL()
        val source = engine.backgroundMusic("TestVorbis", false)
        source.play()
        while(source.isPlaying()) {
            engine.update()
            Thread.sleep(1)
        }
        println("End!")
    }
}