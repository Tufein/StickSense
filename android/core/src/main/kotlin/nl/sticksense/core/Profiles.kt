package nl.sticksense.core

val DEVICES = listOf(
    "AYN Odin 2", "AYN Odin 2 Portal", "AYN Thor",
    "Retroid Pocket 5", "Retroid Pocket Mini", "Retroid Pocket Flip 2", "Ander toestel",
)
val EMULATORS = listOf("RetroArch", "DuckStation", "NetherSX2", "Dolphin", "PPSSPP", "melonDS", "Azahar", "Winlator", "Overig")

/** Een benoemde bundel emulatorbestanden voor één toestel en emulator. */
data class Profile(
    val id: String, val name: String, val device: String, val emulator: String,
    val notes: String, val files: List<String>,
)

fun makeProfile(name: String, device: String = "Ander toestel", emulator: String = "Overig", notes: String = "", files: List<String> = emptyList()): Profile {
    val n = name.trim()
    require(n.isNotEmpty()) { "Geef het profiel een naam" }
    val id = n.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "profiel" }
    return Profile(id, n, device, emulator, notes.take(500), files.distinct())
}

fun upsertProfile(list: List<Profile>, p: Profile): List<Profile> =
    if (list.any { it.id == p.id }) list.map { if (it.id == p.id) p else it } else list + p

fun removeProfile(list: List<Profile>, id: String) = list.filterNot { it.id == id }

fun missingFiles(p: Profile, files: List<LibFile>): List<String> {
    val have = files.map { it.path }.toSet()
    return p.files.filter { it !in have }
}

private fun q(s: String) = "\"" + buildString {
    for (c in s) when (c) {
        '"' -> append("\\\""); '\\' -> append("\\\\"); '\n' -> append("\\n"); '\r' -> append("\\r"); '\t' -> append("\\t")
        else -> append(c)
    }
} + "\""

fun profilesJson(list: List<Profile>): String =
    "{\"profiles\": [" + list.joinToString(",") {
        "\n  {\"name\": ${q(it.name)}, \"device\": ${q(it.device)}, \"emulator\": ${q(it.emulator)}, " +
            "\"notes\": ${q(it.notes)}, \"files\": [${it.files.joinToString(",") { f -> q(f) }}]}"
    } + "\n]}\n"

private val STR = """"((?:[^"\\]|\\.)*)""""
private val PROFILE_RE = Regex(""""name":\s*$STR.*?"device":\s*$STR.*?"emulator":\s*$STR.*?"notes":\s*$STR.*?"files":\s*\[(.*?)]""", RegexOption.DOT_MATCHES_ALL)

fun parseProfiles(json: String): List<Profile> {
    require(json.contains("\"profiles\"")) { "Geen profielen gevonden" }
    return PROFILE_RE.findAll(json).map {
        val g = it.groupValues
        val files = Regex(STR).findAll(g[5]).map { m -> unesc(m.groupValues[1]) }.toList()
        makeProfile(unesc(g[1]), unesc(g[2]), unesc(g[3]), unesc(g[4]), files)
    }.toList()
}
