package nl.sticksense.core

import kotlin.test.*

class CoreTest {
    private fun f(p: String, s: String) = LibFile(p, s.toByteArray())

    @Test fun classifyHerkentTypes() {
        assertEquals(Kind.SAVE, classify("Zelda.srm").kind)
        assertEquals(Kind.STATE, classify("Zelda.state3").kind)
        assertEquals(Kind.STATE, classify("Zelda.state.auto").kind)
        assertEquals(Kind.CONFIG, classify("retroarch.cfg").kind)
        assertEquals(Kind.OVERIG, classify("rom.zip").kind)
        assertEquals(Kind.OVERIG, classify("README").kind)
    }

    @Test fun gameNaam() {
        assertEquals("Zelda", gameName("Zelda.srm"))
        assertEquals("Zelda", gameName("Zelda.state.auto"))
        assertEquals("Zelda", gameName("Zelda.state3"))
    }

    @Test fun diffStatus() {
        val a = buildManifest(listOf(f("A.srm", "1"), f("B.srm", "2")))
        val c = buildManifest(listOf(f("A.srm", "1"), f("B.srm", "3"), f("C.srm", "4")))
        val d = diffManifests(a, c).associate { it.path to it.status }
        assertEquals(mapOf("A.srm" to Status.GELIJK, "B.srm" to Status.GEWIJZIGD, "C.srm" to Status.ALLEEN_LOKAAL), d)
    }

    @Test fun backupRoundtripMetUnicodeEnProfielen() {
        val files = listOf(f("RetroArch/saves/Pokémon \"Rood\".srm", "abc"), f("DuckStation/memcards/x.mcr", ""))
        val profiles = listOf(makeProfile("Odin PS2", "AYN Odin 2", "NetherSX2", "regel1\nregel2", listOf("RetroArch/saves/Pokémon \"Rood\".srm")))
        val back = readBackup(createBackup(files, profiles, "Mijn Odin"))
        assertEquals("Mijn Odin", back.manifest.device)
        assertEquals(2, back.files.size)
        assertContentEquals("abc".toByteArray(), back.files["RetroArch/saves/Pokémon \"Rood\".srm"])
        assertEquals(profiles, back.profiles)
    }

    @Test fun beschadigdeBackupWordtGeweigerd() {
        val z = createBackup(listOf(f("A.srm", "hallo")), emptyList(), "x")
        assertFailsWith<Exception> { readBackup(z.copyOf(10)) }
        assertFailsWith<IllegalStateException> { readBackup(ByteArray(0)) }
    }

    @Test fun zipMetPadTraversalWordtGeweigerd() {
        val out = java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(out).use { it.putNextEntry(java.util.zip.ZipEntry("../evil")); it.write(1); it.closeEntry() }
        assertFailsWith<IllegalArgumentException> { readBackup(out.toByteArray()) }
    }

    @Test fun profielen() {
        assertFailsWith<IllegalArgumentException> { makeProfile(" ") }
        val p = makeProfile("Mijn Odin!", files = listOf("a", "a", "b"))
        assertEquals("mijn-odin", p.id)
        assertEquals(listOf("a", "b"), p.files)
        var l = upsertProfile(emptyList(), p)
        l = upsertProfile(l, p.copy(emulator = "Dolphin"))
        assertEquals(1, l.size)
        assertEquals("Dolphin", l[0].emulator)
        assertTrue(removeProfile(l, "mijn-odin").isEmpty())
        assertEquals(listOf("b"), missingFiles(p, listOf(f("a", "x"))))
    }

    @Test fun groeperenEnGrootte() {
        val g = groupByGame(listOf(f("B.srm", "1"), f("A.srm", "1"), f("A.state1", "1")))
        assertEquals(listOf("A" to 2, "B" to 1), g.map { it.first to it.second.size })
        assertEquals("2.0 KB", formatSize(2048))
    }
}
