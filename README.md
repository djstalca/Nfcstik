# NFC Vizitka

Android aplikacija za lokalno shranjevanje kontaktnih podatkov in neposredno deljenje kot vCard prek NFC ali QR kode.

## Funkcije

- vnos imena, priimka, podjetja, delovnega mesta, telefona, e-pošte, spletne strani in opombe
- lokalno shranjevanje v `SharedPreferences`
- Android Host Card Emulation (HCE), ki emulira NFC Forum Type 4 NDEF tag
- neposreden `text/vcard` zapis brez spletne strani ali strežnika
- QR koda z istim vCard kontaktom
- stikalo za vklop/izklop NFC deljenja
- `requireDeviceUnlock=true`, zato je telefon pri NFC deljenju odklenjen
- brez dovoljenja za internet

## Zahteve

- Android 8.0+ (`minSdk 26`)
- za NFC deljenje telefon z NFC in `android.hardware.nfc.hce`
- JDK 17
- Android SDK 37
- Gradle 9.6.0

## Gradnja

GitHub Actions ob vsakem pushu na `main` požene teste in izdela debug APK.

Lokalno lahko po namestitvi Gradle 9.6.0 poženeš:

```bash
gradle testDebugUnitTest assembleDebug
```

APK nastane v:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Test NFC

1. V aplikaciji vpiši kontakt in pritisni **Shrani podatke**.
2. Vključi **NFC deljenje**.
3. Preveri, da je sistemski NFC vključen in telefon odklenjen.
4. Hrbtno stran telefona približaj NFC anteni drugega telefona.
5. Prejemni telefon prebere emuliran Type 4 NDEF tag in dobi `text/vcard` zapis.

Obdelava vCard NDEF zapisov se lahko razlikuje med modeli telefonov, zato je QR koda vključena kot rezerva.

## Tehnična zasnova

- AID: `D2760000850101`
- CC file: `E103`
- NDEF file: `E104`
- Type 4 mapping: 2.0
- MIME: `text/vcard`
- vCard: 3.0
