/*
 * Copyright (C) 2017 Jens Heuschkel
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
package de.opendiabetes.vault.importer.validator;

import com.csvreader.CsvReader;
import de.opendiabetes.vault.util.TimestampUtils;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author juehv
 */
public class MedtronicCsvValidator extends CsvValidator {

    private static final MultilanguageString CARELINK_HEADER_DATE = new MultilanguageString("Date", "Datum");
    private static final MultilanguageString CARELINK_HEADER_TIME = new MultilanguageString("Time", "Zeit");
    private static final MultilanguageString CARELINK_HEADER_TIMESTAMP = new MultilanguageString("Timestamp", "Zeitstempel");
    private static final MultilanguageString CARELINK_HEADER_TYPE = new MultilanguageString("Raw-Type", "Roh-Typ");
    private static final MultilanguageString CARELINK_HEADER_VALUE = new MultilanguageString("Raw-Values", "Roh-Werte");
    private static final MultilanguageString CARELINK_HEADER_SEQ_NUM = new MultilanguageString("Raw-Seq Num", "Roh-Seq Num");
    private static final MultilanguageString TIME_FORMAT = new MultilanguageString("MM/dd/yy hh:mm a", "dd.MM.yy HH:mm:ss");

    private static Map<MultilanguageString.Language, String[]> createHeaderMap() {
        HashMap<MultilanguageString.Language, String[]> headerMultilanguage = new HashMap<>();
        headerMultilanguage.put(MultilanguageString.Language.EN, new String[]{
                CARELINK_HEADER_DATE.getStringForLanguage(MultilanguageString.Language.EN),
                CARELINK_HEADER_TIME.getStringForLanguage(MultilanguageString.Language.EN),
                CARELINK_HEADER_TIMESTAMP.getStringForLanguage(MultilanguageString.Language.EN),
                CARELINK_HEADER_TYPE.getStringForLanguage(MultilanguageString.Language.EN),
                CARELINK_HEADER_VALUE.getStringForLanguage(MultilanguageString.Language.EN),
                CARELINK_HEADER_SEQ_NUM.getStringForLanguage(MultilanguageString.Language.EN)
        });
        headerMultilanguage.put(MultilanguageString.Language.DE, new String[]{
                CARELINK_HEADER_DATE.getStringForLanguage(MultilanguageString.Language.DE),
                CARELINK_HEADER_TIME.getStringForLanguage(MultilanguageString.Language.DE),
                CARELINK_HEADER_TIMESTAMP.getStringForLanguage(MultilanguageString.Language.DE),
                CARELINK_HEADER_TYPE.getStringForLanguage(MultilanguageString.Language.DE),
                CARELINK_HEADER_VALUE.getStringForLanguage(MultilanguageString.Language.DE),
                CARELINK_HEADER_SEQ_NUM.getStringForLanguage(MultilanguageString.Language.DE)
        });
        return headerMultilanguage;
    }

    public static enum TYPE {
        REWIND("Rewind"),
        PRIME("Prime"),
        EXERCICE("JournalEntryExerciseMarker"),
        BG_CAPTURED_ON_PUMP("BGCapturedOnPump"),
        BG_RECEIVED("BGReceived"),
        SENSOR_CAL_BG("SensorCalBG"),
        SENSOR_CAL_FACTOR("SensorCalFactor"),
        SENSOR_VALUE("GlucoseSensorData"),
        SENSOR_ALERT("AlarmSensor"),
        BOLUS_WIZARD("BolusWizardBolusEstimate"),
        BOLUS_NORMAL("BolusNormal"),
        BOLUS_SQUARE("BolusSquare"),
        BASAL("BasalProfileStart"),
        BASAL_TMP_PERCENT("ChangeTempBasalPercent"),
        BASAL_TMP_RATE("ChangeTempBasal"),
        PUMP_ALERT("AlarmPump"),
        PUMP_SUSPEND_CHANGED("ChangeSuspendState"),
        PUMP_ALERT_NGP("AlarmPumpNGP"),
        PUMP_TYME_SYNC("ChangeTime");

        final String name;

        TYPE(String name) {
            this.name = name;
        }

        static TYPE fromString(String typeString) {
            if (typeString != null && !typeString.isEmpty()) {
                for (TYPE item : TYPE.values()) {
                    if (item.name.equalsIgnoreCase(typeString)) {
                        return item;
                    }
                }
            }
            return null;
        }
    }

    public MedtronicCsvValidator() {
        super(createHeaderMap());
    }

    public String getRawValues(CsvReader creader) throws IOException {
        return creader.get(CARELINK_HEADER_VALUE.getStringForLanguage(languageSelection));
    }

    public String getRawSeqNum(CsvReader creader) throws IOException {
        return creader.get(CARELINK_HEADER_SEQ_NUM.getStringForLanguage(languageSelection));
    }

    public String getCarelinkTypeString(CsvReader creader) throws IOException {
        return creader.get(CARELINK_HEADER_TYPE.getStringForLanguage(languageSelection)).trim();
    }

    public TYPE getCarelinkType(CsvReader creader) throws IOException {
        return TYPE.fromString(getCarelinkTypeString(creader));
    }

    public Date getTimestamp(CsvReader creader) throws IOException, ParseException {
        String timeString = creader.get(CARELINK_HEADER_TIMESTAMP.getStringForLanguage(languageSelection)).trim();
        return TimestampUtils.createCleanTimestamp(timeString, TIME_FORMAT.getStringForLanguage(languageSelection));
    }

    public Date getManualTimestamp(CsvReader creader) throws IOException, ParseException {
        String timeString1 = creader.get(CARELINK_HEADER_DATE.getStringForLanguage(languageSelection)).trim();
        String timeString2 = creader.get(CARELINK_HEADER_TIME.getStringForLanguage(languageSelection)).trim();
        return TimestampUtils.createCleanTimestamp(
                timeString1 + " " + timeString2, TIME_FORMAT.getStringForLanguage(languageSelection));
    }
}
