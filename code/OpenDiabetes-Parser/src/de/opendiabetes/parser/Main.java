package de.opendiabetes.parser;

import de.opendiabetes.vault.engine.container.VaultEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args){
        VaultEntryParser parser = new VaultEntryParser();
        StringBuilder jsonFile = new StringBuilder();
        List<VaultEntry> entries;
        String path = "/home/anna/Daten/Uni/14. Semester/BP/Dataset_Small/00390014/direct-sharing-31/entries_2017-07-10_to_2017-11-08.json";
        try (Stream<String> stream = Files.lines( Paths.get(path), StandardCharsets.UTF_8)) {
            stream.forEach(line -> jsonFile.append(line));
        } catch (IOException e) {
            e.printStackTrace();
        }

        entries = parser.parse(jsonFile.toString());
        System.out.println("Entries:");
        for (VaultEntry entry : entries){
            System.out.println(entry.toString());
        }


        StringBuilder jsonFile2 = new StringBuilder();
        List<VaultEntry> treatments;
        String path2 = "";
        try (Stream<String> stream = Files.lines( Paths.get(path2), StandardCharsets.UTF_8)) {
            stream.forEach(line -> jsonFile2.append(line));
        } catch (IOException e) {
            e.printStackTrace();
        }

        treatments = parser.parse(jsonFile2.toString());
        System.out.println("Treatments:");
        for (VaultEntry entry : treatments){
            System.out.println(entry.toString());
        }

    }

}
