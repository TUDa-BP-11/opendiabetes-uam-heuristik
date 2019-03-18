package de.opendiabetes.vault.main.util;

import de.opendiabetes.vault.container.VaultEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Snippet {

    public static final long TIME_MAX = 4 * 60 * 60 * 1000;  // 4 hours
    public static final long TIME_MAX_GAP = 5 * 60 * 1000;   // 5 minutes
    public static final long TIME_MIN = 2 * 60 * 60 * 1000;  // 2 hours

    private long first;
    private long last;

    private final List<VaultEntry> entries = new ArrayList<>();
    private final List<VaultEntry> boli = new ArrayList<>();
    private final List<VaultEntry> basals = new ArrayList<>();

    public static List<Snippet> getSnippets(List<VaultEntry> entries,
            List<VaultEntry> boli,
            List<VaultEntry> basals,
            long snippetLength,
            long insDuration,
            int n_snippets) {
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

//        for (int i = 0; i < Math.min(5, entries.size()); i++){
//            System.out.println(entries.get(i).getTimestamp());
//        }
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
        for (VaultEntry e : boli) {
            for (Snippet s : snippets) {
                s.addBolus(e, insDuration);
            }
        }
        for (VaultEntry e : basals) {
            for (Snippet s : snippets) {
                s.addBasal(e, insDuration);
            }
        }
        System.out.println("Created " + snippets.size() + " snippets");
        System.out.println("Current " + current.getEntries().size() + " entries");
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

    public void addBasal(VaultEntry entry, long treatmentsInAdvance) {
        if (entry.getTimestamp().getTime() <= last && first - treatmentsInAdvance <= entry.getTimestamp().getTime()) {
            basals.add(entry);
        }
    }

    public void addBolus(VaultEntry entry, long treatmentsInAdvance) {
        if (entry.getTimestamp().getTime() <= last && first - treatmentsInAdvance <= entry.getTimestamp().getTime()) {
            boli.add(entry);
        }
    }

    public boolean isValid(VaultEntry entry, long snippetLength) {
        return entries.isEmpty()
                || (entry.getTimestamp().getTime() - last <= TIME_MAX_GAP
                && entry.getTimestamp().getTime() - first <= snippetLength);
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
}
