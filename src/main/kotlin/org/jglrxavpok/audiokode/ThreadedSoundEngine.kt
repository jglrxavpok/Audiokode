package org.jglrxavpok.audiokode

class ThreadedSoundEngine(val updatePeriod: Long = 10): SoundEngine() {

    var stopped = false

    override fun init() {
        super.init()
        val thread = Thread {
            while(!stopped) {
                this.update()
                Thread.sleep(updatePeriod)
            }
        }
        thread.isDaemon = false
        thread.start()
    }

    override fun dispose() {
        super.dispose()
        stopped = true
    }
}