/*
 * Copyright (C) 2017 juehv
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.opendiabetes.vault.engine.exporter;

import com.csvreader.CsvWriter;
import de.opendiabetes.vault.engine.container.csv.CsvEntry;
import de.opendiabetes.vault.engine.container.csv.ExportEntry;
import de.opendiabetes.vault.engine.container.csv.VaultCsvEntry;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * @author juehv
 */
public abstract class CsvFileExporter extends FileExporter {

    protected CsvFileExporter(ExporterOptions options, String filePath) {
        super(options, filePath);
    }

    protected void writeToFile(List<ExportEntry> csvEntries) throws IOException {
        CsvWriter cwriter = new CsvWriter(fileOutpuStream, VaultCsvEntry.CSV_DELIMITER,
                Charset.forName("UTF-8"));

        cwriter.writeRecord(((CsvEntry) csvEntries.get(0)).getCsvHeaderRecord());
        for (ExportEntry item : csvEntries) {
            cwriter.writeRecord(((CsvEntry) item).toCsvRecord());
        }
        cwriter.flush();
        cwriter.close();
    }
}