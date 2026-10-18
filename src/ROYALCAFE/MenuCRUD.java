package ROYALCAFE;

import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Menu CRUD Management Form - Royal Cafe
 * Hanya bisa diakses oleh admin
 */
public class MenuCRUD extends javax.swing.JFrame {

    private DefaultTableModel tableModel;
    private final Runnable onDataChanged;

    public MenuCRUD() {
        this(null);
    }

    public MenuCRUD(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;
        initComponents();
        setLocationRelativeTo(null);
        initTable();
        loadMenuData();
    }

    private void notifyChanged() {
        if (onDataChanged != null) {
            onDataChanged.run();
        }
    }

    private void initTable() {
        tableModel = new DefaultTableModel(
            new String[]{"ID", "Nama Menu", "Kategori", "Harga", "Stok", "Gambar"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        jTable1.setModel(tableModel);
        jTable1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jTable1.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) fillFormFromTable();
        });
        // Sembunyikan kolom ID
        jTable1.getColumnModel().getColumn(0).setMinWidth(0);
        jTable1.getColumnModel().getColumn(0).setMaxWidth(0);

        // Tampilkan harga dengan format Rupiah yang konsisten (titik ribuan).
        // Nilai asli di model tetap Integer agar form edit masih bisa mem-parse-nya.
        javax.swing.table.DefaultTableCellRenderer hargaRenderer =
                new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                    javax.swing.JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Object tampil = (value instanceof Number)
                        ? "Rp " + Rupiah.format(((Number) value).longValue())
                        : value;
                java.awt.Component c = super.getTableCellRendererComponent(
                        table, tampil, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.RIGHT);
                return c;
            }
        };
        jTable1.getColumnModel().getColumn(3).setCellRenderer(hargaRenderer);
    }

    private void loadMenuData() {
        tableModel.setRowCount(0);
        String keyword = txtCari.getText().trim();
        String sql = "SELECT * FROM menu WHERE nama_menu LIKE ? OR kategori LIKE ? ORDER BY id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("nama_menu"),
                    rs.getString("kategori"),
                    rs.getInt("harga"),
                    rs.getInt("stok"),
                    rs.getString("gambar")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void fillFormFromTable() {
        int row = jTable1.getSelectedRow();
        if (row < 0) return;
        txtNama.setText(tableModel.getValueAt(row, 1).toString());
        String namaKategori = tableModel.getValueAt(row, 2).toString();
        pastikanKategoriAda(namaKategori);
        cmbKategori.setSelectedItem(namaKategori);
        txtHarga.setText(tableModel.getValueAt(row, 3).toString());
        txtStok.setText(tableModel.getValueAt(row, 4).toString());
        txtGambar.setText(tableModel.getValueAt(row, 5) != null ? tableModel.getValueAt(row, 5).toString() : "");
    }

    /**
     * Pastikan kategori dari database ada di combo box. Tanpa ini,
     * setSelectedItem() akan gagal diam-diam untuk kategori yang tidak
     * terdaftar (mis. data lama "Kopi") dan update bisa memakai kategori salah.
     */
    private void pastikanKategoriAda(String kategori) {
        if (kategori == null || kategori.trim().isEmpty()) return;
        for (int i = 0; i < cmbKategori.getItemCount(); i++) {
            if (kategori.equals(cmbKategori.getItemAt(i))) return;
        }
        cmbKategori.addItem(kategori);
    }

    private void clearForm() {
        txtNama.setText("");
        txtHarga.setText("");
        txtStok.setText("");
        txtGambar.setText("");
        cmbKategori.setSelectedIndex(0);
        jTable1.clearSelection();
    }

    private boolean validateForm() {
        if (txtNama.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nama menu tidak boleh kosong!", "Validasi", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        int harga;
        int stok;
        try {
            harga = Integer.parseInt(txtHarga.getText().trim());
            stok  = Integer.parseInt(txtStok.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Harga dan stok harus berupa angka!", "Validasi", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (harga <= 0) {
            JOptionPane.showMessageDialog(this, "Harga harus lebih dari 0!", "Validasi", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (stok < 0) {
            JOptionPane.showMessageDialog(this, "Stok tidak boleh negatif!", "Validasi", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private void tambahMenu() {
        if (!validateForm()) return;
        String sql = "INSERT INTO menu (nama_menu, kategori, harga, stok, gambar) VALUES (?,?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, txtNama.getText().trim());
            ps.setString(2, cmbKategori.getSelectedItem().toString());
            ps.setInt(3, Integer.parseInt(txtHarga.getText().trim()));
            ps.setInt(4, Integer.parseInt(txtStok.getText().trim()));
            ps.setString(5, txtGambar.getText().trim());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Menu berhasil ditambahkan!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                loadMenuData();
                notifyChanged();
            } else {
                JOptionPane.showMessageDialog(this, "Tidak ada data yang tersimpan.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException e) {
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("duplicate")) {
                JOptionPane.showMessageDialog(this,
                    "Nama menu '" + txtNama.getText().trim() + "' sudah ada!\nGunakan nama yang berbeda.",
                    "Duplikat", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Gagal menambah menu:\n" + msg
                    + "\n\nPastikan:\n- MySQL berjalan\n- Database 'faith_coffe' ada\n- Tabel 'menu' ada",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateMenu() {
        int row = jTable1.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Pilih menu yang ingin diupdate!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!validateForm()) return;
        int id = (int) tableModel.getValueAt(row, 0);
        String sql = "UPDATE menu SET nama_menu=?, kategori=?, harga=?, stok=?, gambar=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, txtNama.getText().trim());
            ps.setString(2, cmbKategori.getSelectedItem().toString());
            ps.setInt(3, Integer.parseInt(txtHarga.getText().trim()));
            ps.setInt(4, Integer.parseInt(txtStok.getText().trim()));
            ps.setString(5, txtGambar.getText().trim());
            ps.setInt(6, id);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Menu berhasil diupdate!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                loadMenuData();
                notifyChanged();
            } else {
                JOptionPane.showMessageDialog(this,
                    "Tidak ada data yang diupdate.\nID=" + id + " mungkin tidak ditemukan di database.",
                    "Peringatan", JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException e) {
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("duplicate")) {
                JOptionPane.showMessageDialog(this,
                    "Nama menu '" + txtNama.getText().trim() + "' sudah dipakai menu lain!",
                    "Duplikat", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Gagal update menu:\n" + msg,
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void hapusMenu() {
        int row = jTable1.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Pilih menu yang ingin dihapus!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Yakin ingin menghapus menu ini?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        int id = (int) tableModel.getValueAt(row, 0);
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM menu WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Menu berhasil dihapus!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            loadMenuData();
            notifyChanged();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal hapus menu: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        jPanel1 = new javax.swing.JPanel();
        jLabel0 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        txtNama = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        cmbKategori = new javax.swing.JComboBox<>(new String[]{"Minuman", "Makanan", "Snack", "Lainnya"});
        jLabel3 = new javax.swing.JLabel();
        txtHarga = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        txtStok = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        txtGambar = new javax.swing.JTextField();
        btnTambah = new javax.swing.JButton();
        btnUpdate = new javax.swing.JButton();
        btnHapus = new javax.swing.JButton();
        btnBersih = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        txtCari = new javax.swing.JTextField();
        btnCari = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        btnKembali = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Manajemen Menu - Royal Cofe");
        setPreferredSize(new java.awt.Dimension(900, 600));

        // Header biru-ungu sesuai tema login
        jPanel1.setBackground(new java.awt.Color(51, 0, 255));
        jLabel0.setFont(new java.awt.Font("Bell MT", 3, 22));
        jLabel0.setForeground(new java.awt.Color(255, 255, 255));
        jLabel0.setText("Manajemen Menu - Royal Cofe");
        jLabel0.setHorizontalAlignment(SwingConstants.CENTER);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup()
            .addComponent(jLabel0, javax.swing.GroupLayout.DEFAULT_SIZE, 900, Short.MAX_VALUE));
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup()
            .addComponent(jLabel0, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE));

        // Form panel — background abu-abu sesuai tema
        jPanel2.setBackground(new java.awt.Color(204, 204, 204));
        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(
            javax.swing.BorderFactory.createLineBorder(new java.awt.Color(51, 0, 255), 2),
            "Form Menu",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            new java.awt.Font("Bell MT", java.awt.Font.BOLD, 13),
            new java.awt.Color(51, 0, 255)
        ));

        java.awt.Font labelFont = new java.awt.Font("Times New Roman", java.awt.Font.BOLD, 13);
        jLabel1.setText("Nama Menu:"); jLabel1.setFont(labelFont);
        jLabel2.setText("Kategori:");  jLabel2.setFont(labelFont);
        jLabel3.setText("Harga (Rp):"); jLabel3.setFont(labelFont);
        jLabel4.setText("Stok:");      jLabel4.setFont(labelFont);
        jLabel5.setText("File Gambar:"); jLabel5.setFont(labelFont);

        btnTambah.setText("Tambah");
        btnTambah.setBackground(new java.awt.Color(0, 153, 76));
        btnTambah.setForeground(java.awt.Color.WHITE);
        btnTambah.setFont(new java.awt.Font("Times New Roman", java.awt.Font.BOLD, 13));
        btnTambah.setFocusPainted(false);
        btnTambah.addActionListener(e -> tambahMenu());

        btnUpdate.setText("Update");
        btnUpdate.setBackground(new java.awt.Color(51, 0, 255));
        btnUpdate.setForeground(java.awt.Color.WHITE);
        btnUpdate.setFont(new java.awt.Font("Times New Roman", java.awt.Font.BOLD, 13));
        btnUpdate.setFocusPainted(false);
        btnUpdate.addActionListener(e -> updateMenu());

        btnHapus.setText("Hapus");
        btnHapus.setBackground(new java.awt.Color(204, 0, 0));
        btnHapus.setForeground(java.awt.Color.WHITE);
        btnHapus.setFont(new java.awt.Font("Times New Roman", java.awt.Font.BOLD, 13));
        btnHapus.setFocusPainted(false);
        btnHapus.addActionListener(e -> hapusMenu());

        btnBersih.setText("Bersih");
        btnBersih.setBackground(new java.awt.Color(255, 140, 0));
        btnBersih.setForeground(java.awt.Color.WHITE);
        btnBersih.setFont(new java.awt.Font("Times New Roman", java.awt.Font.BOLD, 13));
        btnBersih.setFocusPainted(false);
        btnBersih.addActionListener(e -> clearForm());

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(jPanel2Layout.createParallelGroup()
            .addGroup(jPanel2Layout.createSequentialGroup().addGap(10)
                .addGroup(jPanel2Layout.createParallelGroup()
                    .addComponent(jLabel1).addComponent(jLabel2)
                    .addComponent(jLabel3).addComponent(jLabel4).addComponent(jLabel5))
                .addGap(10)
                .addGroup(jPanel2Layout.createParallelGroup()
                    .addComponent(txtNama, 200, 200, 200)
                    .addComponent(cmbKategori, 200, 200, 200)
                    .addComponent(txtHarga, 200, 200, 200)
                    .addComponent(txtStok, 200, 200, 200)
                    .addComponent(txtGambar, 200, 200, 200)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(btnTambah).addGap(5).addComponent(btnUpdate)
                        .addGap(5).addComponent(btnHapus).addGap(5).addComponent(btnBersih)))
                .addGap(10))
        );
        jPanel2Layout.setVerticalGroup(jPanel2Layout.createSequentialGroup().addGap(10)
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel1).addComponent(txtNama)).addGap(8)
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel2).addComponent(cmbKategori)).addGap(8)
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel3).addComponent(txtHarga)).addGap(8)
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel4).addComponent(txtStok)).addGap(8)
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel5).addComponent(txtGambar)).addGap(15)
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(btnTambah).addComponent(btnUpdate)
                .addComponent(btnHapus).addComponent(btnBersih))
            .addGap(10)
        );

        // Search panel — background abu-abu
        jPanel3.setBackground(new java.awt.Color(204, 204, 204));
        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(
            javax.swing.BorderFactory.createLineBorder(new java.awt.Color(51, 0, 255), 2),
            "Daftar Menu",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            new java.awt.Font("Bell MT", java.awt.Font.BOLD, 13),
            new java.awt.Color(51, 0, 255)
        ));
        jLabel6.setText("Cari:");
        jLabel6.setFont(new java.awt.Font("Times New Roman", java.awt.Font.BOLD, 13));
        txtCari.setPreferredSize(new java.awt.Dimension(200, 25));

        btnCari.setText("Cari");
        btnCari.setBackground(new java.awt.Color(51, 0, 255));
        btnCari.setForeground(java.awt.Color.WHITE);
        btnCari.setFont(new java.awt.Font("Times New Roman", java.awt.Font.BOLD, 13));
        btnCari.setFocusPainted(false);
        btnCari.addActionListener(e -> loadMenuData());
        txtCari.addActionListener(e -> loadMenuData());

        // Warna tabel
        jTable1.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        jTable1.getTableHeader().setBackground(new java.awt.Color(51, 0, 255));
        jTable1.getTableHeader().setForeground(java.awt.Color.WHITE);
        jTable1.getTableHeader().setFont(new java.awt.Font("Times New Roman", java.awt.Font.BOLD, 12));
        jTable1.setBackground(java.awt.Color.WHITE);
        jTable1.setGridColor(new java.awt.Color(180, 180, 220));
        jTable1.setFont(new java.awt.Font("Times New Roman", java.awt.Font.PLAIN, 12));
        jTable1.setRowHeight(22);
        jScrollPane1.setViewportView(jTable1);

        btnKembali.setText("Kembali ke Dashboard");
        btnKembali.setBackground(new java.awt.Color(204, 0, 0));
        btnKembali.setForeground(java.awt.Color.WHITE);
        btnKembali.setFont(new java.awt.Font("Times New Roman", java.awt.Font.BOLD, 13));
        btnKembali.setFocusPainted(false);
        btnKembali.addActionListener(e -> this.dispose());

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(jPanel3Layout.createParallelGroup()
            .addGroup(jPanel3Layout.createSequentialGroup().addGap(10)
                .addComponent(jLabel6).addGap(5)
                .addComponent(txtCari, 200, 200, 200).addGap(5)
                .addComponent(btnCari).addGap(10))
            .addGroup(jPanel3Layout.createSequentialGroup().addGap(10)
                .addComponent(jScrollPane1).addGap(10))
            .addGroup(jPanel3Layout.createSequentialGroup().addGap(10)
                .addComponent(btnKembali).addGap(10))
        );
        jPanel3Layout.setVerticalGroup(jPanel3Layout.createSequentialGroup().addGap(8)
            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel6).addComponent(txtCari).addComponent(btnCari))
            .addGap(8)
            .addComponent(jScrollPane1, 300, 300, Short.MAX_VALUE)
            .addGap(8).addComponent(btnKembali).addGap(8)
        );

        // Main layout
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup()
            .addComponent(jPanel1)
            .addGroup(layout.createSequentialGroup().addGap(10)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10)
                .addComponent(jPanel3).addGap(10))
        );
        layout.setVerticalGroup(layout.createSequentialGroup()
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(10)
            .addGroup(layout.createParallelGroup()
                .addComponent(jPanel2)
                .addComponent(jPanel3))
            .addGap(10)
        );

        pack();
    }

    // Variables declaration
    private javax.swing.JPanel jPanel1, jPanel2, jPanel3;
    private javax.swing.JLabel jLabel0, jLabel1, jLabel2, jLabel3, jLabel4, jLabel5, jLabel6;
    private javax.swing.JTextField txtNama, txtHarga, txtStok, txtGambar, txtCari;
    private javax.swing.JComboBox<String> cmbKategori;
    private javax.swing.JButton btnTambah, btnUpdate, btnHapus, btnBersih, btnCari, btnKembali;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
}
