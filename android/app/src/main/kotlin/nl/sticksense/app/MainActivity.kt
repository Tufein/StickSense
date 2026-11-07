package nl.sticksense.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.sticksense.core.*

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = if (androidx.compose.foundation.isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) { Root(vm) } }
    }
}

private fun android.content.Context.keep(uri: Uri) {
    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Root(vm: AppViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Saves", "Profielen", "Back-up", "Bronnen")
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(vm.message) { vm.message?.let { snack.showSnackbar(it); vm.message = null } }
    Scaffold(
        topBar = { TopAppBar(title = { Text("StickSense") }) },
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(selected = tab == i, onClick = { tab = i }, label = { Text(t) }, icon = {})
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> SavesScreen(vm)
                1 -> ProfilesScreen(vm)
                2 -> BackupScreen(vm)
                else -> SourcesScreen(vm)
            }
            if (vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }
    }
}

@Composable
fun SavesScreen(vm: AppViewModel) {
    var q by remember { mutableStateOf("") }
    val files = vm.found
        .filter { q.isBlank() || it.path.contains(q, ignoreCase = true) }
        .map { LibFile(it.path, ByteArray(0)) to it }
    if (vm.sources.isEmpty()) return Empty("Voeg eerst een bron toe op het tabblad Bronnen, bijvoorbeeld de saves-map van RetroArch.")
    Column(Modifier.padding(horizontal = 16.dp)) {
        OutlinedTextField(q, { q = it }, label = { Text("Zoek op spel of bestand") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${vm.found.size} bestanden, ${formatSize(vm.found.sumOf { it.size })}", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = vm::rescan) { Text("Vernieuwen") }
        }
        if (files.isEmpty()) return@Column Empty("Geen bestanden gevonden.")
        val groups = files.groupBy { it.first.game }.toList().sortedBy { it.first.lowercase() }
        LazyColumn {
            groups.forEach { (game, items) ->
                item(key = game) {
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(game, style = MaterialTheme.typography.titleMedium)
                            items.forEach { (lf, f) ->
                                Text("${f.path}  ·  ${lf.type.label}  ·  ${formatSize(f.size)}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Empty(text: String) = Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Text(text) }

@Composable
fun ProfilesScreen(vm: AppViewModel) {
    var creating by remember { mutableStateOf(false) }
    Column(Modifier.padding(16.dp)) {
        Button(onClick = { creating = true }) { Text("Nieuw profiel") }
        if (vm.profiles.isEmpty()) Empty("Nog geen profielen. Een profiel bundelt instellingsbestanden per toestel en emulator.")
        LazyColumn {
            items(vm.profiles, key = { it.id }) { p ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(p.name, style = MaterialTheme.typography.titleMedium)
                        Text("${p.device} · ${p.emulator} · ${p.files.size} bestand(en)", style = MaterialTheme.typography.bodySmall)
                        if (p.notes.isNotBlank()) Text(p.notes)
                        val missing = missingFiles(p, vm.found.map { LibFile(it.path, ByteArray(0)) })
                        if (missing.isNotEmpty()) Text("${missing.size} bestand(en) niet gevonden op dit toestel", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { vm.deleteProfile(p.id) }) { Text("Verwijder") }
                    }
                }
            }
        }
    }
    if (creating) ProfileDialog(vm) { creating = false }
}

@Composable
fun ProfileDialog(vm: AppViewModel, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var device by remember { mutableStateOf(DEVICES.first()) }
    var emu by remember { mutableStateOf(EMULATORS.first()) }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val cfgs = remember { vm.found.filter { classify(it.doc.name ?: "").kind == Kind.CONFIG } }
    val chosen = remember { mutableStateListOf<String>() }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text("Nieuw profiel") },
        text = {
            LazyColumn {
                item {
                    OutlinedTextField(name, { name = it }, label = { Text("Naam") }, singleLine = true, isError = error != null, supportingText = { error?.let { Text(it) } })
                    Cycle("Toestel", DEVICES, device) { device = it }
                    Cycle("Emulator", EMULATORS, emu) { emu = it }
                    OutlinedTextField(notes, { notes = it }, label = { Text("Notities") })
                    Text("Instellingsbestanden", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                    if (cfgs.isEmpty()) Text("Geen instellingsbestanden (.cfg, .ini, .json) gevonden in je bronnen.", style = MaterialTheme.typography.bodySmall)
                }
                items(cfgs, key = { it.path }) { f ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(f.path in chosen, { if (it) chosen += f.path else chosen -= f.path })
                        Text(f.path, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                try { vm.saveProfile(makeProfile(name, device, emu, notes, chosen.toList())); onDone() } catch (e: IllegalArgumentException) { error = e.message }
            }) { Text("Opslaan") }
        },
        dismissButton = { TextButton(onClick = onDone) { Text("Annuleer") } },
    )
}

/** Kies uit een korte lijst door op de knop te tikken. */
@Composable
fun Cycle(label: String, options: List<String>, value: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.padding(top = 8.dp)) {
        OutlinedButton(onClick = { open = true }) { Text("$label: $value") }
        DropdownMenu(open, { open = false }) {
            options.forEach { o -> DropdownMenuItem(text = { Text(o) }, onClick = { onChange(o); open = false }) }
        }
    }
}

@Composable
fun BackupScreen(vm: AppViewModel) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val pickDir = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { u -> u?.let { ctx.keep(it); vm.chooseBackupDir(it) } }
    val pickZip = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> u?.let(vm::openBackup) }
    LazyColumn(Modifier.padding(16.dp)) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Back-up maken", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(vm.device, vm::changeDevice, label = { Text("Toestelnaam") }, singleLine = true)
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(vm.includeStates, { vm.includeStates = it }); Text("Savestates meenemen (groot)") }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(vm.includeOther, { vm.includeOther = it }); Text("Overige bestanden meenemen") }
                    Text(if (vm.backupDir == null) "Nog geen back-upmap gekozen" else "Back-upmap ingesteld", style = MaterialTheme.typography.bodySmall)
                    Row {
                        OutlinedButton(onClick = { pickDir.launch(null) }) { Text("Back-upmap kiezen") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = vm::makeBackup, enabled = !vm.busy && vm.sources.isNotEmpty()) { Text("Back-up maken") }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Terugzetten", style = MaterialTheme.typography.titleMedium)
                    Text("Je ziet eerst wat nieuw is en wat verschilt. Gewijzigde saves worden alleen overschreven als je ze zelf aanvinkt.", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { pickZip.launch(arrayOf("application/zip", "application/octet-stream")) }, enabled = !vm.busy) { Text("Back-up kiezen") }
                    vm.pending?.let { c ->
                        Text("Back-up van ${c.manifest.device}, ${c.manifest.createdAt.take(10)}", modifier = Modifier.padding(top = 8.dp))
                        vm.restoreRows.forEach { r ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(r.checked, { vm.toggleRow(r.path) }, enabled = r.status != Status.GELIJK)
                                Column { Text(r.path, style = MaterialTheme.typography.bodySmall); Text(r.status.name.lowercase().replace('_', ' '), style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                        Row {
                            Button(onClick = vm::applyRestore, enabled = !vm.busy) { Text("Geselecteerde terugzetten") }
                            TextButton(onClick = vm::cancelRestore) { Text("Annuleer") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SourcesScreen(vm: AppViewModel) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var label by remember { mutableStateOf("") }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { u ->
        if (u != null) { ctx.keep(u); vm.addSource(label.trim(), u); label = "" }
    }
    Column(Modifier.padding(16.dp)) {
        Text("Een bron is een map met saves of instellingen, bijvoorbeeld de saves-map van RetroArch of DuckStation. De naam wordt in back-ups gebruikt.", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(label, { label = it }, label = { Text("Naam, bijv. RetroArch") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(onClick = { pick.launch(null) }, enabled = label.isNotBlank() && '/' !in label) { Text("Map kiezen") }
        LazyColumn(Modifier.padding(top = 12.dp)) {
            items(vm.sources, key = { it.label }) { s ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${s.label} (${vm.found.count { it.source == s }} bestanden)")
                    TextButton(onClick = { vm.removeSource(s) }) { Text("Verwijder") }
                }
            }
        }
    }
}
