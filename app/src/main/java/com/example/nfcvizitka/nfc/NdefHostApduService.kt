package com.example.nfcvizitka.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import com.example.nfcvizitka.data.ContactRepository
import com.example.nfcvizitka.util.VCardBuilder
import java.nio.charset.StandardCharsets

class NdefHostApduService : HostApduService() {
    private val processor by lazy {
        Type4TagApduProcessor { buildNdefFile() }
    }

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray =
        processor.process(commandApdu)

    override fun onDeactivated(reason: Int) {
        processor.reset()
    }

    private fun buildNdefFile(): ByteArray {
        val profile = ContactRepository(applicationContext).load()
        val vCard = VCardBuilder.build(profile)
        val record = NdefRecord.createMime(
            "text/vcard",
            vCard.toByteArray(StandardCharsets.UTF_8)
        )
        val ndef = NdefMessage(arrayOf(record)).toByteArray()

        require(ndef.size <= 0x7FFF) { "NDEF message is too large for the emulated tag" }
        return byteArrayOf(
            ((ndef.size ushr 8) and 0xFF).toByte(),
            (ndef.size and 0xFF).toByte()
        ) + ndef
    }
}
