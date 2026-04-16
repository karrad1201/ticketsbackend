package com.karrad.bilets.infrastructure.payment

import com.karrad.bilets.config.TBankProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TBankPaymentGatewayTest {

    private val props = TBankProperties(
        terminalKey = "TestTerminal",
        password = "TestPassword"
    )
    private val gateway = TBankPaymentGateway(props)

    @Test
    fun `computeToken sorts params alphabetically and hashes values`() {
        // Example from T-Bank docs: sorted values concatenated, then SHA-256
        val params = linkedMapOf(
            "TerminalKey" to "TestTerminal",
            "Amount" to "1000",
            "OrderId" to "order-42",
            "Password" to "TestPassword"
        )
        val token = gateway.computeToken(params)

        // Sorted keys: Amount, OrderId, Password, TerminalKey
        // Concatenated values: "1000order-42TestPasswordTestTerminal"
        val expected = sha256("1000order-42TestPasswordTestTerminal")
        assertEquals(expected, token)
    }

    @Test
    fun `computeToken is stable for same input`() {
        val params = linkedMapOf("B" to "2", "A" to "1", "C" to "3")
        assertEquals(gateway.computeToken(params), gateway.computeToken(params))
    }

    private fun sha256(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
