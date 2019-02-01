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
package de.opendiabetes.vault.exporter;

import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.exporter.csv.CsvEntry;
import de.opendiabetes.vault.exporter.csv.ExportEntry;
import java.util.ArrayList;
import java.util.List;

/**
 * @author juehv
 */
public class CsvFileExporter extends FileExporter {

    protected CsvFileExporter(ExporterOptions options) {
        super(options);
    }

    public CsvFileExporter() {
        super(new ExporterOptions());
    }

    @Override
    protected List<ExportEntry> prepareData(List<VaultEntry> data) {
        List<ExportEntry> returnValue = new ArrayList<>();

        returnValue.add(CsvEntry.getHeaderEntry());
        for (VaultEntry item : data) {
            returnValue.add(new CsvEntry(item));
        }

        return returnValue;
    }

}
