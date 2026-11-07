package nl.sticksense.app

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

/** Een gekozen map (via Storage Access Framework) met een herkenbaar label. */
data class Source(val label: String, val uri: Uri)

/** Bestand gevonden bij het scannen; de inhoud wordt pas gelezen wanneer nodig. */
data class Found(val source: Source, val rel: String, val doc: DocumentFile) {
    val path: String get() = "${source.label}/$rel"
    val size: Long get() = doc.length()
}

object Saf {
    fun scan(ctx: Context, sources: List<Source>): List<Found> {
        val out = mutableListOf<Found>()
        for (s in sources) {
            val root = DocumentFile.fromTreeUri(ctx, s.uri) ?: continue
            fun walk(dir: DocumentFile, prefix: String) {
                for (f in dir.listFiles()) {
                    val name = f.name ?: continue
                    if (f.isDirectory) walk(f, "$prefix$name/") else if (f.isFile) out += Found(s, prefix + name, f)
                }
            }
            walk(root, "")
        }
        return out
    }

    fun read(ctx: Context, f: DocumentFile): ByteArray =
        ctx.contentResolver.openInputStream(f.uri)?.use { it.readBytes() } ?: error("Kan ${f.name} niet lezen")

    /** Schrijft een bestand op relatief pad in de map, met aanmaken van submappen. */
    fun write(ctx: Context, root: Uri, rel: String, data: ByteArray) {
        var dir = DocumentFile.fromTreeUri(ctx, root) ?: error("Map niet beschikbaar")
        val parts = rel.split('/')
        for (p in parts.dropLast(1)) dir = dir.findFile(p) ?: dir.createDirectory(p) ?: error("Kan map $p niet maken")
        val name = parts.last()
        val existing = dir.findFile(name)
        val target = existing ?: dir.createFile("application/octet-stream", name) ?: error("Kan $name niet maken")
        ctx.contentResolver.openOutputStream(target.uri, "wt")?.use { it.write(data) } ?: error("Kan $name niet schrijven")
    }

    fun writeNew(ctx: Context, root: Uri, name: String, mime: String, data: ByteArray) {
        val dir = DocumentFile.fromTreeUri(ctx, root) ?: error("Map niet beschikbaar")
        val f = dir.createFile(mime, name) ?: error("Kan $name niet maken")
        ctx.contentResolver.openOutputStream(f.uri)?.use { it.write(data) } ?: error("Kan $name niet schrijven")
    }
}

/** Eenvoudige opslag van instellingen. */
class Prefs(ctx: Context) {
    private val p = ctx.getSharedPreferences("sticksense", Context.MODE_PRIVATE)
    var sources: List<Source>
        get() = p.getStringSet("sources", emptySet())!!.mapNotNull {
            val i = it.indexOf('|'); if (i < 0) null else Source(it.substring(0, i), Uri.parse(it.substring(i + 1)))
        }.sortedBy { it.label }
        set(v) = p.edit().putStringSet("sources", v.map { "${it.label}|${it.uri}" }.toSet()).apply()
    var backupDir: Uri?
        get() = p.getString("backupDir", null)?.let(Uri::parse)
        set(v) = p.edit().putString("backupDir", v?.toString()).apply()
    var device: String
        get() = p.getString("device", "") ?: ""
        set(v) = p.edit().putString("device", v).apply()
    var profilesJson: String
        get() = p.getString("profiles", "") ?: ""
        set(v) = p.edit().putString("profiles", v).apply()
}
