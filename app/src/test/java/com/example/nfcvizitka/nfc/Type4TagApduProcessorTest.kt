package com.example.nfcvizitka.nfc

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class Type4TagApduProcessorTest {
    private val ndefFile = Type4TagApduProcessor.hex("0003D10100")
    private val processor = Type4TagApduProcessor { ndefFile }

    @Test
    fun selectsApplicationAndReadsCapabilityContainer() {
        assertArrayEquals(
            Type4TagApduProcessor.STATUS_OK,
            processor.process(Type4TagApduProcessor.hex("00A4040007D276000085010100"))
        )
        assertArrayEquals(
            Type4TagApduProcessor.STATUS_OK,
            processor.process(Type4TagApduProcessor.hex("00A4000C02E103"))
        )

        val response = processor.process(Type4TagApduProcessor.hex("00B000000F"))
        assertArrayEquals(
            Type4TagApduProcessor.CAPABILITY_CONTAINER + Type4TagApduProcessor.STATUS_OK,
            response
        )
    }

    @Test
    fun selectsAndReadsNdefFile() {
        processor.process(Type4TagApduProcessor.hex("00A4040007D276000085010100"))
        processor.process(Type4TagApduProcessor.hex("00A4000C02E104"))

        val response = processor.process(Type4TagApduProcessor.hex("00B0000005"))
        assertArrayEquals(ndefFile + Type4TagApduProcessor.STATUS_OK, response)
    }

    @Test
    fun rejectsReadBeforeFileSelection() {
        assertArrayEquals(
            Type4TagApduProcessor.STATUS_COMMAND_NOT_ALLOWED,
            processor.process(Type4TagApduProcessor.hex("00B000000F"))
        )
    }
}
