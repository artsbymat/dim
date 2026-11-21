# Dim

Dim meredupkan layar Android melebihi batas minimum slider kecerahan bawaan, terutama saat memakai layar AMOLED di ruangan gelap. Filter berupa lapisan hitam transparan dengan opasitas 0–90%.

## Cara pakai

1. Buka Dim dan ketuk **Buka pengaturan Aksesibilitas**. Aktifkan layanan **Dim · Filter layar** dan kembali ke aplikasi.
2. Ketuk **Aktifkan filter**. Geser slider **Opasitas filter** ke kiri untuk makin gelap, atau ke kanan untuk makin terang.
3. Buka menu edit Setelan Cepat dan tambahkan **Filter Dim** agar bisa menjeda dan menyalakan filter dari aplikasi lain.

Filter menggunakan overlay Aksesibilitas yang dirancang untuk mencakup panel notifikasi, Setelan Cepat, status bar, dan navigasi. Lapisan tidak mengambil fokus atau sentuhan. Cakupan panel dan navigasi masih perlu diverifikasi pada perangkat fisik dengan versi Android dan antarmuka produsen yang berbeda. [Dokumentasi tipe jendela Android](https://developer.android.com/reference/android/view/WindowManager.LayoutParams)

## Mode otomatis dan tangkapan layar

- **Dim otomatis** menyesuaikan redup berdasarkan sensor cahaya. Opasitas yang dipilih menjadi batas maksimum. Mode ini bisa dialihkan melalui aplikasi atau tile.
- **Tangkapan bersih** tersedia mulai Android 12. Tile menutup Setelan Cepat, menjeda overlay sementara, lalu menyimpan PNG di `Pictures/Dim`. Android dapat membatasi pengambilan layar yang dilindungi. [Dokumentasi AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)
- **Pulihkan tangkapan layar** membuat salinan PNG dari gambar yang dipilih. Masukkan opasitas saat gambar diambil untuk memperkirakan warna semula. Detail yang hilang akibat penggelapan tidak bisa dipulihkan sepenuhnya. Gambar dengan sisi lebih panjang dari 4.096 piksel dikecilkan saat dibaca.

Fitur dari bagian Premium Super Dim digunakan sebagai referensi. Dim belum menggunakan sistem pembayaran.

## Build

Buka proyek Kotlin dan Jetpack Compose ini di Android Studio dengan Android SDK 37. Modul `app` mendukung Android 8.0 (API 26) ke atas. Konfigurasi Gradle menggunakan JVM 25 untuk daemon.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

APK debug tersedia di `app/build/outputs/apk/debug/app-debug.apk`. Lihat [AGENTS.md](AGENTS.md) untuk pengujian di perangkat.

### Build rilis

Buat `keystore.properties` di root proyek dengan format berikut. Gunakan path absolut atau path relatif terhadap root proyek untuk `storeFile`. Isi `storeType` jika perlu menentukan format keystore, misalnya `PKCS12`.

```properties
storeFile=path/to/release.jks
storePassword=
keyAlias=
keyPassword=
```

```sh
./scripts/package-release.sh
```

APK dan AAB bertanda tangan tersedia di `app/build/outputs/apk/release/` dan `app/build/outputs/bundle/release/`. Skrip berhenti jika `keystore.properties` belum tersedia.

### GitHub Actions

Workflow **Android CI** menjalankan build debug, tes unit, dan lint pada push dan pull request. Push tag `v*` atau **Run workflow** juga membuat APK dan AAB bertanda tangan setelah pemeriksaan CI lulus. Unduh keduanya dari **Artifacts → dim-release** pada halaman run.

Tambahkan dua repository secrets di **Settings → Secrets and variables → Actions**.

- `ANDROID_KEYSTORE_BASE64` berisi hasil `base64 < path/to/release.jks`.
- `ANDROID_KEYSTORE_PROPERTIES` berisi baris `storePassword`, `keyAlias`, dan `keyPassword` dalam format yang sama dengan `keystore.properties`, beserta `storeType` jika diperlukan. Workflow mengisi `storeFile` sementara.

Tag memicu packaging. Versi aplikasi tetap mengikuti `versionCode` dan `versionName` di `app/build.gradle.kts`.
