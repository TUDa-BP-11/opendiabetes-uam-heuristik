package de.opendiabetes.vault.main.util;

import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.nsapi.NSApi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

public class Snippet {

    public static final long TIME_MAX_GAP = 10 * 60 * 1000;   // 5 minutes
    public static final long TIME_MIN = 2 * 60 * 60 * 1000;  // 2 hours

    private long first;
    private long last;

    private final List<VaultEntry> entries = new ArrayList<>();
    private final List<VaultEntry> boli = new ArrayList<>();
    private final List<VaultEntry> basals = new ArrayList<>();

//    private int iExceededGap = 0;
    private long lMaxGap = 0;

    public static List<Snippet> getSnippets(List<VaultEntry> entries,
            List<VaultEntry> boli,
            List<VaultEntry> basals,
            long snippetLength, // 6h bg
            long insulinDuration, // 3h insulin => 6h gesamt, die letzten 3h bg werden berücksichtigt
            int n_snippets) { // so viele snippets
        List<Snippet> snippets = new ArrayList<>();

        if (entries.isEmpty()) {
            return snippets;
        }
        entries.sort(Comparator.comparing(VaultEntry::getTimestamp));
        boli.sort(Comparator.comparing(VaultEntry::getTimestamp));
        basals.sort(Comparator.comparing(VaultEntry::getTimestamp));

        System.out.println("Found " + entries.size() + " entries in source");
        System.out.println("Found " + boli.size() + " boli in source");
        System.out.println("Found " + basals.size() + " basals in source");

        Snippet current = new Snippet();
        for (VaultEntry e : entries) {
            if (current.isValid(e, snippetLength)) {
                current.addEntry(e);
                if (current.isFull(snippetLength)) {
                    snippets.add(current);
                    if (snippets.size() == n_snippets) {
                        break;
                    }
                    current = new Snippet();
                }
            } else {
                if (current.isFull()) {
                    NSApi.LOGGER.log(Level.WARNING, "Snippet length only %d min.", TIME_MIN / 60000);
                    snippets.add(current);
                }
                if (snippets.size() == n_snippets) {
                    break;
                }
                current = new Snippet();
                if (current.isValid(e, snippetLength)) {
                    current.addEntry(e);
                }
            }
        }
        for (Snippet s : snippets) {
            for (VaultEntry e : boli) {
                if (s.isInsulinLate(e)) {
                    break;
                }
                if (s.isInsulinAfterFirst(e)) {
                    s.addBolus(e);
                }
            }
            for (VaultEntry e : basals) {
                if (s.isInsulinLate(e)) {
                    break;
                }
                if (s.isInsulinAfterFirst(e)) {
                    s.addBasal(e);
                }
            }
        }

        NSApi.LOGGER.log(Level.INFO, "Created %d snippets", snippets.size());
        NSApi.LOGGER.log(Level.INFO, "Current %d entries", current.getEntries().size());
        return snippets;
    }

    public List<VaultEntry> getEntries() {
        return entries;
    }

    public List<VaultEntry> getBoli() {
        return boli;
    }

    public List<VaultEntry> getBasals() {
        return basals;
    }

    public void addEntry(VaultEntry entry) {
        if (entries.isEmpty()) {
            first = entry.getTimestamp().getTime();
        }
        entries.add(entry);
        last = entry.getTimestamp().getTime();
    }

    public void addBasal(VaultEntry entry) {
        basals.add(entry);
    }

    public void addBolus(VaultEntry entry) {
        boli.add(entry);
    }

    public boolean isInsulinLate(VaultEntry entry) {
        return entry.getTimestamp().getTime() > last;
    }

    public boolean isInsulinAfterFirst(VaultEntry entry) {
        return entry.getTimestamp().getTime() >= first;
    }

    public boolean isValid(VaultEntry entry, long snippetLength) {
        long lGap = entry.getTimestamp().getTime() - last;
        if (entries.isEmpty()) {
            return true;
        }
        if (entry.getTimestamp().getTime() - first <= snippetLength) {
            if (lGap > TIME_MAX_GAP) {
//                iExceededGap++;
//                NSApi.LOGGER.log(Level.WARNING, "Time Gap of {0} min.", lGap / 60000);
                return false;
            }
            if (lGap > lMaxGap) {
                lMaxGap = lGap;
            }
            return true;
        }
        return false;
    }

    public boolean isFull() {
        return last - first >= TIME_MIN;
    }

    public boolean isFull(long snippetLength) {
        return last - first >= snippetLength;
    }

//    public static void main(String[] args) throws IOException {
//        if (args.length < 2) {
//            throw new IllegalArgumentException("Source and Dist arguments needed");
//        }
//        Path source = Paths.get(args[0]);
//        Path dest = Paths.get(args[1]);
//        if (!Files.isReadable(source)) {
//            throw new IllegalStateException("Path " + source.toAbsolutePath().toString() + " not readable!");
//        }
//        if (Files.exists(dest) && !Files.isDirectory(dest)) {
//            throw new IllegalStateException("Path " + dest.toAbsolutePath().toString() + " is not a directory!");
//        }
//
//        if (!Files.isDirectory(dest)) {
//            Files.createDirectory(dest);
//        }
//
//        VaultEntryParser parser = new VaultEntryParser();
//        List<VaultEntry> entries = parser.parseFile(source);
//        if (entries.isEmpty()) {
//            return;
//        }
//        entries.sort(Comparator.comparing(VaultEntry::getTimestamp).reversed());
//        System.out.println("Found " + entries.size() + " entries in source");
//
//        List<Snippet> snippets = new ArrayList<>();
//        Snippet current = new Snippet();
//        for (VaultEntry e : entries) {
//            if (current.isValid(e)) {
//                current.addEntry(e);
//            } else {
//                if (current.isFull()) {
//                    snippets.add(current);
//                }
//                current = new Snippet();
//            }
//        }
//
//        System.out.println("Created " + snippets.size() + " snippets");
//
//        for (Snippet snippet : snippets) {
//            String output = parser.toJson(snippet.entries);
//            Path file = Paths.get(dest.toString(), snippet.first + ".json");
//            if (!Files.exists(file)) {
//                System.out.println(String.format("Writing %d entries (%.1fh) to %s",
//                        snippet.entries.size(),
//                        (snippet.first - snippet.last) / (60 * 60 * 1000D),
//                        file.toAbsolutePath().toString()));
//                Files.write(file, output.getBytes());
//            } else {
//                System.out.println(String.format("Skipping %s, file already exists", file.toAbsolutePath().toString()));
//            }
//        }
//    }
    /**
     * @return the first
     */
    public long getFirst() {
        return first;
    }

    /**
     * @return the last
     */
    public long getLast() {
        return last;
    }

    /**
     * @return the iExceededGap
     */
//    public int getiExceededGap() {
//        return iExceededGap;
//    }

    /**
     * @return the lMaxGap
     */
    public long getlMaxGap() {
        return lMaxGap;
    }
}
