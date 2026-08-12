# Signal App — XAUUSD (ADMIN / PREMIUM / USER) #

Aplikasi Android sederhana untuk distribusi sinyal trading XAUUSD dengan 3 role:

- **ADMIN** — publish & kelola sinyal (TP/SL/BE/Cancel), generate kode langganan
- **PREMIUM** — lihat semua sinyal secara penuh (selama belum expired)
- **USER** — baru daftar, sinyal terlihat "terkunci"/blur, wajib redeem kode dari admin untuk naik jadi PREMIUM

Stack: **Kotlin + Jetpack Compose + Firebase (Auth + Firestore)**, di-build otomatis lewat **GitHub Actions** (tidak perlu install Android Studio).

---

## V24.5 — Navigasi Samping & Mobile UI

Penyempurnaan tampilan untuk HP berdasarkan basis V24.4:
- Menu utama User/Premium dapat dibuka dari ikon **☰** di kiri atas.
- Drawer berisi **Sinyal** dan **Profil**.
- Bagian bawah drawer menampilkan email, role aktif, dan tombol keluar.
- Drawer tetap nyaman di HP dan tablet, sementara fitur V24 Premium Push/Cloudflare Worker, paket, voucher, referral, approval, dan pencarian admin dipertahankan.

## V24.4 — Panel Sinyal Administrator Lebih Ringkas

Panel **Sinyal** di Administrator sekarang lebih responsif dan mudah dipakai saat jumlah sinyal sudah banyak:

- Pencarian cepat berdasarkan **pair, arah, status, atau catatan**.
- Filter arah **Semua / BUY / SELL**.
- Filter status **Semua / Aktif / TP Hit / SL Hit / BE / Batal**.
- Daftar admin otomatis diringkas menjadi **8 sinyal terbaru** agar tidak memanjang tanpa batas.
- Tombol **Lihat semua / Ringkas** tersedia saat hasil filter lebih dari 8 sinyal.
- Hasil selalu diurutkan dari sinyal terbaru.
- Ringkasan performa mengikuti hasil filter sehingga admin bisa langsung melihat statistik subset yang sedang dipilih.
- Tidak mengubah jalur Firestore maupun Cloudflare Worker/FCM yang sudah digunakan untuk notifikasi Premium.

## V24.3 — Riwayat Sinyal Ringkas & Responsive UI

Perbaikan UI berfokus pada penggunaan harian di HP sekaligus tetap nyaman di tablet, landscape, split-screen, dan layar yang lebih besar.

- **Riwayat sinyal tidak lagi memanjang panjang ke bawah.** Beranda hanya menampilkan **5 riwayat terbaru** dalam bentuk compact list.
- Tombol **Lihat semua riwayat** membuka dialog yang dapat di-scroll, sehingga seluruh riwayat tetap tersedia tanpa membuat halaman utama terlalu panjang.
- Riwayat Premium menampilkan informasi ringkas: arah, pair, status, Entry/TP/SL, dan waktu.
- Riwayat USER tetap terkunci dengan tampilan ringkas dan tidak membocorkan detail harga.
- **Kartu sinyal aktif adaptif**: lebar kartu mengikuti ukuran layar, dengan batas nyaman agar tidak terlalu kecil di HP atau terlalu melebar di tablet.
- Judul bagian dibuat lebih bersih dan profesional tanpa emoji dekoratif yang berlebihan.
- Status pada daftar ringkas menggunakan label pendek (**Aktif, TP, SL, BE, Batal**) agar mudah dipindai.
- Performa, banner Premium, profil, dan navigasi tetap memakai `AdaptiveAppFrame`, sehingga perubahan ini tidak mengubah alur Firebase/Auth/FCM yang sudah bekerja.

### Pola tampilan baru

```text
Beranda
├── Sinyal Aktif       → horizontal, responsif
├── Status Premium     → compact
├── Performa           → ringkas
└── Riwayat Sinyal     → 5 item terbaru
    └── Lihat semua    → dialog scrollable
```

## Update terbaru

- **V24 — Premium Push tanpa Firebase Cloud Functions** — jalur push real-time dipindahkan ke Cloudflare Worker yang aman. Admin Panel mengirim Firebase ID token + event ke Worker, Worker memverifikasi UID admin lalu mengirim FCM HTTP v1 langsung ke topic `premium_signals`. **FCM tetap gratis dan project Firebase tidak perlu di-upgrade ke Blaze hanya untuk push.**
- **V24 — Push low-latency** — tidak menggunakan polling Firestore. Setelah Firestore berhasil menyimpan sinyal, Android langsung memanggil webhook; target jalur server adalah hitungan detik, walaupun waktu tampil di perangkat tetap dipengaruhi jaringan/Android.
- **V23/V22 — Premium Push Android** — subscription topic `premium_signals`, notification channel prioritas tinggi, dan handling role Premium tetap dipertahankan.

- **V21 — Penyempurnaan Bahasa UI & Peran** — seluruh teks yang tampil kepada pengguna diseragamkan ke Bahasa Indonesia yang lebih profesional, jelas, dan mudah dipahami. Terminologi untuk **Pengguna, Premium, dan Administrator** diperjelas, termasuk status pesanan, persetujuan langganan, pencarian pengguna, profil, referal, voucher, dan tindakan di Panel Administrator. Istilah teknis trading seperti **BUY, SELL, Entry, TP, SL, BE, dan UID** tetap dipertahankan agar tidak mengubah makna teknis.


- **V20 — Responsive / Adaptive UI** — seluruh navigation/content sekarang memakai `AdaptiveAppFrame` berbasis `available window width`. Layout otomatis menyesuaikan HP, tablet, landscape, split-screen, dan foldable dengan breakpoint 600dp/840dp serta batas lebar content agar layar besar tidak terlihat terlalu melebar.

- **Fix error compile setelah UI premium** — dua bug Kotlin yang bikin GitHub Actions gagal build (`compileDebugKotlin FAILED`):
  1. `PerformanceSummaryCard.kt` — pemanggilan `Modifier.background(...)` di selector periode mencampur tipe `Brush` dan `Color` dalam satu `if/else`, yang bikin compiler bingung pilih overload. Diperbaiki dengan menyamakan tipe (pakai `SolidColor` transparan untuk kondisi tidak terpilih).
  2. `Theme.kt` — pemakaian `Typography.merge(...)`, yang sebenarnya fungsi Material 2 dan tidak ada di Material 3. Diganti jadi pemakaian langsung `Typography` custom tanpa merge.
- **UI dibuat lebih premium (gold-on-navy)** — tema warna diganti total dari palet Material default jadi nuansa emas di atas navy gelap ala kartu member VIP. Perubahan meliputi:
  - Halaman **Login & Register**: logo bulat bergradasi emas, card "kaca" gelap dengan shadow, input field dengan ikon, tombol utama emas bergradasi.
  - **Card Performa**: gradient gelap, selector periode custom, angka-angka winrate/pip diberi warna aksen (emas/hijau/merah).
  - **Kartu sinyal** (Premium & User): garis aksen kiri hijau/merah sesuai BUY/SELL, badge status transparan berwarna, tampilan lock/blur untuk USER lebih halus.
  - **Halaman Profil**: avatar dengan ring gradasi emas, badge role berwarna, tombol keluar lebih tegas.
  - **Admin Panel**: tab & top bar diberi aksen emas, tombol publish jadi tombol emas, kode langganan baru ditonjolkan dalam card khusus.
  - Warna splash/status bar native juga disesuaikan jadi navy gelap supaya tidak ada "kedipan" putih sebelum app selesai render.
- **Menu Profil untuk ADMIN, PREMIUM, dan USER** — tiap role sekarang punya tab/menu "Profil" (Admin: tab ke-5 di Admin Panel; Premium & User: bottom navigation "Sinyal"/"Profil") yang menampilkan email, role aktif, tanggal premium expired (untuk Premium), tanggal bergabung, dan tombol **Keluar dari Akun** yang jelas.
- **Fix bug tombol "Keluar" (logout) yang bikin app crash/layar putih** — sebelumnya, semua listener real-time ke Firestore (data user, daftar sinyal, daftar kode, daftar semua user di panel Admin) tetap aktif sesaat setelah sesi login diputus. Firestore lalu membalas listener-listener itu dengan error *permission-denied*, dan error itu tidak ditangkap sehingga bikin aplikasi crash sebelum sempat pindah ke halaman Login (baru bisa login lagi setelah app ditutup manual dari recent apps). Sekarang semua listener tersebut menangkap error itu dengan aman, jadi begitu tombol **Keluar** dipencet, aplikasi langsung balik ke menu Login dari role ADMIN/PREMIUM/USER tanpa nge-freeze/crash.

---

## Update terbaru — Referral & Welcome Voucher

Versi ini menambahkan **program referral** end-to-end:

- Setiap akun punya **kode referral pribadi** format `SGXXXXXXXX`.
- Saat register, user bisa memasukkan kode referral teman.
- Teman baru yang mendaftar memakai kode referral otomatis mendapat **voucher welcome** sesuai persentase di Admin Panel. Voucher tampil di Profil dan dapat dimasukkan saat checkout paket.
- **Bonus referral aktif setelah teman benar-benar berlangganan**, yaitu setelah kode langganan berhasil diredeem.
- Referrer otomatis mendapat bonus Premium sesuai pengaturan admin (default **+2 hari**). Kalau referrer sedang Premium, bonus ditambahkan ke expiry yang masih aktif; kalau sudah USER/expired, role diaktifkan kembali menjadi PREMIUM selama durasi bonus.
- Satu teman hanya menghasilkan **satu reward referral**, walaupun teman tersebut memperpanjang Premium lagi di kemudian hari.
- Profil menampilkan kode referral, voucher welcome, jumlah referral yang berhasil, dan total hari bonus yang terkumpul.
- **Custom Referral dari Admin Panel** — admin sekarang punya tab **Referral** untuk mengubah jumlah hari bonus Premium, persentase voucher welcome, dan mengaktifkan/nonaktifkan program referral tanpa mengubah kode aplikasi.
- Logika pemberian bonus dijalankan oleh **Cloud Function** agar reward tidak bergantung pada client Android dan dibuat idempotent untuk mencegah bonus dobel. Cloud Function membaca konfigurasi terbaru dari `appSettings/referral`.

> **Catatan voucher:** pembayaran masih manual. User memasukkan voucher saat checkout, sistem menghitung harga diskon dan menyimpan harga final pada `subscriptionOrders`. Admin tetap memverifikasi pembayaran secara manual sebelum APPROVE.

### Alur referral

```text
USER A
  │
  ├─ Bagikan kode: SGXXXXXXXX
  │
  ▼
USER B daftar + memasukkan kode referral
  │
  ├─ Voucher welcome 10% dibuat otomatis
  │
  ▼
USER B berlangganan / redeem kode Premium
  │
  ├─ lastSubscriptionActivatedAt dicatat
  ▼
Cloud Function mendeteksi aktivasi
  │
  ├─ USER A +2 hari Premium
  └─ referral B ditandai sudah mendapat reward
```

### Custom Referral dari Admin Panel

Admin dapat membuka **Admin Panel → Referral** dan mengubah:

- **Program referral**: ON/OFF.
- **Bonus Premium untuk referrer (hari)**: default `2`, bisa diubah dari `0` sampai `365`.
- **Voucher welcome (%)**: default `10%`, bisa diubah dari `0%` sampai `100%`.

Konfigurasi disimpan di Firestore:

```text
appSettings/referral
  enabled: boolean
  rewardPremiumDays: number
  welcomeVoucherPercent: number
```

Perubahan **rewardPremiumDays** langsung dipakai Cloud Function saat referral baru berhasil berlangganan. Perubahan **welcomeVoucherPercent** dipakai untuk user baru yang mendaftar setelah pengaturan berubah; voucher yang sudah dibuat sebelumnya tidak diubah otomatis.

### Deploy Cloud Functions referral

Setelah project Firebase sudah terhubung, deploy Functions agar bonus referral benar-benar aktif:

```bash
cd functions
npm install
cd ..
firebase deploy --only functions
```

Jika Firebase CLI belum terpasang, gunakan Firebase CLI sesuai environment yang kamu pakai. **Cloud Function `onReferralSubscriptionActivated` wajib ter-deploy** karena fungsi inilah yang memberi bonus 2 hari secara server-side.

## V24 — Premium Push tanpa Blaze (Cloudflare Worker)

V24 mengganti ketergantungan push Premium pada **Firebase Cloud Functions**. Ini dibuat khusus supaya SevenGold tetap bisa memakai FCM tanpa meng-upgrade Firebase ke Blaze hanya untuk notifikasi.

### Arsitektur

```text
Admin Panel
    │
    ├─ Firestore write (sumber data utama)
    │
    └─ HTTPS webhook + Firebase ID token
             │
             ▼
      Cloudflare Worker
      - verifikasi token Firebase
      - cek UID ADMIN
      - OAuth service account
             │
             ▼
       FCM HTTP v1
             │
             ▼
      topic: premium_signals
             │
             ▼
       Device PREMIUM
```

**Tidak ada credential FCM di APK.** APK hanya mengetahui URL Worker dan mengirim Firebase ID token milik admin yang sedang login. Private key service account disimpan sebagai **Worker Secret**.

### 1. Siapkan Cloudflare Worker

Masuk ke folder:

```bash
cd push-worker
npm install
npx wrangler login
```

Deploy Worker setelah mengisi secret berikut:

```bash
npx wrangler secret put FIREBASE_PROJECT_ID
npx wrangler secret put ADMIN_UIDS
npx wrangler secret put FIREBASE_SERVICE_ACCOUNT_JSON
npx wrangler deploy
```

- `FIREBASE_PROJECT_ID`: project ID Firebase SevenGold.
- `ADMIN_UIDS`: UID administrator yang boleh mengirim push. Jika lebih dari satu, pisahkan dengan koma.
- `FIREBASE_SERVICE_ACCOUNT_JSON`: **isi JSON service account Firebase/Google Cloud** yang mempunyai izin mengirim FCM. Jangan commit file JSON tersebut ke GitHub.

Worker akan memberikan URL seperti:

```text
https://sevengold-premium-push.<subdomain>.workers.dev
```

URL inilah yang dipakai aplikasi Android.

> **Keamanan:** jangan menaruh `FIREBASE_SERVICE_ACCOUNT_JSON`, private key, atau GitHub/Cloudflare token di aplikasi Android. Secret hanya boleh berada di Worker/secret manager.

### 2. Pastikan FCM API dan service account siap

Di Google Cloud/Firebase project yang sama, pastikan **Firebase Cloud Messaging API** aktif. Service account yang dipakai Worker harus memiliki izin untuk mengirim pesan FCM.

FCM tidak mengenakan biaya per pesan. Yang dipindahkan keluar dari Firebase adalah proses backend push-nya.

### 3. Hubungkan URL Worker ke Android build

Tambahkan GitHub Actions repository secret:

```text
SEVENGOLD_PUSH_WEBHOOK_URL = https://sevengold-premium-push.<subdomain>.workers.dev
```

Workflow `Build APK` sudah meneruskan secret ini sebagai Gradle property:

```text
-PSEVENGOLD_PUSH_WEBHOOK_URL=...
```

Untuk build lokal, bisa memakai:

```bash
gradle assembleDebug -PSEVENGOLD_PUSH_WEBHOOK_URL="https://...workers.dev"
```

### 4. Event yang dikirim

- `SIGNAL_CREATED` — admin menerbitkan sinyal baru.
- `SIGNAL_ACTIVE` — status sinyal diaktifkan kembali.
- `TP_HIT` — TP tercapai.
- `SL_HIT` — SL tercapai.
- `BE` — Break Even.
- `CANCELLED` — sinyal dibatalkan.

FireStore tetap menjadi sumber data utama. Jika Worker sedang down, data sinyal **tetap tersimpan**; UI akan memberi pesan bahwa data tersimpan tetapi notifikasi belum terkirim.

### 5. Perkiraan latency

Tidak memakai polling 1–5 menit. Jalurnya langsung:

```text
Admin klik → Firestore → Worker → FCM → Device
```

Target normal adalah **hitungan detik**, tetapi tidak ada jaminan waktu tampil yang absolut karena jaringan, cold start Worker, FCM, koneksi perangkat, battery optimization, dan Doze Android dapat memengaruhi delivery.

Untuk sinyal BUY/SELL LIMIT TF 30 menit, desain ini jauh lebih sesuai dibanding polling berkala.

### 6. Tes setelah deploy

1. Install APK hasil GitHub Actions setelah secret `SEVENGOLD_PUSH_WEBHOOK_URL` ditambahkan.
2. Login sebagai Premium di HP target dan pastikan izin notifikasi aktif.
3. Login sebagai Administrator di device admin.
4. Terbitkan sinyal baru.
5. Periksa Logcat dengan tag `PremiumPush`.
6. Uji **TP Hit**, **SL Hit**, **Set BE**, dan **Batalkan** dari Admin Panel.

Jika Worker membalas `403`, UID admin belum dimasukkan ke `ADMIN_UIDS`. Jika `401`, Firebase ID token tidak valid/expired. Jika `500` saat FCM, periksa service account dan izin FCM.

> **Catatan:** folder `functions/` pada project masih dipertahankan karena fitur referral dan approval langganan lama menggunakannya. **V24 tidak memerlukan Cloud Functions untuk push Premium.** Jika fitur referral/approval server-side tersebut tetap dipakai, Cloud Functions masih perlu di-deploy dan Firebase plan yang diperlukan mengikuti persyaratan Firebase untuk Functions.

## V23 — Premium Push Notification Fix

Versi ini memperbaiki alur push agar lebih mudah didiagnosis dan lebih kompatibel dengan konfigurasi FCM Android.

Perbaikan utama:
- Cloud Function sekarang mengirim konfigurasi **Android high priority**, channel `premium_signals`, dan `sound: default`.
- Setiap pengiriman FCM dicatat ke Cloud Functions log dengan nama event dan hasil `messageId`. Jika FCM gagal, error juga tercatat.
- Perubahan status **ACTIVE** pada dokumen `signals` juga dianggap sebagai sinyal aktif dan memicu notifikasi, bukan hanya pembuatan dokumen baru.
- Android mencatat hasil subscribe/unsubscribe topic ke Logcat (`PremiumPush`), sehingga bisa dipastikan apakah device benar-benar sudah masuk topic `premium_signals`.

### Wajib setelah update V23

Deploy Cloud Functions dari folder project:

```bash
cd functions
npm install
cd ..
firebase deploy --only functions
```

Kemudian login sebagai **Premium**, pastikan izin notifikasi Android aktif, dan lihat Logcat dengan tag `PremiumPush`. Harus muncul `Berhasil subscribe topic premium_signals`. Setelah itu publish sinyal baru dari Panel Administrator.

> **Penting:** ZIP/source code tidak dapat otomatis mengaktifkan Cloud Functions di project Firebase kamu. Deployment Functions tetap wajib dilakukan ke Firebase project yang sama dengan aplikasi Android.

## V22 — Premium Push Notification Otomatis & Reliable

Notifikasi sinyal Premium sekarang disinkronkan otomatis berdasarkan status Premium yang aktif.

### Perilaku notifikasi

- **PREMIUM aktif** → perangkat otomatis subscribe ke topic FCM `premium_signals`.
- **USER / Premium expired / ADMIN** → perangkat otomatis unsubscribe dari topic tersebut.
- Jika masa Premium habis ketika aplikasi tetap terbuka, aplikasi menjadwalkan unsubscribe tepat setelah waktu expiry.
- Saat login atau profil user berubah, status subscription FCM disinkronkan kembali secara real-time.
- Saat logout, perangkat langsung unsubscribe dari topic Premium.
- Notifikasi dapat diterima saat aplikasi berada di **background** atau **ditutup**, selama izin notifikasi Android aktif dan Firebase Cloud Functions sudah ter-deploy.
- Event yang dikirim ke Premium meliputi **Sinyal Baru, TP HIT, SL, Break Even, dan Sinyal Dibatalkan**.
- User biasa tidak menerima topic `premium_signals`.

### Alur

```text
Admin publish/update sinyal
        │
        ▼
Firestore /signals
        │
        ▼
Cloud Functions
        │
        ▼
FCM topic: premium_signals
        │
        ├── PREMIUM aktif → menerima notif
        └── USER / expired / ADMIN → tidak subscribe
```

### Syarat agar push benar-benar muncul

1. Firebase Cloud Messaging aktif pada project Firebase.
2. Android 13+ sudah memberikan izin **Notifikasi**.
3. Cloud Functions sudah di-deploy:
   ```bash
   cd functions
   npm install
   cd ..
   firebase deploy --only functions
   ```
4. Device Premium sudah login dan profil user terbaca sebagai `PREMIUM` dengan `premiumExpiryMillis` yang masih aktif.
5. Device memiliki koneksi internet.

> **Catatan:** metode topic messaging ini sangat cocok untuk broadcast sinyal Premium. Sinkronisasi role dilakukan dari Android berdasarkan data Firestore. Jika akun sudah expired ketika aplikasi benar-benar tidak pernah dibuka lagi setelah expiry, perangkat baru melakukan unsubscribe saat aplikasi kembali aktif; untuk kontrol expiry server-side yang lebih ketat per-device diperlukan arsitektur FCM token registry.

## 1. Setup Firebase

1. Buka https://console.firebase.google.com → **Add project** → beri nama bebas (mis. `signal-app`).
2. Di dashboard project → klik ikon **Android** → tambahkan aplikasi:
   - Package name: `com.sevengold.signalapp` (harus persis sama, ini dipakai di `app/build.gradle.kts`)
   - Download file **`google-services.json`** yang diberikan (jangan taruh di folder project dulu, simpan di tempat aman).
3. Aktifkan **Authentication**:
   - Menu **Build → Authentication → Get started**
   - Tab **Sign-in method** → aktifkan **Email/Password**
4. Aktifkan **Firestore Database**:
   - Menu **Build → Firestore Database → Create database**
   - Pilih **Production mode** (aturan keamanan sudah disiapkan di `firestore.rules`)
   - Setelah dibuat, buka tab **Rules**, copy-paste isi file `firestore.rules` dari project ini, lalu **Publish**.

---

## 2. Push kode ke GitHub

1. Buat repository baru di GitHub (public/private bebas).
2. Upload seluruh isi folder project ini ke repo (lewat GitHub web editor / drag-drop upload, sesuai cara kerjamu — tidak perlu terminal).
3. **Jangan** upload file asli `google-services.json` ke repo (sudah otomatis di-ignore lewat `.gitignore`). Kita kirim lewat GitHub Secret di langkah berikutnya.

---

## 3. Setup GitHub Secret (biar Actions bisa build)

1. Ubah `google-services.json` yang tadi didownload jadi teks base64:
   - Cara termudah: buka https://www.base64encode.org → upload/paste isi file → encode → copy hasilnya.
2. Di repo GitHub → **Settings → Secrets and variables → Actions → New repository secret**
   - Name: `GOOGLE_SERVICES_JSON_BASE64`
   - Value: paste hasil base64 tadi
3. Simpan.

> Catatan soal `FIREBASE_SERVICE_ACCOUNT_JSON`: file ini dipakai untuk **Admin SDK di sisi server** (misalnya Cloud Function atau script backend), **bukan** dipakai langsung di dalam aplikasi Android. Simpan sebagai secret terpisah kalau nanti kamu bikin backend tambahan — jangan pernah dimasukkan ke dalam kode aplikasi Android.

---

## 4. Jalankan build

1. Push commit apapun ke branch `main` (atau buka tab **Actions** di repo → pilih workflow **Build APK** → **Run workflow** manual).
2. Tunggu sampai selesai (2–5 menit).
3. Buka run yang sukses → scroll ke bagian **Artifacts** → download `signal-app-debug` → isinya `app-debug.apk`.
4. Kirim APK itu ke HP Android (via WhatsApp/Drive/dsb), install seperti biasa (izinkan "install dari sumber tidak dikenal" kalau diminta).

---

## 5. Jadikan diri kamu ADMIN pertama kali

Karena setiap akun baru otomatis dibuat dengan role `USER`, ADMIN pertama harus di-set manual:

1. Register akun baru lewat aplikasi (email + password).
2. Buka Firebase Console → **Firestore Database** → collection `users` → cari dokumen dengan UID akun kamu.
3. Ubah field `role` dari `"USER"` menjadi `"ADMIN"`.
4. Buka lagi aplikasinya (atau tunggu sebentar) — otomatis pindah ke Admin Panel tanpa perlu logout, karena role di-listen secara real-time.

---

## Struktur data Firestore

```
users/{uid}
  email: string
  role: "ADMIN" | "PREMIUM" | "USER"
  premiumExpiryMillis: number | null   // timestamp kapan premium berakhir
  createdAt: number
  referralCode: string
  referredByUid: string | null
  referralRewardGranted: boolean
  referralSuccessfulCount: number
  referralRewardDaysEarned: number
  welcomeVoucherCode: string
  welcomeVoucherPercent: number
  welcomeVoucherUsed: boolean
  lastSubscriptionActivatedAt: number | null

referralCodes/{referralCode}
  uid: string
  createdAt: number

signals/{signalId}
  pair: "XAUUSD"
  type: "BUY" | "SELL"
  entry, tp, sl: number
  status: "ACTIVE" | "BE" | "CANCELLED" | "CLOSED"
  note: string
  createdBy: uid
  createdAt: number

subscriptionCodes/{code}
  code: string           // juga jadi document ID, mis. "K7X9QF2A"
  durationDays: number
  isUsed: boolean
  usedByUid: string | null
  createdBy: uid (admin)
  createdAt: number

appSettings/referral
  enabled: boolean
  rewardPremiumDays: number
  welcomeVoucherPercent: number
```

## Alur langganan

1. ADMIN buka tab **Kode** di Admin Panel → isi durasi (hari) → **Buat** → dapat kode acak 8 karakter.
2. Kode itu dikirim manual ke user (WhatsApp/dsb) — misalnya setelah user transfer pembayaran ke admin.
3. USER buka tombol **Masukkan Kode Langganan** → input kode → kalau valid, role otomatis naik jadi `PREMIUM` dan `premiumExpiryMillis` di-set.
4. Kalau user (yang sudah PREMIUM) redeem kode lain lagi sebelum expired, durasinya **ditambahkan** ke sisa waktu yang ada (bukan menimpa).
5. Saat redeem berhasil, field `lastSubscriptionActivatedAt` dicatat. Jika akun punya `referredByUid`, Cloud Function memproses bonus referral.
6. Kalau `premiumExpiryMillis` sudah lewat, aplikasi otomatis menganggap dia balik jadi tampilan USER lagi (dicek di `AppUser.effectiveRole`), tanpa perlu admin turunkan manual.

## Catatan keamanan (penting dibaca sebelum production)

Versi ini punya batasan yang wajar untuk MVP tapi perlu kamu tahu:

- **Blur sinyal untuk USER dilakukan di sisi aplikasi (client-side)**, bukan di server. Artinya data sinyal sebenarnya tetap terkirim ke HP semua user yang login, cuma disembunyikan di tampilan. Untuk keamanan lebih ketat (data premium benar-benar tidak sampai ke device non-premium), langkah lanjutannya adalah pindahkan pembacaan sinyal ke **Cloud Function** yang mengecek role user dulu sebelum mengirim data — bukan baca langsung dari Firestore di client.
- Redeem kode saat ini dieksekusi dari sisi client lewat Firestore transaction. Ini cukup aman untuk mencegah satu kode dipakai dua kali secara bersamaan, tetapi untuk production yang lebih ketat logika redeem juga sebaiknya dipindahkan ke Cloud Function.
- **Reward referral sudah dipindahkan ke Cloud Function** sehingga referrer tidak bisa sekadar mengubah UI/client untuk mengklaim bonus 2 hari. Sistem memakai flag `referralRewardGranted` agar reward per teman hanya diberikan sekali.
- Login Google/provider lain (yang disebut untuk fase berikutnya) belum diimplementasikan di versi ini — baru email/password sesuai permintaan awal.

Kalau nanti mau lanjut ke tahap Cloud Functions atau login Google, tinggal bilang — bisa dilanjutkan dari fondasi ini.

## Fix build terbaru

- Memperbaiki error Kotlin pada `AdminPanelScreen.kt` yang menyebabkan `compileDebugKotlin` gagal dengan pesan `Smart cast to 'String' is impossible` pada state `message`.
- Nilai `message` sekarang disalin ke local `currentMessage` sebelum dipakai di `startsWith`, sehingga aman untuk property yang berasal dari `StateFlow`/custom getter.
- Custom Referral tetap menggunakan `appSettings/referral`, sehingga admin dapat mengubah bonus Premium referrer dan persentase voucher welcome tanpa mengubah source code.



### Referral login fix
- Fixed `PERMISSION_DENIED` on login caused by `ensureReferralData()` attempting to overwrite an existing `referralCodes/{code}` document.
- Existing referral code documents are now read and preserved; a new document is only created when missing.
- Firestore rules also verify that a referral code can only be created/updated by its owner and its UID cannot be changed.

## Paket Langganan + Approval Manual (MVP)

Sistem langganan sekarang memakai paket resmi dan approval admin manual. Paket default:

| Paket | Harga | Durasi |
|---|---:|---:|
| Starter | Rp10.000 | 7 hari |
| Basic | Rp15.000 | 10 hari |
| Pro | Rp30.000 | 20 hari |
| VIP | Rp50.000 | 30 hari |

### Alur USER

```text
USER / PREMIUM
  ↓
Lihat banner Paket Premium
  ↓
Pilih paket
  ↓
Buat Pesanan (PENDING)
  ↓
Lakukan pembayaran manual sesuai instruksi admin
  ↓
Admin cek pembayaran
  ↓
APPROVE
  ↓
Cloud Function
  ↓
USER → PREMIUM + durasi paket
PREMIUM → expiry lama + durasi paket
```

- User hanya boleh membuat dan melihat pesanan miliknya sendiri.
- PREMIUM bisa membeli paket lagi untuk **menambah durasi**, bukan mereset expiry.
- USER yang disetujui otomatis menjadi PREMIUM.
- Admin melihat jumlah pesanan pending langsung di tab **Pesanan**.
- Admin bisa **Approve** atau **Tolak**.
- Saat approve, perubahan role/expiry dilakukan Cloud Function server-side, bukan oleh Android client.
- Voucher referral tetap ada dan **belum dipotong otomatis dari harga paket**; untuk sementara admin memproses voucher secara manual.

### Firestore tambahan

```text
subscriptionOrders/{orderId}
  uid: string
  email: string
  packageId: string
  packageName: string
  price: number
  durationDays: number
  status: "PENDING" | "APPROVED" | "REJECTED"
  createdAt: number
  approvedAt: number | null
  rejectedAt: number | null
  approvalProcessedAt: number | null
  processedExpiryMillis: number | null
  adminNote: string

appSettings/subscriptionPackages
  packages: [
    { id, name, price, durationDays, label, enabled, sortOrder }
  ]
```

### Deploy Cloud Functions setelah fitur ini ditambahkan

```bash
cd functions
npm install
cd ..
firebase deploy --only functions
```

`onSubscriptionOrderUpdated` wajib ter-deploy agar tombol **Approve** benar-benar mengaktifkan Premium secara server-side.

## Automatic Firebase deployment from GitHub

Project ini sekarang memiliki workflow `.github/workflows/deploy-firebase.yml`.
Setiap push ke branch `main` yang mengubah `functions/`, `firestore.rules`, `firebase.json`, atau workflow deploy akan otomatis:

1. install Firebase CLI,
2. install dependency Cloud Functions,
3. deploy semua Cloud Functions, termasuk `onSubscriptionOrderUpdated`,
4. deploy Firestore Rules.

### GitHub Secrets yang wajib disiapkan sekali

Di GitHub: **Settings → Secrets and variables → Actions → New repository secret**.

Tambahkan:

- `FIREBASE_PROJECT_ID` = Firebase Project ID kamu.
- `FIREBASE_SERVICE_ACCOUNT_JSON` = isi lengkap JSON Service Account Firebase/Google Cloud (bukan file path dan bukan base64).

Setelah dua secret ini ada, cukup push ke `main`. Tidak perlu menjalankan `firebase deploy` dari komputer untuk perubahan backend berikutnya.

### Subscription approval

Saat admin mengubah order `PENDING` menjadi `APPROVED`, Cloud Function `onSubscriptionOrderUpdated` akan otomatis:

- memvalidasi paket berdasarkan `appSettings/subscriptionPackages`,
- mengubah USER menjadi `PREMIUM`,
- menambah durasi ke `premiumExpiryMillis` jika user sudah Premium,
- mencatat `lastSubscriptionActivatedAt` dan `lastSubscriptionOrderId`,
- membuat proses idempotent agar order yang sama tidak memberi Premium dua kali.

Jika function belum pernah aktif di Firebase, workflow deploy di atas akan mengaktifkannya otomatis setelah perubahan ini dipush ke `main`.


## Subscription approval v5
- Admin approval now activates Premium atomically from the Android admin panel.
- This manual-phase flow activates Premium atomically from the Admin app, so it does not depend on a deployed Cloud Function for the approval action.
- Cloud Function remains a server-side backup; `approvalProcessedAt` prevents double activation.
- Referral welcome voucher is now a real discount voucher: user enters the voucher before buying a package, the order stores original price, discount percent, discount amount, and final price.
- Voucher is consumed only when the order is approved. Rejected orders do not consume it.
- Premium purchases extend the current expiry; they never reset an active subscription.


## Fix V6 - Voucher & Approval

- Harga voucher sekarang terlihat langsung di layar user sebelum membeli: harga normal → diskon → total bayar.
- Voucher hanya menjadi diskon; tidak memberikan Premium secara langsung.
- Admin approval tidak lagi menolak order lama hanya karena harga paket di katalog sudah berubah. Validasi approval menggunakan `packageId` + `durationDays`; nominal tetap diverifikasi admin secara manual sebelum APPROVE.
- Panel Pesanan menampilkan harga normal, diskon, dan total yang harus dibayar.
- Order yang sama tetap idempotent dan tidak dapat diberi Premium dua kali.


## Update V8 — UX Voucher Welcome
- Voucher welcome tidak lagi ditampilkan sebagai langkah wajib sebelum memilih paket.
- User memilih paket terlebih dahulu, lalu setelah klik **Beli Paket** muncul dialog untuk memasukkan voucher.
- Jika voucher valid, total harga langsung berubah di dialog sebelum konfirmasi order.
- Jika user memiliki voucher welcome, halaman paket menampilkan banner **"Kamu punya Voucher Welcome!"** agar tidak mudah terlewat.
- Setelah registrasi menggunakan referral, user langsung melihat dialog berisi kode voucher welcome dan instruksi bahwa voucher digunakan saat membeli paket.
- Voucher tetap opsional dan baru dicatat sebagai terpakai setelah order disetujui.

## Subscription approval fix (V10)

Manual approval now uses the `durationDays` snapshot stored in the order. Approval no longer checks whether the package still exists or is enabled in `appSettings/subscriptionPackages`. This prevents valid pending orders from becoming unapprovable after an admin edits/deactivates a package. Admin still verifies the transfer amount manually before approving.



## V11 — Checkout Voucher UX

- Setiap pembelian paket selalu menampilkan kolom **Voucher Diskon (opsional)** di checkout.
- Jika user memiliki voucher welcome yang masih aktif, kode voucher otomatis diisi pada checkout.
- Kode voucher juga ditampilkan jelas di checkout agar user tidak perlu kembali ke Profil untuk menyalin kode.
- User Premium juga mendapatkan kolom voucher yang sama saat memperpanjang langganan.
- Voucher tetap hanya ditandai terpakai setelah order berhasil **APPROVED**.
- Jika checkout dibatalkan atau order ditolak, voucher tidak dianggap terpakai.


## Update terbaru — UI Sinyal Aktif & History

- **Sinyal ACTIVE dipisahkan dari history** agar sinyal yang sedang berjalan langsung terlihat di bagian paling atas.
- Jika ada lebih dari satu sinyal aktif, sinyal ditampilkan sebagai **kartu horizontal** yang bisa digeser ke kiri/kanan.
- **History Sinyal** hanya berisi sinyal yang sudah tidak ACTIVE (`BE`, `TP HIT`, `SL HIT`, `CANCELLED`) dan tetap ditampilkan vertikal di bawah.
- Tampilan ini diterapkan pada **USER dan PREMIUM**.
- Untuk USER, sinyal aktif maupun history tetap menggunakan tampilan terkunci/blur sesuai role.


## Admin Package Manager

Admin Panel sekarang memiliki tab **Paket** untuk mengelola paket langganan tanpa mengubah source code.

Admin dapat:
- Menambah paket baru
- Mengubah nama paket
- Mengubah harga
- Mengubah durasi Premium (hari)
- Mengubah label paket
- Mengaktifkan/nonaktifkan paket
- Menghapus paket

Konfigurasi paket disimpan di:
`appSettings/subscriptionPackages`

Perubahan paket hanya berlaku untuk **order baru**. Order yang sudah dibuat tetap menyimpan `packageName`, `price`, dan `durationDays` pada dokumen order sehingga aman jika harga/durasi paket diubah setelah checkout.

Firestore Rules yang ada sudah mengizinkan ADMIN menulis `appSettings`; tidak perlu membuat collection baru untuk Package Manager.


## UI Paket Langganan (V14)
Ringkasan paket pada banner Premium kini memakai grid 2 kolom yang lebih rapi. Setiap paket ditampilkan sebagai kartu kecil berisi nama, harga, durasi, dan label. Jika jumlah paket ganjil, kartu terakhir tetap tersusun rapi tanpa melebar penuh.


## V15 — Preview Paket Lebih Ringkas
- Section Perpanjang/Upgrade Premium sekarang hanya menampilkan maksimal 2 paket aktif.
- Paket berlabel promo/diskon/sale/best value diprioritaskan.
- Jika tidak ada label promo, paket dengan harga per hari Premium paling rendah diprioritaskan.
- Tombol **Lihat Semua Paket & Beli** tetap membuka seluruh paket aktif.
- Admin tetap dapat mengelola semua paket dari Admin Panel > Paket Langganan.


## V16 - Admin User Role Filter
- Admin Users panel now has role filters for Semua, USER, PREMIUM, and ADMIN.
- Each filter shows the current user count.
- Filtering uses `effectiveRole`, so expired Premium users are classified consistently with the app's effective role logic.
- No new Firestore collections or fields are required.


## V17 — Admin User Search
- Menambahkan pencarian user di Admin → Users berdasarkan email atau UID.
- Pencarian dapat dipakai bersamaan dengan filter role (Semua, USER, PREMIUM, ADMIN).
- Pencarian tidak mengubah data Firestore dan tidak memerlukan collection atau Rules baru.


## V18 — Admin User Search (UID & Email)

Panel **Admin → Users** sekarang memiliki pencarian user yang lebih jelas dan praktis:

- Search berjalan **real-time saat admin mengetik**.
- Pencarian mendukung **UID** dan **email** sekaligus.
- Pencarian bersifat **case-insensitive** dan mendukung kecocokan sebagian, jadi admin tidak harus mengetik email/UID secara lengkap.
- Search dapat digunakan bersamaan dengan filter role **Semua / USER / PREMIUM / ADMIN**.
- Jumlah hasil ditampilkan sebagai `X user cocok dari Y user` saat pencarian aktif.
- Jika tidak ada hasil, panel menampilkan empty state yang menjelaskan bahwa email atau UID perlu dicek kembali.
- Setiap baris user sekarang menampilkan **email dan UID** agar hasil pencarian lebih mudah diverifikasi sebelum admin mengubah role.
- Tidak ada collection, field, index Firestore, atau perubahan Firestore Rules yang diperlukan karena pencarian dilakukan terhadap daftar user yang memang sudah dimuat oleh panel ADMIN.

### Cara memakai

1. Login sebagai **ADMIN**.
2. Buka **Admin Panel → Users**.
3. Masukkan sebagian atau seluruh **email** atau **UID** pada kolom **Cari User**.
4. Opsional, kombinasikan dengan filter role.
5. Klik tombol **X** pada kolom pencarian untuk menghapus query dan kembali melihat seluruh user.

> Catatan: versi ini menggunakan listener daftar user yang sudah dipakai panel ADMIN, kemudian melakukan filtering di sisi aplikasi. Untuk database dengan jumlah user sangat besar, pencarian server-side/pagination dapat ditambahkan pada tahap berikutnya.

## V19 — Admin User Search Build Fix
- Memperbaiki error compile Kotlin pada `AdminPanelScreen.kt` untuk ikon pencarian dan clear pada field **Cari User**.
- Menambahkan import `Icons.Filled.Search` dan `Icons.Filled.Clear` yang sebelumnya belum dideklarasikan, sehingga error `Unresolved reference: Search` dan `Unresolved reference: Clear` tidak lagi terjadi pada source tersebut.
- Fitur pencarian UID/email dari V18 tetap dipertahankan; tidak ada perubahan pada Firestore schema, collection, index, atau Rules.



## V20 — Responsive / Adaptive UI

UI SevenGold sekarang menggunakan **adaptive layout berbasis lebar window**, bukan mendeteksi merek atau tipe device tertentu.

Perilaku utama:
- **HP / layar kecil (< 600dp):** layout tetap compact dengan padding 16dp.
- **Tablet / layar medium (600–839dp):** padding 24dp dan content dibatasi sekitar 960dp.
- **Tablet besar / landscape (≥ 840dp):** padding 32dp dan content dibatasi sekitar 1200dp.
- **Split-screen dan foldable:** ikut menyesuaikan berdasarkan ukuran window yang tersedia.
- Konten pada layar besar tetap berada di tengah agar tidak melebar berlebihan.
- Perubahan ini bersifat global melalui `AdaptiveAppFrame`, sehingga screen yang sudah ada ikut mendapatkan perilaku responsive tanpa mengubah alur fitur Firebase/Auth.

### Catatan implementasi

Breakpoint menggunakan `dp` dari **available window width**, sehingga aplikasi tidak bergantung pada nama/model device. Ini membuat layout lebih aman untuk HP portrait, HP landscape, tablet portrait, tablet landscape, split-screen, dan device baru.

File utama:
`app/src/main/java/com/sevengold/signalapp/ui/common/AdaptiveLayout.kt`

Integrasi utama:
`app/src/main/java/com/sevengold/signalapp/ui/navigation/AppNav.kt`

---

## V24.1 — Cloudflare Worker deployment & GitHub Actions fix

Update ini memperbaiki bagian deployment push yang sebelumnya bisa terlihat berhasil di Cloudflare tetapi APK belum menerima URL Worker saat build.

### Perbaikan utama

- GitHub Actions sekarang **wajib membaca** repository secret:
  `SEVENGOLD_PUSH_WEBHOOK_URL`.
- URL tersebut diteruskan ke Gradle melalui:
  `-PSEVENGOLD_PUSH_WEBHOOK_URL="..."`.
- Jika secret belum dibuat, workflow **gagal dengan pesan yang jelas** daripada menghasilkan APK tanpa URL push.
- Nama Worker pada `push-worker/wrangler.toml` disamakan dengan Worker Cloudflare yang digunakan:
  `sevengoldapp`.
- `PremiumPushGateway` memvalidasi bahwa URL Worker menggunakan HTTPS.
- Credential FCM/service account tetap hanya berada di **Cloudflare Secret** dan tidak pernah dimasukkan ke APK.
- Firestore tetap menjadi sumber data utama. Jika push gagal, perubahan sinyal/status tetap tersimpan dan Admin Panel menampilkan pesan bahwa notifikasi belum terkirim.
- Event push yang didukung:
  `SIGNAL_CREATED`, `SIGNAL_ACTIVE`, `TP_HIT`, `SL_HIT`, `BE`, `CANCELLED`.

### Konfigurasi Cloudflare Worker

Worker production:

```text
https://sevengoldapp.coin7star.workers.dev
```

Secret yang harus dibuat di:

**Cloudflare → Workers & Pages → sevengoldapp → Settings → Variables and Secrets**

Gunakan tipe **Secret**, bukan Plaintext/Variable:

```text
FIREBASE_PROJECT_ID
ADMIN_UIDS
FIREBASE_SERVICE_ACCOUNT_JSON
```

`ADMIN_UIDS` berisi UID Firebase Authentication administrator yang boleh mengirim push. Jika lebih dari satu, pisahkan dengan koma.

`FIREBASE_SERVICE_ACCOUNT_JSON` berisi seluruh JSON service account yang memiliki izin mengirim FCM. **Jangan commit JSON tersebut ke GitHub dan jangan memasukkannya ke APK.**

### GitHub Actions secret

Di:

**GitHub → Repository → Settings → Secrets and variables → Actions**

buat:

```text
SEVENGOLD_PUSH_WEBHOOK_URL
```

value:

```text
https://sevengoldapp.coin7star.workers.dev
```

Workflow build akan menghentikan proses jika secret tersebut kosong.

### Catatan keamanan penting

Jika private key Firebase service account pernah muncul di build log, chat, screenshot, atau tempat lain yang tidak seharusnya, anggap key tersebut **terekspos**. Hapus/revoke key lama di Google Cloud/Firebase Service Account lalu buat private key baru. Simpan key baru hanya sebagai Cloudflare Secret.

Jangan pernah memasukkan:

- `FIREBASE_SERVICE_ACCOUNT_JSON`
- private key
- Cloudflare API token
- Firebase Admin credential

ke source Android atau repository GitHub.

### Alur push

```text
ADMIN
  │
  ├── Simpan perubahan ke Firestore
  │
  └── POST + Firebase ID token
          │
          ▼
  Cloudflare Worker
  ├── Verifikasi Firebase ID token
  ├── Cek UID terhadap ADMIN_UIDS
  └── OAuth service account
          │
          ▼
      FCM HTTP v1
          │
          ▼
  topic: premium_signals
          │
          ▼
     DEVICE PREMIUM
```

### Checklist setelah build

1. Pastikan `SEVENGOLD_PUSH_WEBHOOK_URL` ada di GitHub Actions.
2. Build APK baru dari workflow.
3. Install APK baru di device Premium.
4. Login Premium dan izinkan notifikasi Android.
5. Login Admin di device Admin.
6. Terbitkan sinyal.
7. Buka **Cloudflare → sevengoldapp → Logs** dan pastikan invocation tercatat.
8. Pastikan notifikasi muncul di device Premium.
9. Uji `TP`, `SL`, `BE`, dan `Cancel`.

Target jalur server adalah hitungan detik, tetapi waktu tampil di device tetap dapat dipengaruhi jaringan, FCM, Doze/battery optimization, dan pengaturan notifikasi Android.

### Cloudflare Observability

Untuk debugging, aktifkan:

- **Logs: ON**
- **Include Invocation Logs: ON**
- **Persist Logs to Workers Dashboard: ON**

Jika Cloudflare menampilkan peringatan bahwa konfigurasi Wrangler berbeda dengan Dashboard, pastikan konfigurasi repository tetap konsisten dengan Worker `sevengoldapp` sebelum deployment berikutnya.

---

## V24.2 — Push Worker URL build fix

V24.2 memperbaiki penyebab APK dapat ter-build tetapi tidak pernah memanggil Cloudflare Worker.

### Penyebab

`app/build.gradle.kts` sebelumnya menggunakan URL kosong jika `SEVENGOLD_PUSH_WEBHOOK_URL` tidak diteruskan oleh GitHub Actions. Workflow lama memang belum meneruskan secret tersebut ke Gradle.

Akibatnya:

```text
Admin APK
   ↓
Firestore ✅
   ↓
PremiumPushGateway
   ↓
URL Worker kosong ❌
   ↓
Cloudflare Worker tidak menerima request
```

### Perbaikan

V24.2:

- menggunakan fallback production URL:
  `https://sevengoldapp.coin7star.workers.dev`
- tetap mendukung GitHub Actions secret:
  `SEVENGOLD_PUSH_WEBHOOK_URL`
- workflow meneruskan secret ke Gradle bila tersedia
- gateway menulis diagnostic log tanpa mencetak token Firebase
- response error Worker ditampilkan secara terbatas agar mudah didiagnosis
- FCM notification service mencatat event yang diterima di Logcat
- Worker tetap menggunakan `premium_signals` sebagai topic Premium

### GitHub Secret

Disarankan tetap membuat:

```text
SEVENGOLD_PUSH_WEBHOOK_URL
```

dengan value:

```text
https://sevengoldapp.coin7star.workers.dev
```

Fallback di APK membuat build tidak gagal hanya karena secret ini belum dibuat, tetapi repository secret tetap lebih mudah dipelihara jika URL Worker berubah di masa depan.

### Test V24.2

1. Build APK baru dari GitHub Actions.
2. Install APK baru di HP Admin.
3. Install APK baru di HP Premium.
4. Login Premium dan pastikan status Premium aktif.
5. Izinkan notifikasi Android.
6. Buka Cloudflare:
   **Workers & Pages → sevengoldapp → Observability → Live**.
7. Dari HP Admin, publish satu sinyal.
8. Harus muncul invocation `POST` pada Worker.
9. Jika HTTP `200`, Worker sudah berhasil menerima dan meneruskan request ke FCM.
10. Jika HTTP `401/403/500`, buka detail event untuk melihat error Worker.


## V24.7 – Full-width mobile drawer
- Drawer menu on User/Premium screens now expands to the available device width instead of a narrow fixed 280–340dp sheet.
- Existing V24.6 features and business logic are preserved.


## V24.8 - Admin Side Menu

- Admin navigation moved from the crowded horizontal tab row into a side drawer.
- Admin menu includes Publish Signal, Signals, Codes/Vouchers, Subscription Packages, Orders, Users, Referral, and Admin Profile.
- Pending subscription count remains visible in the Orders drawer item.
- Admin header now uses a compact menu button and current-page title.
- Existing admin features and data structures are preserved.


## V24.9 — Admin Drawer Fix

- Admin side menu now uses a custom full-screen Surface instead of the Material3 ModalDrawerSheet width constraint.
- Prevents the admin drawer from appearing clipped/narrow on phone layouts.
- Existing admin navigation, pending-order badge, profile, logout, and application logic are preserved.


## V24.10
- Admin drawer close/toggle fix: explicit close button, Android back handling, enabled swipe gestures, and scrim behavior.


## V24.11 — Admin Drawer State Fix

- Admin navigation now uses a deterministic full-screen drawer overlay.
- `☰` opens the drawer.
- `✕`, Android back, and a right swipe on the drawer close it reliably.
- The drawer no longer remains stuck over the Admin content.
- Admin content returns to the full device width after closing.
- Existing Admin features and menu actions are preserved.

## V24.12 — Admin Drawer State & Layout Fix

- Fixed malformed Admin drawer composition structure from the previous V24.11 patch.
- Admin drawer is now a single full-screen overlay controlled directly by `drawerOpen`.
- `X` closes the drawer immediately.
- Android Back closes the drawer when it is open.
- Swiping to the right closes the drawer.
- Selecting any Admin menu closes the drawer automatically.
- Admin navigation logic and existing features are preserved.



## V24.14
- Fixed AdminPanelScreen Kotlin import corruption introduced during the Admin drawer patch.
- Preserved the V24.12 Admin drawer behavior and existing features.

## V24.14 — Admin Drawer Compile Fix
- Restored the AdminTab declaration and required imports removed during the previous drawer patch.
- Restored theme colors, SignalListViewModel and rupiah references used by AdminPanelScreen.
- Preserved the V24.13 drawer UI/state behavior.


## V25.0 — Full UI Refresh

- Visual redesign focused on a cleaner, more professional trading-app feel.
- Larger typography and clearer hierarchy for headings, values, labels, and body text.
- Softer, more rounded surfaces with reduced heavy shadows.
- More generous spacing for mobile readability and touch comfort.
- Subtle full-app background treatment while preserving the existing dark/gold identity.
- Signal history and performance sections use a cleaner, less box-heavy presentation.
- No business logic, Firebase data model, referral, voucher, subscription, approval, signal, or admin functionality was intentionally changed.


## V25.2 — Full UI Refresh Build Fix

- Fixed missing `androidx.compose.ui.graphics.Color` import in `MainActivity.kt`.
- No application logic or UI redesign features were otherwise changed.


## V25.2 UI Fix
- Memperbaiki drawer User dan Premium agar tombol tutup menggunakan ikon Close yang jelas.
- Menambahkan BackHandler untuk menutup drawer dengan tombol Back Android.
- Drawer gestures tetap diaktifkan untuk swipe close/open.
- Tidak mengubah logic Firebase, referral, voucher, subscription, signal, maupun Admin.


## V25.4 — Responsive UI
- Added adaptive content width for admin, user, premium and profile screens.
- Subscription approval cards adapt to compact phone widths: price/header stacks and action buttons become full width.
- Admin role filters are horizontally scrollable on narrow screens instead of overflowing.
- Desktop/tablet layouts use a readable maximum content width while phones use available width with safe padding.
- Existing Firebase, referral, voucher, subscription, signal and drawer logic is unchanged.


## V25.4
- Fix compile error in Admin user role filter by adding the missing Jetpack Compose `LazyRow` import.
- No feature or business-logic changes.

## V25.5 — Perbaikan Login Google & Keystore Debug Permanen

**Masalah:** login dengan Google gagal dengan pesan "Tidak ada akun Google yang dapat digunakan", walau akun Google ada di HP dan konfigurasi Firebase (package name, SHA-1) sudah benar.

**Root cause yang ditemukan:** workflow GitHub Actions (`.github/workflows/build-apk.yml`) selalu membuat `debug.keystore` baru secara acak di setiap run, karena runner GitHub Actions selalu berupa mesin bersih (tidak menyimpan file dari run sebelumnya). Akibatnya SHA-1 certificate fingerprint berubah di setiap build, sehingga APK hasil build terbaru tidak pernah cocok dengan SHA-1 yang sempat didaftarkan di Firebase Console.

**Perbaikan:**
- `AuthRepository.kt` — error dari Credential Manager (`GetCredentialException`) tidak lagi ditelan jadi pesan generik; sekarang pesan error asli & detail exception dicatat ke Logcat dan ditampilkan, supaya penyebab sebenarnya kelihatan.
- Ditambahkan **jalur cadangan Google Sign-In "klasik"** (`play-services-auth` / `GoogleSignInClient`) yang otomatis dipakai kalau Credential Manager gagal — untuk jaga-jaga di device/ROM yang kurang kompatibel dengan Credential Manager API.
- `.github/workflows/build-apk.yml` — debug keystore sekarang **permanen**, diambil dari GitHub secret `DEBUG_KEYSTORE_BASE64`, bukan dibuat ulang secara acak setiap build. Ini memastikan SHA-1/SHA-256 APK **tidak pernah berubah lagi** antar build.

**SHA-1 & SHA-256 permanen (debug), wajib terdaftar di Firebase Console → Project Settings → SHA certificate fingerprints:**

```text
SHA-1:   25:2A:56:55:A2:51:BA:38:8A:B9:5E:ED:62:82:A5:0D:AA:C1:C2:C0
SHA-256: E1:25:9C:00:3A:F2:D7:D5:0F:2F:2E:3E:2A:ED:27:77:72:96:A0:22:70:24:04:11:A2:A1:B7:45:81:40:3D:8E
```

**Setup wajib (sekali saja):**
1. Tambah GitHub repository secret `DEBUG_KEYSTORE_BASE64` (isinya base64 dari keystore debug permanen).
2. Tambahkan SHA-1 & SHA-256 di atas ke Firebase Console untuk app Android `com.sevengold.signalapp`.
3. Setelah itu, setiap build dari GitHub Actions akan selalu memakai keystore yang sama, jadi SHA-1/SHA-256 tidak perlu diupdate lagi ke Firebase di masa depan.


## V24.5 — Google Sign-In fix

- Debug build now uses a stable `debug.keystore`, so the SHA-1 fingerprint does not change between GitHub Actions runs.
- Workflow validates that `google-services.json` matches package `com.sevengold.signalapp` and contains a Web OAuth client (`client_type=3`).
- Google Sign-In errors now expose the Google status code instead of always showing a generic cancellation message.
- In Firebase Console, add the SHA-1 printed by GitHub Actions under Android app settings, then download the updated `google-services.json` and update the `GOOGLE_SERVICES_JSON_BASE64` GitHub secret.
- This change is for debug APK authentication only; never use the debug keystore as a production release signing key.
## V24.7.1 — Build fix

Restored the missing `ensureReferralData` and `ensureUserProfile` helpers in `AuthRepository`. Google login now creates/repairs a minimal Firestore profile without overwriting existing role, premium, referral, or voucher data.

## V24.8 — Telegram Connect + Notification

Telegram ditambahkan sebagai **channel notifikasi tambahan** untuk user Premium. FCM tetap menjadi channel utama dan tidak bergantung pada Telegram.

### Alur

```text
Admin publish
   ↓
Cloudflare Worker
   ├── FCM → Premium App
   └── Telegram → Premium yang sudah terhubung
```

Telegram hanya mengirim ke user yang:
- role-nya `PREMIUM`,
- masa Premium masih aktif,
- sudah menghubungkan akun Telegram,
- dan mengaktifkan event notifikasi tersebut.

Jika Telegram gagal, **FCM tetap dianggap berhasil** dan publish sinyal tidak dibatalkan.

### Hubungkan Telegram dari aplikasi

1. Login sebagai Premium.
2. Buka **Profil → Notifikasi Telegram**.
3. Tekan **Buat Kode Koneksi**.
4. Aplikasi menampilkan kode 6 karakter dan perintah:
   `/start KODE`
5. Buka bot Telegram SevenGold.
6. Kirim `/start KODE`.
7. Worker memvalidasi kode dan menghubungkan Telegram Chat ID ke Firebase UID.
8. Aplikasi akan otomatis menampilkan status **Terhubung** melalui listener profil.

Kode koneksi berlaku **10 menit** dan hanya dapat digunakan untuk satu koneksi.

### Secret Cloudflare

Di **Cloudflare → Workers & Pages → sevengoldapp → Settings → Variables and Secrets**, tambahkan sebagai **Secret**:

```text
FIREBASE_PROJECT_ID
ADMIN_UIDS
FIREBASE_SERVICE_ACCOUNT_JSON
TELEGRAM_BOT_TOKEN
TELEGRAM_WEBHOOK_SECRET
```

`TELEGRAM_BOT_TOKEN` adalah token dari BotFather. **Jangan masukkan token ke APK, GitHub repository, README, screenshot, atau chat.**

### Set webhook Telegram

Setelah `TELEGRAM_BOT_TOKEN` disimpan di Cloudflare, Telegram harus diarahkan ke endpoint Worker:

```text
https://sevengoldapp.coin7star.workers.dev/telegram/webhook
```

Gunakan Bot API `setWebhook` untuk bot tersebut. Contoh:

```text
https://api.telegram.org/bot<BOT_TOKEN>/setWebhook?url=https://sevengoldapp.coin7star.workers.dev/telegram/webhook&secret_token=<TELEGRAM_WEBHOOK_SECRET>
```

Jangan menyimpan atau commit URL yang masih mengandung token. Setelah webhook aktif, `/start KODE` dari user akan diterima oleh Worker.

### Event Telegram

Default event:

```text
SIGNAL_CREATED
TP_HIT
SL_HIT
BE
CANCELLED
```

User Premium dapat mengaktifkan/nonaktifkan event dari Profil. `SIGNAL_ACTIVE` tetap tersedia di backend tetapi tidak diaktifkan secara default agar tidak menghasilkan notifikasi ganda.

### Security

- Bot token hanya berada di Cloudflare Secret.
- APK tidak pernah menerima bot token.
- Kode koneksi acak dan berlaku 10 menit.
- Worker melakukan lookup kode langsung ke Firestore menggunakan service account.
- Telegram hanya diproses untuk user Premium aktif.
- Admin push tetap membutuhkan Firebase ID token + UID yang terdaftar di `ADMIN_UIDS`.
- Telegram adalah channel tambahan; kegagalan Telegram tidak menggagalkan FCM.

### Cloudflare permissions

Karena Worker sekarang juga membaca/memperbarui dokumen Firestore untuk koneksi Telegram dan daftar Premium, service account yang digunakan oleh Worker harus memiliki akses yang sesuai ke Firestore selain izin FCM. Jika service account adalah Firebase Admin SDK service account standar, pastikan IAM role yang diperlukan untuk Firestore tersedia pada project.

### Testing

1. Deploy Worker.
2. Pastikan `TELEGRAM_BOT_TOKEN` sudah tersimpan sebagai Secret.
3. Set webhook Telegram ke endpoint `/telegram/webhook`.
4. Login Premium.
5. Buat kode koneksi.
6. Kirim `/start KODE` ke bot.
7. Pastikan aplikasi berubah menjadi **Terhubung**.
8. Publish sinyal dari Admin.
9. Pastikan:
   - FCM masuk ke aplikasi Premium.
   - Telegram masuk ke chat Premium yang terhubung.
10. Uji TP, SL, BE, dan Cancel.
11. Putuskan Telegram dari Profil dan pastikan pesan Telegram tidak lagi dikirim.

### Catatan skala

Implementasi V24.8 menggunakan query Firestore untuk mencari Premium yang terhubung setiap kali event push terjadi. Ini sederhana dan cocok untuk tahap awal. Jika jumlah Premium sudah besar, sebaiknya dipindahkan ke koleksi subscription Telegram khusus agar pengiriman tidak perlu memindai daftar user Premium.


## V24.8.2 — Telegram Connect UX

Perbaikan koneksi Telegram Premium:
- Kode koneksi sekarang menggunakan format **`SG-XXXXXX`** agar mudah dikenali.
- Tombol **Buka Telegram & Hubungkan** langsung membuka bot resmi SevenGold: **@signalalertsniper_bot**.
- Jika aplikasi Telegram tersedia, Android mencoba deep link Telegram terlebih dahulu; jika tidak tersedia, otomatis memakai halaman `t.me`.
- Deep link membawa kode `SG-XXXXXX` melalui parameter `/start`, sehingga user tidak perlu mengetik kode manual.
- Worker tetap menerima format `SG-XXXXXX` dan juga menormalisasi kode lama `XXXXXX` menjadi `SG-XXXXXX` untuk kompatibilitas.
- Token bot tetap hanya disimpan sebagai Cloudflare Secret; tidak pernah ditanam di APK.
