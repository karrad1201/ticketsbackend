package com.karrad.bilets

import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertTrue

class SandboxSmokeTests {

    @Test
    fun `sandbox main should print venue struct and inventory summary`() {
        val originalOut = System.out
        val buffer = ByteArrayOutputStream()

        try {
            System.setOut(PrintStream(buffer, true, Charsets.UTF_8))
            main()
        } finally {
            System.setOut(originalOut)
        }

        val output = buffer.toString(Charsets.UTF_8)
        assertTrue(output.contains("=== JSON вывод ==="))
        assertTrue(output.contains("=== Map вывод ==="))
        assertTrue(output.contains("=== Event inventory ==="))
        assertTrue(output.contains("Seats to sell: 60"))
    }
}
