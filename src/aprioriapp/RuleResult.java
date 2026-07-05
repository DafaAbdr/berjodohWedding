package aprioriapp;

import java.util.Set;
import java.util.TreeSet;

/**
 * Merepresentasikan satu aturan asosiasi: Antecedent -> Consequent
 * beserta nilai support & confidence-nya.
 */
public class RuleResult {

    private Set<String> antecedent; // sisi "jika" (A)
    private Set<String> consequent; // sisi "maka" (B)
    private int jumlahItemset;      // jumlah transaksi yang mengandung A dan B sekaligus
    private double supportPercent;  // support gabungan A,B dalam %
    private double confidencePercent; // confidence A -> B dalam %
    private boolean lolos;          // lolos minimum confidence atau tidak

    public RuleResult(Set<String> antecedent, Set<String> consequent, int jumlahItemset,
                       double supportPercent, double confidencePercent, boolean lolos) {
        this.antecedent = new TreeSet<>(antecedent);
        this.consequent = new TreeSet<>(consequent);
        this.jumlahItemset = jumlahItemset;
        this.supportPercent = supportPercent;
        this.confidencePercent = confidencePercent;
        this.lolos = lolos;
    }

    public String getAturan() {
        return String.join(", ", antecedent) + "  ->  " + String.join(", ", consequent);
    }

    public Set<String> getAntecedent() {
        return antecedent;
    }

    public Set<String> getConsequent() {
        return consequent;
    }

    public int getJumlahItemset() {
        return jumlahItemset;
    }

    public double getSupportPercent() {
        return supportPercent;
    }

    public double getConfidencePercent() {
        return confidencePercent;
    }

    public boolean isLolos() {
        return lolos;
    }

    public String getKeterangan() {
        return lolos ? "Lolos" : "Tidak Lolos";
    }
}
