package com.karrad.bilets.domain.sms

abstract class SmsGateway {
    /** Sends a one-time verification code to the given phone number. */
    abstract fun sendCode(phone: String, code: String)
}
