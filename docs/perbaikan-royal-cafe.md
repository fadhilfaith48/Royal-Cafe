# Perbaikan & Peningkatan — Royal Cafe POS System

Dokumen ini mencatat semua perbaikan dan peningkatan yang sudah dikerjakan
pada proyek **Royal Cafe POS System** (Java Swing + MySQL), beserta roadmap
pekerjaan berikutnya.

Terakhir diperbarui: 18 Oktober 2026

---

## Ringkasan Progres

| Fase | Judul | Status |
|---|---|---|
| 1 | Perbaikan Bug (fondasi) | ✅ Selesai |
| 2.1 | Manajemen User (admin) | ✅ Selesai |
| 2.2 | Ganti Password + Kelola User | ✅ Selesai |
| 2.3 | Filter tanggal + pagination Riwayat | ✅ Selesai |
| 2.4 | Konfigurasi pajak & diskon | ✅ Selesai |
| 2.5 | Void / Refund transaksi | ⏳ Belum |
| 3 | Laporan & Analitik | ⏳ Belum |
| 4 | Refactor & Kualitas | ⏳ Belum |

---

## Fase 1 — Perbaikan Bug

Tujuan: menstabilkan fondasi, menghilangkan bug logika, dan menyeragamkan
tampilan sebelum menambah fitur.

### 1.1 Kolom password terlihat (keamanan)
- **Masalah:** `login.java` memakai `JTextField` biasa untuk password, sehingga
  teks password terlihat saat diketik.
- **Perbaikan:** diganti `JPasswordField`, memakai `getPassword()` (`char[]`),
  dan array password dibersihkan (`Arrays.fill('\0')`) setelah dipakai.
- **File:** `login.java`, `login.form`.

### 1.2 Bayar/Kembalian balik `Rp 0` setelah transaksi
- **Masalah:** setelah bayar, `txtBayar`/`txtKembalian` diisi lalu langsung
  dihapus oleh `reset()` (dan `hitungTotal()`), sehingga panel utama selalu
  menampilkan `Rp 0`.
- **Perbaikan:** setelah `reset()` + `loadMenuFromDB()`, nominal bayar &
  kembalian ditampilkan kembali.
- **File:** `Dashboard.java` (`simpanTransaksi`).

### 1.3 `ADD COLUMN IF NOT EXISTS` tidak portabel
- **Masalah:** `pastikanKolomBayarAda()` memakai sintaks MariaDB yang **tidak
  didukung MySQL 8**, dan errornya ditelan tanpa jejak.
- **Perbaikan:** kolom dicek via `DatabaseMetaData.getColumns()`, lalu
  `ALTER TABLE ... ADD COLUMN` ditambahkan satu per satu bila belum ada.
  Error kini dicatat ke `System.err`.
- **File:** `Dashboard.java`.

### 1.4 Validasi harga/stok di MenuCRUD
- **Masalah:** harga/stok negatif bisa tersimpan.
- **Perbaikan:** validasi `harga > 0` dan `stok >= 0` (selain cek angka).
- **File:** `MenuCRUD.java` (`validateForm`).

### 1.5 Format Rupiah tidak konsisten
- **Masalah:** Dashboard pakai titik ribuan (`25.000`), Riwayat pakai koma
  (`25,000`), MenuCRUD pakai angka mentah.
- **Perbaikan:** dibuat utilitas `Rupiah.format()` dan dipakai di seluruh
  aplikasi. Kolom Harga di tabel MenuCRUD diberi cell renderer agar tetap
  berformat Rupiah tanpa merusak nilai model.
- **File:** `Rupiah.java` (baru), `Dashboard.java`, `MenuCRUD.java`,
  `RiwayatTransaksi.java`.

### 1.6 Thread jam tidak berhenti
- **Masalah:** thread jam berjalan tanpa kondisi berhenti; menumpuk setiap
  login ulang.
- **Perbaikan:** flag `volatile boolean clockRunning` + `WindowListener`
  (windowClosing/windowClosed) untuk menghentikan thread.
- **File:** `Dashboard.java`.

### 1.7 Kategori combo gagal senyap (MenuCRUD)
- **Masalah:** `cmbKategori.setSelectedItem(...)` gagal diam-diam bila kategori
  di DB tidak ada di daftar (mis. data lama "Kopi").
- **Perbaikan:** ditambah `pastikanKategoriAda()` yang menambahkan kategori ke
  combo bila belum terdaftar.
- **File:** `MenuCRUD.java`.

### 1.8 Branding tidak konsisten
- **Perbaikan:** komentar kelas "Cafe Senja"/"Cafe Faith" diseragamkan
  menjadi "Royal Cafe".
- **File:** `login.java`, `MenuCRUD.java`, `RiwayatTransaksi.java`.

### Commit Fase 1
| Hash | Pesan |
|---|---|
| `e549de1` | `fix(login): JPasswordField + bersihkan char[]` |
| `9d9c803` | `refactor: util Rupiah untuk format nominal konsisten` |

---

## Fase 2.1 — Manajemen User (admin)

- **File baru:** `ManajemenUser.java` (JFrame, pola mengikuti `MenuCRUD.java`).
- **Fitur:**
  - Tambah user (username, role `kasir`/`admin`, password).
  - Reset password user lain (dialog password baru).
  - Hapus user (dengan konfirmasi).
  - Tabel daftar user: Username, Role, Dibuat.
- **Pengaman self-lock:**
  - Tidak bisa menghapus user yang sedang login.
  - Tidak bisa menghapus admin terakhir (`COUNT(*) WHERE role='admin'`).
  - Reset password user sendiri diarahkan ke menu Ganti Password.
- **Validasi:** username `[A-Za-z0-9_]{3,50}`, password minimal 4 karakter,
  konfirmasi harus cocok.
- **Keamanan:** password disimpan sebagai hash PBKDF2 via `PasswordUtil`.

### Commit
| Hash | Pesan |
|---|---|
| `190bf85` | `feat: manajemen user UI (admin)` |

---

## Fase 2.2 — Ganti Password + Kelola User

- **`Dashboard.java`:**
  - Tombol **👥 Kelola User** (hanya admin) → membuka `ManajemenUser`.
  - Tombol **🔑 Ganti Password** (semua role) → dialog ganti password.
- **Dialog Ganti Password:**
  - Field password lama, baru, ulangi.
  - Verifikasi password lama via `PasswordUtil.verify`.
  - Simpan hash baru via `PasswordUtil.hash` + `UPDATE users`.
  - Setelah sukses: logout otomatis lalu buka halaman login.
  - `char[]` password dibersihkan di `finally`.
- **Logout:** tombol "Keluar" tetap membuka login kembali.

### Commit
| Hash | Pesan |
|---|---|
| `df4875c` | `feat(dashboard): tombol Kelola User + Ganti Password` |

---

## Fase 2.3 — Filter tanggal + pagination (Riwayat Transaksi)

- **Panel Filter Tanggal:**
  - Input manual "Dari" & "Sampai" (format `dd/MM/yyyy`) + tombol Terapkan.
  - Preset: **Hari Ini**, **7 Hari**, **Bulan Ini**, **Semua**.
- **Pagination:**
  - 20 baris per halaman (`PAGE_SIZE = 20`).
  - Tombol ◀ Sebelumnya / Selanjutnya ▶ (auto-enable/disable).
  - Label: `Menampilkan 1-20 dari 145 data (Halaman 1/8)`.
- **Teknis:** query `WHERE DATE(tanggal) >= ? AND DATE(tanggal) <= ? ORDER BY
  tanggal DESC LIMIT ? OFFSET ?`; total dihitung terpisah untuk info halaman.
- **Memenuhi janji README** ("riwayat dengan filter") yang sebelumnya belum ada.

### Rekomendasi optimasi DB
```sql
CREATE INDEX idx_transaksi_tanggal ON transaksi(tanggal);
```

### Commit
| Hash | Pesan |
|---|---|
| `bf88488` | `feat(riwayat): filter tanggal + pagination` |

---

## Fase 2.4 — Konfigurasi Pajak & Diskon (✅ Selesai)

- **File baru:** `Pengaturan.java` — utilitas key-value load/save ke tabel `pengaturan`.
- **Tabel `pengaturan`** (key-value): `pajak_persen`, `nama_toko`, `alamat_toko`, `footer_struk`.
- **Migrasi DB:** `migrasi_fase2_4.sql` + update `Royal_cofe.sql` (instalasi baru).
- **Perubahan `Dashboard.java`:**
  - Load pengaturan di constructor → `pajakPersen = Pengaturan.getInt("pajak_persen", 10)`.
  - Method `hitungPajak(int subtotal)` pakai persentase dinamis.
  - Label panel total "Pajak (10%):" → dinamis via `updatePajakLabel()`.
  - Struk teks, dialog struk, dan PDF pakai persentase dinamis.
  - Regex parsing PDF diperluas (`Pajak \d+%`) agar fleksibel.
- **Kolom diskon** ditambah di `transaksi` (`diskon_tipe`, `diskon_nilai`) untuk Fase 2.4 lanjutan.
- **Index** `idx_transaksi_tanggal` untuk performa filter (Fase 2.3).

### Commit
| Hash | Pesan |
|---|---|
| `339d476` | `feat: konfigurasi pajak dari DB (pengaturan table) + label dinamis` |
| `f1cae63` | `docs: update Royal_cofe.sql (tambah tabel pengaturan, kolom diskon, index tanggal)` |

---

## Roadmap Selanjutnya (Belum dikerjakan)

### Fase 2.5 — Void / Refund transaksi *(butuh migrasi DB)*

### Fase 2.5 — Void / Refund transaksi *(butuh migrasi DB)*
- `ALTER TABLE transaksi ADD status ENUM('selesai','void') DEFAULT 'selesai',
  ADD alasan_void, ADD void_by, ADD void_at`.
- Admin bisa void transaksi → stok dikembalikan (`UPDATE menu SET stok = stok + qty`).
- Riwayat menandai transaksi void.

### Fase 3 — Laporan & Analitik
- `LaporanPenjualan.java`: total pendapatan, jumlah transaksi, rata-rata.
- Menu terlaris (agregasi `detail_transaksi`).
- Grafik sederhana + ekspor CSV/Excel + cetak PDF laporan.

### Fase 4 — Refactor & Kualitas
- `PdfUtil.java` (satukan `Dashboard.buatPDF` & `RiwayatTransaksi.buatPDFManual`).
- `ReceiptDialog.java` (satukan dialog struk yang duplikat).
- Rename `login` → `Login`, hapus field mati `jSplitPane1`.
- Ganti `SELECT *` jadi kolom eksplisit + format timestamp rapi di Riwayat.
- Unit test (`PasswordUtil`, `Rupiah`, pajak/diskon) + CI GitHub Actions.

### Bonus (opsional)
- Shift kasir (buka/tutup kas, saldo awal, rekap).
- Manajemen stok: peringatan stok minimum, restock, riwayat stok.
- Cetak printer thermal / preview struk.
- Backup/restore database dari aplikasi.
- Audit log aktivitas admin.
- Barcode/shortcut, multi-bahasa, cek role di level query.

---

## Catatan Penting

- **Build JAR:** `dist/fadhil.jar` belum di-rebuild otomatis (Ant tidak selalu
  tersedia). Build ulang via NetBeans (Clean and Build) atau `ant clean dist`.
- **Migrasi DB:** Fase 2.4 & 2.5 memerlukan skrip migrasi. Rencana: file
  `migrasi_fase2.sql` (aman dijalankan di DB lama) + pembaruan `Royal_cofe.sql`
  untuk instalasi baru.
- **Setiap fase** sebaiknya di-commit terpisah agar mudah di-rollback.

---

## Log Commit (terbaru di atas)

| Hash | Pesan |
|---|---|
| `f1cae63` | `docs: update Royal_cofe.sql (tambah tabel pengaturan, kolom diskon, index tanggal)` |
| `339d476` | `feat: konfigurasi pajak dari DB (pengaturan table) + label dinamis` |
| `9e8514b` | `docs: tambah perbaikan-royal-cafe.md (ringkasan perbaikan Fase 1 & 2)` |
| `bf88488` | `feat(riwayat): filter tanggal + pagination` |
| `df4875c` | `feat(dashboard): tombol Kelola User + Ganti Password` |
| `190bf85` | `feat: manajemen user UI (admin)` |
| `9d9c803` | `refactor: util Rupiah untuk format nominal konsisten` |
| `e549de1` | `fix(login): JPasswordField + bersihkan char[]` |