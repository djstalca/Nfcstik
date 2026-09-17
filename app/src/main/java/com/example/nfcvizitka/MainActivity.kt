package com.example.nfcvizitka

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.PhoneNumberUtils
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nfcvizitka.data.ContactProfile
import com.example.nfcvizitka.data.ContactRepository
import com.example.nfcvizitka.nfc.NfcShareController
import com.example.nfcvizitka.util.ContactFormat
import com.example.nfcvizitka.util.QrCodeGenerator
import com.example.nfcvizitka.util.VCardBuilder
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val resumeRevision = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = ContactRepository(this)
        val controller = NfcShareController(this)
        val sharingCanRun = repository.isNfcSharingEnabled() && controller.isHceAvailable

        if (!sharingCanRun && repository.isNfcSharingEnabled()) {
            repository.setNfcSharingEnabled(false)
        }
        controller.setSharingEnabled(sharingCanRun)

        setContent {
            NfcVizitkaTheme {
                NfcVizitkaApp(
                    repository = repository,
                    controller = controller,
                    systemStateRevision = resumeRevision.intValue
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeRevision.intValue++
    }
}

private enum class AppScreen { HOME, EDIT }

@Composable
private fun NfcVizitkaTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
private fun NfcVizitkaApp(
    repository: ContactRepository,
    controller: NfcShareController,
    systemStateRevision: Int
) {
    var profileRevision by remember { mutableIntStateOf(0) }
    val profile = remember(profileRevision) { repository.load() }
    var screen by rememberSaveable {
        mutableStateOf(if (profile.displayName.isBlank()) AppScreen.EDIT.name else AppScreen.HOME.name)
    }
    var sharingEnabled by remember {
        mutableStateOf(repository.isNfcSharingEnabled() && controller.isHceAvailable)
    }
    val nfcEnabled = remember(systemStateRevision) { controller.isNfcEnabled }

    BackHandler(enabled = screen == AppScreen.EDIT.name) {
        screen = AppScreen.HOME.name
    }

    when (screen) {
        AppScreen.EDIT.name -> EditProfileScreen(
            initial = profile,
            onCancel = { screen = AppScreen.HOME.name },
            onSave = { updated ->
                repository.save(updated)
                profileRevision++
                screen = AppScreen.HOME.name
            }
        )

        else -> HomeScreen(
            profile = profile,
            controller = controller,
            nfcEnabled = nfcEnabled,
            sharingEnabled = sharingEnabled,
            onEdit = { screen = AppScreen.EDIT.name },
            onSharingChanged = { enabled ->
                repository.setNfcSharingEnabled(enabled)
                controller.setSharingEnabled(enabled)
                sharingEnabled = enabled
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    profile: ContactProfile,
    controller: NfcShareController,
    nfcEnabled: Boolean,
    sharingEnabled: Boolean,
    onEdit: () -> Unit,
    onSharingChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var showQr by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("NFC Vizitka") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Tvoja digitalna vizitka",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Kontakt deliš neposredno prek NFC ali QR kode. Internet ni potreben.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ProfileCard(profile = profile, onEdit = onEdit)

            NfcCard(
                hasProfile = profile.displayName.isNotBlank(),
                controller = controller,
                nfcEnabled = nfcEnabled,
                sharingEnabled = sharingEnabled,
                onSharingChanged = onSharingChanged,
                onOpenNfcSettings = {
                    context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                }
            )

            Button(
                onClick = { showQr = true },
                enabled = profile.displayName.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text("Prikaži QR kodo")
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Zasebnost", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Podatki ostanejo v telefonu. NFC se odziva samo, ko je deljenje vklopljeno, sistemski NFC aktiven in telefon odklenjen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showQr) {
        QrDialog(profile = profile, onDismiss = { showQr = false })
    }
}

@Composable
private fun ProfileCard(profile: ContactProfile, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initials(profile),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.displayName.ifBlank { "Vizitka še ni nastavljena" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitle = listOf(profile.jobTitle, profile.company)
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .joinToString(" • ")
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (profile.displayName.isNotBlank()) {
                HorizontalDivider()
                if (profile.phone.isNotBlank()) {
                    ProfileLine("Telefon", formatPhone(profile.phone))
                }
                if (profile.email.isNotBlank()) {
                    ProfileLine("E-pošta", profile.email)
                }
                if (profile.website.isNotBlank()) {
                    ProfileLine("Spletna stran", profile.website)
                }
            } else {
                Text(
                    "Vnesi vsaj ime ali priimek, nato lahko vklopiš NFC deljenje.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (profile.displayName.isBlank()) "Vnesi podatke" else "Uredi podatke")
            }
        }
    }
}

@Composable
private fun ProfileLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NfcCard(
    hasProfile: Boolean,
    controller: NfcShareController,
    nfcEnabled: Boolean,
    sharingEnabled: Boolean,
    onSharingChanged: (Boolean) -> Unit,
    onOpenNfcSettings: () -> Unit
) {
    val state = when {
        !controller.isNfcAvailable -> NfcUiState(
            title = "NFC ni na voljo",
            detail = "Ta telefon nima NFC strojne podpore.",
            active = false
        )
        !controller.isHceAvailable -> NfcUiState(
            title = "NFC deljenje ni podprto",
            detail = "Telefon nima Android HCE podpore. QR koda še vedno deluje.",
            active = false
        )
        !hasProfile -> NfcUiState(
            title = "Najprej vnesi kontakt",
            detail = "Za vklop NFC deljenja potrebuješ vsaj ime ali priimek.",
            active = false
        )
        !sharingEnabled -> NfcUiState(
            title = "Deljenje je izklopljeno",
            detail = "Vklopi stikalo, ko želiš telefon uporabljati kot NFC vizitko.",
            active = false
        )
        !nfcEnabled -> NfcUiState(
            title = "Sistemski NFC je izklopljen",
            detail = "Vizitka je pripravljena. Vklopi NFC v nastavitvah telefona.",
            active = false
        )
        else -> NfcUiState(
            title = "Pripravljeno za deljenje",
            detail = "Odkleni telefon in približaj hrbtni strani telefonov.",
            active = true
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Deljenje prek NFC",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Neposreden vCard kontakt",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = sharingEnabled,
                    enabled = controller.isHceAvailable && hasProfile,
                    onCheckedChange = onSharingChanged
                )
            }

            StatusPill(state)

            Text(
                text = state.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (controller.isNfcAvailable && !nfcEnabled) {
                OutlinedButton(
                    onClick = onOpenNfcSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Odpri NFC nastavitve")
                }
            }
        }
    }
}

private data class NfcUiState(
    val title: String,
    val detail: String,
    val active: Boolean
)

@Composable
private fun StatusPill(state: NfcUiState) {
    val color = if (state.active) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(100.dp),
        color = color.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = state.title,
                style = MaterialTheme.typography.labelLarge,
                color = color
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileScreen(
    initial: ContactProfile,
    onCancel: () -> Unit,
    onSave: (ContactProfile) -> Unit
) {
    var firstName by rememberSaveable(initial.firstName) { mutableStateOf(initial.firstName) }
    var lastName by rememberSaveable(initial.lastName) { mutableStateOf(initial.lastName) }
    var company by rememberSaveable(initial.company) { mutableStateOf(initial.company) }
    var jobTitle by rememberSaveable(initial.jobTitle) { mutableStateOf(initial.jobTitle) }
    var phone by rememberSaveable(initial.phone) { mutableStateOf(initial.phone) }
    var email by rememberSaveable(initial.email) { mutableStateOf(initial.email) }
    var website by rememberSaveable(initial.website) { mutableStateOf(initial.website) }
    var note by rememberSaveable(initial.note) { mutableStateOf(initial.note) }
    var nameError by rememberSaveable { mutableStateOf(false) }
    var emailError by rememberSaveable { mutableStateOf(false) }
    var websiteError by rememberSaveable { mutableStateOf(false) }

    fun save() {
        val normalizedWebsite = ContactFormat.normalizeWebsite(website)
        nameError = firstName.isBlank() && lastName.isBlank()
        emailError = email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
        websiteError = normalizedWebsite.isNotBlank() && !isValidHttpUrl(normalizedWebsite)

        if (nameError || emailError || websiteError) return

        onSave(
            ContactProfile(
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                company = company.trim(),
                jobTitle = jobTitle.trim(),
                phone = phone.trim(),
                email = email.trim(),
                website = normalizedWebsite,
                note = note.trim()
            )
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Uredi vizitko") },
                navigationIcon = {
                    TextButton(onClick = onCancel) { Text("Nazaj") }
                },
                actions = {
                    TextButton(onClick = { save() }) { Text("Shrani") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Kontaktni podatki",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Ime ali priimek sta obvezna. Ostala polja lahko pustiš prazna.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = firstName,
                onValueChange = {
                    if (it.length <= 80) firstName = it
                    if (it.isNotBlank()) nameError = false
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Ime") },
                isError = nameError,
                supportingText = if (nameError) {
                    { Text("Vpiši ime ali priimek.") }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = lastName,
                onValueChange = {
                    if (it.length <= 80) lastName = it
                    if (it.isNotBlank()) nameError = false
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Priimek") },
                isError = nameError,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = company,
                onValueChange = { if (it.length <= 120) company = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Podjetje") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = jobTitle,
                onValueChange = { if (it.length <= 120) jobTitle = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Delovno mesto") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { if (it.length <= 40) phone = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Telefon") },
                placeholder = { Text("+386 30 342 529") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                )
            )

            OutlinedTextField(
                value = email,
                onValueChange = {
                    if (it.length <= 254) email = it
                    emailError = false
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("E-pošta") },
                isError = emailError,
                supportingText = if (emailError) {
                    { Text("Preveri zapis e-poštnega naslova.") }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )

            OutlinedTextField(
                value = website,
                onValueChange = {
                    if (it.length <= 300) website = it
                    websiteError = false
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Spletna stran") },
                placeholder = { Text("www.example.si") },
                isError = websiteError,
                supportingText = {
                    Text(
                        if (websiteError) "Preveri spletni naslov."
                        else "Če izpustiš https://, ga aplikacija doda samodejno."
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                )
            )

            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 500) note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Opomba") },
                minLines = 3,
                maxLines = 5,
                supportingText = { Text("${note.length}/500") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
            )

            Button(
                onClick = { save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text("Shrani vizitko")
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun QrDialog(profile: ContactProfile, onDismiss: () -> Unit) {
    val vCard = remember(profile) { VCardBuilder.build(profile) }
    val bitmap = remember(vCard) { QrCodeGenerator.create(vCard, sizePx = 760) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) { Text("Zapri") }
        },
        title = { Text(profile.displayName.ifBlank { "Vizitka" }) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "QR koda z vCard kontaktom",
                        modifier = Modifier
                            .size(260.dp)
                            .padding(10.dp)
                    )
                }
                Text(
                    "Skeniraj s kamero. QR vsebuje isti vCard kontakt kot NFC in ne potrebuje interneta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

private fun initials(profile: ContactProfile): String {
    val first = profile.firstName.trim().firstOrNull()?.uppercaseChar()
    val last = profile.lastName.trim().firstOrNull()?.uppercaseChar()
    return listOfNotNull(first, last).joinToString("").ifBlank { "N" }
}

private fun formatPhone(value: String): String =
    PhoneNumberUtils.formatNumber(value, Locale.getDefault().country) ?: value

private fun isValidHttpUrl(value: String): Boolean = runCatching {
    val uri = Uri.parse(value)
    (uri.scheme.equals("http", ignoreCase = true) || uri.scheme.equals("https", ignoreCase = true)) &&
        !uri.host.isNullOrBlank()
}.getOrDefault(false)
