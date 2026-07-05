/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package master;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import koneksi.koneksi;

import aprioriapp.AprioriEngine;
import aprioriapp.ItemsetResult;
import aprioriapp.RuleResult;
import aprioriapp.TransaksiBasket;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.JTable;

/**
 *
 * @author DafaAbdr
 */
public class dataProses extends javax.swing.JPanel {

    // Menyimpan hasil "Rule Asosiasi yang Terbentuk" dari proses terakhir,
    // dipakai saat tombol Simpan diklik. Juga menyimpan tanggal proses (hari ini)
    // sebagai penanda saat insert ke tabel hasil_asosiasi.
    private List<RuleResult> ruleTerbentukTerakhir = null;
    private String tanggalProsesTerakhir = null;
    
    private static final Color WARNA_SECTION_CANDIDATE = new Color(207, 226, 243); // biru muda
    private static final Color WARNA_SECTION_FREQUENT  = new Color(255, 224, 178); // oranye muda
    private static final Color WARNA_LOLOS              = new Color(224, 247, 224); // hijau muda
    private static final Color WARNA_TIDAK_LOLOS        = new Color(253, 224, 224); // merah muda

    /**
     * Creates new form dataProses
     */
    public dataProses() {
        initComponents();
        // Wiring tombol Simpan secara manual (tidak lewat GUI Builder Events tab)
        bSimpan.addActionListener(evt -> bSimpanActionPerformed(evt));
        aturRendererIterasiItemset();
        aturRendererLolosTidak(jTable3);
        aturRendererLolosTidak(jTable4);
    }
    
    private String generateIdHasilAsosiasi(Connection conn) throws SQLException {

        String sql = "SELECT id FROM hasil_asosiasi ORDER BY id DESC LIMIT 1";

        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql);

        String idBaru = "A001";

        if (rs.next()) {
            String idTerakhir = rs.getString("id"); // contoh A005
            int nomor = Integer.parseInt(idTerakhir.substring(1));
            nomor++;
            idBaru = String.format("A%03d", nomor);
        }

        rs.close();
        st.close();

        return idBaru;
    }

    private void aturRendererIterasiItemset() {
        jTable2.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                             boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    Object kolom0 = table.getValueAt(row, 0);
                    String teks0 = kolom0 == null ? "" : kolom0.toString();
                    if (teks0.startsWith("=== Candidate")) {
                        c.setBackground(WARNA_SECTION_CANDIDATE);
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                    } else if (teks0.startsWith("=== Frequent")) {
                        c.setBackground(WARNA_SECTION_FREQUENT);
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                    } else {
                        c.setFont(c.getFont().deriveFont(Font.PLAIN));
                        Object ket = table.getValueAt(row, table.getColumnCount() - 1);
                        String ketStr = ket == null ? "" : ket.toString();
                        if ("Lolos".equals(ketStr)) {
                            c.setBackground(WARNA_LOLOS);
                        } else if ("Tidak Lolos".equals(ketStr)) {
                            c.setBackground(WARNA_TIDAK_LOLOS);
                        } else {
                            c.setBackground(Color.WHITE);
                        }
                    }
                }
                return c;
            }
        });
    }
 
    /**
     * Renderer sederhana untuk jTable3 & jTable4: hijau muda jika kolom
     * Keterangan (kolom terakhir) = "Lolos", merah muda jika "Tidak Lolos".
     */
    private void aturRendererLolosTidak(JTable table) {
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                             boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    Object ket = t.getValueAt(row, t.getColumnCount() - 1);
                    String ketStr = ket == null ? "" : ket.toString();
                    if ("Lolos".equals(ketStr)) {
                        c.setBackground(WARNA_LOLOS);
                    } else if ("Tidak Lolos".equals(ketStr)) {
                        c.setBackground(WARNA_TIDAK_LOLOS);
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                }
                return c;
            }
        });
    }
    
    /**
     * Mengambil data transaksi (gabungan tabel transaksi + detail_transaksi)
     * dalam rentang tanggal yang dipilih, dikelompokkan per id_transaksi
     * menjadi satu "keranjang" (basket) untuk keperluan Apriori.
     */
    private List<TransaksiBasket> ambilDataTransaksi(java.sql.Date tglAwal, java.sql.Date tglAkhir) throws SQLException {
        LinkedHashMap<String, TransaksiBasket> map = new LinkedHashMap<>();

        String sql =
                "SELECT t.id_transaksi, t.tanggal, t.total_harga, d.nama_layanan "
              + "FROM transaksi t "
              + "JOIN detail_transaksi d ON t.id_transaksi = d.id_transaksi "
              + "WHERE t.tanggal BETWEEN ? AND ? "
              + "ORDER BY t.tanggal, t.id_transaksi, d.id_layanan";

        Connection conn = koneksi.getConnection();
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setDate(1, tglAwal);
            pst.setDate(2, tglAkhir);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    String idTransaksi = rs.getString("id_transaksi");
                    String tanggal = rs.getDate("tanggal").toString(); // format yyyy-MM-dd
                    long totalHarga = rs.getLong("total_harga");
                    String namaLayanan = rs.getString("nama_layanan");

                    TransaksiBasket basket = map.get(idTransaksi);
                    if (basket == null) {
                        basket = new TransaksiBasket(idTransaksi, tanggal, totalHarga);
                        map.put(idTransaksi, basket);
                    }
                    basket.tambahLayanan(namaLayanan);
                }
            }
        }
        return new ArrayList<>(map.values());
    }

    /** Menampilkan Data Transaksi ke tabel dataTransaksi (No, Tanggal, Nama Layanan, Total Harga). */
    private void tampilkanDataTransaksi(List<TransaksiBasket> basketList, SimpleDateFormat sdf) {
        DefaultTableModel model = new DefaultTableModel(
                new Object[][]{},
                new String[]{"No", "Tanggal", "Nama Layanan", "Total Harga"}
        ) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        int no = 1;
        for (TransaksiBasket b : basketList) {
            model.addRow(new Object[]{
                    no++,
                    sdf.format(java.sql.Date.valueOf(b.getTanggal())),
                    b.getNamaLayananGabungan(),
                    b.getTotalHarga()
            });
        }
        dataTransaksi.setModel(model);
        if (dataTransaksi.getColumnModel().getColumnCount() > 0) {
            dataTransaksi.getColumnModel().getColumn(0).setPreferredWidth(5);
            dataTransaksi.getColumnModel().getColumn(2).setPreferredWidth(250);
        }
    }

    /**
     * Menampilkan seluruh tahap Candidate/Frequent k-Itemset ke jTable2 (Proses Apriori - Iterasi Itemset),
     * dipisah dengan baris section abu-abu "=== Nama Tahap ===".
     */
    private void tampilkanIterasiItemset(LinkedHashMap<String, List<ItemsetResult>> stepResults) {
        DefaultTableModel model = new DefaultTableModel(
                new Object[][]{},
                new String[]{"Nama Layanan", "Jumlah", "Support (%)", "Keterangan"}
        ) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        for (String namaTahap : stepResults.keySet()) {
            model.addRow(new Object[]{"=== " + namaTahap + " ===", "", "", ""});
            List<ItemsetResult> daftar = stepResults.get(namaTahap);
            if (daftar.isEmpty()) {
                model.addRow(new Object[]{"(tidak ada itemset)", "", "", ""});
                continue;
            }
            for (ItemsetResult ir : daftar) {
                model.addRow(new Object[]{
                        ir.getNamaLayanan(),
                        ir.getJumlah(),
                        String.format("%.2f", ir.getSupport()),
                        ir.getKeterangan()
                });
            }
        }
        jTable2.setModel(model);
    }

    /** Menampilkan seluruh kemungkinan rule (lolos maupun tidak) ke jTable3 (Confidence Itemset). */
    private void tampilkanConfidenceItemset(List<RuleResult> semuaRule) {
        DefaultTableModel model = new DefaultTableModel(
                new Object[][]{},
                new String[]{"Aturan (Antecedent -> Consequent)", "Support (%)", "Confidence (%)", "Keterangan"}
        ) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        for (RuleResult r : semuaRule) {
            model.addRow(new Object[]{
                    r.getAturan(),
                    String.format("%.2f", r.getSupportPercent()),
                    String.format("%.2f", r.getConfidencePercent()),
                    r.getKeterangan()
            });
        }
        jTable3.setModel(model);
    }

    /** Menampilkan hanya rule yang lolos minimum confidence ke jTable4 (Rule Asosiasi yang Terbentuk). */
    private void tampilkanRuleAsosiasi(List<RuleResult> ruleLolos) {
        DefaultTableModel model = new DefaultTableModel(
                new Object[][]{},
                new String[]{"Aturan (Antecedent -> Consequent)", "Support (%)", "Confidence (%)", "Keterangan"}
        ) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        for (RuleResult r : ruleLolos) {
            model.addRow(new Object[]{
                    r.getAturan(),
                    String.format("%.2f", r.getSupportPercent()),
                    String.format("%.2f", r.getConfidencePercent()),
                    r.getKeterangan()
            });
        }
        jTable4.setModel(model);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        tTanggalAwal = new com.toedter.calendar.JDateChooser();
        jLabel3 = new javax.swing.JLabel();
        tTanggalAkhir = new com.toedter.calendar.JDateChooser();
        jLabel4 = new javax.swing.JLabel();
        tSupport = new javax.swing.JTextField();
        tConfidence = new javax.swing.JTextField();
        bProses = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        lMinimumAbsolute = new javax.swing.JLabel();
        lMinimumRelative = new javax.swing.JLabel();
        lMinimumConfidence = new javax.swing.JLabel();
        tanggalProses = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        dataTransaksi = new javax.swing.JTable();
        jLabel20 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jLabel21 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTable3 = new javax.swing.JTable();
        jLabel22 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTable4 = new javax.swing.JTable();
        bSimpan = new javax.swing.JButton();

        setBackground(new java.awt.Color(255, 255, 255));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(102, 0, 51));
        jLabel1.setText("| Data Proses Apriori");

        jScrollPane5.setBackground(new java.awt.Color(255, 255, 255));
        jScrollPane5.setForeground(new java.awt.Color(255, 255, 255));
        jScrollPane5.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPane5.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        jLabel2.setText("Tanggal Awal :");

        jLabel3.setText("Tanggal Akhir:");

        jLabel4.setText("Minimum Support (%)");

        bProses.setBackground(new java.awt.Color(102, 0, 51));
        bProses.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        bProses.setForeground(new java.awt.Color(255, 255, 255));
        bProses.setText("Proses");
        bProses.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bProsesActionPerformed(evt);
            }
        });

        jLabel5.setText("Minimum Confidence (%)");

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel6.setText("Data Proses");

        jLabel7.setText("Minimum Support Absolute");

        jLabel8.setText("Minimum Support Relative");

        jLabel9.setText("Minimum Confidence");

        jLabel10.setText("Tanggal Proses");

        jLabel11.setText(":");

        jLabel12.setText(":");

        jLabel13.setText(":");

        jLabel14.setText(":");

        lMinimumAbsolute.setText("...");

        lMinimumRelative.setText("...");

        lMinimumConfidence.setText("...");

        tanggalProses.setText("...");

        jLabel19.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel19.setText("Data Transaksi");

        dataTransaksi.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "No", "Tanggal", "Nama Layanan", "Title 4"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, true
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(dataTransaksi);
        if (dataTransaksi.getColumnModel().getColumnCount() > 0) {
            dataTransaksi.getColumnModel().getColumn(0).setResizable(false);
            dataTransaksi.getColumnModel().getColumn(0).setPreferredWidth(5);
            dataTransaksi.getColumnModel().getColumn(1).setResizable(false);
            dataTransaksi.getColumnModel().getColumn(1).setPreferredWidth(30);
            dataTransaksi.getColumnModel().getColumn(2).setResizable(false);
            dataTransaksi.getColumnModel().getColumn(3).setResizable(false);
        }

        jLabel20.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel20.setText("Proses Apriori (Iterasi Itemset)");

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane2.setViewportView(jTable2);

        jLabel21.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel21.setText("Confidence Itemset");

        jTable3.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane3.setViewportView(jTable3);

        jLabel22.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel22.setText("Rule Asosiasi yang Terbentuk");

        jTable4.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane4.setViewportView(jTable4);

        bSimpan.setBackground(new java.awt.Color(102, 0, 51));
        bSimpan.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        bSimpan.setForeground(new java.awt.Color(255, 255, 255));
        bSimpan.setText("Simpan");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(bSimpan, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel22)
                        .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 666, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel21)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 666, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel20)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 666, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel19)
                        .addComponent(jLabel6)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel2)
                                .addComponent(tTanggalAwal, javax.swing.GroupLayout.PREFERRED_SIZE, 223, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(tTanggalAkhir, javax.swing.GroupLayout.PREFERRED_SIZE, 223, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel3))
                            .addGap(36, 36, 36)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel4)
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel5)
                                        .addComponent(tConfidence, javax.swing.GroupLayout.PREFERRED_SIZE, 223, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(tSupport, javax.swing.GroupLayout.PREFERRED_SIZE, 223, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGap(18, 18, 18)
                                    .addComponent(bProses, javax.swing.GroupLayout.PREFERRED_SIZE, 139, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel7)
                                .addComponent(jLabel8)
                                .addComponent(jLabel9)
                                .addComponent(jLabel10))
                            .addGap(18, 18, 18)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addComponent(jLabel14)
                                    .addGap(18, 18, 18)
                                    .addComponent(tanggalProses, javax.swing.GroupLayout.PREFERRED_SIZE, 285, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addComponent(jLabel13)
                                    .addGap(18, 18, 18)
                                    .addComponent(lMinimumConfidence, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addComponent(jLabel12)
                                    .addGap(18, 18, 18)
                                    .addComponent(lMinimumRelative, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addComponent(jLabel11)
                                    .addGap(18, 18, 18)
                                    .addComponent(lMinimumAbsolute, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 666, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(20, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(tTanggalAwal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(tSupport, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(tTanggalAkhir, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(tConfidence, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addComponent(bProses, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jLabel11)
                    .addComponent(lMinimumAbsolute))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(jLabel12)
                    .addComponent(lMinimumRelative))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(jLabel13)
                    .addComponent(lMinimumConfidence))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(jLabel14)
                    .addComponent(tanggalProses))
                .addGap(18, 18, 18)
                .addComponent(jLabel19)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel20)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel21)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel22)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(bSimpan, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(9, Short.MAX_VALUE))
        );

        jScrollPane5.setViewportView(jPanel1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 718, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(18, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 496, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>                        

    private void bProsesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bProsesActionPerformed
        if (tTanggalAwal.getDate() == null
            || tTanggalAkhir.getDate() == null
            || tSupport.getText().trim().isEmpty()
            || tConfidence.getText().trim().isEmpty()) {

            JOptionPane.showMessageDialog(this, "Lengkapi semua data!");
            return;
        }

        double support, confidence;
        try {
            support = Double.parseDouble(tSupport.getText().trim());
            confidence = Double.parseDouble(tConfidence.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Minimum Support dan Minimum Confidence harus berupa angka.");
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

            java.sql.Date tglAwalSql = new java.sql.Date(tTanggalAwal.getDate().getTime());
            java.sql.Date tglAkhirSql = new java.sql.Date(tTanggalAkhir.getDate().getTime());

            // ---- 1. Ambil data transaksi dari database ----
            List<TransaksiBasket> basketList = ambilDataTransaksi(tglAwalSql, tglAkhirSql);

            if (basketList.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tidak ada data transaksi pada rentang tanggal tersebut.");
                dataTransaksi.setModel(new DefaultTableModel(new Object[][]{}, new String[]{"No", "Tanggal", "Nama Layanan", "Total Harga"}));
                jTable2.setModel(new DefaultTableModel(new Object[][]{}, new String[]{"Nama Layanan", "Jumlah", "Support (%)", "Keterangan"}));
                jTable3.setModel(new DefaultTableModel(new Object[][]{}, new String[]{"Aturan", "Support (%)", "Confidence (%)", "Keterangan"}));
                jTable4.setModel(new DefaultTableModel(new Object[][]{}, new String[]{"Aturan", "Support (%)", "Confidence (%)", "Keterangan"}));
                ruleTerbentukTerakhir = null;
                return;
            }

            // ---- 2. Tampilkan Data Transaksi ----
            tampilkanDataTransaksi(basketList, sdf);

            // ---- 3. Jalankan Apriori ----
            List<Set<String>> transactionsUntukEngine = new ArrayList<>();
            for (TransaksiBasket b : basketList) {
                transactionsUntukEngine.add(b.getLayanan());
            }

            AprioriEngine engine = new AprioriEngine(transactionsUntukEngine, support);
            LinkedHashMap<String, List<ItemsetResult>> stepResults = engine.run();
            tampilkanIterasiItemset(stepResults);

            List<RuleResult> semuaRule = engine.generateRules(confidence);
            tampilkanConfidenceItemset(semuaRule);

            List<RuleResult> ruleLolos = new ArrayList<>();
            for (RuleResult r : semuaRule) {
                if (r.isLolos()) {
                    ruleLolos.add(r);
                }
            }
            tampilkanRuleAsosiasi(ruleLolos);
            ruleTerbentukTerakhir = ruleLolos;

            // ---- 4. Update panel "Data Proses" ----
            int jumlahTransaksi = basketList.size();
            int minimumSupportAbsolute = (int) Math.ceil((support / 100.0) * jumlahTransaksi);

            // NB: sebelumnya nilai Absolute & Relative tertukar, sudah diperbaiki di sini
            lMinimumAbsolute.setText(minimumSupportAbsolute + " dari " + jumlahTransaksi + " transaksi");
            lMinimumRelative.setText(support + " %");
            lMinimumConfidence.setText(confidence + " %");

            String tglAwalStr = sdf.format(tTanggalAwal.getDate());
            String tglAkhirStr = sdf.format(tTanggalAkhir.getDate());
            tanggalProses.setText(tglAwalStr + " sampai " + tglAkhirStr);

            // dipakai sebagai kolom tanggal_proses saat Simpan (format yyyy-MM-dd, tanggal hari ini)
            tanggalProsesTerakhir = new java.sql.Date(System.currentTimeMillis()).toString();

            JOptionPane.showMessageDialog(this, "Data berhasil diproses.");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }//GEN-LAST:event_bProsesActionPerformed

    /**
     * Menyimpan Rule Asosiasi yang Terbentuk (hasil proses terakhir) ke tabel hasil_asosiasi.
     * Tabel akan dibuat otomatis jika belum ada.
     */
    private void bSimpanActionPerformed(java.awt.event.ActionEvent evt) {
        if (ruleTerbentukTerakhir == null || ruleTerbentukTerakhir.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Belum ada Rule Asosiasi yang bisa disimpan.\nSilakan klik \"Proses\" terlebih dahulu.");
            return;
        }
        Connection conn = koneksi.getConnection();
        String sqlMaster = "INSERT INTO hasil_asosiasi "
                + "(id, tanggal_proses, mulai_tanggal, sampai_tanggal, min_support, min_confidence) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        String sqlDetail = "INSERT INTO detail_hasil_asosiasi "
                + "(id_hasil_asosiasi, aturan, support, confidence, keterangan) "
                + "VALUES (?, ?, ?, ?, ?)";

        try {
            conn.setAutoCommit(false);

            String idHasilAsosiasi = generateIdHasilAsosiasi(conn);
            PreparedStatement psMaster = conn.prepareStatement(sqlMaster);
            psMaster.setString(1, idHasilAsosiasi);
            psMaster.setDate(2, new java.sql.Date(System.currentTimeMillis()));
            psMaster.setDate(3, new java.sql.Date(tTanggalAwal.getDate().getTime()));

            psMaster.setDate(4, new java.sql.Date(tTanggalAkhir.getDate().getTime()));
            psMaster.setDouble(5,Double.parseDouble(tSupport.getText()));
            psMaster.setDouble(6,Double.parseDouble(tConfidence.getText()));

            psMaster.executeUpdate();
            psMaster.close();

            PreparedStatement psDetail = conn.prepareStatement(sqlDetail);

            for (RuleResult r : ruleTerbentukTerakhir) {

                psDetail.setString(1, idHasilAsosiasi);
                psDetail.setString(2, r.getAturan());
                psDetail.setDouble(3, r.getSupportPercent());
                psDetail.setDouble(4, r.getConfidencePercent());
                psDetail.setString(5, r.getKeterangan());

                psDetail.addBatch();
            }

            psDetail.executeBatch();
            psDetail.close();

            conn.commit();
            conn.setAutoCommit(true);
            JOptionPane.showMessageDialog(this,
                    "Data berhasil disimpan.\n"
                    + "ID Proses : " + idHasilAsosiasi
                    + "\nJumlah Rule : " + ruleTerbentukTerakhir.size());
        } catch (SQLException ex) {
            try {
                conn.rollback();
                conn.setAutoCommit(true);
            } catch (SQLException e) {
            }
            JOptionPane.showMessageDialog(this,
                    "Gagal menyimpan data.\n" + ex.getMessage());
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bProses;
    private javax.swing.JButton bSimpan;
    private javax.swing.JTable dataTransaksi;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JTable jTable2;
    private javax.swing.JTable jTable3;
    private javax.swing.JTable jTable4;
    private javax.swing.JLabel lMinimumAbsolute;
    private javax.swing.JLabel lMinimumConfidence;
    private javax.swing.JLabel lMinimumRelative;
    private javax.swing.JTextField tConfidence;
    private javax.swing.JTextField tSupport;
    private com.toedter.calendar.JDateChooser tTanggalAkhir;
    private com.toedter.calendar.JDateChooser tTanggalAwal;
    private javax.swing.JLabel tanggalProses;
    // End of variables declaration//GEN-END:variables
}
