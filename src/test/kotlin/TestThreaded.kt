import org.jglrxavpok.audiokode.SoundEngine
import org.jglrxavpok.audiokode.ThreadedSoundEngine

object TestThreaded {
    @JvmStatic fun main(args: Array<String>) {
        val engine = ThreadedSoundEngine()
        engine.initWithDefaultOpenAL()
        engine.quickplayBackgroundMusic("TestWav")
    }
}