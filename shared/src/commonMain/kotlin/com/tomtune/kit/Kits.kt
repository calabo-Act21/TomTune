package com.tomtune.kit

/** Canonical identifiers and values are independent of the display language. */
enum class DrumFamily(val key: String, val diameters: List<Int>) {
    RACK("rack", listOf(8, 10, 12, 13, 14)),
    FLOOR("floor", listOf(14, 15, 16, 18)),
    SNARE("snare", listOf(12, 13, 14, 15)),
    KICK("kick", listOf(18, 20, 22, 24, 26));
    val isTom get() = this == RACK || this == FLOOR
}

data class Drum(val id: String, val family: DrumFamily, val order: Int, val diameter: Int,
                val lugs: Int? = null, val manualLugs: Boolean = false)
data class Kit(val id: String, val name: String, val drums: List<Drum>, val revision: Int = 0) {
    fun ordered(family: DrumFamily) = drums.filter { it.family == family }
        .sortedWith(compareBy<Drum> { it.diameter }.thenBy { it.order })
    fun visualDrums() = DrumFamily.entries.flatMap { ordered(it) }
    fun visualKey() = "r${ordered(DrumFamily.RACK).size}-f${ordered(DrumFamily.FLOOR).size}"
    fun atZone(key: String): Drum? = when {
        key == "snare" -> ordered(DrumFamily.SNARE).singleOrNull()
        key == "kick" -> ordered(DrumFamily.KICK).singleOrNull()
        key.startsWith("rack") -> ordered(DrumFamily.RACK).getOrNull((key.removePrefix("rack").toIntOrNull() ?: 0) - 1)
        key.startsWith("floor") -> ordered(DrumFamily.FLOOR).getOrNull((key.removePrefix("floor").toIntOrNull() ?: 0) - 1)
        else -> null
    }
}
data class KitStore(val schema: Int = 1, val kits: List<Kit> = emptyList(), val activeId: String? = null) {
    val active get() = kits.find { it.id == activeId }
    fun validated(): KitStore = apply {
        require(schema == 1)
        require(kits.map { it.id }.distinct().size == kits.size)
        require(kits.map { it.name.lowercase() }.distinct().size == kits.size)
        require(if (kits.isEmpty()) activeId == null else active != null)
        kits.forEach { kit ->
            require(kit.id.isNotBlank() && kit.name == kit.name.trim() && kit.name.length in 1..60 && kit.revision >= 0)
            require(kit.drums.map { it.id }.distinct().size == kit.drums.size)
            require(kit.drums.all { it.id.isNotBlank() && it.order >= 0 && it.diameter in it.family.diameters &&
                (if (it.family.isTom) it.lugs in 3..12 else it.lugs == null) })
            DrumFamily.entries.forEach { family ->
                val drums = kit.drums.filter { it.family == family }
                require(drums.map { it.order }.distinct().size == drums.size)
                require(when (family) {
                    DrumFamily.RACK -> drums.size in 0..3
                    DrumFamily.FLOOR -> drums.size in 0..2
                    else -> drums.size == 1
                })
            }
        }
    }
}

interface KitRepository {
    suspend fun load(): KitStore
    /** Implementations commit the complete snapshot atomically, or throw without replacing it. */
    suspend fun write(store: KitStore)
}

data class FocusVisual(val family: DrumFamily, val diameter: Int, val lugs: Int, val path: String)
data class NormalRect(val left: Float, val top: Float, val right: Float, val bottom: Float)
data class ImageRect(val left: Float, val top: Float, val width: Float, val height: Float) {
    fun zone(rect: NormalRect) = ImageRect(left + width * rect.left, top + height * rect.top,
        width * (rect.right - rect.left), height * (rect.bottom - rect.top))
}
/** Same centered Fit transform for the bitmap and all its hit rectangles. */
fun fitImage(containerWidth: Float, containerHeight: Float, imageWidth: Float, imageHeight: Float): ImageRect {
    val scale = minOf(containerWidth / imageWidth, containerHeight / imageHeight)
    val width = imageWidth * scale
    val height = imageHeight * scale
    return ImageRect((containerWidth - width) / 2, (containerHeight - height) / 2, width, height)
}
class VisualCatalogue(val focus: List<FocusVisual>, val zones: Map<String, Map<String, NormalRect>>) {
    fun focusFor(drum: Drum): FocusVisual? = focus.firstOrNull {
        it.family == drum.family && it.diameter == drum.diameter &&
            it.lugs == (if (drum.family == DrumFamily.SNARE) 8 else drum.lugs)
    }
    fun lugChoices(family: DrumFamily, diameter: Int) = focus.filter { it.family == family && it.diameter == diameter }
        .map { it.lugs }.distinct().sorted()
}

class KitRules(private val newId: () -> String, private val catalogue: VisualCatalogue) {
    fun suggested(family: DrumFamily, diameter: Int): Int {
        val base = if (diameter <= 10) 5 else if (diameter <= 14) 6 else 8
        val covered = catalogue.lugChoices(family, diameter)
        return if (covered.isEmpty() || base in covered) base else covered.first()
    }
    private fun drum(family: DrumFamily, order: Int, diameter: Int) = Drum(newId(), family, order, diameter,
        if (family.isTom) suggested(family, diameter) else null)
    fun fresh() = Kit(newId(), "", listOf(drum(DrumFamily.RACK, 0, 12), drum(DrumFamily.FLOOR, 1, 16),
        drum(DrumFamily.SNARE, 2, 14), drum(DrumFamily.KICK, 3, 22)))
    fun count(kit: Kit, family: DrumFamily, count: Int): Kit {
        require(family.isTom && count in 0..(if (family == DrumFamily.RACK) 3 else 2))
        val existing = kit.drums.filter { it.family == family }.sortedBy { it.order }
        var result = kit.drums.filterNot { it.family == family } + existing.take(count)
        val preferred = if (family == DrumFamily.RACK) (if (count == 1) listOf(12, 10, 13, 14, 8) else listOf(10, 12, 13, 14, 8))
            else listOf(16, 18, 14, 15)
        repeat((count - existing.size).coerceAtLeast(0)) {
            val used = result.filter { it.family == family }.map { it.diameter }
            val size = preferred.firstOrNull { it !in used } ?: preferred.first()
            result = result + drum(family, (result.maxOfOrNull { it.order } ?: -1) + 1, size)
        }
        return kit.copy(drums = result)
    }
    fun diameter(kit: Kit, id: String, diameter: Int) = kit.copy(drums = kit.drums.map {
        if (it.id != id) it else {
            require(diameter in it.family.diameters)
            it.copy(diameter = diameter, lugs = if (it.family.isTom && !it.manualLugs) suggested(it.family, diameter) else it.lugs)
        }
    })
    fun lugs(kit: Kit, id: String, count: Int): Kit {
        require(count in 3..12)
        return kit.copy(drums = kit.drums.map { if (it.id == id && it.family.isTom) it.copy(lugs = count, manualLugs = true) else it })
    }
}
