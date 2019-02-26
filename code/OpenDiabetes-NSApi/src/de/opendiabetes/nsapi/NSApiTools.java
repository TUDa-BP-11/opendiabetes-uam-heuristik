package de.opendiabetes.nsapi;

import de.opendiabetes.nsapi.exception.NightscoutIOException;
import de.opendiabetes.nsapi.exporter.NightscoutExporter;
import de.opendiabetes.nsapi.importer.NightscoutImporter;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.util.SortVaultEntryByDate;

import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A collection of methods to work with Nightscout data
 */
public class NSApiTools {

    /**
     * Loads VaultEntries from a file using the {@link NightscoutImporter}.
     *
     * @param path the path of the file
     * @return the loaded vault entries
     * @throws NightscoutIOException if the file does not exist, is a directory, cannot be read, or any other IOException occurs during execution
     */
    public static List<VaultEntry> loadDataFromFile(String path) {
        return loadDataFromFile(path, false);
    }

    /**
     * Loads VaultEntries from a file using the {@link NightscoutImporter}.
     *
     * @param path the path of the file
     * @param sort set to true to sort the resulting list by date
     * @return the loaded vault entries
     * @throws NightscoutIOException if the file does not exist, is a directory, cannot be read, or any other IOException occurs during execution
     */
    public static List<VaultEntry> loadDataFromFile(String path, boolean sort) {
        return loadDataFromFile(path, null, sort);
    }

    /**
     * Loads VaultEntries from a file using the {@link NightscoutImporter}.
     *
     * @param path the path of the file
     * @param type removes all entries that don't match this type. Set to null not remove any entries
     * @param sort set to true to sort the resulting list by date
     * @return the loaded vault entries
     * @throws NightscoutIOException if the file does not exist, is a directory, cannot be read, or any other IOException occurs during execution
     */
    public static List<VaultEntry> loadDataFromFile(String path, VaultEntryType type, boolean sort) {
        File file = new File(path);

        if (!file.exists())
            throw new NightscoutIOException("File does not exist: " + path);
        if (file.isDirectory())
            throw new NightscoutIOException("Cannot load from directory: " + path);
        if (!file.canRead())
            throw new NightscoutIOException("File is not readable, are you missing permissions? " + path);

        FileInputStream stream;
        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new NightscoutIOException("File does not exist: " + path);
        }
        NightscoutImporter importer = new NightscoutImporter();
        List<VaultEntry> data = importer.importData(stream);
        try {
            stream.close();
        } catch (IOException e) {
            throw new NightscoutIOException("Could not close stream", e);
        }

        if (type != null)
            data = filterData(data, type);
        if (sort)
            data.sort(new SortVaultEntryByDate());

        return data;
    }

    /**
     * Iterates over the list and returns a new list containing only entries with the given type.
     * The order of the entries is kept.
     *
     * @param data a list auf vault entries
     * @param type the vault entry type that has to match each entry
     * @return a list containing only entries matching the given type
     */
    public static List<VaultEntry> filterData(List<VaultEntry> data, VaultEntryType type) {
        return filterData(data, Collections.singleton(type));
    }

    /**
     * Iterates over the list and returns a new list containing only entries with the given types.
     * The order of the entries is kept.
     *
     * @param data  a list auf vault entries
     * @param types the allowed vault entry types
     * @return a list containing only entries matching the given type
     */
    public static List<VaultEntry> filterData(List<VaultEntry> data, Collection<VaultEntryType> types) {
        return data.stream().filter(e -> types.contains(e.getType())).collect(Collectors.toList());
    }

    /**
     * Writes data to a file using the {@link NightscoutExporter}. Does not overwrite existing files.
     * Use {@link NSApiTools#writeDataToFile(String, List, boolean)} to overwrite existing files.
     *
     * @param path the path of the file
     * @param data data
     * @throws NightscoutIOException if the file is a directory, or already exists, or the file cannot be
     *                               written to for any reason, or an exception occurs while writing to the file
     */
    public static void writeDataToFile(String path, List<VaultEntry> data) {
        writeDataToFile(path, data, false);
    }

    /**
     * Writes data to a file using the {@link NightscoutExporter}.
     *
     * @param path      the path of the file
     * @param data      data
     * @param overwrite whether or not to overwrite an existing file
     * @throws NightscoutIOException if the file is a directory, or already exists and overwrite is set to false,
     *                               or the file cannot be written to for any reason, or an exception occurs while writing to the file
     */
    public static void writeDataToFile(String path, List<VaultEntry> data, boolean overwrite) {
        File file = new File(path);

        if (file.isDirectory())
            throw new NightscoutIOException("Cannot write to directory: " + path);
        if (file.exists() && !overwrite)
            throw new NightscoutIOException("File already exists: " + path);
        if (file.exists() && !file.canWrite())
            throw new NightscoutIOException("File is not writable, are you missing permissions? " + path);
        if (file.exists() && !file.delete())
            throw new NightscoutIOException("Could not delete old file: " + path);

        FileOutputStream stream;
        try {
            stream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            throw new NightscoutIOException("Could not open file: " + path, e);
        }
        NightscoutExporter exporter = new NightscoutExporter();
        exporter.exportData(stream, data);
        try {
            stream.close();
        } catch (IOException e) {
            throw new NightscoutIOException("Could not close stream", e);
        }
    }
}
