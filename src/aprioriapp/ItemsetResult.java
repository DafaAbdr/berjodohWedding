package aprioriapp;

import java.util.Set;
import java.util.TreeSet;

/**
 * Merepresentasikan satu itemset (baik Candidate maupun Frequent)
 * beserta jumlah kemunculan dan nilai support-nya.
 */
public class ItemsetResult {

    private Set<String> items;
    private int jumlah;
    private double support;
    private boolean lolos;

    public ItemsetResult(Set<String> items, int jumlah, double support, boolean lolos) {
        this.items = new TreeSet<>(items);
        this.jumlah = jumlah;
        this.support = support;
        this.lolos = lolos;
    }

    public Set<String> getItems() {
        return items;
    }

    public String getNamaLayanan() {
        return String.join(", ", items);
    }

    public int getJumlah() {
        return jumlah;
    }

    public double getSupport() {
        return support;
    }

    public boolean isLolos() {
        return lolos;
    }

    public String getKeterangan() {
        return lolos ? "Lolos" : "Tidak Lolos";
    }

    /** Kunci unik (kanonik) untuk itemset ini, dipakai sebagai key pada lookup map. */
    public String getKunci() {
        return String.join("|", items);
    }
}
