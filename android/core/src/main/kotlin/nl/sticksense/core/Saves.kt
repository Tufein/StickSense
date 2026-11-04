package nl.sticksense.core

import java.security.MessageDigest

enum class Kind { SAVE, STATE, CONFIG, OVERIG }

data class FileType(val ext: String, val kind: Kind, val label: String)

private val TYPES = mapOf(
    "srm" to ("RetroArch-save" to Kind.SAVE), "sav" to ("Save" to Kind.SAVE),
    "mcr" to ("PS1-geheugenkaart" to Kind.SAVE), "mcd" to ("PS1-geheugenkaart" to Kind.SAVE),
    "ps2" to ("PS2-geheugenkaart" to Kind.SAVE), "eep" to ("N64-save" to Kind.SAVE),
    "fla" to ("N64-save" to Kind.SAVE), "dsv" to ("DS-save" to Kind.SAVE),
    "ini" to ("Emulatorinstelling" to Kind.CONFIG), "cfg" to ("Emulatorinstelling" to Kind.CONFIG),
    "json" to ("Profiel" to Kind.CONFIG),
)
private val STATE_RE = Regex("""\.state\d*(\.auto)?$""", RegexOption.IGNORE_CASE)
private val EXT_RE = Regex("""\.([a-z0-9]+)$""", RegexOption.IGNORE_CASE)
private val GAME_RE = Regex("""\.(state\d*(\.auto)?|[a-z0-9]{2,4})$""", RegexOption.IGNORE_CASE)

/** Bepaalt het type van een bestandsnaam; onbekende bestanden krijgen Kind.OVERIG. */
fun classify(name: String): FileType {
    if (STATE_RE.containsMatchIn(name)) return FileType("state", Kind.STATE, "Savestate")
    val ext = EXT_RE.find(name)?.groupValues?.get(1)?.lowercase() ?: return FileType("", Kind.OVERIG, "Bestand")
    val t = TYPES[ext] ?: return FileType(ext, Kind.OVERIG, "Bestand")
    return FileType(ext, t.second, t.first)
}

fun gameName(fileName: String): String = fileName.replace(GAME_RE, "").trim()

fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

fun formatSize(n: Long): String = when {
    n < 1024 -> "$n B"
    n < 1_048_576 -> "%.1f KB".format(java.util.Locale.US, n / 1024.0)
    else -> "%.1f MB".format(java.util.Locale.US, n / 1_048_576.0)
}

/** Een bestand in de bibliotheek. `path` is relatief, bv. "RetroArch/saves/Zelda.srm". */
class LibFile(val path: String, val data: ByteArray) {
    val name: String get() = path.substringAfterLast('/')
    val type: FileType get() = classify(name)
    val game: String get() = gameName(name)
    val size: Long get() = data.size.toLong()
}

data class ManifestEntry(
    val path: String, val kind: Kind, val label: String, val game: String, val size: Long, val sha256: String,
)

data class Manifest(val device: String, val createdAt: String, val entries: List<ManifestEntry>)

fun buildManifest(files: List<LibFile>, device: String = "onbekend", createdAt: String = java.time.Instant.now().toString()) =
    Manifest(device, createdAt, files.map {
        ManifestEntry(it.path, it.type.kind, it.type.label, it.game, it.size, sha256(it.data))
    })

enum class Status { NIEUW, GEWIJZIGD, GELIJK, ALLEEN_LOKAAL }

data class DiffRow(val path: String, val status: Status)

/** Vergelijkt een back-up met de huidige bibliotheek. Pad en hash bepalen de status. */
fun diffManifests(backup: Manifest, current: Manifest): List<DiffRow> {
    val cur = current.entries.associateBy { it.path }.toMutableMap()
    val rows = mutableListOf<DiffRow>()
    for (b in backup.entries) {
        val c = cur.remove(b.path)
        rows += DiffRow(b.path, when {
            c == null -> Status.NIEUW
            c.sha256 == b.sha256 -> Status.GELIJK
            else -> Status.GEWIJZIGD
        })
    }
    for (p in cur.keys) rows += DiffRow(p, Status.ALLEEN_LOKAAL)
    return rows
}

fun groupByGame(files: List<LibFile>): List<Pair<String, List<LibFile>>> =
    files.groupBy { it.game }.toList().sortedBy { it.first.lowercase() }
