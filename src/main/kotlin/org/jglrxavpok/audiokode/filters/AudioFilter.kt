package org.jglrxavpok.audiokode.filters

import java.nio.ShortBuffer

interface AudioFilter {
    operator fun invoke(pcmData: ShortBuffer): ShortBuffer

    operator fun times(other: AudioFilter): AudioFilter {
        return CompositeFilter(this, other)
    }
}

class CompositeFilter(val first: AudioFilter, val second: AudioFilter): AudioFilter {
    override fun invoke(pcmData: ShortBuffer): ShortBuffer = first(second(pcmData))
}

val NoFilter = object : AudioFilter {
    override fun invoke(pcmData: ShortBuffer) = pcmData
}