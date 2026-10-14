package ROYALCAFE;

import java.awt.*;
import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Dashboard - Royal Cafe POS System
 * Menu load dari database, scrollable, tampilan modern.
 */
public class Dashboard extends javax.swing.JFrame {

    // ── State ─────────────────────────────────────────────────────────────────
    private String currentUser = "kasir";
    private String currentRole = "kasir";
    private String lastStruk   = "";
    private long   bayarInput  = 0;

    // ── Data menu ─────────────────────────────────────────────────────────────
    private static class MenuRow {
        int id, harga, stok;
        String nama, kategori, gambar;
        JSpinner  spinner;
        JCheckBox checkBox;
    }
    private final List<MenuRow> menuRows = new ArrayList<>();

    // ── Komponen UI ───────────────────────────────────────────────────────────
    private JLabel  lblUserInfo, lblClock;
    private JPanel  menuGridPanel;
    private JTextArea  txtStruk;
    private JTextField txtSubtotal, txtPajak, txtTotal, txtBayar, txtKembalian;
    private JButton btnBayar, btnReset, btnKelolaMenu, btnRiwayat, btnKeluar;

    // ── Warna tema (sesuai login: header biru-ungu, bg abu-abu) ──────────────
    private static final Color HEADER_BG  = new Color(51, 0, 255);    // biru-ungu = login header
    private static final Color MAIN_BG    = new Color(204, 204, 204); // abu-abu   = login background
    private static final Color CARD_BG    = new Color(255, 255, 255); // putih kartu menu
    private static final Color GREEN      = new Color(0, 153, 76);    // tombol Bayar
    private static final Color ORANGE     = new Color(255, 140, 0);   // tombol Reset
    private static final Color BLUE       = new Color(51, 0, 255);    // tombol Kelola Menu
    private static final Color PURPLE     = new Color(102, 0, 204);   // tombol Riwayat
    private static final Color RED        = new Color(204, 0, 0);     // tombol Keluar
    private static final Color PRICE_GREEN= new Color(0, 130, 0);     // harga menu

    // ─────────────────────────────────────────────────────────────────────────
    public Dashboard() { this("kasir", "kasir"); }

    public Dashboard(String username, String role) {
        this.currentUser = username;
        this.currentRole = role;
        buildUI();
        startClock();
        loadMenuFromDB();
    }

    // ── Build UI ──────────────────────────────────────────────────────────────
    private void buildUI() {
        setTitle("ROYAL COFFE - POS System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 680);
        setLocationRelativeTo(null);
        setBackground(Color.WHITE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(MAIN_BG);

        root.add(buildHeader(),  BorderLayout.NORTH);
        root.add(buildCenter(),  BorderLayout.CENTER);
        root.add(buildFooter(),  BorderLayout.SOUTH);

        setContentPane(root);
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(HEADER_BG);
        p.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel lblTitle = new JLabel("  \u25A1  ROYAL CAFE");
        lblTitle.setFont(new Font("Bell MT", Font.BOLD | Font.ITALIC, 22));
        lblTitle.setForeground(Color.WHITE);

        JPanel rightPanel = new JPanel(new GridLayout(2, 1));
        rightPanel.setOpaque(false);
        JLabel lblUser = new JLabel("User: " + currentUser.toUpperCase()
                + " (" + currentRole + ")", SwingConstants.RIGHT);
        lblUser.setFont(new Font("Times New Roman", Font.PLAIN, 13));
        lblUser.setForeground(Color.WHITE);
        lblClock = new JLabel("--:--:--", SwingConstants.RIGHT);
        lblClock.setFont(new Font("Times New Roman", Font.PLAIN, 13));
        lblClock.setForeground(Color.WHITE);
        rightPanel.add(lblUser);
        rightPanel.add(lblClock);

        p.add(lblTitle,   BorderLayout.WEST);
        p.add(rightPanel, BorderLayout.EAST);
        return p;
    }

    // ── Center: menu kiri + struk kanan ──────────────────────────────────────
    private JPanel buildCenter() {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setBackground(MAIN_BG);
        p.setBorder(new EmptyBorder(8, 8, 4, 8));

        p.add(buildMenuPanel(), BorderLayout.CENTER);
        p.add(buildStrukPanel(), BorderLayout.EAST);
        return p;
    }

    // ── Panel menu (kiri) ─────────────────────────────────────────────────────
    private JPanel buildMenuPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 4));
        wrapper.setBackground(MAIN_BG);

        JLabel lblMenu = new JLabel("Menu");
        lblMenu.setFont(new Font("Bell MT", Font.BOLD, 16));
        lblMenu.setForeground(new Color(51, 0, 255));
        lblMenu.setBorder(new EmptyBorder(0, 4, 0, 0));
        wrapper.add(lblMenu, BorderLayout.NORTH);

        menuGridPanel = new JPanel(new GridLayout(0, 2, 6, 6));
        menuGridPanel.setBackground(MAIN_BG);

        JScrollPane scroll = new JScrollPane(menuGridPanel);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.setBorder(new LineBorder(new Color(200, 200, 200)));
        scroll.setPreferredSize(new Dimension(620, 0));

        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    // ── Panel struk (kanan) ───────────────────────────────────────────────────
    private JPanel buildStrukPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(MAIN_BG);
        p.setPreferredSize(new Dimension(290, 0));

        JLabel lblStruk = new JLabel("Struk");
        lblStruk.setFont(new Font("Bell MT", Font.BOLD, 16));
        lblStruk.setForeground(new Color(51, 0, 255));
        p.add(lblStruk, BorderLayout.NORTH);

        txtStruk = new JTextArea();
        txtStruk.setEditable(false);
        txtStruk.setFont(new Font("Monospaced", Font.PLAIN, 11));
        txtStruk.setBackground(Color.WHITE);
        JScrollPane sp = new JScrollPane(txtStruk);
        sp.setBorder(new LineBorder(new Color(200, 200, 200)));
        p.add(sp, BorderLayout.CENTER);

        p.add(buildTotalPanel(), BorderLayout.SOUTH);
        return p;
    }

    // ── Panel total ───────────────────────────────────────────────────────────
    private JPanel buildTotalPanel() {
        JPanel p = new JPanel(new GridLayout(5, 2, 4, 4));
        p.setBackground(MAIN_BG);
        p.setBorder(new EmptyBorder(4, 0, 4, 0));

        txtSubtotal  = roField("Rp 0", Color.BLACK, Font.PLAIN);
        txtPajak     = roField("Rp 0", Color.BLACK, Font.PLAIN);
        txtTotal     = roField("Rp 0", Color.BLACK, Font.BOLD);
        txtBayar     = roField("Rp 0", Color.BLACK, Font.PLAIN);
        txtKembalian = roField("Rp 0", PRICE_GREEN,  Font.BOLD);

        p.add(lbl("Subtotal:"));    p.add(txtSubtotal);
        p.add(lbl("Pajak (10%):")); p.add(txtPajak);
        p.add(lbl("Total:"));       p.add(txtTotal);
        p.add(lbl("Bayar:"));       p.add(txtBayar);
        p.add(lbl("Kembalian:"));   p.add(txtKembalian);
        return p;
    }

    // ── Footer tombol ─────────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        p.setBackground(MAIN_BG);
        p.setBorder(new MatteBorder(2, 0, 0, 0, HEADER_BG));

        btnBayar      = footBtn("\u2713 Bayar",       GREEN);
        btnReset      = footBtn("\u21BA Reset",        ORANGE);
        btnKelolaMenu = footBtn("\u2699 Kelola Menu",  BLUE);
        btnRiwayat    = footBtn("\u2630 Riwayat",      PURPLE);
        btnKeluar     = footBtn("\u2715 Keluar",       RED);

        btnBayar.addActionListener(e -> prosesBayar());
        btnReset.addActionListener(e -> reset());
        btnKelolaMenu.addActionListener(e -> {
            MenuCRUD crud = new MenuCRUD(this::loadMenuFromDB);
            crud.setVisible(true);
            crud.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override public void windowClosed(java.awt.event.WindowEvent we) {
                    loadMenuFromDB();
                }
            });
        });
        btnRiwayat.addActionListener(e -> new RiwayatTransaksi().setVisible(true));
        btnKeluar.addActionListener(e -> konfirmasiKeluar());

        // Tombol Edit Transaksi — hanya admin
        JButton btnEdit = footBtn("\u270E Edit Transaksi", new Color(0, 128, 128));
        btnEdit.addActionListener(e -> bukaEditTransaksi());
        btnEdit.setVisible("admin".equals(currentRole));

        btnKelolaMenu.setVisible("admin".equals(currentRole));

        p.add(btnBayar); p.add(btnReset); p.add(btnKelolaMenu);
        p.add(btnRiwayat); p.add(btnEdit); p.add(btnKeluar);
        return p;
    }

    // ── Helper UI ─────────────────────────────────────────────────────────────
    private JTextField roField(String text, Color fg, int style) {
        JTextField f = new JTextField(text);
        f.setEditable(false);
        f.setHorizontalAlignment(JTextField.RIGHT);
        f.setFont(new Font("Arial", style, 13));
        f.setForeground(fg);
        f.setBackground(Color.WHITE);
        f.setBorder(new LineBorder(new Color(200, 200, 200)));
        return f;
    }
    private JLabel lbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Times New Roman", Font.BOLD, 13));
        l.setForeground(new Color(30, 30, 30));
        return l;
    }
    private JButton footBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Arial", Font.BOLD, 14));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setPreferredSize(new Dimension(150, 42));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ── Load menu dari database ───────────────────────────────────────────────
    public void loadMenuFromDB() {
        menuRows.clear();
        menuGridPanel.removeAll();

        String sql = "SELECT id, nama_menu, kategori, harga, stok, gambar FROM menu ORDER BY kategori, nama_menu";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                MenuRow m = new MenuRow();
                m.id       = rs.getInt("id");
                m.nama     = rs.getString("nama_menu");
                m.kategori = rs.getString("kategori");
                m.harga    = rs.getInt("harga");
                m.stok     = rs.getInt("stok");
                m.gambar   = rs.getString("gambar");

                m.spinner  = new JSpinner(new SpinnerNumberModel(0, 0, m.stok, 1));
                m.spinner.setFont(new Font("Arial", Font.PLAIN, 13));
                m.checkBox = new JCheckBox("Pilih");
                m.checkBox.setFont(new Font("Arial", Font.PLAIN, 12));
                m.checkBox.setBackground(Color.WHITE);

                m.spinner.addChangeListener(e -> hitungTotal());
                m.checkBox.addActionListener(e -> hitungTotal());

                menuGridPanel.add(buildCard(m));
                menuRows.add(m);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Gagal memuat menu:\n" + e.getMessage(),
                "Error Database", JOptionPane.ERROR_MESSAGE);
        }

        menuGridPanel.revalidate();
        menuGridPanel.repaint();
        hitungTotal();
    }

    // ── Kartu menu ────────────────────────────────────────────────────────────
    private JPanel buildCard(MenuRow m) {
        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setBackground(CARD_BG);
        card.setBorder(new CompoundBorder(
            new LineBorder(new Color(51, 0, 255), 1),
            new EmptyBorder(0, 0, 6, 0)
        ));

        // Gambar
        JLabel imgLbl = new JLabel();
        imgLbl.setPreferredSize(new Dimension(200, 150));
        imgLbl.setHorizontalAlignment(SwingConstants.CENTER);
        imgLbl.setBackground(new Color(230, 230, 230));
        imgLbl.setOpaque(true);
        loadImg(imgLbl, m.gambar);
        card.add(imgLbl, BorderLayout.NORTH);

        // Info bawah gambar
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(CARD_BG);
        info.setBorder(new EmptyBorder(6, 10, 4, 10));

        JLabel lblNama = new JLabel(m.nama);
        lblNama.setFont(new Font("Bell MT", Font.BOLD, 14));
        lblNama.setForeground(new Color(30, 30, 80));
        lblNama.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblHarga = new JLabel("Rp " + String.format("%,d", m.harga).replace(',', '.'));
        lblHarga.setFont(new Font("Times New Roman", Font.BOLD, 13));
        lblHarga.setForeground(PRICE_GREEN);
        lblHarga.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblStok = new JLabel("Stok: " + m.stok);
        lblStok.setFont(new Font("Times New Roman", Font.PLAIN, 11));
        lblStok.setForeground(m.stok <= 5 ? Color.RED : new Color(100, 100, 100));
        lblStok.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel qtyRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        qtyRow.setBackground(CARD_BG);
        qtyRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblQty = new JLabel("Qty:");
        lblQty.setFont(new Font("Times New Roman", Font.BOLD, 12));
        m.spinner.setPreferredSize(new Dimension(70, 24));
        qtyRow.add(lblQty);
        qtyRow.add(m.spinner);

        m.checkBox.setText("Pilih");
        m.checkBox.setFont(new Font("Times New Roman", Font.BOLD, 12));
        m.checkBox.setBackground(CARD_BG);
        m.checkBox.setForeground(new Color(51, 0, 255));
        m.checkBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        info.add(lblNama);
        info.add(Box.createVerticalStrut(2));
        info.add(lblHarga);
        info.add(lblStok);
        info.add(Box.createVerticalStrut(4));
        info.add(qtyRow);
        info.add(Box.createVerticalStrut(2));
        info.add(m.checkBox);

        card.add(info, BorderLayout.CENTER);
        return card;
    }

    private void loadImg(JLabel label, String gambar) {
        if (gambar == null || gambar.trim().isEmpty()) return;
        try {
            java.net.URL url = getClass().getResource("/Image/" + gambar.trim());
            if (url != null) {
                Image img = new ImageIcon(url).getImage()
                        .getScaledInstance(200, 150, Image.SCALE_SMOOTH);
                label.setIcon(new ImageIcon(img));
            }
        } catch (Exception ignored) {}
    }

    // ── Hitung total ──────────────────────────────────────────────────────────
    private void hitungTotal() {
        // Header struk: nama user dan tanggal/jam
        String waktu = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date());
        StringBuilder sb = new StringBuilder();
        sb.append("User: ").append(currentUser.toUpperCase())
          .append(" (").append(currentRole).append(")\n");
        sb.append(waktu).append("\n");
        sb.append("------------------------------\n");

        int subtotal = 0;
        for (MenuRow m : menuRows) {
            if (m.checkBox.isSelected()) {
                int qty = (Integer) m.spinner.getValue();
                int sub = qty * m.harga;
                subtotal += sub;
                sb.append(String.format("%-18s x%d\n  = Rp %s\n",
                    m.nama.length() > 18 ? m.nama.substring(0, 16) + ".." : m.nama,
                    qty,
                    String.format("%,d", sub).replace(',', '.')));
            }
        }
        int pajak      = (int) Math.round(subtotal * 0.1);
        int totalBayar = subtotal + pajak;

        txtStruk.setText(sb.toString());
        txtSubtotal.setText("Rp " + fmt(subtotal));
        txtPajak.setText("Rp " + fmt(pajak));
        txtTotal.setText("Rp " + fmt(totalBayar));
        txtBayar.setText("Rp 0");
        txtKembalian.setText("Rp 0");
        bayarInput = 0;
    }

    private String fmt(long v) {
        return String.format("%,d", v).replace(',', '.');
    }

    // ── Proses bayar ──────────────────────────────────────────────────────────
    private void prosesBayar() {
        int subtotal = 0;
        for (MenuRow m : menuRows)
            if (m.checkBox.isSelected())
                subtotal += (Integer) m.spinner.getValue() * m.harga;

        if (subtotal <= 0) {
            JOptionPane.showMessageDialog(this, "Belum ada item yang dipilih!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int pajak    = (int) Math.round(subtotal * 0.1);
        int totalInt = subtotal + pajak;

        // ── Dialog pembayaran custom ──────────────────────────────────────────
        JDialog dlg = new JDialog(this, "Pembayaran", true);
        dlg.setLayout(new BorderLayout(10, 10));
        dlg.setSize(380, 280);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        // Panel info
        JPanel pInfo = new JPanel(new GridLayout(0, 2, 8, 6));
        pInfo.setBackground(new Color(204, 204, 204));
        pInfo.setBorder(BorderFactory.createEmptyBorder(14, 16, 8, 16));

        java.awt.Font fBold = new java.awt.Font("Times New Roman", java.awt.Font.BOLD, 14);
        java.awt.Font fPlain = new java.awt.Font("Times New Roman", java.awt.Font.PLAIN, 14);

        JLabel lblTotalKey = new JLabel("Total Bayar:");
        lblTotalKey.setFont(fBold);
        JLabel lblTotalVal = new JLabel("Rp " + fmt(totalInt));
        lblTotalVal.setFont(fBold);
        lblTotalVal.setForeground(new Color(51, 0, 255));

        JLabel lblBayarKey = new JLabel("Uang Bayar:");
        lblBayarKey.setFont(fBold);
        JTextField tfBayar = new JTextField();
        tfBayar.setFont(fPlain);
        tfBayar.setHorizontalAlignment(JTextField.RIGHT);

        JLabel lblStatusKey = new JLabel("Status:");
        lblStatusKey.setFont(fBold);
        JLabel lblStatus = new JLabel("Masukkan uang bayar");
        lblStatus.setFont(fBold);
        lblStatus.setForeground(Color.GRAY);

        JLabel lblKembaliKey = new JLabel("Kembalian:");
        lblKembaliKey.setFont(fBold);
        JLabel lblKembaliVal = new JLabel("Rp 0");
        lblKembaliVal.setFont(fBold);
        lblKembaliVal.setForeground(new Color(0, 130, 0));

        pInfo.add(lblTotalKey);   pInfo.add(lblTotalVal);
        pInfo.add(lblBayarKey);   pInfo.add(tfBayar);
        pInfo.add(lblStatusKey);  pInfo.add(lblStatus);
        pInfo.add(lblKembaliKey); pInfo.add(lblKembaliVal);

        // Update status & kembalian + format Rp real-time saat mengetik
        final long[] hasilBayar = {0};
        final boolean[] updating = {false};
        tfBayar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void update() {
                if (updating[0]) return;
                SwingUtilities.invokeLater(() -> {
                    if (updating[0]) return;
                    updating[0] = true;
                    try {
                        String raw = tfBayar.getText().trim().replaceAll("[^0-9]", "");
                        if (raw.isEmpty()) {
                            tfBayar.setText("");
                            lblStatus.setText("Masukkan uang bayar");
                            lblStatus.setForeground(Color.GRAY);
                            lblKembaliVal.setText("Rp 0");
                            hasilBayar[0] = 0;
                            return;
                        }
                        long bayar = Long.parseLong(raw);
                        hasilBayar[0] = bayar;
                        // Format dengan titik ribuan
                        String formatted = fmt(bayar);
                        int caretPos = tfBayar.getCaretPosition();
                        tfBayar.setText(formatted);
                        // Jaga posisi kursor di akhir
                        tfBayar.setCaretPosition(Math.min(formatted.length(), tfBayar.getText().length()));
                        // Update status
                        long selisih = bayar - totalInt;
                        if (selisih < 0) {
                            lblStatus.setText("✗ Kurang Rp " + fmt(-selisih));
                            lblStatus.setForeground(Color.RED);
                            lblKembaliVal.setText("Rp 0");
                        } else if (selisih == 0) {
                            lblStatus.setText("✓ Pas");
                            lblStatus.setForeground(new Color(0, 130, 0));
                            lblKembaliVal.setText("Rp 0");
                        } else {
                            lblStatus.setText("✓ Lebih");
                            lblStatus.setForeground(new Color(0, 130, 0));
                            lblKembaliVal.setText("Rp " + fmt(selisih));
                        }
                    } catch (NumberFormatException ex) {
                        lblStatus.setText("Angka tidak valid");
                        lblStatus.setForeground(Color.RED);
                    } finally {
                        updating[0] = false;
                    }
                });
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });

        // Tombol
        JPanel pBtn = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        pBtn.setBackground(new Color(204, 204, 204));

        JButton btnOk = new JButton("Bayar");
        btnOk.setBackground(new Color(0, 153, 76));
        btnOk.setForeground(Color.WHITE);
        btnOk.setFont(fBold);
        btnOk.setPreferredSize(new Dimension(110, 36));
        btnOk.setFocusPainted(false);

        JButton btnBatal = new JButton("Batal");
        btnBatal.setBackground(new Color(204, 0, 0));
        btnBatal.setForeground(Color.WHITE);
        btnBatal.setFont(fBold);
        btnBatal.setPreferredSize(new Dimension(110, 36));
        btnBatal.setFocusPainted(false);

        final boolean[] konfirmasi = {false};
        btnOk.addActionListener(e -> {
            long bayar = hasilBayar[0];
            if (bayar < totalInt) {
                lblStatus.setForeground(Color.RED);
                tfBayar.requestFocus();
                return;
            }
            konfirmasi[0] = true;
            dlg.dispose();
        });
        btnBatal.addActionListener(e -> dlg.dispose());
        // Enter = Bayar
        tfBayar.addActionListener(e -> btnOk.doClick());

        pBtn.add(btnOk);
        pBtn.add(btnBatal);

        dlg.getContentPane().setBackground(new Color(204, 204, 204));
        dlg.add(pInfo, BorderLayout.CENTER);
        dlg.add(pBtn,  BorderLayout.SOUTH);
        tfBayar.requestFocusInWindow();
        dlg.setVisible(true);

        // Setelah dialog ditutup
        if (!konfirmasi[0]) return;
        long bayar     = hasilBayar[0];
        long kembalian = bayar - totalInt;
        txtBayar.setText("Rp " + fmt(bayar));
        txtKembalian.setText("Rp " + fmt(kembalian));
        simpanTransaksi(subtotal, pajak, totalInt, bayar, kembalian);
    }

    // ── Simpan transaksi ──────────────────────────────────────────────────────
    private void simpanTransaksi(int subtotal, int pajak, int totalInt, long bayar, long kembalian) {
        String kode = "TRX" + new SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());

        try (Connection conn = DBConnection.getConnection()) {
            // Pastikan kolom bayar/kembalian ada. DDL dijalankan SEBELUM transaksi
            // karena ALTER TABLE memicu implicit commit yang akan memutus transaksi.
            pastikanKolomBayarAda(conn);

            conn.setAutoCommit(false);
            try {
                // Header
                String sqlH = "INSERT INTO transaksi (kode_transaksi, kasir, subtotal, pajak, total, bayar, kembalian) VALUES (?,?,?,?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(sqlH)) {
                    ps.setString(1, kode); ps.setString(2, currentUser);
                    ps.setInt(3, subtotal); ps.setInt(4, pajak); ps.setInt(5, totalInt);
                    ps.setLong(6, bayar); ps.setLong(7, kembalian);
                    ps.executeUpdate();
                }

                // Detail
                String sqlD = "INSERT INTO detail_transaksi (kode_transaksi, nama_menu, harga, qty, subtotal) VALUES (?,?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(sqlD)) {
                    for (MenuRow m : menuRows) {
                        if (m.checkBox.isSelected()) {
                            int qty = (Integer) m.spinner.getValue();
                            ps.setString(1, kode); ps.setString(2, m.nama);
                            ps.setInt(3, m.harga); ps.setInt(4, qty); ps.setInt(5, qty * m.harga);
                            ps.addBatch();
                        }
                    }
                    ps.executeBatch();
                }

                // Kurangi stok di database sesuai qty yang dibeli.
                // Jika ada baris yang tidak ter-update (stok kurang), batalkan semua.
                String sqlStok = "UPDATE menu SET stok = stok - ? WHERE id = ? AND stok >= ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlStok)) {
                    for (MenuRow m : menuRows) {
                        if (m.checkBox.isSelected()) {
                            int qty = (Integer) m.spinner.getValue();
                            ps.setInt(1, qty);
                            ps.setInt(2, m.id);
                            ps.setInt(3, qty); // hanya kurangi jika stok cukup
                            ps.addBatch();
                        }
                    }
                    int[] hasil = ps.executeBatch();
                    for (int jumlah : hasil) {
                        if (jumlah == 0) {
                            throw new SQLException("Stok tidak cukup untuk salah satu menu yang dipilih.");
                        }
                    }
                }

                conn.commit();
            } catch (SQLException ex) {
                try { conn.rollback(); } catch (SQLException ignored) {}
                throw ex;
            } finally {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
            }

            // ── Tersimpan: baru susun & tampilkan struk ───────────────────────
            StringBuilder struk = new StringBuilder();
            struk.append("===== STRUK PEMBAYARAN =====\n");
            struk.append("Kode  : ").append(kode).append("\n");
            struk.append("Kasir : ").append(currentUser).append("\n");
            struk.append("Tgl   : ").append(new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date())).append("\n");
            struk.append("----------------------------\n");
            for (MenuRow m : menuRows) {
                if (m.checkBox.isSelected()) {
                    int qty = (Integer) m.spinner.getValue();
                    struk.append(String.format("%-18s x%d\n  Rp %s\n", m.nama, qty, fmt((long) qty * m.harga)));
                }
            }
            struk.append("----------------------------\n");
            struk.append("Subtotal  : Rp ").append(fmt(subtotal)).append("\n");
            struk.append("Pajak 10% : Rp ").append(fmt(pajak)).append("\n");
            struk.append("Total     : Rp ").append(fmt(totalInt)).append("\n");
            struk.append("Bayar     : Rp ").append(fmt(bayar)).append("\n");
            struk.append("Kembalian : Rp ").append(fmt(kembalian)).append("\n");
            struk.append("============================");
            lastStruk = struk.toString();
            txtStruk.setText(lastStruk);

            // ── Dialog struk custom ───────────────────────────────────────────
            tampilkanDialogStruk(kode, currentUser,
                new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date()),
                menuRows, subtotal, pajak, totalInt, bayar, kembalian);
            reset();
            loadMenuFromDB(); // refresh stok di kartu menu

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal simpan transaksi:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Pastikan kolom bayar/kembalian ada (untuk database lama). Dijalankan di luar transaksi.
    private void pastikanKolomBayarAda(Connection conn) {
        try (PreparedStatement alt = conn.prepareStatement(
            "ALTER TABLE transaksi ADD COLUMN IF NOT EXISTS bayar BIGINT DEFAULT 0, ADD COLUMN IF NOT EXISTS kembalian BIGINT DEFAULT 0")) {
            alt.executeUpdate();
        } catch (SQLException ignored) {}
    }

    // ── Dialog struk cantik ───────────────────────────────────────────────────
    private void tampilkanDialogStruk(String kode, String kasir, String tgl,
            List<MenuRow> rows, int subtotal, int pajak, int totalInt, long bayar, long kembalian) {

        JDialog dlg = new JDialog(this, "Transaksi Berhasil", true);
        dlg.setSize(420, 580);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);
        dlg.setLayout(new BorderLayout());

        // ── Header dialog ─────────────────────────────────────────────────────
        JPanel pHeader = new JPanel(new BorderLayout());
        pHeader.setBackground(new Color(51, 0, 255));
        pHeader.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        JLabel lblTitle = new JLabel("✓  Transaksi Berhasil");
        lblTitle.setFont(new Font("Bell MT", Font.BOLD, 18));
        lblTitle.setForeground(Color.WHITE);
        pHeader.add(lblTitle, BorderLayout.WEST);

        // ── Body struk ────────────────────────────────────────────────────────
        JPanel pBody = new JPanel();
        pBody.setLayout(new BoxLayout(pBody, BoxLayout.Y_AXIS));
        pBody.setBackground(Color.WHITE);
        pBody.setBorder(BorderFactory.createEmptyBorder(14, 20, 10, 20));

        // Judul struk
        JLabel lblCafe = new JLabel("ROYAL CAFE", SwingConstants.CENTER);
        lblCafe.setFont(new Font("Bell MT", Font.BOLD, 20));
        lblCafe.setForeground(new Color(51, 0, 255));
        lblCafe.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel("Struk Pembayaran", SwingConstants.CENTER);
        lblSub.setFont(new Font("Times New Roman", Font.ITALIC, 13));
        lblSub.setForeground(Color.GRAY);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JSeparator sep1 = new JSeparator();
        sep1.setForeground(new Color(51, 0, 255));
        sep1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));

        // Info transaksi
        JPanel pInfo = new JPanel(new GridLayout(3, 2, 4, 2));
        pInfo.setBackground(Color.WHITE);
        pInfo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        Font fInfo = new Font("Times New Roman", Font.PLAIN, 12);
        for (String[] row : new String[][]{
                {"Kode", kode}, {"Kasir", kasir}, {"Tgl", tgl}}) {
            JLabel k = new JLabel(row[0] + " :"); k.setFont(fInfo);
            JLabel v = new JLabel(row[1]);         v.setFont(fInfo);
            pInfo.add(k); pInfo.add(v);
        }

        JSeparator sep2 = new JSeparator();
        sep2.setForeground(new Color(200, 200, 200));
        sep2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));

        // Item pesanan
        JPanel pItems = new JPanel();
        pItems.setLayout(new BoxLayout(pItems, BoxLayout.Y_AXIS));
        pItems.setBackground(Color.WHITE);
        Font fItem = new Font("Times New Roman", Font.PLAIN, 12);
        for (MenuRow m : rows) {
            if (m.checkBox.isSelected()) {
                int qty = (Integer) m.spinner.getValue();
                JPanel row2 = new JPanel(new BorderLayout());
                row2.setBackground(Color.WHITE);
                row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
                JLabel lNama = new JLabel(m.nama + "  x" + qty);
                lNama.setFont(fItem);
                JLabel lHarga = new JLabel("Rp " + fmt((long) qty * m.harga));
                lHarga.setFont(fItem);
                lHarga.setHorizontalAlignment(SwingConstants.RIGHT);
                row2.add(lNama, BorderLayout.WEST);
                row2.add(lHarga, BorderLayout.EAST);
                pItems.add(row2);
                pItems.add(Box.createVerticalStrut(2));
            }
        }

        JSeparator sep3 = new JSeparator();
        sep3.setForeground(new Color(200, 200, 200));
        sep3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));

        // Ringkasan pembayaran
        JPanel pSum = new JPanel(new GridLayout(5, 2, 4, 3));
        pSum.setBackground(Color.WHITE);
        pSum.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        Font fSum  = new Font("Times New Roman", Font.PLAIN, 13);
        Font fBold = new Font("Times New Roman", Font.BOLD, 13);

        String[][] sumRows = {
            {"Subtotal",  "Rp " + fmt(subtotal)},
            {"Pajak 10%", "Rp " + fmt(pajak)},
            {"Total",     "Rp " + fmt(totalInt)},
            {"Bayar",     "Rp " + fmt(bayar)},
            {"Kembalian", "Rp " + fmt(kembalian)}
        };
        for (int i = 0; i < sumRows.length; i++) {
            boolean isBold = (i == 2 || i == 4);
            JLabel lk = new JLabel(sumRows[i][0] + " :");
            lk.setFont(isBold ? fBold : fSum);
            JLabel lv = new JLabel(sumRows[i][1]);
            lv.setFont(isBold ? fBold : fSum);
            lv.setHorizontalAlignment(SwingConstants.RIGHT);
            if (i == 4) lv.setForeground(new Color(0, 130, 0)); // kembalian hijau
            if (i == 2) lv.setForeground(new Color(51, 0, 255)); // total biru
            pSum.add(lk); pSum.add(lv);
        }

        JSeparator sep4 = new JSeparator();
        sep4.setForeground(new Color(51, 0, 255));
        sep4.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));

        pBody.add(lblCafe);
        pBody.add(Box.createVerticalStrut(2));
        pBody.add(lblSub);
        pBody.add(Box.createVerticalStrut(8));
        pBody.add(sep1);
        pBody.add(Box.createVerticalStrut(8));
        pBody.add(pInfo);
        pBody.add(Box.createVerticalStrut(6));
        pBody.add(sep2);
        pBody.add(Box.createVerticalStrut(6));
        pBody.add(pItems);
        pBody.add(Box.createVerticalStrut(6));
        pBody.add(sep3);
        pBody.add(Box.createVerticalStrut(6));
        pBody.add(pSum);
        pBody.add(Box.createVerticalStrut(8));
        pBody.add(sep4);

        JScrollPane scroll = new JScrollPane(pBody);
        scroll.setBorder(null);

        // ── Tombol bawah ──────────────────────────────────────────────────────
        JPanel pBtn = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 10));
        pBtn.setBackground(new Color(204, 204, 204));
        pBtn.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(51, 0, 255)));

        JButton btnCetak = new JButton("Cetak PDF");
        btnCetak.setBackground(new Color(0, 153, 76));
        btnCetak.setForeground(Color.WHITE);
        btnCetak.setFont(fBold);
        btnCetak.setFocusPainted(false);
        btnCetak.setPreferredSize(new Dimension(130, 36));

        JButton btnOk = new JButton("OK");
        btnOk.setBackground(new Color(51, 0, 255));
        btnOk.setForeground(Color.WHITE);
        btnOk.setFont(fBold);
        btnOk.setFocusPainted(false);
        btnOk.setPreferredSize(new Dimension(90, 36));

        btnCetak.addActionListener(e -> { cetakPDF(lastStruk, kode); dlg.dispose(); });
        btnOk.addActionListener(e -> dlg.dispose());

        pBtn.add(btnCetak);
        pBtn.add(btnOk);

        dlg.add(pHeader, BorderLayout.NORTH);
        dlg.add(scroll,  BorderLayout.CENTER);
        dlg.add(pBtn,    BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // ── Edit Transaksi (admin only) ───────────────────────────────────────────
    private void bukaEditTransaksi() {
        // Ambil daftar transaksi dari DB
        java.util.List<String[]> daftarTrx = new java.util.ArrayList<>();
        String sqlList = "SELECT kode_transaksi, kasir, total, bayar, kembalian, tanggal FROM transaksi ORDER BY tanggal DESC LIMIT 50";
        try (java.sql.Connection conn = DBConnection.getConnection();
             java.sql.Statement st = conn.createStatement();
             java.sql.ResultSet rs = st.executeQuery(sqlList)) {
            while (rs.next()) {
                daftarTrx.add(new String[]{
                    rs.getString("kode_transaksi"),
                    rs.getString("kasir"),
                    "Rp " + fmt(rs.getLong("total")),
                    "Rp " + fmt(rs.getLong("bayar")),
                    "Rp " + fmt(rs.getLong("kembalian")),
                    rs.getString("tanggal")
                });
            }
        } catch (java.sql.SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat transaksi:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (daftarTrx.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Belum ada transaksi.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // ── Dialog pilih transaksi ────────────────────────────────────────────
        JDialog dlg = new JDialog(this, "Edit Transaksi - Admin", true);
        dlg.setSize(700, 500);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(0, 0));

        // Header
        JPanel pHdr = new JPanel(new BorderLayout());
        pHdr.setBackground(new Color(51, 0, 255));
        pHdr.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        JLabel lblHdr = new JLabel("\u270E  Edit Transaksi (Admin)");
        lblHdr.setFont(new Font("Bell MT", Font.BOLD, 18));
        lblHdr.setForeground(Color.WHITE);
        pHdr.add(lblHdr, BorderLayout.WEST);

        // Tabel transaksi
        String[] cols = {"Kode", "Kasir", "Total", "Bayar", "Kembalian", "Tanggal"};
        javax.swing.table.DefaultTableModel mdl = new javax.swing.table.DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (String[] row : daftarTrx) mdl.addRow(row);

        JTable tbl = new JTable(mdl);
        tbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tbl.getTableHeader().setBackground(new Color(51, 0, 255));
        tbl.getTableHeader().setForeground(Color.WHITE);
        tbl.getTableHeader().setFont(new Font("Times New Roman", Font.BOLD, 12));
        tbl.setFont(new Font("Times New Roman", Font.PLAIN, 12));
        tbl.setRowHeight(22);
        tbl.setBackground(Color.WHITE);
        JScrollPane sp = new JScrollPane(tbl);

        // Tombol
        JPanel pBtn = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        pBtn.setBackground(new Color(204, 204, 204));

        JButton btnEdit2 = new JButton("Edit Transaksi Ini");
        btnEdit2.setBackground(new Color(0, 128, 128));
        btnEdit2.setForeground(Color.WHITE);
        btnEdit2.setFont(new Font("Times New Roman", Font.BOLD, 13));
        btnEdit2.setFocusPainted(false);

        JButton btnTutup = new JButton("Tutup");
        btnTutup.setBackground(new Color(204, 0, 0));
        btnTutup.setForeground(Color.WHITE);
        btnTutup.setFont(new Font("Times New Roman", Font.BOLD, 13));
        btnTutup.setFocusPainted(false);

        btnEdit2.addActionListener(e -> {
            int row = tbl.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(dlg, "Pilih transaksi yang ingin diedit!", "Peringatan", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String kode = mdl.getValueAt(row, 0).toString();
            dlg.dispose();
            bukaFormEditTransaksi(kode);
        });
        btnTutup.addActionListener(e -> dlg.dispose());

        pBtn.add(btnEdit2); pBtn.add(btnTutup);
        dlg.add(pHdr, BorderLayout.NORTH);
        dlg.add(sp,   BorderLayout.CENTER);
        dlg.add(pBtn, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void bukaFormEditTransaksi(String kode) {
        // Ambil data transaksi
        long[] data = new long[3]; // total, bayar, kembalian
        try (java.sql.Connection conn = DBConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(
                 "SELECT total, bayar, kembalian FROM transaksi WHERE kode_transaksi=?")) {
            ps.setString(1, kode);
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                data[0] = rs.getLong("total");
                data[1] = rs.getLong("bayar");
                data[2] = rs.getLong("kembalian");
            }
        } catch (java.sql.SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal ambil data:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // ── Form edit ─────────────────────────────────────────────────────────
        JDialog dlg = new JDialog(this, "Edit Transaksi: " + kode, true);
        dlg.setSize(400, 320);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        JPanel pHdr = new JPanel(new BorderLayout());
        pHdr.setBackground(new Color(51, 0, 255));
        pHdr.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        JLabel lblHdr = new JLabel("\u270E  Edit: " + kode);
        lblHdr.setFont(new Font("Bell MT", Font.BOLD, 15));
        lblHdr.setForeground(Color.WHITE);
        pHdr.add(lblHdr, BorderLayout.WEST);

        JPanel pForm = new JPanel(new GridLayout(4, 2, 8, 8));
        pForm.setBackground(new Color(204, 204, 204));
        pForm.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        Font fBold = new Font("Times New Roman", Font.BOLD, 13);
        Font fPlain = new Font("Times New Roman", Font.PLAIN, 13);

        JLabel lTotal = new JLabel("Total:"); lTotal.setFont(fBold);
        JLabel vTotal = new JLabel("Rp " + fmt(data[0])); vTotal.setFont(fBold);
        vTotal.setForeground(new Color(51, 0, 255));

        JLabel lBayar = new JLabel("Bayar (Rp):"); lBayar.setFont(fBold);
        JTextField tfBayar = new JTextField(String.valueOf(data[1]));
        tfBayar.setFont(fPlain);

        JLabel lKembali = new JLabel("Kembalian:"); lKembali.setFont(fBold);
        JLabel vKembali = new JLabel("Rp " + fmt(data[2])); vKembali.setFont(fBold);
        vKembali.setForeground(new Color(0, 130, 0));

        JLabel lStatus = new JLabel(""); lStatus.setFont(fBold);

        // Update kembalian real-time + format titik ribuan
        final boolean[] updatingEdit = {false};
        tfBayar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void update() {
                if (updatingEdit[0]) return;
                SwingUtilities.invokeLater(() -> {
                    if (updatingEdit[0]) return;
                    updatingEdit[0] = true;
                    try {
                        String raw = tfBayar.getText().trim().replaceAll("[^0-9]", "");
                        if (raw.isEmpty()) { vKembali.setText("Rp 0"); return; }
                        long bayarBaru = Long.parseLong(raw);
                        String formatted = fmt(bayarBaru);
                        tfBayar.setText(formatted);
                        tfBayar.setCaretPosition(formatted.length());
                        long kembaliBaru = bayarBaru - data[0];
                        if (kembaliBaru < 0) {
                            vKembali.setText("Kurang Rp " + fmt(-kembaliBaru));
                            vKembali.setForeground(Color.RED);
                        } else {
                            vKembali.setText("Rp " + fmt(kembaliBaru));
                            vKembali.setForeground(new Color(0, 130, 0));
                        }
                    } catch (NumberFormatException ex) {
                        vKembali.setText("-");
                    } finally {
                        updatingEdit[0] = false;
                    }
                });
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });
        // Format Rp saat focus keluar
        tfBayar.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                String raw = tfBayar.getText().trim().replaceAll("[^0-9]", "");
                if (!raw.isEmpty()) {
                    try { tfBayar.setText("Rp " + fmt(Long.parseLong(raw))); }
                    catch (NumberFormatException ignored) {}
                }
            }
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                String raw = tfBayar.getText().trim().replaceAll("[^0-9]", "");
                tfBayar.setText(raw);
            }
        });

        pForm.add(lTotal);   pForm.add(vTotal);
        pForm.add(lBayar);   pForm.add(tfBayar);
        pForm.add(lKembali); pForm.add(vKembali);
        pForm.add(lStatus);  pForm.add(new JLabel());

        JPanel pBtn = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        pBtn.setBackground(new Color(204, 204, 204));

        JButton btnSimpan = new JButton("Simpan");
        btnSimpan.setBackground(new Color(0, 153, 76));
        btnSimpan.setForeground(Color.WHITE);
        btnSimpan.setFont(fBold); btnSimpan.setFocusPainted(false);
        btnSimpan.setPreferredSize(new Dimension(110, 34));

        JButton btnBatal = new JButton("Batal");
        btnBatal.setBackground(new Color(204, 0, 0));
        btnBatal.setForeground(Color.WHITE);
        btnBatal.setFont(fBold); btnBatal.setFocusPainted(false);
        btnBatal.setPreferredSize(new Dimension(110, 34));

        btnSimpan.addActionListener(e -> {
            try {
                long bayarBaru = Long.parseLong(tfBayar.getText().trim().replaceAll("[^0-9]", ""));
                long kembaliBaru = bayarBaru - data[0];
                if (kembaliBaru < 0) {
                    JOptionPane.showMessageDialog(dlg, "Uang bayar kurang dari total!", "Peringatan", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                // Update ke database
                try (java.sql.Connection conn = DBConnection.getConnection();
                     java.sql.PreparedStatement ps = conn.prepareStatement(
                         "UPDATE transaksi SET bayar=?, kembalian=? WHERE kode_transaksi=?")) {
                    ps.setLong(1, bayarBaru);
                    ps.setLong(2, kembaliBaru);
                    ps.setString(3, kode);
                    ps.executeUpdate();
                }
                JOptionPane.showMessageDialog(dlg,
                    "Transaksi berhasil diupdate!\n"
                    + "Kode     : " + kode + "\n"
                    + "Bayar    : Rp " + fmt(bayarBaru) + "\n"
                    + "Kembalian: Rp " + fmt(kembaliBaru),
                    "Berhasil", JOptionPane.INFORMATION_MESSAGE);
                dlg.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dlg, "Nominal tidak valid!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            } catch (java.sql.SQLException ex) {
                JOptionPane.showMessageDialog(dlg, "Gagal update:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        btnBatal.addActionListener(e -> dlg.dispose());

        pBtn.add(btnSimpan); pBtn.add(btnBatal);
        dlg.add(pHdr,  BorderLayout.NORTH);
        dlg.add(pForm, BorderLayout.CENTER);
        dlg.add(pBtn,  BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // ── Reset ─────────────────────────────────────────────────────────────────
    public void reset() {
        for (MenuRow m : menuRows) {
            m.spinner.setValue(0);
            m.checkBox.setSelected(false);
        }
        txtSubtotal.setText("Rp 0"); txtPajak.setText("Rp 0");
        txtTotal.setText("Rp 0");   txtBayar.setText("Rp 0");
        txtKembalian.setText("Rp 0");
        bayarInput = 0;
        // Tampilkan ulang header user & waktu tanpa item menu
        hitungTotal();
    }

    private void konfirmasiKeluar() {
        int c = JOptionPane.showConfirmDialog(this, "Yakin ingin keluar?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (c == JOptionPane.YES_OPTION) { new login().setVisible(true); dispose(); }
    }

    // ── Jam berjalan ──────────────────────────────────────────────────────────
    private void startClock() {
        Thread t = new Thread(() -> {
            while (true) {
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                String now = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date());
                SwingUtilities.invokeLater(() -> lblClock.setText(now));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ── Cetak PDF ─────────────────────────────────────────────────────────────
    private void cetakPDF(String struk, String kode) {
        File dir = new File(System.getProperty("user.home"), "Struk_RoyalCafe");
        dir.mkdirs();
        File f = new File(dir, "Struk_" + kode + ".pdf");
        try {
            buatPDF(f, struk);
            JOptionPane.showMessageDialog(this, "PDF disimpan:\n" + f.getAbsolutePath(), "Berhasil", JOptionPane.INFORMATION_MESSAGE);
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(f);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Gagal PDF:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void buatPDF(File file, String struk) throws IOException {
        String[] rawLines = struk.split("\n");
        List<String> itemLines = new ArrayList<>();
        String kode="",kasir="",tgl="",subtotalStr="",pajakStr="",totalStr="",bayarStr="",kembalianStr="";
        boolean inItems=false; int dashCount=0;
        for (String line : rawLines) {
            String t = line.trim();
            if      (t.startsWith("Kode"))      kode=t.replaceFirst("Kode\\s*:\\s*","");
            else if (t.startsWith("Kasir"))     kasir=t.replaceFirst("Kasir\\s*:\\s*","");
            else if (t.startsWith("Tgl"))       tgl=t.replaceFirst("Tgl\\s*:\\s*","");
            else if (t.startsWith("---"))       { dashCount++; inItems=(dashCount==1); }
            else if (t.startsWith("Subtotal"))  subtotalStr=t.replaceFirst("Subtotal\\s*:\\s*","");
            else if (t.startsWith("Pajak"))     pajakStr=t.replaceFirst("Pajak 10%\\s*:\\s*","");
            else if (t.startsWith("Total"))     totalStr=t.replaceFirst("Total\\s*:\\s*","");
            else if (t.startsWith("Bayar"))     bayarStr=t.replaceFirst("Bayar\\s*:\\s*","");
            else if (t.startsWith("Kembalian")) kembalianStr=t.replaceFirst("Kembalian\\s*:\\s*","");
            else if (inItems && !t.isEmpty())   itemLines.add(t);
        }
        final float PW=226,MARGIN=14,cW=PW-2*MARGIN;
        float PH=Math.max(420,(10+itemLines.size()*2+6)*13f+80);
        List<String> objs=new ArrayList<>();
        objs.add("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        objs.add("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");
        StringBuilder cs=new StringBuilder(); float y=PH-MARGIN-8;
        // Judul
        cs.append("BT\n/F1 13 Tf\n");
        cs.append(String.format("%.2f %.2f Td\n",MARGIN+(cW-13*0.6f*"ROYAL CAFE".length())/2,y));
        cs.append("(ROYAL CAFE) Tj\nET\n"); y-=14;
        cs.append("BT\n/F2 8 Tf\n");
        cs.append(String.format("%.2f %.2f Td\n",MARGIN+(cW-8*0.5f*"Struk Pembayaran".length())/2,y));
        cs.append("(Struk Pembayaran) Tj\nET\n"); y-=11;
        cs.append(String.format("%.2f %.2f m %.2f %.2f l S\n",MARGIN,y,PW-MARGIN,y)); y-=9;
        // Info
        for (String[] row : new String[][]{{"Kode",kode},{"Kasir",kasir},{"Tgl",tgl}}) {
            cs.append("BT\n/F2 8 Tf\n"); cs.append(String.format("%.2f %.2f Td\n",MARGIN,y));
            cs.append("("+row[0]+" : "+escapePdf(row[1])+") Tj\nET\n"); y-=10;
        }
        cs.append(String.format("%.2f %.2f m %.2f %.2f l S\n",MARGIN,y,PW-MARGIN,y)); y-=9;
        // Header tabel
        cs.append("BT\n/F1 8 Tf\n"); cs.append(String.format("%.2f %.2f Td\n",MARGIN,y));
        cs.append("(Item) Tj\nET\n");
        cs.append("BT\n/F1 8 Tf\n"); cs.append(String.format("%.2f %.2f Td\n",MARGIN+120,y));
        cs.append("(Subtotal) Tj\nET\n"); y-=9;
        cs.append(String.format("%.2f %.2f m %.2f %.2f l S\n",MARGIN,y,PW-MARGIN,y)); y-=10;
        // Item rows
        for (String il : itemLines) {
            if (il.startsWith("Rp")||il.startsWith("=")) continue;
            String[] p=il.split("\\s{2,}");
            String nm=p.length>0?p[0]:""; String sb2=p.length>1?p[p.length-1]:"";
            cs.append("BT\n/F2 8 Tf\n"); cs.append(String.format("%.2f %.2f Td\n",MARGIN,y));
            cs.append("("+escapePdf(nm)+") Tj\nET\n");
            cs.append("BT\n/F2 8 Tf\n"); cs.append(String.format("%.2f %.2f Td\n",MARGIN+120,y));
            cs.append("("+escapePdf(sb2)+") Tj\nET\n"); y-=11;
        }
        cs.append(String.format("%.2f %.2f m %.2f %.2f l S\n",MARGIN,y,PW-MARGIN,y)); y-=11;
        // Summary
        String[][] sum={{"Subtotal",subtotalStr},{"Pajak 10%",pajakStr},{"Total",totalStr},{"Bayar",bayarStr},{"Kembalian",kembalianStr}};
        for (int i=0;i<sum.length;i++) {
            String fn=(i==2||i==4)?"/F1":"/F2"; float fs=(i==2||i==4)?9f:8f;
            cs.append("BT\n"+fn+" "+fs+" Tf\n"); cs.append(String.format("%.2f %.2f Td\n",MARGIN+5,y));
            cs.append("("+escapePdf(sum[i][0])+") Tj\nET\n");
            cs.append("BT\n"+fn+" "+fs+" Tf\n"); cs.append(String.format("%.2f %.2f Td\n",MARGIN+100,y));
            cs.append("(: "+escapePdf(sum[i][1])+") Tj\nET\n"); y-=(i==1?12:10);
        }
        y-=4; cs.append(String.format("%.2f %.2f m %.2f %.2f l S\n",MARGIN,y,PW-MARGIN,y)); y-=11;
        String footer="Terima kasih atas kunjungan Anda!";
        cs.append("BT\n/F2 8 Tf\n");
        cs.append(String.format("%.2f %.2f Td\n",MARGIN+(cW-8*0.45f*footer.length())/2,y));
        cs.append("("+footer+") Tj\nET\n");
        String cStr=cs.toString(); byte[] csB=cStr.getBytes("ISO-8859-1");
        objs.add("3 0 obj\n<< /Type /Page /Parent 2 0 R\n   /MediaBox [0 0 "+String.format("%.2f",PW)+" "+String.format("%.2f",PH)+"]\n   /Contents 4 0 R\n   /Resources << /Font << /F1 5 0 R /F2 6 0 R >> >>\n>>\nendobj\n");
        objs.add("4 0 obj\n<< /Length "+csB.length+" >>\nstream\n"+cStr+"\nendstream\nendobj\n");
        objs.add("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\nendobj\n");
        objs.add("6 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n");
        try (OutputStream out=new FileOutputStream(file)) {
            byte[] hdr="%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n".getBytes("ISO-8859-1");
            out.write(hdr); int offset=hdr.length;
            int[] xo=new int[objs.size()];
            for (int i=0;i<objs.size();i++) { xo[i]=offset; byte[] ob=objs.get(i).getBytes("ISO-8859-1"); out.write(ob); offset+=ob.length; }
            int xrefOff=offset;
            StringBuilder xref=new StringBuilder("xref\n0 "+(objs.size()+1)+"\n0000000000 65535 f \n");
            for (int x:xo) xref.append(String.format("%010d 00000 n \n",x));
            xref.append("trailer\n<< /Size "+(objs.size()+1)+" /Root 1 0 R >>\nstartxref\n"+xrefOff+"\n%%EOF\n");
            out.write(xref.toString().getBytes("ISO-8859-1"));
        }
    }

    private String escapePdf(String s) {
        if (s==null) return "";
        return s.replace("\\","\\\\").replace("(","\\(").replace(")","\\)").replace("\r","").replace("\n"," ");
    }

    // ── Main ──────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
                if ("Nimbus".equals(info.getName())) { UIManager.setLookAndFeel(info.getClassName()); break; }
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new login().setVisible(true));
    }
}
