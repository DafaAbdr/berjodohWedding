package aprioriapp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Engine Apriori:
 * 1. run()          -> menghasilkan seluruh tahap Candidate/Frequent k-Itemset
 * 2. generateRules() -> dari Frequent Itemset (ukuran >= 2) yang terbentuk,
 *                        membuat semua kemungkinan aturan asosiasi (Antecedent -> Consequent)
 *                        beserta nilai confidence-nya.
 */
public class AprioriEngine {

    private List<Set<String>> transactions;
    private double minSupportPercent;

    // Seluruh tahapan proses: "Candidate 1-Itemset", "Frequent 1-Itemset", dst.
    private LinkedHashMap<String, List<ItemsetResult>> stepResults = new LinkedHashMap<>();

    // Lookup cepat: kunci kanonik itemset -> ItemsetResult, HANYA untuk itemset yang FREQUENT (lolos).
    // Dipakai untuk generate rule (butuh support dari subset antecedent).
    private LinkedHashMap<String, ItemsetResult> frequentLookup = new LinkedHashMap<>();

    public AprioriEngine(List<Set<String>> transactions, double minSupportPercent) {
        this.transactions = transactions;
        this.minSupportPercent = minSupportPercent;
    }

    public LinkedHashMap<String, List<ItemsetResult>> run() {
        stepResults.clear();
        frequentLookup.clear();
        int totalTransaksi = transactions.size();

        int k = 1;
        List<Set<String>> candidates = generateCandidate1();
        List<ItemsetResult> candidateResults = hitungSupport(candidates, totalTransaksi);
        stepResults.put("Candidate " + k + "-Itemset", candidateResults);

        List<ItemsetResult> frequent = saring(candidateResults);
        stepResults.put("Frequent " + k + "-Itemset", frequent);
        for (ItemsetResult ir : frequent) {
            frequentLookup.put(ir.getKunci(), ir);
        }

        while (!frequent.isEmpty()) {
            k++;
            List<Set<String>> nextCandidates = generateCandidateNext(frequent, k);
            if (nextCandidates.isEmpty()) {
                break;
            }
            List<ItemsetResult> nextCandidateResults = hitungSupport(nextCandidates, totalTransaksi);
            stepResults.put("Candidate " + k + "-Itemset", nextCandidateResults);

            List<ItemsetResult> nextFrequent = saring(nextCandidateResults);
            stepResults.put("Frequent " + k + "-Itemset", nextFrequent);
            for (ItemsetResult ir : nextFrequent) {
                frequentLookup.put(ir.getKunci(), ir);
            }

            frequent = nextFrequent;
        }

        return stepResults;
    }

    /**
     * Membentuk semua kemungkinan aturan asosiasi dari Frequent Itemset berukuran >= 2.
     * Untuk itemset {A,B,C}, kemungkinan antecedent adalah semua subset tak-kosong & bukan
     * itemset itu sendiri, misal: {A}->{B,C}, {B}->{A,C}, {A,B}->{C}, dst.
     */
    public List<RuleResult> generateRules(double minConfidencePercent) {
        List<RuleResult> hasil = new ArrayList<>();

        for (ItemsetResult ir : frequentLookup.values()) {
            List<String> itemList = new ArrayList<>(ir.getItems());
            int n = itemList.size();
            if (n < 2) continue; // rule butuh minimal 2 item dalam itemset

            // iterasi semua subset tak-kosong & bukan seluruh itemset (1 .. 2^n - 2)
            for (int mask = 1; mask < (1 << n) - 1; mask++) {
                Set<String> antecedent = new TreeSet<>();
                Set<String> consequent = new TreeSet<>();
                for (int i = 0; i < n; i++) {
                    if ((mask & (1 << i)) != 0) {
                        antecedent.add(itemList.get(i));
                    } else {
                        consequent.add(itemList.get(i));
                    }
                }

                String kunciAntecedent = String.join("|", new TreeSet<>(antecedent));
                ItemsetResult antecedentIr = frequentLookup.get(kunciAntecedent);
                if (antecedentIr == null || antecedentIr.getJumlah() == 0) {
                    continue; // secara teori tidak akan terjadi (downward closure Apriori)
                }

                double confidence = (ir.getJumlah() * 100.0) / antecedentIr.getJumlah();
                boolean lolos = confidence >= minConfidencePercent;

                hasil.add(new RuleResult(antecedent, consequent, ir.getJumlah(),
                        ir.getSupport(), confidence, lolos));
            }
        }

        // urutkan dari confidence tertinggi
        hasil.sort((a, b) -> Double.compare(b.getConfidencePercent(), a.getConfidencePercent()));
        return hasil;
    }

    private List<Set<String>> generateCandidate1() {
        TreeSet<String> semuaItem = new TreeSet<>();
        for (Set<String> t : transactions) {
            semuaItem.addAll(t);
        }
        List<Set<String>> hasil = new ArrayList<>();
        for (String item : semuaItem) {
            Set<String> s = new TreeSet<>();
            s.add(item);
            hasil.add(s);
        }
        return hasil;
    }

    private List<Set<String>> generateCandidateNext(List<ItemsetResult> frequentPrev, int k) {
        List<Set<String>> frequentSets = new ArrayList<>();
        for (ItemsetResult ir : frequentPrev) {
            frequentSets.add(ir.getItems());
        }

        Set<Set<String>> candidateSet = new LinkedHashSet<>();
        for (int i = 0; i < frequentSets.size(); i++) {
            for (int j = i + 1; j < frequentSets.size(); j++) {
                Set<String> gabungan = new TreeSet<>(frequentSets.get(i));
                gabungan.addAll(frequentSets.get(j));
                if (gabungan.size() == k) {
                    candidateSet.add(gabungan);
                }
            }
        }

        List<Set<String>> hasil = new ArrayList<>();
        for (Set<String> candidate : candidateSet) {
            if (semuaSubsetFrequent(candidate, frequentSets)) {
                hasil.add(candidate);
            }
        }
        return hasil;
    }

    private boolean semuaSubsetFrequent(Set<String> candidate, List<Set<String>> frequentPrev) {
        List<String> itemList = new ArrayList<>(candidate);
        for (String item : itemList) {
            Set<String> subset = new TreeSet<>(candidate);
            subset.remove(item);
            if (!frequentPrev.contains(subset)) {
                return false;
            }
        }
        return true;
    }

    private List<ItemsetResult> hitungSupport(List<Set<String>> candidates, int totalTransaksi) {
        List<ItemsetResult> hasil = new ArrayList<>();
        for (Set<String> candidate : candidates) {
            int jumlah = 0;
            for (Set<String> t : transactions) {
                if (t.containsAll(candidate)) {
                    jumlah++;
                }
            }
            double support = (totalTransaksi == 0) ? 0 : (jumlah * 100.0 / totalTransaksi);
            boolean lolos = support >= minSupportPercent;
            hasil.add(new ItemsetResult(candidate, jumlah, support, lolos));
        }
        return hasil;
    }

    private List<ItemsetResult> saring(List<ItemsetResult> semua) {
        List<ItemsetResult> hasil = new ArrayList<>();
        for (ItemsetResult ir : semua) {
            if (ir.isLolos()) {
                hasil.add(ir);
            }
        }
        return hasil;
    }
}
