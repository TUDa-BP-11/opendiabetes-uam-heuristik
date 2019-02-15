/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.opendiabetes.vault.examples;

import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.exporter.CsvFileExporter;
import de.opendiabetes.vault.exporter.FileExporter;
import de.opendiabetes.vault.importer.FileImporter;
import de.opendiabetes.vault.importer.MedtronicCsvImporter;
import de.opendiabetes.vault.util.SortVaultEntryByDate;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author mswin
 */
public class ConvertMedtronicCarelinkToVaultCsv {

    public static void main(String[] args) {
        // Prepare
        String sourceFile = "./testdata/medtronicData.csv";
        String sinkFile = "./testOutput.csv";

        FileImporter importMedtronic = new MedtronicCsvImporter();
        FileExporter exportCsv = new CsvFileExporter();

        try {
            // Import
            List<VaultEntry> data = importMedtronic.importDataFromFile(sourceFile);
            if (data == null) {
                Logger.getLogger("No data imported. Exit.");
                System.exit(1);
            }

            // Process
            Collections.sort(data, new SortVaultEntryByDate());

            // Export
            exportCsv.exportDataToFile(sinkFile, data);

        } catch (IllegalAccessException ex) {
            Logger.getLogger(ConvertMedtronicCarelinkToVaultCsv.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
