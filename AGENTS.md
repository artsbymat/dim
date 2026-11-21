# Panduan pengembangan Dim

## Proyek

- Gunakan Kotlin dan Jetpack Compose Material 3 pada modul `app`. Kode aplikasi berada di `app/src/main/java/com/example/dim`.
- Pertahankan dukungan API 26 ke atas. Periksa ketersediaan API sebelum memakai fitur yang diperkenalkan pada versi Android yang lebih baru.
- Gunakan tangkapan layar Super Dim sebagai referensi perilaku fitur. Buat antarmuka Dim dengan desain dan aset sendiri.

## Prioritas implementasi

1. Buat filter manual dengan slider opasitas, pratinjau, dan aksi jeda. Sinkronkan status filter pada layar aplikasi dan tile Setelan Cepat.
2. Buktikan cakupan panel notifikasi, Setelan Cepat, status bar, dan navigasi pada perangkat fisik sebelum menganggap peredupan antarmuka sistem selesai.
3. Tambahkan mode otomatis beserta tile pengalih, tile tangkapan layar tanpa filter, dan pemulihan tangkapan layar sesuai cakupan di [README.md](README.md). Kembangkan fungsi tersebut tanpa mengasumsikan model pembayaran.

## Overlay dan izin

- Gunakan lapisan gelap yang tidak mengambil fokus atau menerima sentuhan. Selalu sediakan cara menjeda filter dan batasi opasitas agar kontrol tetap bisa terbaca.
- Uji `TYPE_ACCESSIBILITY_OVERLAY` melalui `AccessibilityService` untuk cakupan sistem. Jelaskan tujuannya sebelum mengarahkan pengguna ke pengaturan Aksesibilitas. Peredupan panel hanya memerlukan penggambaran lapisan. Jangan meminta akses isi notifikasi atau hierarki tampilan untuk fitur ini. [Dokumentasi AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)
- Untuk peredupan di atas aplikasi biasa, `TYPE_APPLICATION_OVERLAY` bisa dipertimbangkan jika akses Aksesibilitas tidak aktif. Patuhi batas opasitas Android untuk penerusan sentuhan dan tampilkan status cakupan sistem yang sebenarnya. Pastikan hanya satu overlay Dim aktif pada satu waktu. [Dokumentasi WindowManager.LayoutParams](https://developer.android.com/reference/android/view/WindowManager.LayoutParams)
- Lepas overlay saat filter dijeda, izin dicabut, atau layanan berhenti. Jika tangkapan layar bersih memakai jeda sementara, pulihkan filter setelah pengambilan selesai, dibatalkan, atau gagal. Hormati pembatasan tangkapan layar Android.

## Verifikasi

- Jalankan `./gradlew :app:assembleDebug :app:testDebugUnitTest` setelah mengubah kode.
- Pada perangkat AMOLED fisik, atur kecerahan sistem ke minimum dan pastikan slider Dim masih memberikan redup tambahan yang terlihat.
- Saat filter aktif, tarik panel notifikasi lalu perluas Setelan Cepat. Periksa bahwa keduanya tetap redup saat dibuka, dan uji ketuk notifikasi, tile jeda, gestur, serta navigasi tiga tombol jika tersedia.
- Uji perubahan orientasi, jeda dan lanjut, serta pencabutan akses Aksesibilitas saat filter aktif. Catat model perangkat, versi Android, dan area sistem yang berhasil diredupkan.
