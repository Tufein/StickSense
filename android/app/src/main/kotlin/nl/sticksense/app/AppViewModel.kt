package nl.sticksense.app

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.sticksense.core.*

data class RestoreRow(val path: String, val status: Status, val checked: Boolean)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx get() = getApplication<Application>()
    private val prefs = Prefs(app)

    var sources by mutableStateOf(prefs.sources); private set
    var backupDir by mutableStateOf(prefs.backupDir); private set
    var device by mutableStateOf(prefs.device); private set
    var found by mutableStateOf<List<Found>>(emptyList()); private set
    var profiles by mutableStateOf(runCatching { parseProfiles(prefs.profilesJson) }.getOrDefault(emptyList())); private set
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null)

    var includeStates by mutableStateOf(false)
    var includeOther by mutableStateOf(false)

    var pending by mutableStateOf<BackupContents?>(null); private set
    var restoreRows by mutableStateOf<List<RestoreRow>>(emptyList()); private set

    init { if (sources.isNotEmpty()) rescan() }

    private fun launch(block: suspend () -> Unit) = viewModelScope.launch {
        busy = true
        try { block() } catch (e: Exception) { message = "Fout: ${e.message ?: e.javaClass.simpleName}" } finally { busy = false }
    }

    fun rescan() = launch { found = withContext(Dispatchers.IO) { Saf.scan(ctx, sources) } }

    fun addSource(label: String, uri: Uri) {
        sources = (sources.filter { it.label != label } + Source(label, uri)).sortedBy { it.label }
        prefs.sources = sources
        rescan()
    }
    fun removeSource(s: Source) { sources = sources - s; prefs.sources = sources; rescan() }
    fun chooseBackupDir(uri: Uri) { backupDir = uri; prefs.backupDir = uri }
    fun changeDevice(v: String) { device = v; prefs.device = v }

    fun saveProfile(p: Profile) { profiles = upsertProfile(profiles, p); prefs.profilesJson = profilesJson(profiles) }
    fun deleteProfile(id: String) { profiles = removeProfile(profiles, id); prefs.profilesJson = profilesJson(profiles) }

    private fun selected(f: Found) = when (classify(f.doc.name ?: "").kind) {
        Kind.SAVE, Kind.CONFIG -> true
        Kind.STATE -> includeStates
        Kind.OVERIG -> includeOther
    }

    fun makeBackup() = launch {
        val dir = backupDir ?: run { message = "Kies eerst een map voor back-ups"; return@launch }
        val n = withContext(Dispatchers.IO) {
            val files = found.filter(::selected).map { LibFile(it.path, Saf.read(ctx, it.doc)) }
            val zip = createBackup(files, profiles, device.ifBlank { "onbekend" })
            val stamp = java.time.LocalDate.now().toString()
            Saf.writeNew(ctx, dir, "sticksense-backup-$stamp-${System.currentTimeMillis() % 100000}.zip", "application/zip", zip)
            files.size
        }
        message = "Back-up gemaakt met $n bestanden"
    }

    fun openBackup(uri: Uri) = launch {
        val (contents, rows) = withContext(Dispatchers.IO) {
            val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Kan bestand niet lezen")
            val c = readBackup(bytes)
            // Alleen bestanden die in de back-up staan hoeven gehasht te worden
            val inBackup = c.files.keys
            val local = found.filter { it.path in inBackup }.map { LibFile(it.path, Saf.read(ctx, it.doc)) }
            val diff = diffManifests(
                Manifest(c.manifest.device, c.manifest.createdAt, c.manifest.entries.map { it.copy(path = it.path.removePrefix(FILES_DIR)) }),
                buildManifest(local),
            ).filter { it.status != Status.ALLEEN_LOKAAL }
            c to diff.map { RestoreRow(it.path, it.status, it.status == Status.NIEUW) }
        }
        pending = contents
        restoreRows = rows
    }

    fun toggleRow(path: String) { restoreRows = restoreRows.map { if (it.path == path && it.status != Status.GELIJK) it.copy(checked = !it.checked) else it } }
    fun cancelRestore() { pending = null; restoreRows = emptyList() }

    fun applyRestore() = launch {
        val c = pending ?: return@launch
        var done = 0; var skipped = 0
        withContext(Dispatchers.IO) {
            for (row in restoreRows.filter { it.checked }) {
                val label = row.path.substringBefore('/')
                val src = sources.firstOrNull { it.label == label }
                if (src == null) { skipped++; continue }
                Saf.write(ctx, src.uri, row.path.substringAfter('/'), c.files.getValue(row.path))
                done++
            }
        }
        for (p in c.profiles) saveProfile(p)
        cancelRestore()
        message = "$done bestand(en) teruggezet" + if (skipped > 0) ", $skipped overgeslagen (bron niet ingesteld)" else ""
        rescan()
    }
}
