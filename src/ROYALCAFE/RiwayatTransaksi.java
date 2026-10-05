package ROYALCAFE;

import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Riwayat Transaksi - Cafe Senja
 */
public class RiwayatTransaksi extends javax.swing.JFrame {

    private DefaultTableModel modelTransaksi;
    private DefaultTableModel modelDetail;

    public RiwayatTransaksi() {
        initComponents();
        setLocationRelativeTo(null);
        initTables();
        loadTransaksi();
    }

    private void initTables() {
        modelTransaksi = new DefaultTableModel(
            new String[]{"Kode", "Kasir", "Subtotal", "Pajak", "Total", "Bayar", "Kembalian", "Tanggal"}, 0
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblTransaksi.setModel(modelTransaksi);
        tblTransaksi.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Klik baris → langsung buka dialog struk custom
        tblTransaksi.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadDetail();       // tetap isi tabel detail bawah
                bukaDialogStruk();  // langsung buka dialog custom
            }
        });

        modelDetail = new DefaultTableModel(
            new String[]{"Nama Menu", "Harga", "Qty", "Subtotal"}, 0
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblDetail.setModel(modelDetail);
    }

    private void loadTransaksi() {
        modelTransaksi.setRowCount(0);
        String sql = "SELECT * FROM transaksi ORDER BY tanggal DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                modelTransaksi.addRow(new Object[]{
                    rs.getString("kode_transaksi"),
                    rs.getString("kasir"),
                    "Rp " + String.format("%,d", rs.getInt("subtotal")),
                    "Rp " + String.format("%,d", rs.getInt("pajak")),
                    "Rp " + String.format("%,d", rs.getInt("total")),
                    "Rp " + String.format("%,d", rs.getLong("bayar")),
                    "Rp " + String.format("%,d", rs.getLong("kembalian")),
                    rs.getTimestamp("tanggal")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat transaksi: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadDetail() {
        modelDetail.setRowCount(0);
        int row = tblTransaksi.getSelectedRow();
        if (row < 0) return;
        String kode = modelTransaksi.getValueAt(row, 0).toString();
        String sql = "SELECT * FROM detail_transaksi WHERE kode_transaksi = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kode);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                modelDetail.addRow(new Object[]{
                    rs.getString("nama_menu"),
                    "Rp " + String.format("%,d", rs.getInt("harga")),
                    rs.getInt("qty"),
                    "Rp " + String.format("%,d", rs.getInt("subtotal"))
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat detail: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cetakPDF() {
        int row = tblTransaksi.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Pilih transaksi terlebih dahulu!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }
        bukaDialogStruk();
    }

    /** Buka dialog struk custom langsung tanpa dialog lama */
    private void bukaDialogStruk() {
        int row = tblTransaksi.getSelectedRow();
        if (row < 0) return;

        String kode     = modelTransaksi.getValueAt(row, 0).toString();
        String kasir    = modelTransaksi.getValueAt(row, 1).toString();
        String subtotal = modelTransaksi.getValueAt(row, 2).toString();
        String pajak    = modelTransaksi.getValueAt(row, 3).toString();
        String total    = modelTransaksi.getValueAt(row, 4).toString();
        String bayar    = modelTransaksi.getValueAt(row, 5).toString();
        String kembali  = modelTransaksi.getValueAt(row, 6).toString();
        String tanggal  = modelTransaksi.getValueAt(row, 7).toString();

        // Ambil detail item dari database
        java.util.List<String[]> items = new java.util.ArrayList<>();
        String sql = "SELECT * FROM detail_transaksi WHERE kode_transaksi = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kode);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                items.add(new String[]{
                    rs.getString("nama_menu"),
                    String.valueOf(rs.getInt("qty")),
                    "Rp " + String.format("%,d", rs.getInt("subtotal")).replace(',', '.')
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat detail: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        tampilkanDialogStruk(kode, kasir, tanggal, items, subtotal, pajak, total, bayar, kembali);
    }

    private void tampilkanDialogStruk(String kode, String kasir, String tgl,
            java.util.List<String[]> items,
            String subtotal, String pajak, String total, String bayar, String kembali) {

        JDialog dlg = new JDialog(this, "Detail Transaksi", true);
        dlg.setSize(420, 560);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);
        dlg.setLayout(new java.awt.BorderLayout());

        // Header biru-ungu
        JPanel pHeader = new JPanel(new java.awt.BorderLayout());
        pHeader.setBackground(new java.awt.Color(51, 0, 255));
        pHeader.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        JLabel lblTitle = new JLabel("\u2713  Detail Transaksi");
        lblTitle.setFont(new java.awt.Font("Bell MT", java.awt.Font.BOLD, 18));
        lblTitle.setForeground(java.awt.Color.WHITE);
        pHeader.add(lblTitle, java.awt.BorderLayout.WEST);

        // Body putih
        JPanel pBody = new JPanel();
        pBody.setLayout(new BoxLayout(pBody, BoxLayout.Y_AXIS));
        pBody.setBackground(java.awt.Color.WHITE);
        pBody.setBorder(BorderFactory.createEmptyBorder(14, 20, 10, 20));

        JLabel lblCafe = new JLabel("ROYAL CAFE", SwingConstants.CENTER);
        lblCafe.setFont(new java.awt.Font("Bell MT", java.awt.Font.BOLD, 20));
        lblCafe.setForeground(new java.awt.Color(51, 0, 255));
        lblCafe.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel("Struk Pembayaran", SwingConstants.CENTER);
        lblSub.setFont(new java.awt.Font("Times New Roman", java.awt.Font.ITALIC, 13));
        lblSub.setForeground(java.awt.Color.GRAY);
        lblSub.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        JSeparator sep1 = new JSeparator();
        sep1.setForeground(new java.awt.Color(51, 0, 255));
        sep1.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 2));

        // Info transaksi
        JPanel pInfo = new JPanel(new java.awt.GridLayout(3, 2, 4, 2));
        pInfo.setBackground(java.awt.Color.WHITE);
        pInfo.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 60));
        java.awt.Font fInfo = new java.awt.Font("Times New Roman", java.awt.Font.PLAIN, 12);
        for (String[] r : new String[][]{{"Kode", kode}, {"Kasir", kasir}, {"Tgl", tgl}}) {
            JLabel k = new JLabel(r[0] + " :"); k.setFont(fInfo);
            JLabel v = new JLabel(r[1]);         v.setFont(fInfo);
            pInfo.add(k); pInfo.add(v);
        }

        JSeparator sep2 = new JSeparator();
        sep2.setForeground(new java.awt.Color(200, 200, 200));
        sep2.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 1));

        // Item pesanan
        JPanel pItems = new JPanel();
        pItems.setLayout(new BoxLayout(pItems, BoxLayout.Y_AXIS));
        pItems.setBackground(java.awt.Color.WHITE);
        java.awt.Font fItem = new java.awt.Font("Times New Roman", java.awt.Font.PLAIN, 12);
        for (String[] item : items) {
            JPanel row2 = new JPanel(new java.awt.BorderLayout());
            row2.setBackground(java.awt.Color.WHITE);
            row2.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 20));
            JLabel lNama  = new JLabel(item[0] + "  x" + item[1]); lNama.setFont(fItem);
            JLabel lHarga = new JLabel(item[2]); lHarga.setFont(fItem);
            lHarga.setHorizontalAlignment(SwingConstants.RIGHT);
            row2.add(lNama, java.awt.BorderLayout.WEST);
            row2.add(lHarga, java.awt.BorderLayout.EAST);
            pItems.add(row2);
            pItems.add(Box.createVerticalStrut(2));
        }

        JSeparator sep3 = new JSeparator();
        sep3.setForeground(new java.awt.Color(200, 200, 200));
        sep3.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 1));

        // Ringkasan pembayaran
        JPanel pSum = new JPanel(new java.awt.GridLayout(5, 2, 4, 3));
        pSum.setBackground(java.awt.Color.WHITE);
        pSum.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 100));
        java.awt.Font fSum  = new java.awt.Font("Times New Roman", java.awt.Font.PLAIN, 13);
        java.awt.Font fBold = new java.awt.Font("Times New Roman", java.awt.Font.BOLD, 13);
        String[][] sumRows = {
            {"Subtotal",  subtotal}, {"Pajak 10%", pajak},
            {"Total",     total},    {"Bayar",     bayar}, {"Kembalian", kembali}
        };
        for (int i = 0; i < sumRows.length; i++) {
            boolean bold = (i == 2 || i == 4);
            JLabel lk = new JLabel(sumRows[i][0] + " :"); lk.setFont(bold ? fBold : fSum);
            JLabel lv = new JLabel(sumRows[i][1]);         lv.setFont(bold ? fBold : fSum);
            lv.setHorizontalAlignment(SwingConstants.RIGHT);
            if (i == 2) lv.setForeground(new java.awt.Color(51, 0, 255));
            if (i == 4) lv.setForeground(new java.awt.Color(0, 130, 0));
            pSum.add(lk); pSum.add(lv);
        }

        JSeparator sep4 = new JSeparator();
        sep4.setForeground(new java.awt.Color(51, 0, 255));
        sep4.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 2));

        pBody.add(lblCafe); pBody.add(Box.createVerticalStrut(2));
        pBody.add(lblSub);  pBody.add(Box.createVerticalStrut(8));
        pBody.add(sep1);    pBody.add(Box.createVerticalStrut(8));
        pBody.add(pInfo);   pBody.add(Box.createVerticalStrut(6));
        pBody.add(sep2);    pBody.add(Box.createVerticalStrut(6));
        pBody.add(pItems);  pBody.add(Box.createVerticalStrut(6));
        pBody.add(sep3);    pBody.add(Box.createVerticalStrut(6));
        pBody.add(pSum);    pBody.add(Box.createVerticalStrut(8));
        pBody.add(sep4);

        JScrollPane scroll = new JScrollPane(pBody);
        scroll.setBorder(null);

        // Tombol
        JPanel pBtn = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 14, 10));
        pBtn.setBackground(new java.awt.Color(204, 204, 204));
        pBtn.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new java.awt.Color(51, 0, 255)));

        JButton btnCetak = new JButton("Cetak PDF");
        btnCetak.setBackground(new java.awt.Color(0, 153, 76));
        btnCetak.setForeground(java.awt.Color.WHITE);
        btnCetak.setFont(fBold); btnCetak.setFocusPainted(false);
        btnCetak.setPreferredSize(new java.awt.Dimension(130, 36));

        JButton btnOk = new JButton("OK");
        btnOk.setBackground(new java.awt.Color(51, 0, 255));
        btnOk.setForeground(java.awt.Color.WHITE);
        btnOk.setFont(fBold); btnOk.setFocusPainted(false);
        btnOk.setPreferredSize(new java.awt.Dimension(90, 36));

        btnCetak.addActionListener(e -> {
            javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
            fc.setDialogTitle("Simpan Struk PDF");
            fc.setSelectedFile(new java.io.File("Struk_" + kode + ".pdf"));
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Files (*.pdf)", "pdf"));
            if (fc.showSaveDialog(dlg) != javax.swing.JFileChooser.APPROVE_OPTION) return;
            java.io.File f = fc.getSelectedFile();
            if (!f.getName().toLowerCase().endsWith(".pdf"))
                f = new java.io.File(f.getAbsolutePath() + ".pdf");
            try {
                buatPDFManual(f, kode, kasir, tgl, items, subtotal, pajak, total, bayar, kembali);
                JOptionPane.showMessageDialog(dlg,
                    "Struk disimpan ke:\n" + f.getAbsolutePath(),
                    "Berhasil", JOptionPane.INFORMATION_MESSAGE);
                if (java.awt.Desktop.isDesktopSupported())
                    java.awt.Desktop.getDesktop().open(f);
                dlg.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Gagal PDF:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        btnOk.addActionListener(e -> dlg.dispose());

        pBtn.add(btnCetak); pBtn.add(btnOk);
        dlg.add(pHeader, java.awt.BorderLayout.NORTH);
        dlg.add(scroll,  java.awt.BorderLayout.CENTER);
        dlg.add(pBtn,    java.awt.BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    /**
     * Generate PDF struk menggunakan pure Java (sama dengan Dashboard).
     * Format: ROYAL CAFE header, tabel item, ringkasan pembayaran.
     */
    private void buatPDFManual(java.io.File file, String kode, String kasir, String tanggal,
            java.util.List<String[]> items, String subtotal, String pajak,
            String total, String bayar, String kembali) throws Exception {

        final float PW = 226, MARGIN = 14, cW = PW - 2 * MARGIN;
        float PH = Math.max(420, (10 + items.size() * 2 + 6) * 13f + 80);

        java.util.List<String> objs = new java.util.ArrayList<>();
        objs.add("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        objs.add("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

        StringBuilder cs = new StringBuilder();
        float y = PH - MARGIN - 8;

        // Judul ROYAL CAFE
        cs.append("BT\n/F1 13 Tf\n");
        cs.append(String.format("%.2f %.2f Td\n", MARGIN + (cW - 13 * 0.6f * "ROYAL CAFE".length()) / 2, y));
        cs.append("(ROYAL CAFE) Tj\nET\n"); y -= 14;

        cs.append("BT\n/F2 8 Tf\n");
        cs.append(String.format("%.2f %.2f Td\n", MARGIN + (cW - 8 * 0.5f * "Struk Pembayaran".length()) / 2, y));
        cs.append("(Struk Pembayaran) Tj\nET\n"); y -= 11;

        cs.append(String.format("%.2f %.2f m %.2f %.2f l S\n", MARGIN, y, PW - MARGIN, y)); y -= 9;

        // Info transaksi
        for (String[] row : new String[][]{{"Kode", kode}, {"Kasir", kasir}, {"Tgl", tanggal}}) {
            cs.append("BT\n/F2 8 Tf\n");
            cs.append(String.format("%.2f %.2f Td\n", MARGIN, y));
            cs.append("(" + row[0] + " : " + esc(row[1]) + ") Tj\nET\n"); y -= 10;
        }

        cs.append(String.format("%.2f %.2f m %.2f %.2f l S\n", MARGIN, y, PW - MARGIN, y)); y -= 9;

        // Header tabel
        cs.append("BT\n/F1 8 Tf\n"); cs.append(String.format("%.2f %.2f Td\n", MARGIN, y));
        cs.append("(Item) Tj\nET\n");
        cs.append("BT\n/F1 8 Tf\n"); cs.append(String.format("%.2f %.2f Td\n", MARGIN + 120, y));
        cs.append("(Subtotal) Tj\nET\n"); y -= 9;
        cs.append(String.format("%.2f %.2f m %.2f %.2f l S\n", MARGIN, y, PW - MARGIN, y)); y -= 10;

        // Baris item
        for (String[] item : items) {
            // Nama + qty
            cs.append("BT\n/F2 8 Tf\n"); cs.append(String.format("%.2f %.2f Td\n", MARGIN, y));
            cs.append("(" + esc(item[0]) + "  x" + esc(item[1]) + ") Tj\nET\n");
            // Subtotal
            cs.append("BT\n/F2 8 Tf\n"); cs.append(String.format("%.2f %.2f Td\n", MARGIN + 120, y));
            cs.append("(" + esc(item[2]) + ") Tj\nET\n"); y -= 11;
        }

        cs.append(String.format("%.2f %.2f m %.2f %.2f l S\n", MARGIN, y, PW - MARGIN, y)); y -= 11;

        // Ringkasan
        String[][] sum = {
            {"Subtotal",  subtotal},
            {"Pajak 10%", pajak},
            {"Total",     total},
            {"Bayar",     bayar},
            {"Kembalian", kembali}
        };
        for (int i = 0; i < sum.length; i++) {
            String fn = (i == 2 || i == 4) ? "/F1" : "/F2";
            float fs = (i == 2 || i == 4) ? 9f : 8f;
            cs.append("BT\n" + fn + " " + fs + " Tf\n");
            cs.append(String.format("%.2f %.2f Td\n", MARGIN + 5, y));
            cs.append("(" + esc(sum[i][0]) + ") Tj\nET\n");
            cs.append("BT\n" + fn + " " + fs + " Tf\n");
            cs.append(String.format("%.2f %.2f Td\n", MARGIN + 100, y));
            cs.append("(: " + esc(sum[i][1]) + ") Tj\nET\n");
            y -= (i == 1 ? 12 : 10);
        }

        y -= 4;
        cs.append(String.format("%.2f %.2f m %.2f %.2f l S\n", MARGIN, y, PW - MARGIN, y)); y -= 11;

        // Footer
        String footer = "Terima kasih atas kunjungan Anda!";
        cs.append("BT\n/F2 8 Tf\n");
        cs.append(String.format("%.2f %.2f Td\n", MARGIN + (cW - 8 * 0.45f * footer.length()) / 2, y));
        cs.append("(" + footer + ") Tj\nET\n");

        String cStr = cs.toString();
        byte[] csB = cStr.getBytes("ISO-8859-1");

        objs.add("3 0 obj\n<< /Type /Page /Parent 2 0 R\n"
            + "   /MediaBox [0 0 " + String.format("%.2f", PW) + " " + String.format("%.2f", PH) + "]\n"
            + "   /Contents 4 0 R\n"
            + "   /Resources << /Font << /F1 5 0 R /F2 6 0 R >> >>\n>>\nendobj\n");
        objs.add("4 0 obj\n<< /Length " + csB.length + " >>\nstream\n" + cStr + "\nendstream\nendobj\n");
        objs.add("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\nendobj\n");
        objs.add("6 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n");

        try (java.io.OutputStream out = new java.io.FileOutputStream(file)) {
            byte[] hdr = "%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n".getBytes("ISO-8859-1");
            out.write(hdr); int offset = hdr.length;
            int[] xo = new int[objs.size()];
            for (int i = 0; i < objs.size(); i++) {
                xo[i] = offset;
                byte[] ob = objs.get(i).getBytes("ISO-8859-1");
                out.write(ob); offset += ob.length;
            }
            int xrefOff = offset;
            StringBuilder xref = new StringBuilder("xref\n0 " + (objs.size() + 1) + "\n0000000000 65535 f \n");
            for (int x : xo) xref.append(String.format("%010d 00000 n \n", x));
            xref.append("trailer\n<< /Size " + (objs.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xrefOff + "\n%%EOF\n");
            out.write(xref.toString().getBytes("ISO-8859-1"));
        }
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)").replace("\r", "").replace("\n", " ");
    }

    private void initComponents() {
        jPanel1 = new javax.swing.JPanel();
        jLabelTitle = new javax.swing.JLabel();
        jSplitPane1 = new javax.swing.JSplitPane(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jScrollPane1 = new javax.swing.JScrollPane();
        tblTransaksi = new javax.swing.JTable();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblDetail = new javax.swing.JTable();
        jLabelDetail = new javax.swing.JLabel();
        btnRefresh = new javax.swing.JButton();
        btnKembali = new javax.swing.JButton();
        btnCetakPDF = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Riwayat Transaksi - Royal Cofe");
        setPreferredSize(new java.awt.Dimension(800, 600));

        // Header biru-ungu sesuai tema login
        jPanel1.setBackground(new java.awt.Color(51, 0, 255));
        jLabelTitle.setFont(new java.awt.Font("Bell MT", 3, 22));
        jLabelTitle.setForeground(java.awt.Color.WHITE);
        jLabelTitle.setHorizontalAlignment(SwingConstants.CENTER);
        jLabelTitle.setText("Riwayat Transaksi - Royal Cofe");

        javax.swing.GroupLayout p1 = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(p1);
        p1.setHorizontalGroup(p1.createParallelGroup().addComponent(jLabelTitle, Short.MAX_VALUE, Short.MAX_VALUE, Short.MAX_VALUE));
        p1.setVerticalGroup(p1.createParallelGroup().addComponent(jLabelTitle, 50, 50, 50));

        // Background abu-abu sesuai tema login
        getContentPane().setBackground(new java.awt.Color(204, 204, 204));

        jScrollPane1.setViewportView(tblTransaksi);
        jScrollPane2.setViewportView(tblDetail);

        // Warna header tabel
        tblTransaksi.getTableHeader().setBackground(new java.awt.Color(51, 0, 255));
        tblTransaksi.getTableHeader().setForeground(java.awt.Color.WHITE);
        tblTransaksi.getTableHeader().setFont(new java.awt.Font("Times New Roman", java.awt.Font.BOLD, 12));
        tblTransaksi.setBackground(java.awt.Color.WHITE);
        tblTransaksi.setGridColor(new java.awt.Color(180, 180, 220));
        tblTransaksi.setFont(new java.awt.Font("Times New Roman", java.awt.Font.PLAIN, 12));
        tblTransaksi.setRowHeight(22);

        tblDetail.getTableHeader().setBackground(new java.awt.Color(51, 0, 255));
        tblDetail.getTableHeader().setForeground(java.awt.Color.WHITE);
        tblDetail.getTableHeader().setFont(new java.awt.Font("Times New Roman", java.awt.Font.BOLD, 12));
        tblDetail.setBackground(java.awt.Color.WHITE);
        tblDetail.setGridColor(new java.awt.Color(180, 180, 220));
        tblDetail.setFont(new java.awt.Font("Times New Roman", java.awt.Font.PLAIN, 12));
        tblDetail.setRowHeight(22);

        jLabelDetail.setFont(new java.awt.Font("Bell MT", java.awt.Font.BOLD, 14));
        jLabelDetail.setForeground(new java.awt.Color(51, 0, 255));
        jLabelDetail.setText("Detail Transaksi:");

        btnRefresh.setText("Refresh");
        btnRefresh.setBackground(new java.awt.Color(51, 0, 255));
        btnRefresh.setForeground(java.awt.Color.WHITE);
        btnRefresh.setFont(new java.awt.Font("Times New Roman", java.awt.Font.BOLD, 13));
        btnRefresh.setFocusPainted(false);
        btnRefresh.addActionListener(e -> loadTransaksi());

        btnKembali.setText("Kembali");
        btnKembali.setBackground(new java.awt.Color(204, 0, 0));
        btnKembali.setForeground(java.awt.Color.WHITE);
        btnKembali.setFont(new java.awt.Font("Times New Roman", java.awt.Font.BOLD, 13));
        btnKembali.setFocusPainted(false);
        btnKembali.addActionListener(e -> this.dispose());

        btnCetakPDF.setText("Cetak PDF");
        btnCetakPDF.setBackground(new java.awt.Color(0, 153, 76));
        btnCetakPDF.setForeground(java.awt.Color.WHITE);
        btnCetakPDF.setFont(new java.awt.Font("Times New Roman", java.awt.Font.BOLD, 13));
        btnCetakPDF.setFocusPainted(false);
        btnCetakPDF.addActionListener(e -> cetakPDF());

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup()
            .addComponent(jPanel1)
            .addGroup(layout.createSequentialGroup().addGap(10)
                .addGroup(layout.createParallelGroup()
                    .addComponent(jScrollPane1)
                    .addComponent(jLabelDetail)
                    .addComponent(jScrollPane2)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnRefresh).addGap(10).addComponent(btnCetakPDF).addGap(10).addComponent(btnKembali)))
                .addGap(10))
        );
        layout.setVerticalGroup(layout.createSequentialGroup()
            .addComponent(jPanel1, 50, 50, 50).addGap(10)
            .addComponent(jScrollPane1, 200, 200, 200).addGap(10)
            .addComponent(jLabelDetail).addGap(5)
            .addComponent(jScrollPane2, 150, 150, 150).addGap(10)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(btnRefresh).addComponent(btnCetakPDF).addComponent(btnKembali))
            .addGap(10)
        );

        pack();
    }

    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel jLabelTitle, jLabelDetail;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JScrollPane jScrollPane1, jScrollPane2;
    private javax.swing.JTable tblTransaksi, tblDetail;
    private javax.swing.JButton btnRefresh, btnKembali, btnCetakPDF;
}
