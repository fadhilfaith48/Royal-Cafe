# Royal Cafe POS System

Aplikasi Point of Sale (kasir) untuk kedai kopi, berbasis Java Swing + MySQL.
Dibuat untuk tugas kuliah/pSMA.

![Java](https://img.shields.io/badge/Java-17-orange)
![MySQL](https://img.shields.io/badge/MySQL-8-blue)

## Fitur

- Login dengan role **admin** dan **kasir**
- Dashboard kasir: pilih menu, hitung subtotal + pajak, cek pembayaran, kembalian
- Cetak struk ke PDF
- Manajemen menu (CRUD + pencarian + upload gambar) — khusus admin
- Riwayat transaksi dengan filter dan cetak PDF

## Kebutuhan

| Tools | Versi |
|---|---|
| JDK | 17 |
| NetBeans IDE | 17+ (atau IDE lain yang mendukung Ant) |
| XAMPP | MySQL 8 |

## Struktur Project

```
Cafe Management Shop/
├── src/ROYALCAFE/
│   ├── login.java              Login + verifikasi password
│   ├── Dashboard.java          Layar kasir + cetak struk PDF
│   ├── MenuCRUD.java           CRUD menu (admin)
│   ├── RiwayatTransaksi.java   Riwayat transaksi + cetak PDF
│   ├── DBConnection.java       Koneksi MySQL (kredensial dari env var)
│   ├── PasswordUtil.java       Hash/verifikasi password PBKDF2
│   └── PasswordTool.java       CLI membuat & reset user
├── src/Image/                  Gambar menu
├── lib/                        Driver MySQL (mysql-connector-j) — sudah disertakan
├── nbproject/                  Konfigurasi build NetBeans
├── Royal_cofe.sql              Skema database + data menu
├── TUTORIAL_BUILD_EXE.txt      Panduan membuat file .exe
└── installer.iss               Script Inno Setup untuk installer
```

## Cara Setup

### 1. Jalankan MySQL
Buka XAMPP Control Panel, klik **START** pada MySQL (harus hijau).

### 2. Import database
1. Buka `http://localhost/phpmyadmin`
2. Klik **New** di panel kiri, nama database `faith_coffe`, klik **Create**
3. Klik database `faith_coffe`, tab **Import**
4. Pilih file `Royal_cofe.sql`, klik **Go**

### 3. Konfigurasi koneksi (opsional)

Default sudah cocok dengan XAMPP standar (`localhost:3306`, user `root`, password kosong).
Kalau MySQL kamu berbeda, set environment variable berikut **sebelum menjalankan aplikasi**:

| Variabel | Default | Keterangan |
|---|---|---|
| `DB_HOST` | `localhost` | Host MySQL |
| `DB_PORT` | `3306` | Port MySQL |
| `DB_NAME` | `faith_coffe` | Nama database |
| `DB_USER` | `root` | User MySQL |
| `DB_PASS` | *(kosong)* | Password MySQL |

Di NetBeans: klik kanan project → **Properties** → **Run** → field **Run Environment**, lalu tambahkan `DB_PASS=passwordmu`.

Kredensial **tidak** disimpan di dalam kode, jadi tidak ikut ter-*upload* ke repository.

### 4. Buat user pertama

Tidak ada akun default. Karena password disimpan sebagai hash (bukan teks biasa),
user harus dibuat lewat tool:

```bash
java -cp lib/mysql-connector-j-8.0.31.jar ROYALCAFE.PasswordTool admin admin
```

Format: `PasswordTool <username> <admin|kasir>`. Tool akan menanyakan password
langsung di terminal (tidak terlihat saat diketik).

Buat user kasir juga kalau perlu:

```bash
java -cp lib/mysql-connector-j-8.0.31.jar ROYALCAFE.PasswordTool kasir kasir
```

## Menjalankan Aplikasi

- **NetBeans**: klik kanan project → **Run**
- **Command line**: `java -cp "lib/mysql-connector-j-8.0.31.jar;build/classes" ROYALCAFE.login`

## Build

```bash
ant clean dist
```

Hasilnya `dist/fadhil.jar`.

Untuk membuat file `.exe` dan installer Windows, ikuti `TUTORIAL_BUILD_EXE.txt`
(membutuhkan Launch4j dan Inno Setup).

## Reset Password

Jalankan perintah `PasswordTool` lagi dengan username yang sama — password akan
ditimpa yang baru:

```bash
java -cp lib/mysql-connector-j-8.0.31.jar ROYALCAFE.PasswordTool admin admin
```

## Struktur Database

Database `faith_coffe` terdiri dari 4 tabel:

| Tabel | Fungsi |
|---|---|
| `users` | Akun login (username, password hash, role) |
| `menu` | Daftar menu, harga, stok, nama file gambar |
| `transaksi` | Header transaksi (kode, kasir, subtotal, pajak, total, bayar, kembalian) |
| `detail_transaksi` | Rincian item per transaksi |

## Catatan Keamanan

- Password user disimpan dengan **PBKDF2-HMAC-SHA256**, salt acak 16 byte per
  user, 210.000 iterasi, perbandingan tahan timing attack
- Password database MySQL dibaca dari environment variable, tidak di-hardcode
- Tidak ada akun/password default di repository — user dibuat sendiri via `PasswordTool`
- Akun lama yang passwordnya masih plaintext harus dibuat ulang dengan `PasswordTool`

## Lisensi

Untuk keperluan tugas pembelajaran.