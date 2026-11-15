# Personal Phone Monitor V2

Proyek Android Kotlin untuk perangkat yang dimiliki/dikelola dengan izin jelas. Aplikasi tetap memiliki ikon dan layar pengaturan yang terlihat; tidak ada mode sembunyi atau penghalang uninstall.

## Konfigurasi Worker
- Worker URL default: `https://personal-phone-monitor.rosadahwk.workers.dev`
- Device ID: dari dashboard Cloudflare saat mendaftarkan perangkat.
- Device Token: token perangkat dari dashboard. Masukkan langsung di aplikasi; token dienkripsi memakai Android Keystore.

## Build APK termudah dari HP Android (GitHub Actions)
1. Unduh ZIP proyek ini dan ekstrak di HP.
2. Buat akun/masuk ke GitHub.
3. Buat repository baru, misalnya `PersonalPhoneMonitorV2`.
4. Upload seluruh isi folder proyek (bukan folder ZIP sebagai satu file) ke repository. Pastikan `.github/workflows/build-apk.yml` ikut terunggah.
5. Buka tab **Actions** di repository. Jika diminta, pilih **I understand my workflows, go ahead and enable them**.
6. Pilih workflow **Build Android APK**, lalu tekan **Run workflow**. Jika workflow belum terlihat, refresh halaman.
7. Tunggu sampai proses berstatus hijau. Buka hasil run, cari **Artifacts**, unduh `PersonalPhoneMonitorV2-debug`, lalu ekstrak.
8. Instal `app-debug.apk`. Android mungkin meminta izin instalasi dari browser/Files; izinkan hanya untuk aplikasi yang kamu gunakan untuk membuka file.

## Pengaturan di Android
1. Buka aplikasi Personal Phone Monitor.
2. Masukkan Worker URL, Device ID, dan Device Token.
3. Tekan **Simpan konfigurasi**. Token disimpan terenkripsi dengan Android Keystore.
4. Tekan **Aktifkan akses notifikasi**. Di pengaturan Android, aktifkan akses untuk Personal Phone Monitor setelah membaca penjelasan izin.
5. Tekan **Tes koneksi dan aktifkan heartbeat**.
6. Periksa dashboard. Heartbeat dijadwalkan menggunakan WorkManager dengan interval minimum 15 menit; Android dapat menunda pekerjaan karena penghematan baterai.
7. Jika ingin berhenti, tekan **Hentikan heartbeat** dan cabut Notification Access melalui Pengaturan Android.

## Catatan kompatibilitas API
Implementasi ini memakai:
- `POST /api/heartbeat` dengan Bearer Device Token dan JSON `device_id`, `battery_level`, `is_charging`, `network_type`.
- `POST /api/notifications` dengan Bearer Device Token dan JSON `device_id`, `app_name`, `package_name`, `title`, `content`.

Sesuaikan payload jika Worker yang aktif memakai nama field atau autentikasi berbeda. Build berhasil tidak otomatis membuktikan endpoint cocok; uji koneksi dan periksa dashboard.

## Privasi dan batasan
- Notification Access dapat membaca konten notifikasi yang Android/aplikasi sumber tampilkan. Notifikasi tersembunyi/dirahasiakan tidak dapat dipulihkan.
- Jangan pasang pada perangkat orang lain tanpa sepengetahuan dan izin yang sesuai.
- Jangan mengirim OTP, kode keamanan, atau notifikasi pribadi yang tidak diperlukan.
- Aplikasi tidak menyembunyikan ikon, tidak mencegah uninstall, dan tidak mengambil alih kontrol perangkat.
- APK debug cocok untuk uji coba. Untuk distribusi jangka panjang, buat signing release key sendiri dan simpan aman.
