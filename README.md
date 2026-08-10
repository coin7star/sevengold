# Signal App — XAUUSD (ADMIN / PREMIUM / USER)

Aplikasi Android sederhana untuk distribusi sinyal trading XAUUSD dengan 3 role:

- **ADMIN** — publish & kelola sinyal (TP/SL/BE/Cancel), generate kode langganan
- **PREMIUM** — lihat semua sinyal secara penuh (selama belum expired)
- **USER** — baru daftar, sinyal terlihat "terkunci"/blur, wajib redeem kode dari admin untuk naik jadi PREMIUM

Stack: **Kotlin + Jetpack Compose + Firebase (Auth + Firestore)**, di-build otomatis lewat **GitHub Actions** (tidak perlu install Android Studio).

---

## Update terbaru

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
- Teman baru yang mendaftar memakai kode referral otomatis mendapat **voucher welcome 10%** untuk pembelian/berlangganan pertama. Voucher tampil di Profil dan bisa ditunjukkan ke admin saat pembayaran.
- **Bonus referral aktif setelah teman benar-benar berlangganan**, yaitu setelah kode langganan berhasil diredeem.
- Referrer otomatis mendapat bonus Premium sesuai pengaturan admin (default **+2 hari**). Kalau referrer sedang Premium, bonus ditambahkan ke expiry yang masih aktif; kalau sudah USER/expired, role diaktifkan kembali menjadi PREMIUM selama durasi bonus.
- Satu teman hanya menghasilkan **satu reward referral**, walaupun teman tersebut memperpanjang Premium lagi di kemudian hari.
- Profil menampilkan kode referral, voucher welcome, jumlah referral yang berhasil, dan total hari bonus yang terkumpul.
- **Custom Referral dari Admin Panel** — admin sekarang punya tab **Referral** untuk mengubah jumlah hari bonus Premium, persentase voucher welcome, dan mengaktifkan/nonaktifkan program referral tanpa mengubah kode aplikasi.
- Logika pemberian bonus dijalankan oleh **Cloud Function** agar reward tidak bergantung pada client Android dan dibuat idempotent untuk mencegah bonus dobel. Cloud Function membaca konfigurasi terbaru dari `appSettings/referral`.

> **Catatan voucher:** sistem pembayaran otomatis belum ada di project ini. Voucher 10% disimpan sebagai benefit welcome dan ditampilkan ke user; admin tetap memproses harga/diskon saat transaksi berlangganan manual.

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
