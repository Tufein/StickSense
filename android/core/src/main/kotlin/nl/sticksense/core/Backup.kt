package nl.sticksense.core

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

const val FILES_DIR = "bestanden/"

class BackupContents(val manifest: Manifest, val profiles: List<Profile>, val files: Map<String, ByteArray>)

private fun esc(s: String) = buildString {
    for (c in s) when (c) {
        '"' -> append("\\\""); '\\' -> append("\\\\"); '\n' -> append("\\n"); '\r' -> append("\\r"); '\t' -> append("\\t")
        else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
    }
}

fun manifestJson(m: Manifest): String = buildString {
    append("{\n  \"app\": \"StickSense\",\n  \"version\": 1,\n")
    append("  \"device\": \"${esc(m.device)}\",\n  \"createdAt\": \"${esc(m.createdAt)}\",\n  \"entries\": [")
    m.entries.forEachIndexed { i, e ->
        if (i > 0) append(",")
        append("\n    {\"path\": \"${esc(e.path)}\", \"kind\": \"${e.kind.name.lowercase()}\", \"label\": \"${esc(e.label)}\", ")
        append("\"game\": \"${esc(e.game)}\", \"size\": ${e.size}, \"sha256\": \"${e.sha256}\"}")
    }
    append("\n  ]\n}\n")
}

/** Maakt een back-up-zip. Entry-namen: manifest.json, profielen.json, bestanden/<pad>. */
fun createBackup(files: List<LibFile>, profiles: List<Profile>, device: String): ByteArray {
    val manifest = buildManifest(files.map { LibFile(FILES_DIR + it.path, it.data) }, device)
    val out = ByteArrayOutputStream()
    ZipOutputStream(out).use { z ->
        fun put(name: String, data: ByteArray) { z.putNextEntry(ZipEntry(name)); z.write(data); z.closeEntry() }
        put("manifest.json", manifestJson(manifest).toByteArray())
        put("profielen.json", profilesJson(profiles).toByteArray())
        files.forEach { put(FILES_DIR + it.path, it.data) }
    }
    return out.toByteArray()
}

/** Leest een back-up en controleert de hashes uit het manifest. */
fun readBackup(bytes: ByteArray): BackupContents {
    val raw = linkedMapOf<String, ByteArray>()
    ZipInputStream(ByteArrayInputStream(bytes)).use { z ->
        var e = z.nextEntry
        while (e != null) {
            // Voorkom path traversal vanuit een kwaadaardige zip
            require(!e.name.startsWith("/") && ".." !in e.name.split('/')) { "Onveilige bestandsnaam: ${e.name}" }
            if (!e.isDirectory) raw[e.name] = z.readBytes()
            e = z.nextEntry
        }
    }
    val mf = raw["manifest.json"] ?: error("Geen StickSense-back-up (manifest ontbreekt)")
    val manifest = parseManifest(mf.decodeToString())
    val files = HashMap<String, ByteArray>()
    for (en in manifest.entries) {
        val data = raw[en.path] ?: error("Bestand ontbreekt in back-up: ${en.path}")
        check(sha256(data) == en.sha256) { "Hash klopt niet voor ${en.path}" }
        files[en.path.removePrefix(FILES_DIR)] = data
    }
    val profiles = raw["profielen.json"]?.let { parseProfiles(it.decodeToString()) } ?: emptyList()
    return BackupContents(manifest, profiles, files)
}

private val ENTRY_RE = Regex(""""path":\s*"((?:[^"\\]|\\.)*)".*?"kind":\s*"(\w+)".*?"label":\s*"((?:[^"\\]|\\.)*)".*?"game":\s*"((?:[^"\\]|\\.)*)".*?"size":\s*(\d+).*?"sha256":\s*"([0-9a-f]+)"""")

internal fun unesc(s: String): String {
    val sb = StringBuilder()
    var i = 0
    while (i < s.length) {
        val c = s[i]
        if (c == '\\' && i + 1 < s.length) {
            when (val n = s[i + 1]) {
                'n' -> sb.append('\n'); 'r' -> sb.append('\r'); 't' -> sb.append('\t')
                'u' -> { sb.append(s.substring(i + 2, i + 6).toInt(16).toChar()); i += 4 }
                else -> sb.append(n)
            }
            i += 2
        } else { sb.append(c); i++ }
    }
    return sb.toString()
}

fun parseManifest(json: String): Manifest {
    val device = Regex(""""device":\s*"((?:[^"\\]|\\.)*)"""").find(json)?.groupValues?.get(1)?.let(::unesc) ?: "onbekend"
    val created = Regex(""""createdAt":\s*"((?:[^"\\]|\\.)*)"""").find(json)?.groupValues?.get(1)?.let(::unesc) ?: ""
    val entries = ENTRY_RE.findAll(json).map {
        val (p, k, l, g, s, h) = it.destructured
        ManifestEntry(unesc(p), Kind.entries.firstOrNull { x -> x.name.equals(k, true) } ?: Kind.OVERIG, unesc(l), unesc(g), s.toLong(), h)
    }.toList()
    return Manifest(device, created, entries)
}
