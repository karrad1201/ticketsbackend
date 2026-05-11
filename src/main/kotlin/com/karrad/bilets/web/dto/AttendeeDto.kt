package com.karrad.bilets.web.dto

data class AttendeeDto(
    val userId: String,
    val name: String,
    val maskedPhone: String?
)

fun String.maskPhone(): String {
    val digits = this.filter { it.isDigit() }
    if (digits.length < 10) return this
    val last2 = digits.takeLast(2)
    val prefix = digits.take(digits.length - 7)
    return "+$prefix *** ** $last2"
}
