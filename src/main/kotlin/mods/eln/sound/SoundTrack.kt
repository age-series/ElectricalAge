package mods.eln.sound

class SoundTrack {
    var track: String? = null
    var trackLength: Double = 0.0
    var volume: Float = 1f
    var pitch: Float = 1f
    var rangeNominal: Float = 0f
    var rangeMax: Float = 0f
    var blockFactor: Float = 0f
    var uuid: ArrayList<Int> = ArrayList()

    enum class Range {
        Small, Mid, Far
    }

    constructor()

    constructor(track: String?) {
        this.track = track
        mediumRange()
    }

    constructor(track: String?, trackLength: Double) {
        this.track = track
        this.trackLength = trackLength
        mediumRange()
    }

    fun copy(): SoundTrack {
        val c = SoundTrack()
        c.track = track
        c.trackLength = trackLength
        c.volume = volume
        c.pitch = pitch
        c.rangeNominal = rangeNominal
        c.rangeMax = rangeMax
        c.blockFactor = blockFactor
        c.uuid = uuid.clone() as ArrayList<Int>
        return c
    }

    fun applyRange(range: Range?) {
        when (range) {
            Range.Small -> smallRange()
            Range.Far -> longRange()
            Range.Mid -> mediumRange()
            else -> mediumRange()
        }
    }

    fun mediumRange(): SoundTrack {
        rangeNominal = 4f
        rangeMax = 16f
        blockFactor = 1f
        return this
    }

    fun smallRange(): SoundTrack {
        rangeNominal = 2f
        rangeMax = 8f
        blockFactor = 3f
        return this
    }

    fun longRange(): SoundTrack {
        rangeNominal = 8f
        rangeMax = 48f
        blockFactor = 0.5f
        return this
    }

    fun setVolume(volume: Float, pitch: Float): SoundTrack {
        this.volume = volume
        this.pitch = pitch
        return this
    }

    fun addUuid(uuid: Int): SoundTrack {
        this.uuid.add(uuid)
        return this
    }
}
