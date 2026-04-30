package com.karrad.bilets.domain.sms

abstract class SmsGateway {
    /**
     * Sends a verification code to the given phone number.
     * Returns the actual 4-digit code that was delivered
     * (for flash call — last 4 digits of the calling number;
     *  for SMS — the code included in the message text).
     */
    abstract fun sendCode(phone: String): String
}
