package com.example.nfcvizitka.nfc

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcManager

class NfcShareController(private val context: Context) {
    private val packageManager = context.packageManager
    private val serviceComponent = ComponentName(context, NdefHostApduService::class.java)

    val isNfcAvailable: Boolean
        get() = context.getSystemService(NfcManager::class.java)?.defaultAdapter != null

    val isHceAvailable: Boolean
        get() = packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)

    val isNfcEnabled: Boolean
        get() = context.getSystemService(NfcManager::class.java)?.defaultAdapter?.isEnabled == true

    fun setSharingEnabled(enabled: Boolean) {
        packageManager.setComponentEnabledSetting(
            serviceComponent,
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}
