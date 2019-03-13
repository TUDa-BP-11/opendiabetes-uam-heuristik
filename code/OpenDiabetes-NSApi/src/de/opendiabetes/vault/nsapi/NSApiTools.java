package de.opendiabetes.vault.nsapi;

import com.google.gson.JsonArray;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.nsapi.exception.NightscoutIOException;
import de.opendiabetes.vault.nsapi.exporter.NightscoutExporter;
import de.opendiabetes.vault.nsapi.importer.NightscoutImporter;
import de.opendiabetes.vault.util.SortVaultEntryByDate;

import java.io.*;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
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
    public static List<VaultEntry> loadDataFromFile(String path) throws NightscoutIOException {
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
    public static List<VaultEntry> loadDataFromFile(String path, boolean sort) throws NightscoutIOException {
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
    public static List<VaultEntry> loadDataFromFile(String path, VaultEntryType type, boolean sort) throws NightscoutIOException {
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
     * Writes data to a file using the default {@link NightscoutExporter}. Does not overwrite existing files.
     * Use {@link NSApiTools#writeDataToFile(String, List, boolean, NightscoutExporter)} to overwrite existing files.
     *
     * @param path the path of the file
     * @param data data
     * @throws NightscoutIOException if the file is a directory, or already exists, or the file cannot be
     *                               written to for any reason, or an exception occurs while writing to the file
     */
    public static void writeDataToFile(String path, List<VaultEntry> data) throws NightscoutIOException {
        writeDataToFile(path, data, false, new NightscoutExporter());
    }

    /**
     * Writes data to a file using the provided {@link NightscoutExporter}.
     *
     * @param path      the path of the file
     * @param data      data
     * @param overwrite whether or not to overwrite an existing file
     * @param exporter  the exporter to use
     * @throws NightscoutIOException if the file is a directory, or already exists and overwrite is set to false,
     *                               or the file cannot be written to for any reason, or an exception occurs while writing to the file
     */
    public static void writeDataToFile(String path, List<VaultEntry> data, boolean overwrite, NightscoutExporter exporter) throws NightscoutIOException {
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
        exporter.exportData(stream, data);
        try {
            stream.close();
        } catch (IOException e) {
            throw new NightscoutIOException("Could not close stream", e);
        }
    }

    /**
     * Parses the given string as an iso date time. Applies any timezone information if possible,
     * if no timezone is defined in the string it is treated as UTC time.
     *
     * @param value date and time string according to {@link DateTimeFormatter#ISO_DATE_TIME}
     * @return object representing the given time
     * @throws NightscoutIOException if the value cannot be parsed for any reason
     */
    public static ZonedDateTime getZonedDateTime(String value) throws NightscoutIOException {
        try {
            TemporalAccessor t = DateTimeFormatter.ISO_DATE_TIME.parse(value);
            if (t.isSupported(ChronoField.OFFSET_SECONDS))
                return ZonedDateTime.from(t);
            else return LocalDateTime.from(t).atZone(ZoneId.of("UTC"));
        } catch (DateTimeException e) {
            throw new NightscoutIOException("Could not parse date: " + value, e);
        }
    }

    /**
     * Splits the list into multiple partitions. The Order is kept, so concatenating all partitions would
     * result in the original list. Changes to the original list are not reflected in the partitions.
     *
     * @param list      list to split
     * @param batchSize maximum size of each partition
     * @param <T>       type of objects in the list
     * @return list of partitions
     */
    public static <T> List<List<T>> split(List<T> list, int batchSize) {
        return split(list, batchSize, ArrayList::new, List::add);
    }

    /**
     * Splits the array into multiple partitions. The Order is kept, so concatenating all partitions would
     * result in the original array.
     *
     * @param array     array to split
     * @param batchSize maximum size of each partition
     * @return list of partitions
     */
    public static List<JsonArray> split(JsonArray array, int batchSize) {
        return split(array, batchSize, JsonArray::new, JsonArray::add);
    }

    /**
     * Splits the list into multiple partitions. The Order is kept, so concatenating all partitions would
     * result in the original list. Changes to the original list are not reflected in the partitions.
     *
     * @param list      list to split
     * @param batchSize maximum size of each partition
     * @param supplier  a supplier for new partitions such as {@link ArrayList#ArrayList()}
     * @param consumer  consumer that gets a partition and an object and adds the object to the partition such as {@link List#add}
     * @param <I>       iterable
     * @param <T>       type of objects in the iterable
     * @return list of partitions
     */
    public static <I extends Iterable<T>, T> List<I> split(I list, int batchSize, Supplier<I> supplier, BiConsumer<I, T> consumer) {
        List<I> batches = new ArrayList<>();
        int i = 0;
        I batch = null;
        for (T item : list) {
            if (i == 0)
                batch = supplier.get();
            consumer.accept(batch, item);
            i++;
            if (i == batchSize) {
                batches.add(batch);
                i = 0;
            }
        }
        // add last batch
        if (i > 0)
            batches.add(batch);
        return batches;
    }
}
