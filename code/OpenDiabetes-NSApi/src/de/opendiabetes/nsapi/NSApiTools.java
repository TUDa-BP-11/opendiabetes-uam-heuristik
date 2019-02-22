package de.opendiabetes.nsapi;

import de.opendiabetes.nsapi.exception.NightscoutIOException;
import de.opendiabetes.nsapi.importer.NightscoutImporter;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.util.SortVaultEntryByDate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A collection of methods to work with Nightscout data
 */
public class NSApiTools {

    /**
     * Loads VaultEntries from a file.
     *
     * @param path the path of the file
     * @return the loaded vault entries
     * @throws NightscoutIOException if the file does not exist, is a directory, cannot be read, or any other IOException occurs during execution
     */
    public static List<VaultEntry> loadDataFromFile(String path) {
        return loadDataFromFile(path, false);
    }

    /**
     * Loads VaultEntries from a file.
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
     * Loads VaultEntries from a file.
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
        return data.stream().filter(e -> e.getType().equals(type)).collect(Collectors.toList());
    }
}
