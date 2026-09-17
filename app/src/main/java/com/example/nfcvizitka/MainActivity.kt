package com.example.nfcvizitka

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.nfcvizitka.data.ContactProfile
import com.example.nfcvizitka.data.ContactRepository
import com.example.nfcvizitka.nfc.NfcShareController
import com.example.nfcvizitka.util.QrCodeGenerator
import com.example.nfcvizitka.util.VCardBuilder

class MainActivity : ComponentActivity() {
    private val resumeTick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = ContactRepository(this)
        val controller = NfcShareController(this)

        controller.setSharingEnabled(repository.isNfcSharingEnabled() && controller.isHceAvailable)

        setContent {
            MaterialTheme {
                @Suppress("UNUSED_VARIABLE")
                val tick = resumeTick.intValue
                NfcVizitkaScreen(repository, controller)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick.intValue++
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NfcVizitkaScreen(
    repository: ContactRepository,
    controller: NfcShareController
) {
    val context = LocalContext.current
    val initial = remember { repository.load() }

    var firstName by remember { mutableStateOf(initial.firstName) }
    var lastName by remember { mutableStateOf(initial.lastName) }
    var company by remember { mutableStateOf(initial.company) }
    var jobTitle by remember { mutableStateOf(initial.jobTitle) }
    var phone by remember { mutableStateOf(initial.phone) }
    var email by remember { mutableStateOf(initial.email) }
    var website by remember { mutableStateOf(initial.website) }
    var note by remember { mutableStateOf(initial.note) }
    var sharingEnabled by remember { mutableStateOf(repository.isNfcSharingEnabled()) }
    var showQr by remember { mutableStateOf(false) }

    fun currentProfile() = ContactProfile(
        firstName = firstName,
        lastName = lastName,
        company = company,
        jobTitle = jobTitle,
        phone = phone,
        email = email,
        website = website,
        note = note
    )

    fun saveProfile(showToast: Boolean = true): Boolean {
        val profile = currentProfile()
        if (profile.displayName.isBlank()) {
            Toast.makeText(context, "Vpiši vsaj ime ali priimek.", Toast.LENGTH_SHORT).show()
            return false
        }
        repository.save(profile)
        if (showToast) {
            Toast.makeText(context, "Podatki so shranjeni.", Toast.LENGTH_SHORT).show()
        }
        return true
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("NFC Vizitka") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Tvoji podatki",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Podatki ostanejo v telefonu. NFC in QR vsebujeta neposreden vCard kontakt.",
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Ime") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Priimek") },
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = company,
                onValueChange = { company = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Podjetje") },
                singleLine = true
            )
            OutlinedTextField(
                value = jobTitle,
                onValueChange = { jobTitle = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Delovno mesto") },
                singleLine = true
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Telefon") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("E-pošta") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )
            OutlinedTextField(
                value = website,
                onValueChange = { website = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Spletna stran") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                singleLine = true
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Opomba") },
                minLines = 2,
                maxLines = 4
            )

            Button(
                onClick = { saveProfile() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Shrani podatke")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                "Deljenje",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("NFC deljenje", fontWeight = FontWeight.SemiBold)
                            Text(
                                when {
                                    !controller.isNfcAvailable -> "Ta telefon nima NFC."
                                    !controller.isHceAvailable -> "Ta telefon ne podpira HCE."
                                    sharingEnabled && controller.isNfcEnabled -> "Aktivno – približaj odklenjen telefon drugemu telefonu."
                                    sharingEnabled -> "Aktivno, vendar je sistemski NFC izklopljen."
                                    else -> "Izklopljeno"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = sharingEnabled,
                            enabled = controller.isHceAvailable,
                            onCheckedChange = { enabled ->
                                if (enabled && !saveProfile(showToast = false)) return@Switch
                                sharingEnabled = enabled
                                repository.setNfcSharingEnabled(enabled)
                                controller.setSharingEnabled(enabled)
                            }
                        )
                    }

                    if (controller.isNfcAvailable && !controller.isNfcEnabled) {
                        OutlinedButton(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Odpri NFC nastavitve")
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    if (saveProfile(showToast = false)) showQr = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Prikaži QR vizitko")
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Za NFC mora biti telefon odklenjen. Podatki se prenesejo kot standardni vCard; odziv prejemnega telefona je odvisen od njegove NFC in kontaktne aplikacije.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    if (showQr) {
        QrDialog(
            profile = currentProfile(),
            onDismiss = { showQr = false }
        )
    }
}

@Composable
private fun QrDialog(profile: ContactProfile, onDismiss: () -> Unit) {
    val vCard = remember(profile) { VCardBuilder.build(profile) }
    val bitmap = remember(vCard) { QrCodeGenerator.create(vCard) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) { Text("Zapri") }
        },
        title = { Text(profile.displayName.ifBlank { "Vizitka" }) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "QR koda z vCard kontaktom",
                    modifier = Modifier.size(280.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Skeniraj s kamero. QR vsebuje iste kontaktne podatke kot NFC.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    )
}
