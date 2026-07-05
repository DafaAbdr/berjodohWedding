package aprioriapp;

import java.util.Set;
import java.util.TreeSet;

/**
 * Merepresentasikan satu transaksi (1 baris pada tabel "transaksi")
 * beserta seluruh nama layanan yang ada di dalamnya (dari "detail_transaksi").
 * Satu objek ini = satu "keranjang" pada analisis Apriori.
 */
public class TransaksiBasket {

    private String idTransaksi;
    private String tanggal;      // format yyyy-MM-dd, langsung dari kolom DATE
    private long totalHarga;
    private Set<String> layanan; // kumpulan nama_layanan dalam transaksi ini

    public TransaksiBasket(String idTransaksi, String tanggal, long totalHarga) {
        this.idTransaksi = idTransaksi;
        this.tanggal = tanggal;
        this.totalHarga = totalHarga;
        this.layanan = new TreeSet<>();
    }

    public void tambahLayanan(String namaLayanan) {
        layanan.add(namaLayanan);
    }

    public String getIdTransaksi() {
        return idTransaksi;
    }

    public String getTanggal() {
        return tanggal;
    }

    public long getTotalHarga() {
        return totalHarga;
    }

    public Set<String> getLayanan() {
        return layanan;
    }

    public String getNamaLayananGabungan() {
        return String.join(", ", layanan);
    }
}
