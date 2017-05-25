import org.jglrxavpok.audiokode.SoundEngine

object TestStreaming {

    @JvmStatic fun main(args: Array<String>) {
        val engine = SoundEngine()
        engine.initWithDefaultOpenAL()
        val source = engine.backgroundSound("Test streaming", false)
        source.play()
        while(source.isPlaying()) {
            Thread.sleep(1)
        }
    }
}