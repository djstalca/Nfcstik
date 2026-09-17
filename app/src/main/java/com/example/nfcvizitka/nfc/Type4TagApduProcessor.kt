package com.example.nfcvizitka.nfc

/**
 * Minimal read-only NFC Forum Type 4 Tag APDU processor.
 *
 * It exposes the standard NDEF Tag Application (D2760000850101),
 * Capability Container file E103 and NDEF file E104.
 */
class Type4TagApduProcessor(
    private val ndefFileProvider: () -> ByteArray
) {
    private enum class SelectedFile { CC, NDEF }

    private var selectedFile: SelectedFile? = null

    fun process(command: ByteArray?): ByteArray {
        val apdu = command ?: return STATUS_WRONG_LENGTH
        if (apdu.size < 4) return STATUS_WRONG_LENGTH

        return when {
            isSelectNdefApplication(apdu) -> {
                selectedFile = null
                STATUS_OK
            }
            isSelectFile(apdu, CC_FILE_ID) -> {
                selectedFile = SelectedFile.CC
                STATUS_OK
            }
            isSelectFile(apdu, NDEF_FILE_ID) -> {
                selectedFile = SelectedFile.NDEF
                STATUS_OK
            }
            isReadBinary(apdu) -> readBinary(apdu)
            else -> STATUS_INS_NOT_SUPPORTED
        }
    }

    fun reset() {
        selectedFile = null
    }

    private fun readBinary(command: ByteArray): ByteArray {
        if (command.size < 5) return STATUS_WRONG_LENGTH

        val file = when (selectedFile) {
            SelectedFile.CC -> CAPABILITY_CONTAINER
            SelectedFile.NDEF -> ndefFileProvider()
            null -> return STATUS_COMMAND_NOT_ALLOWED
        }

        val offset = ((command[2].toInt() and 0xFF) shl 8) or (command[3].toInt() and 0xFF)
        val requested = command[4].toInt() and 0xFF
        val length = if (requested == 0) 256 else requested

        if (offset > file.size) return STATUS_WRONG_PARAMETERS
        val end = minOf(offset + length, file.size)
        return file.copyOfRange(offset, end) + STATUS_OK
    }

    private fun isSelectNdefApplication(command: ByteArray): Boolean {
        if (command.size < 12) return false
        if (command[0] != 0x00.toByte() || command[1] != 0xA4.toByte() || command[2] != 0x04.toByte()) return false
        val lc = command[4].toInt() and 0xFF
        if (lc != NDEF_AID.size || command.size < 5 + lc) return false
        return command.copyOfRange(5, 5 + lc).contentEquals(NDEF_AID)
    }

    private fun isSelectFile(command: ByteArray, fileId: ByteArray): Boolean {
        if (command.size < 7) return false
        if (command[0] != 0x00.toByte() || command[1] != 0xA4.toByte()) return false
        val lc = command[4].toInt() and 0xFF
        if (lc != 2) return false
        return command[5] == fileId[0] && command[6] == fileId[1]
    }

    private fun isReadBinary(command: ByteArray): Boolean =
        command.size >= 5 && command[0] == 0x00.toByte() && command[1] == 0xB0.toByte()

    companion object {
        private val NDEF_AID = hex("D2760000850101")
        private val CC_FILE_ID = hex("E103")
        private val NDEF_FILE_ID = hex("E104")

        // CCLEN=15, mapping 2.0, MLe=255, MLc=255,
        // NDEF file E104, max size 32767, read access granted, write denied.
        val CAPABILITY_CONTAINER: ByteArray = hex("000F2000FF00FF0406E1047FFF00FF")

        val STATUS_OK: ByteArray = hex("9000")
        val STATUS_WRONG_LENGTH: ByteArray = hex("6700")
        val STATUS_COMMAND_NOT_ALLOWED: ByteArray = hex("6986")
        val STATUS_WRONG_PARAMETERS: ByteArray = hex("6B00")
        val STATUS_INS_NOT_SUPPORTED: ByteArray = hex("6D00")

        fun hex(value: String): ByteArray =
            value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
