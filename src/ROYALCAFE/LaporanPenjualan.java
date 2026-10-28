package ROYALCAFE;

import java.awt.*;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;

/**
 * Laporan Penjualan - Royal Cafe
 * Ringkasan pendapatan, menu terlaris, dan rekap harian.
 * Hanya untuk admin. Transaksi berstatus void tidak dihitung.
 */
public class LaporanPenjualan extends javax.swing.JFrame {

    private String currentUser = "admin";
    private String currentRole = "admin";

    // Filter tanggal
    private Date filterDari = null;
    private Date filterSampai = null;

    // Komponen
    private JFormattedTextField txtDari, txtSampai;
    private JLabel lblPendapatan, lblTransaksi, lblRata, lblItem, lblPeriode;
    private DefaultTableModel modelMenu, modelHarian;
    private JTable tblMenu, tblHarian;

    // Data terakhir (untuk ekspor)
    private final List<String[]> dataMenu = new ArrayList<>();
    private final List<String[]> dataHarian = new ArrayList<>();
    private long totalPendapatan = 0;
    private long totalTransaksi = 0;
    private long totalItem = 0;

    private static final Color HEADER_BG = new Color(51, 0, 255);
    private static final Color MAIN_BG   = new Color(204, 204, 204);
    private static final Color GREEN     = new Color(0, 153, 76);
    private static final Color RED       = new Color(204, 0, 0);
    private static final Color ORANGE    = new Color(230, 120, 0);
    private static final Color TEAL      = new Color(0, 128, 128);

    public LaporanPenjualan() {
        this("admin", "admin");
    }

    public LaporanPenjualan(String username, String role) {
        this.currentUser = (username == null || username.trim().isEmpty()) ? "admin" : username;
        this.currentRole = (role == null || role.trim().isEmpty()) ? "admin" : role;

        if (!"admin".equalsIgnoreCase(currentRole)) {
            JOptionPane.showMessageDialog(null,
                "Halaman laporan hanya dapat diakses oleh admin.",
                "Akses Ditolak", JOptionPane.WARNING_MESSAGE);
            dispose();
            return;
        }

        buildUI();
        setLocationRelativeTo(null);
        applyPreset(3); // default: semua periode
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    private void buildUI() {
        setTitle("Laporan Penjualan - Royal Cafe");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setSize(1000, 720);
        setLocationRelativeTo(null);
        getContentPane().setBackground(MAIN_BG);
        setLayout(new BorderLayout());

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildCenter(),  BorderLayout.CENTER);
        add(buildFooter(),  BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(HEADER_BG);
        p.setBorder(new EmptyBorder(10, 18, 10, 18));

        JLabel title = new JLabel("\uD83D\uDCCA  Laporan Penjualan - Royal Cafe");
        title.setFont(new Font("Bell MT", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        p.add(title, BorderLayout.WEST);

        lblPeriode = new JLabel(" ");
        lblPeriode.setFont(new Font("Times New Roman", Font.PLAIN, 13));
        lblPeriode.setForeground(Color.WHITE);
        p.add(lblPeriode, BorderLayout.EAST);
        return p;
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setBackground(MAIN_BG);
        center.setBorder(new EmptyBorder(8, 10, 0, 10));

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setBackground(MAIN_BG);
        top.add(buildFilterPanel(), BorderLayout.NORTH);
        top.add(buildSummaryPanel(), BorderLayout.CENTER);
        center.add(top, BorderLayout.NORTH);

        // Dua tabel: menu terlaris & rekap harian
        JPanel tables = new JPanel(new GridLayout(1, 2, 10, 0));
        tables.setBackground(MAIN_BG);

        modelMenu = new DefaultTableModel(new String[]{"Nama Menu", "Qty", "Omzet"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        modelHarian = new DefaultTableModel(new String[]{"Tanggal", "Transaksi", "Pendapatan"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tblMenu = new JTable(modelMenu);
        tblHarian = new JTable(modelHarian);
        styleTable(tblMenu);
        styleTable(tblHarian);

        tables.add(wrapTable("Menu Terlaris (Top 10)", tblMenu));
        tables.add(wrapTable("Rekap Harian", tblHarian));
        center.add(tables, BorderLayout.CENTER);
        return center;
    }

    private JPanel buildFilterPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        p.setBackground(MAIN_BG);
        p.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(HEADER_BG, 2), "Filter Periode",
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            new Font("Bell MT", Font.BOLD, 13), HEADER_BG));

        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        txtDari = new JFormattedTextField(df);
        txtDari.setPreferredSize(new Dimension(110, 28));
        txtDari.setFont(new Font("Times New Roman", Font.PLAIN, 13));

        txtSampai = new JFormattedTextField(df);
        txtSampai.setPreferredSize(new Dimension(110, 28));
        txtSampai.setFont(new Font("Times New Roman", Font.PLAIN, 13));

        p.add(label("Dari:"));
        p.add(txtDari);
        p.add(label("Sampai:"));
        p.add(txtSampai);

        JButton btnTerapkan = smallBtn("Terapkan", TEAL);
        btnTerapkan.addActionListener(e -> applyFilter());

        JButton btnHariIni = smallBtn("Hari Ini", HEADER_BG);
        btnHariIni.addActionListener(e -> applyPreset(0));
        JButton btn7 = smallBtn("7 Hari", HEADER_BG);
        btn7.addActionListener(e -> applyPreset(1));
        JButton btnBulan = smallBtn("Bulan Ini", HEADER_BG);
        btnBulan.addActionListener(e -> applyPreset(2));
        JButton btnSemua = smallBtn("Semua", new Color(100, 100, 100));
        btnSemua.addActionListener(e -> applyPreset(3));

        p.add(btnHariIni); p.add(btn7); p.add(btnBulan); p.add(btnSemua);
        p.add(btnTerapkan);
        return p;
    }

    private JPanel buildSummaryPanel() {
        JPanel p = new JPanel(new GridLayout(1, 4, 10, 0));
        p.setBackground(MAIN_BG);

        lblPendapatan = new JLabel("Rp 0");
        lblTransaksi  = new JLabel("0");
        lblRata       = new JLabel("Rp 0");
        lblItem       = new JLabel("0");

        p.add(kartu("Total Pendapatan", lblPendapatan, new Color(0, 130, 0)));
        p.add(kartu("Jumlah Transaksi",  lblTransaksi,  HEADER_BG));
        p.add(kartu("Rata-rata / Transaksi", lblRata,   ORANGE));
        p.add(kartu("Total Item Terjual", lblItem,      TEAL));
        return p;
    }

    private JPanel kartu(String judul, JLabel nilai, Color warna) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 5, 0, 0, warna),
            new EmptyBorder(8, 12, 8, 12)));

        JLabel l = new JLabel(judul);
        l.setFont(new Font("Times New Roman", Font.BOLD, 12));
        l.setForeground(Color.GRAY);

        nilai.setFont(new Font("Arial", Font.BOLD, 20));
        nilai.setForeground(warna);
        nilai.setHorizontalAlignment(SwingConstants.LEFT);

        p.add(l, BorderLayout.NORTH);
        p.add(nilai, BorderLayout.CENTER);
        return p;
    }

    private JPanel wrapTable(String judul, JTable tbl) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(MAIN_BG);
        p.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(HEADER_BG, 2), judul,
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            new Font("Bell MT", Font.BOLD, 13), HEADER_BG));
        p.add(new JScrollPane(tbl), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildFooter() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        p.setBackground(MAIN_BG);
        p.setBorder(new MatteBorder(2, 0, 0, 0, HEADER_BG));

        JButton btnRefresh = smallBtn("\u21BB Refresh", HEADER_BG);
        btnRefresh.addActionListener(e -> loadLaporan());

        JButton btnCSV = smallBtn("Ekspor CSV", GREEN);
        btnCSV.addActionListener(e -> eksporCSV());

        JButton btnPDF = smallBtn("Cetak PDF", ORANGE);
        btnPDF.addActionListener(e -> cetakPDF());

        JButton btnKembali = smallBtn("Kembali", RED);
        btnKembali.addActionListener(e -> dispose());

        p.add(btnRefresh); p.add(btnCSV); p.add(btnPDF); p.add(btnKembali);
        return p;
    }

    private void styleTable(JTable t) {
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.getTableHeader().setBackground(HEADER_BG);
        t.getTableHeader().setForeground(Color.WHITE);
        t.getTableHeader().setFont(new Font("Times New Roman", Font.BOLD, 12));
        t.setBackground(Color.WHITE);
        t.setGridColor(new Color(180, 180, 220));
        t.setFont(new Font("Times New Roman", Font.PLAIN, 12));
        t.setRowHeight(22);
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Times New Roman", Font.BOLD, 13));
        return l;
    }

    private JButton smallBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Times New Roman", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(120, 32));
        return b;
    }

    // ── Filter ────────────────────────────────────────────────────────────────
    private void applyPreset(int preset) {
        Calendar cal = Calendar.getInstance();
        switch (preset) {
            case 0: // Hari ini
                filterDari = cal.getTime();
                filterSampai = cal.getTime();
                break;
            case 1: // 7 hari terakhir
                cal.add(Calendar.DAY_OF_MONTH, -6);
                filterDari = cal.getTime();
                filterSampai = new Date();
                break;
            case 2: // Bulan ini
                cal.set(Calendar.DAY_OF_MONTH, 1);
                filterDari = cal.getTime();
                filterSampai = new Date();
                break;
            case 3: // Semua
            default:
                filterDari = null;
                filterSampai = null;
                break;
        }
        txtDari.setValue(filterDari);
        txtSampai.setValue(filterSampai);
        loadLaporan();
    }

    private void applyFilter() {
        Object dariVal = txtDari.getValue();
        Object sampaiVal = txtSampai.getValue();
        filterDari = (dariVal instanceof Date) ? (Date) dariVal : null;
        filterSampai = (sampaiVal instanceof Date) ? (Date) sampaiVal : null;

        if (filterDari != null && filterSampai != null && filterDari.after(filterSampai)) {
            JOptionPane.showMessageDialog(this,
                "Tanggal 'Dari' tidak boleh lebih besar dari 'Sampai'.",
                "Validasi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        loadLaporan();
    }

    /** Bangun klausa WHERE (selalu mengecualikan transaksi void). */
    private String buildWhere(List<Object> params) {
        StringBuilder w = new StringBuilder(" WHERE COALESCE(t.status,'selesai') <> 'void'");
        if (filterDari != null) {
            w.append(" AND DATE(t.tanggal) >= ?");
            params.add(new java.sql.Date(filterDari.getTime()));
        }
        if (filterSampai != null) {
            w.append(" AND DATE(t.tanggal) <= ?");
            params.add(new java.sql.Date(filterSampai.getTime()));
        }
        return w.toString();
    }

    private String periodeText() {
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        if (filterDari == null && filterSampai == null) return "Semua periode";
        if (filterDari != null && filterSampai != null) {
            return df.format(filterDari) + " s.d. " + df.format(filterSampai);
        }
        if (filterDari != null) return "Sejak " + df.format(filterDari);
        return "Sampai " + df.format(filterSampai);
    }

    // ── Load data ─────────────────────────────────────────────────────────────
    private void loadLaporan() {
        modelMenu.setRowCount(0);
        modelHarian.setRowCount(0);
        dataMenu.clear();
        dataHarian.clear();
        totalPendapatan = 0;
        totalTransaksi = 0;
        totalItem = 0;

        lblPeriode.setText("Periode: " + periodeText());

        try (Connection conn = DBConnection.getConnection()) {
            List<Object> params = new ArrayList<>();
            String where = buildWhere(params);

            // 1. Ringkasan transaksi
            String sqlSum = "SELECT COUNT(*) AS jml, COALESCE(SUM(t.total),0) AS pendapatan "
                          + "FROM transaksi t" + where;
            try (PreparedStatement ps = conn.prepareStatement(sqlSum)) {
                bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        totalTransaksi = rs.getLong("jml");
                        totalPendapatan = rs.getLong("pendapatan");
                    }
                }
            }

            // 2. Total item terjual
            String sqlItem = "SELECT COALESCE(SUM(d.qty),0) AS item "
                           + "FROM detail_transaksi d "
                           + "JOIN transaksi t ON t.kode_transaksi = d.kode_transaksi"
                           + where;
            try (PreparedStatement ps = conn.prepareStatement(sqlItem)) {
                bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) totalItem = rs.getLong("item");
                }
            }

            // 3. Menu terlaris
            String sqlMenu = "SELECT d.nama_menu, SUM(d.qty) AS qty, COALESCE(SUM(d.subtotal),0) AS omzet "
                           + "FROM detail_transaksi d "
                           + "JOIN transaksi t ON t.kode_transaksi = d.kode_transaksi"
                           + where
                           + " GROUP BY d.nama_menu ORDER BY qty DESC, omzet DESC LIMIT 10";
            try (PreparedStatement ps = conn.prepareStatement(sqlMenu)) {
                bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String nama = rs.getString("nama_menu");
                        long qty = rs.getLong("qty");
                        long omzet = rs.getLong("omzet");
                        dataMenu.add(new String[]{nama, String.valueOf(qty), String.valueOf(omzet)});
                        modelMenu.addRow(new Object[]{
                            nama, qty, "Rp " + Rupiah.format(omzet)
                        });
                    }
                }
            }

            // 4. Rekap harian
            String sqlHarian = "SELECT DATE(t.tanggal) AS tgl, COUNT(*) AS jml, "
                             + "COALESCE(SUM(t.total),0) AS pendapatan "
                             + "FROM transaksi t" + where
                             + " GROUP BY DATE(t.tanggal) ORDER BY tgl DESC";
            try (PreparedStatement ps = conn.prepareStatement(sqlHarian)) {
                bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
                    while (rs.next()) {
                        java.sql.Date tgl = rs.getDate("tgl");
                        long jml = rs.getLong("jml");
                        long pend = rs.getLong("pendapatan");
                        String tglStr = tgl == null ? "-" : df.format(tgl);
                        dataHarian.add(new String[]{tglStr, String.valueOf(jml), String.valueOf(pend)});
                        modelHarian.addRow(new Object[]{
                            tglStr, jml, "Rp " + Rupiah.format(pend)
                        });
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Gagal memuat laporan:\n" + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }

        long rata = totalTransaksi == 0 ? 0 : Math.round((double) totalPendapatan / totalTransaksi);
        lblPendapatan.setText("Rp " + Rupiah.format(totalPendapatan));
        lblTransaksi.setText(String.valueOf(totalTransaksi));
        lblRata.setText("Rp " + Rupiah.format(rata));
        lblItem.setText(String.valueOf(totalItem));
    }

    private void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    // ── Ekspor CSV ────────────────────────────────────────────────────────────
    private void eksporCSV() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Simpan Laporan CSV");
        fc.setSelectedFile(new File("Laporan_RoyalCafe_"
                + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".csv"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".csv")) {
            f = new File(f.getAbsolutePath() + ".csv");
        }

        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8")) {
            w.write('\uFEFF'); // BOM agar Excel membaca UTF-8
            String nl = System.lineSeparator();

            w.write("Laporan Penjualan Royal Cafe" + nl);
            w.write("Periode;" + csv(periodeText()) + nl);
            w.write(nl);
            w.write("Ringkasan;Nilai" + nl);
            w.write("Total Pendapatan;" + totalPendapatan + nl);
            w.write("Jumlah Transaksi;" + totalTransaksi + nl);
            long rata = totalTransaksi == 0 ? 0 : Math.round((double) totalPendapatan / totalTransaksi);
            w.write("Rata-rata per Transaksi;" + rata + nl);
            w.write("Total Item Terjual;" + totalItem + nl);
            w.write(nl);

            w.write("Menu Terlaris" + nl);
            w.write("Nama Menu;Qty;Omzet" + nl);
            for (String[] row : dataMenu) {
                w.write(csv(row[0]) + ";" + row[1] + ";" + row[2] + nl);
            }
            w.write(nl);

            w.write("Rekap Harian" + nl);
            w.write("Tanggal;Jumlah Transaksi;Pendapatan" + nl);
            for (String[] row : dataHarian) {
                w.write(csv(row[0]) + ";" + row[1] + ";" + row[2] + nl);
            }

            JOptionPane.showMessageDialog(this,
                "Laporan CSV disimpan ke:\n" + f.getAbsolutePath(),
                "Berhasil", JOptionPane.INFORMATION_MESSAGE);
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(f);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Gagal menyimpan CSV:\n" + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String csv(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    // ── Cetak PDF ─────────────────────────────────────────────────────────────
    private void cetakPDF() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Simpan Laporan PDF");
        fc.setSelectedFile(new File("Laporan_RoyalCafe_"
                + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".pdf"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Files (*.pdf)", "pdf"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".pdf")) {
            f = new File(f.getAbsolutePath() + ".pdf");
        }

        try {
            buatPDFLaporan(f);
            JOptionPane.showMessageDialog(this,
                "Laporan PDF disimpan ke:\n" + f.getAbsolutePath(),
                "Berhasil", JOptionPane.INFORMATION_MESSAGE);
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(f);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Gagal membuat PDF:\n" + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Susun laporan menjadi baris teks ber-style, lalu tulis ke PDF. */
    private void buatPDFLaporan(File file) throws Exception {
        String namaToko = Pengaturan.get("nama_toko");
        if (namaToko == null || namaToko.trim().isEmpty()) namaToko = "ROYAL CAFE";

        List<String[]> lines = new ArrayList<>();
        lines.add(new String[]{"T", namaToko});
        lines.add(new String[]{"S", "Laporan Penjualan"});
        lines.add(new String[]{"S", "Periode: " + periodeText()});
        lines.add(new String[]{"", ""});

        lines.add(new String[]{"B", "RINGKASAN"});
        long rata = totalTransaksi == 0 ? 0 : Math.round((double) totalPendapatan / totalTransaksi);
        lines.add(new String[]{"C", pad("Total Pendapatan", 28) + ": Rp " + Rupiah.format(totalPendapatan)});
        lines.add(new String[]{"C", pad("Jumlah Transaksi", 28) + ": " + totalTransaksi});
        lines.add(new String[]{"C", pad("Rata-rata per Transaksi", 28) + ": Rp " + Rupiah.format(rata)});
        lines.add(new String[]{"C", pad("Total Item Terjual", 28) + ": " + totalItem});
        lines.add(new String[]{"", ""});

        lines.add(new String[]{"B", "MENU TERLARIS"});
        lines.add(new String[]{"C", String.format("%-30s %6s %18s", "Nama Menu", "Qty", "Omzet")});
        lines.add(new String[]{"C", String.format("%-30s %6s %18s",
                "------------------------------", "-----", "-----------------")});
        if (dataMenu.isEmpty()) {
            lines.add(new String[]{"C", "(tidak ada data)"});
        } else {
            for (String[] row : dataMenu) {
                lines.add(new String[]{"C", String.format("%-30s %6s %18s",
                        cut(row[0], 30), row[1], "Rp " + Rupiah.format(Long.parseLong(row[2]))) });
            }
        }
        lines.add(new String[]{"", ""});

        lines.add(new String[]{"B", "REKAP HARIAN"});
        lines.add(new String[]{"C", String.format("%-16s %12s %20s", "Tanggal", "Transaksi", "Pendapatan")});
        lines.add(new String[]{"C", String.format("%-16s %12s %20s",
                "----------------", "-----------", "-------------------")});
        if (dataHarian.isEmpty()) {
            lines.add(new String[]{"C", "(tidak ada data)"});
        } else {
            for (String[] row : dataHarian) {
                lines.add(new String[]{"C", String.format("%-16s %12s %20s",
                        row[0], row[1], "Rp " + Rupiah.format(Long.parseLong(row[2]))) });
            }
        }
        lines.add(new String[]{"", ""});
        lines.add(new String[]{"S", "Dicetak: "
                + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date())
                + "  oleh " + currentUser});

        tulisPDF(file, lines);
    }

    /** Tulis daftar baris (style, teks) ke PDF A4 multi-halaman. */
    private void tulisPDF(File file, List<String[]> lines) throws Exception {
        final float PW = 595f, PH = 842f, MARGIN = 45f;
        final int perPage = 44;
        final float leading = 15f;

        int pageCount = Math.max(1, (lines.size() + perPage - 1) / perPage);

        List<String> objs = new ArrayList<>();
        objs.add("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        StringBuilder kids = new StringBuilder();
        for (int p = 0; p < pageCount; p++) {
            int pageId = 6 + p * 2;
            kids.append(pageId).append(" 0 R ");
        }
        objs.add("2 0 obj\n<< /Type /Pages /Kids [" + kids.toString().trim()
                + "] /Count " + pageCount + " >>\nendobj\n");
        objs.add("3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\nendobj\n");
        objs.add("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n");
        objs.add("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding >>\nendobj\n");

        for (int p = 0; p < pageCount; p++) {
            int pageId = 6 + p * 2;
            int contentId = pageId + 1;
            StringBuilder cs = new StringBuilder();
            float y = PH - MARGIN;

            // Header halaman
            cs.append("BT\n/F1 10 Tf\n");
            cs.append(String.format("%.2f %.2f Td\n", MARGIN, y));
            cs.append("(Laporan Penjualan - Royal Cafe) Tj\nET\n");
            y -= 6;
            cs.append(String.format("%.2f %.2f m %.2f %.2f l S\n", MARGIN, y, PW - MARGIN, y));
            y -= 20;

            int start = p * perPage;
            int end = Math.min(start + perPage, lines.size());
            for (int i = start; i < end; i++) {
                String style = lines.get(i)[0];
                String text = lines.get(i)[1];
                if (text == null) text = "";
                if ("T".equals(style)) {
                    cs.append("BT\n/F1 17 Tf\n");
                    cs.append(String.format("%.2f %.2f Td\n", MARGIN + (PW - 2 * MARGIN - 17 * 0.55f * text.length()) / 2, y));
                    cs.append("(" + esc(text) + ") Tj\nET\n");
                    y -= leading + 4;
                } else if ("S".equals(style)) {
                    cs.append("BT\n/F2 10 Tf\n");
                    cs.append(String.format("%.2f %.2f Td\n", MARGIN + (PW - 2 * MARGIN - 10 * 0.5f * text.length()) / 2, y));
                    cs.append("(" + esc(text) + ") Tj\nET\n");
                    y -= leading;
                } else if ("B".equals(style)) {
                    cs.append("BT\n/F1 11 Tf\n");
                    cs.append(String.format("%.2f %.2f Td\n", MARGIN, y));
                    cs.append("(" + esc(text) + ") Tj\nET\n");
                    y -= leading + 2;
                } else if ("C".equals(style)) {
                    cs.append("BT\n/F3 9 Tf\n");
                    cs.append(String.format("%.2f %.2f Td\n", MARGIN, y));
                    cs.append("(" + esc(text) + ") Tj\nET\n");
                    y -= leading - 2;
                } else {
                    y -= leading - 4;
                }
            }

            // Footer halaman
            cs.append("BT\n/F2 8 Tf\n");
            cs.append(String.format("%.2f %.2f Td\n", MARGIN, MARGIN / 2));
            cs.append("(Halaman " + (p + 1) + " dari " + pageCount + ") Tj\nET\n");

            String cStr = cs.toString();
            byte[] cBytes = cStr.getBytes("ISO-8859-1");

            objs.add(pageId + " 0 obj\n<< /Type /Page /Parent 2 0 R\n"
                + "   /MediaBox [0 0 " + String.format("%.2f", PW) + " " + String.format("%.2f", PH) + "]\n"
                + "   /Contents " + contentId + " 0 R\n"
                + "   /Resources << /Font << /F1 3 0 R /F2 4 0 R /F3 5 0 R >> >>\n>>\nendobj\n");
            objs.add(contentId + " 0 obj\n<< /Length " + cBytes.length + " >>\nstream\n"
                + cStr + "\nendstream\nendobj\n");
        }

        try (OutputStream out = new FileOutputStream(file)) {
            byte[] hdr = "%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n".getBytes("ISO-8859-1");
            out.write(hdr);
            int offset = hdr.length;
            int[] xo = new int[objs.size()];
            for (int i = 0; i < objs.size(); i++) {
                xo[i] = offset;
                byte[] ob = objs.get(i).getBytes("ISO-8859-1");
                out.write(ob);
                offset += ob.length;
            }
            int xrefOff = offset;
            StringBuilder xref = new StringBuilder("xref\n0 " + (objs.size() + 1) + "\n0000000000 65535 f \n");
            for (int x : xo) xref.append(String.format("%010d 00000 n \n", x));
            xref.append("trailer\n<< /Size " + (objs.size() + 1) + " /Root 1 0 R >>\nstartxref\n")
                .append(xrefOff).append("\n%%EOF\n");
            out.write(xref.toString().getBytes("ISO-8859-1"));
        }
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
                .replace("\r", "").replace("\n", " ")
                .replace("\u2013", "-").replace("\u2014", "-");
    }

    private String pad(String s, int n) {
        if (s.length() >= n) return s.substring(0, n);
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < n) sb.append(' ');
        return sb.toString();
    }

    private String cut(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n - 3) + "...";
    }
}
