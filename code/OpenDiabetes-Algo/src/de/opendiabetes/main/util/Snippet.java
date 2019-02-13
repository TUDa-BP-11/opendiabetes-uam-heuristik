package de.opendiabetes.main.util;

import de.opendiabetes.parser.VaultEntryParser;
import de.opendiabetes.vault.engine.container.VaultEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Snippet {
    public static final long TIME_MAX = 4 * 60 * 60 * 1000;  // 4 hours
    public static final long TIME_MAX_GAP = 5 * 60 * 1000;   // 5 minutes
    public static final long TIME_MIN = 2 * 60 * 60 * 1000;  // 2 hours

    public static void main(String[] args) throws IOException {
        if (args.length < 2)
            throw new IllegalArgumentException("Source and Dist arguments needed");
        Path source = Paths.get(args[0]);
        Path dist = Paths.get(args[1]);
        if (!Files.isReadable(source))
            throw new IllegalStateException("Path " + source.toAbsolutePath().toString() + " not readable!");
        if (Files.exists(dist) && !Files.isDirectory(dist))
            throw new IllegalStateException("Path " + dist.toAbsolutePath().toString() + " is not a directory!");

        if (!Files.isDirectory(dist))
            Files.createDirectory(dist);

        VaultEntryParser parser = new VaultEntryParser();
        List<VaultEntry> entries = parser.parseFile(source);
        if (entries.isEmpty())
            return;
        entries.sort(Comparator.comparing(VaultEntry::getTimestamp).reversed());
        System.out.println("Found " + entries.size() + " entries in source");

        List<Snippet> snippets = new ArrayList<>();
        Snippet current = new Snippet();
        for (VaultEntry e : entries) {
            if (current.isValid(e)) {
                current.addEntry(e);
            } else {
                if (current.isFull())
                    snippets.add(current);
                current = new Snippet();
            }
        }

        System.out.println("Created " + snippets.size() + " snippets");

        for (Snippet snippet : snippets) {
            String output = parser.toJson(snippet.entries);
            Path file = Paths.get(dist.toString(), snippet.first + ".json");
            System.out.println(String.format("Writing %d entries (%.1fh) to %s",
                    snippet.entries.size(),
                    (snippet.first - snippet.last) / (60 * 60 * 1000D),
                    file.toAbsolutePath().toString()));
            Files.write(file, output.getBytes());
        }
    }

    private long first;
    private long last;
    private List<VaultEntry> entries = new ArrayList<>();

    public void addEntry(VaultEntry entry) {
        if (entries.isEmpty())
            first = entry.getTimestamp().getTime();
        entries.add(entry);
        last = entry.getTimestamp().getTime();
    }

    public boolean isValid(VaultEntry entry) {
        return entries.isEmpty() || (last - entry.getTimestamp().getTime() <= TIME_MAX_GAP && first - entry.getTimestamp().getTime() <= TIME_MAX);
    }

    public boolean isFull() {
        return first - last >= TIME_MIN;
    }
}
