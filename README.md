# NFC Vizitka

Android aplikacija za lokalno shranjevanje kontaktnih podatkov in neposredno deljenje kot vCard prek NFC ali QR kode.

## Funkcije

- čist začetni zaslon s predogledom vizitke in stanjem NFC
- ločen zaslon za urejanje podatkov z validacijo imena, e-pošte in spletnega naslova
- vnos imena, priimka, podjetja, delovnega mesta, telefona, e-pošte, spletne strani in opombe
- lokalno shranjevanje v `SharedPreferences`
- Android Host Card Emulation (HCE), ki emulira NFC Forum Type 4 NDEF tag
- neposreden `text/vcard` zapis brez spletne strani ali strežnika
- QR koda z istim vCard kontaktom
- stikalo za vklop/izklop NFC deljenja
- pregled sistemskega NFC stanja in bližnjica do NFC nastavitev
- `requireDeviceUnlock=true`, zato mora biti telefon pri NFC deljenju odklenjen
- Material 3, dinamične sistemske barve in podpora temnemu načinu
- prilagodljiva ikona aplikacije
- brez dovoljenja za internet

## Zahteve

- Android 8.0+ (`minSdk 26`)
- za NFC deljenje telefon z NFC in `android.hardware.nfc.hce`
- JDK 17
- Android SDK 36
- Gradle 9.6.0

## Gradnja

GitHub Actions ob vsakem pushu na `main` požene teste in izdela debug APK. Debug signing key se hrani v GitHub Actions cache, zato imajo zaporedni APK-ji z `main` isto podpisno identiteto, dokler je ta cache na voljo.

Lokalno lahko po namestitvi Gradle 9.6.0 poženeš:

```bash
gradle testDebugUnitTest assembleDebug
```

APK nastane v:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Test NFC

1. V aplikaciji vpiši kontakt in ga shrani.
2. Vključi **Deljenje prek NFC**.
3. Preveri, da je sistemski NFC vključen in telefon odklenjen.
4. Hrbtno stran telefona približaj NFC območju drugega telefona.
5. Prejemni telefon prebere emuliran Type 4 NDEF tag in dobi `text/vcard` zapis.

Obdelava vCard NDEF zapisov se lahko razlikuje med modeli telefonov, zato je QR koda vključena kot rezerva.

## Tehnična zasnova

- AID: `D2760000850101`
- CC file: `E103`
- NDEF file: `E104`
- Type 4 mapping: 2.0
- MIME: `text/vcard`
- vCard: 3.0
- `compileSdk` / `targetSdk`: 36
- Compose BOM: `2026.04.01`
- trenutna aplikacijska različica: `1.1.0` (`versionCode 2`)
