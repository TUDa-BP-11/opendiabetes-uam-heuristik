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
package de.opendiabetes.vault.importer;

import com.csvreader.CsvReader;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryAnnotation;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.importer.validator.MedtronicCsvValidator;
import de.opendiabetes.vault.util.SortVaultEntryByDate;
import de.opendiabetes.vault.util.TimestampUtils;

import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Jens
 */
public class MedtronicCsvImporter extends CsvFileImporter {

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(.*\\s)?AMOUNT=(\\d+([\\.,]\\d+)?).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern ISIG_PATTERN = Pattern.compile("(.*\\s)?ISIG=(\\d+([\\.,]\\d+)?).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern RATE_PATTERN = Pattern.compile("(.*\\s)?RATE=(\\d+([\\.,]\\d+)?).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern CARB_INPUT_PATTERN = Pattern.compile("(.*\\s)?CARB_INPUT=(\\d+([\\.,]\\d+)?).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern BG_INPUT_PATTERN = Pattern.compile("(.*\\s)?BG_INPUT=(\\d+([\\.,]\\d+)?).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern DURATION_PATTERN = Pattern.compile("(.*\\s)?DURATION=(\\d+([\\.,]\\d+)?).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern RAW_TYPE_PATTERN = Pattern.compile("(.*\\s)?RAW_TYPE=(\\d+([\\.,]\\d+)?).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALARM_TYPE_PATTERN = Pattern.compile("(.*\\s)?ALARM_TYPE=(\\d+([\\.,]\\d+)?).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern STATE_PATTERN = Pattern.compile("(.*\\s)?STATE=(\\w*).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern PERCENT_OF_RATE_PATTERN = Pattern.compile("(.*\\s)?PERCENT_OF_RATE=(\\w*).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern METER_PATTERN = Pattern.compile("(.*\\s)?METER_SERIAL_NUMBER=(\\w*).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern CALIBRATION_BG_PATTERN = Pattern.compile("(.*\\s)?LAST_CAL_BG=(\\w*).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern PREDICTION_PATTERN = Pattern.compile("(.*\\s)?PREDICTED_SENSOR_GLUCOSE_AMOUNT=(\\w*).*", Pattern.CASE_INSENSITIVE);

    // nested enum for alert codes
    public enum MedtronicAltertCodes {
        UNKNOWN_ALERT(0),
        NO_DELIVERY(4),
        INSULIN_FLOW_BLOCKED(7),
        BATTERY_EMPTY_30_MIN_LEFT(73),
        BATTERY_EMPTY_SUSPENDED(84),
        NO_BOLUS_DELIVERED_INPUT_THRESHOULD(100),
        PUMP_BATTERY_WEAK(104),
        RESERVOIR_EMPTY_SOON(105),
        CHANGE_KATHETER_REMINDER(109),
        EMPTY_RESERVOIR(113),
        CALIBRATE_NOW(775),
        CALIBRATION_ERROR(776),
        CHANGE_SENSOR(778),
        NO_SENSOR_CONNECTION(780),
        RISE_ALERT(784),
        SENSOR_EXPIRED(794),
        SENSOR_ALERT_1(797),
        SENSOR_FINISHED(798),
        SENSOR_INITIALIZATIN_STARTED(799),
        LOW(802),
        LOW_WHEN_SUSPENDED(803),
        UNSUSPEND_AFTER_LOW_PROTECTION(806),
        UNSUSPEND_AFTER_LOW_PROTECTION_MAX_TIMESPAN(808),
        SUSPEND_ON_LOW(809),
        SUSPEND_BEVORE_LOW(810),
        HIGH(816),
        APPROACHING_HIGH(817),
        REMINDER_ON_SENSOR_CALIBRATION(869);

        private final int code;

        MedtronicAltertCodes(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static MedtronicAltertCodes fromCode(int code) {
            for (MedtronicAltertCodes codeObj : MedtronicAltertCodes.values()) {
                if (codeObj.getCode() == code) {
                    return codeObj;
                }
            }
            return UNKNOWN_ALERT; // nothing found
        }

    }

    private static VaultEntry extractDoubleEntry(Date timestamp, VaultEntryType type,
                                                 String rawValues, Pattern pattern, String[] fullEntry) {
        if (rawValues != null && !rawValues.isEmpty()) {
            Matcher m = pattern.matcher(rawValues);
            if (m.matches()) {
                String matchedString = m.group(2).replace(",", ".");
                try {
                    double value = Double.parseDouble(matchedString);
                    return new VaultEntry(type,
                            timestamp,
                            value);
                } catch (NumberFormatException ex) {
                    LOG.log(Level.WARNING, "{0} -- Record: {1}",
                            new Object[]{ex.getMessage(), Arrays.toString(fullEntry)});
                }
            }
        }
        return null;
    }

    public static VaultEntry extractSecondValue(VaultEntry entry,
                                                String rawValues, Pattern pattern, String[] fullEntry) {
        if (rawValues != null && !rawValues.isEmpty() && entry != null) {
            Matcher m = pattern.matcher(rawValues);
            if (m.matches()) {
                String matchedString = m.group(2).replace(",", ".");
                try {
                    double value = Double.parseDouble(matchedString);
                    entry.setValue2(value);
                    return entry;
                } catch (NumberFormatException ex) {
                    LOG.log(Level.WARNING, "{0} -- Record: {1}",
                            new Object[]{ex.getMessage(), Arrays.toString(fullEntry)});
                }
            }
        }
        return null;
    }

    //    private static MedtronicAnnotatedVaultEntry annotateBasalEntry(
//            VaultEntry oldEntry, String rawValues, MedtronicCsvValidator.TYPE rawType,
//            String[] fullEntry) {
//        if (rawValues != null && !rawValues.isEmpty() && oldEntry != null) {
//            Matcher m = DURATION_PATTERN.matcher(rawValues);
//            if (m.matches()) {
//                String matchedString = m.group(2).replace(",", ".");
//                try {
//                    double value = Double.parseDouble(matchedString);
//                    oldEntry.setValue2(value);
//                    return new MedtronicAnnotatedVaultEntry(
//                            oldEntry, rawType);
//                } catch (NumberFormatException ex) {
//                    LOG.log(Level.WARNING, "{0} -- Record: {1}",
//                            new Object[]{ex.getMessage(), Arrays.toString(fullEntry)});
//                }
//            }
//        }
//        return null;
//    }
    public MedtronicCsvImporter() {
        super(new ImporterOptions(), new MedtronicCsvValidator(), new char[]{',', ';'});
    }

    @Override
    protected List<VaultEntry> parseEntry(CsvReader creader) throws Exception {
        List<VaultEntry> retVal = new ArrayList<>();
        MedtronicCsvValidator parseValidator = (MedtronicCsvValidator) validator;

        MedtronicCsvValidator.TYPE type = parseValidator.getCarelinkType(creader);
        if (type == null) {
            LOG.log(Level.FINER, "Ignore Type: {0}",
                    parseValidator.getCarelinkTypeString(creader));
            return null;
        }
        Date timestamp;
        try {
            timestamp = parseValidator.getTimestamp(creader);
        } catch (ParseException ex) {
            // maybe old format without good timestamp
            // try again with seperated fields
            timestamp = parseValidator.getManualTimestamp(creader);
        }
        if (timestamp == null) {
            return null;
        }
        String rawValues = parseValidator.getRawValues(creader);
        VaultEntry tmpEntry;

        switch (type) {
            case BASAL:
                tmpEntry = extractDoubleEntry(timestamp,
                        VaultEntryType.BASAL_PROFILE, rawValues,
                        RATE_PATTERN, creader.getValues());
                if (tmpEntry != null) {
                    retVal.add(tmpEntry);
                }
                break;
            case BASAL_TMP_PERCENT:
//                tmpEntry = extractDoubleEntry(timestamp,
//                        VaultEntryType.BASAL_MANUAL, rawValues,
//                        PERCENT_OF_RATE_PATTERN, creader.getValues());
//                if (tmpEntry != null) {
//                    tmpEntry = annotateBasalEntry(tmpEntry, rawValues, type,
//                            creader.getValues());
//                    retVal.add(tmpEntry);
//                }
//                break;
            case BASAL_TMP_RATE:
//                tmpEntry = extractDoubleEntry(timestamp,
//                        VaultEntryType.BASAL_MANUAL, rawValues,
//                        RATE_PATTERN, creader.getValues());
//                if (tmpEntry != null) {
//                    tmpEntry = annotateBasalEntry(tmpEntry, rawValues, type,
//                            creader.getValues());
//                    retVal.add(tmpEntry);
//                }
                break;
            case BG_CAPTURED_ON_PUMP:
                tmpEntry = extractDoubleEntry(timestamp,
                        VaultEntryType.GLUCOSE_BG_MANUAL, rawValues,
                        AMOUNT_PATTERN, creader.getValues());
                if (tmpEntry != null) {
                    // check if it was received by a meter (new format doesn't use BG_RECEIVED)
                    Matcher m = METER_PATTERN.matcher(rawValues);
                    if (m.matches()) {
                        String meterSerial = m.group(2);
                        if (!meterSerial.isEmpty()) {
                            VaultEntryAnnotation annotation
                                    = new VaultEntryAnnotation(
                                    VaultEntryAnnotation.TYPE.GLUCOSE_BG_METER_SERIAL);
                            annotation.setValue(meterSerial);
                            tmpEntry.addAnnotation(annotation);
                            tmpEntry.setType(VaultEntryType.GLUCOSE_BG);
                        }
                    }
                    retVal.add(tmpEntry);
                }
                break;
            case BG_RECEIVED:
                tmpEntry = extractDoubleEntry(timestamp,
                        VaultEntryType.GLUCOSE_BG, rawValues,
                        AMOUNT_PATTERN, creader.getValues());
                if (tmpEntry != null) {
                    retVal.add(tmpEntry);
                }
                break;
            case BOLUS_WIZARD:
                // meal information
                tmpEntry = extractDoubleEntry(timestamp,
                        VaultEntryType.MEAL_BOLUS_CALCULATOR, rawValues,
                        CARB_INPUT_PATTERN, creader.getValues());
                if (tmpEntry != null && tmpEntry.getValue() > 0.0) {
                    retVal.add(tmpEntry);
                }

                tmpEntry = extractDoubleEntry(timestamp,
                        VaultEntryType.GLUCOSE_BOLUS_CALCULATION, rawValues,
                        BG_INPUT_PATTERN, creader.getValues());
                if (tmpEntry != null && tmpEntry.getValue() > 0.0) {
                    retVal.add(tmpEntry);
                }
                break;
            case BOLUS_NORMAL:
                tmpEntry = extractDoubleEntry(timestamp,
                        VaultEntryType.BOLUS_NORMAL, rawValues,
                        AMOUNT_PATTERN, creader.getValues());
                if (tmpEntry != null) {
                    retVal.add(tmpEntry);
                }
                break;
            case BOLUS_SQUARE:
                tmpEntry = extractDoubleEntry(timestamp,
                        VaultEntryType.BOLUS_SQUARE, rawValues,
                        AMOUNT_PATTERN, creader.getValues());
                if (tmpEntry != null) {
                    tmpEntry = extractSecondValue(tmpEntry, rawValues,
                            DURATION_PATTERN, creader.getValues());
                    if (tmpEntry != null) {
                        tmpEntry.setValue2(tmpEntry.getValue2() / 1000);
                        retVal.add(tmpEntry);
                    }
                }
                break;
            case EXERCICE:
                tmpEntry = extractDoubleEntry(timestamp,
                        VaultEntryType.EXERCISE_MANUAL, rawValues,
                        DURATION_PATTERN, creader.getValues());

                if (tmpEntry != null) {
                    retVal.add(tmpEntry);
                } else {
                    // add marker without duration for old pumps
                    retVal.add(new VaultEntry(VaultEntryType.EXERCISE_MANUAL,
                            timestamp, VaultEntry.VALUE_UNUSED));
                }
                break;
            case PRIME:
                tmpEntry = extractDoubleEntry(timestamp,
                        VaultEntryType.PUMP_PRIME, rawValues,
                        AMOUNT_PATTERN, creader.getValues());
                if (tmpEntry != null) {
                    retVal.add(tmpEntry);
                }

                break;
            case PUMP_ALERT:
            case PUMP_ALERT_NGP:
                tmpEntry = extractDoubleEntry(timestamp,
                        VaultEntryType.PUMP_NO_DELIVERY, rawValues,
                        RAW_TYPE_PATTERN, creader.getValues());

                if (tmpEntry != null) {
                    MedtronicAltertCodes codeObj = MedtronicAltertCodes.fromCode(
                            (int) Math.round(tmpEntry.getValue()));
                    String codeString = codeObj == MedtronicAltertCodes.UNKNOWN_ALERT
                            ? String.valueOf(Math.round(tmpEntry.getValue()))
                            : codeObj.toString();

                    switch (codeObj) {
                        case NO_DELIVERY:
                            // already done
                            break;
                        case SUSPEND_ON_LOW:
                        case SUSPEND_BEVORE_LOW:
                            VaultEntry extraTmpEntry = new VaultEntry(VaultEntryType.PUMP_AUTONOMOUS_SUSPEND, timestamp);
                            retVal.add(extraTmpEntry);
                        case LOW_WHEN_SUSPENDED:
                        case LOW:
                        case RISE_ALERT:
                        case UNSUSPEND_AFTER_LOW_PROTECTION:
                        case UNSUSPEND_AFTER_LOW_PROTECTION_MAX_TIMESPAN:
                        case HIGH:
                        case APPROACHING_HIGH:
                            tmpEntry.setType(VaultEntryType.GLUCOSE_CGM_ALERT);
                            tmpEntry.setValue(VaultEntry.VALUE_UNUSED); // mark as unused to inform interpreter to add a BG value
                            tmpEntry.addAnnotation(new VaultEntryAnnotation(codeString,
                                    VaultEntryAnnotation.TYPE.PUMP_INFORMATION_CODE));
                            break;
                        case NO_SENSOR_CONNECTION:
                            tmpEntry.setType(VaultEntryType.CGM_CONNECTION_ERROR);
                            tmpEntry.setValue(VaultEntry.VALUE_UNUSED);
                            break;
                        case CALIBRATION_ERROR:
                            tmpEntry.setType(VaultEntryType.CGM_CALIBRATION_ERROR);
                            tmpEntry.setValue(VaultEntry.VALUE_UNUSED);
                            break;
                        case EMPTY_RESERVOIR:
                            tmpEntry.setType(VaultEntryType.PUMP_RESERVOIR_EMPTY);
                            tmpEntry.setValue(VaultEntry.VALUE_UNUSED);
                            break;
                        case INSULIN_FLOW_BLOCKED:
                            tmpEntry.setType(VaultEntryType.PUMP_NO_DELIVERY);
                            tmpEntry.setValue(VaultEntry.VALUE_UNUSED);
                            tmpEntry.addAnnotation(new VaultEntryAnnotation(codeString,
                                    VaultEntryAnnotation.TYPE.PUMP_ERROR_CODE));
                            break;
                        case SENSOR_EXPIRED:
                        case SENSOR_FINISHED:
                            tmpEntry.setType(VaultEntryType.CGM_SENSOR_FINISHED);
                            tmpEntry.setValue(VaultEntry.VALUE_UNUSED);
                            break;
                        case SENSOR_INITIALIZATIN_STARTED:
                            tmpEntry.setType(VaultEntryType.CGM_SENSOR_START);
                            tmpEntry.setValue(VaultEntry.VALUE_UNUSED);
                            break;
                        default:
                            tmpEntry.setType(VaultEntryType.PUMP_UNTRACKED_ERROR);
                            tmpEntry.setValue(VaultEntry.VALUE_UNUSED);
                            tmpEntry.addAnnotation(new VaultEntryAnnotation(codeString,
                                    VaultEntryAnnotation.TYPE.PUMP_ERROR_CODE));
                            break;
                    }

                    retVal.add(tmpEntry);
                }

                break;
            case PUMP_SUSPEND_CHANGED:
                if (rawValues != null && !rawValues.isEmpty()) {
                    Matcher m = STATE_PATTERN.matcher(rawValues);
                    if (m.matches()) {
                        String matchedString = m.group(2);
                        VaultEntryType entryType;
                        if (matchedString.contains("suspend")
                                || matchedString.contains("predicted_low_sg")
                                || matchedString.contains("lowsg_suspend")) {
                            entryType = VaultEntryType.PUMP_SUSPEND;
                        } else if (matchedString.contains("normal")) {
                            entryType = VaultEntryType.PUMP_UNSUSPEND;
                        } else {
                            entryType = VaultEntryType.PUMP_UNTRACKED_ERROR;
                        }
                        tmpEntry = new VaultEntry(entryType,
                                timestamp,
                                VaultEntry.VALUE_UNUSED);
                        retVal.add(tmpEntry);
                    }
                }

                break;
            case PUMP_TYME_SYNC:
                retVal.add(new VaultEntry(VaultEntryType.PUMP_TIME_SYNC,
                        timestamp,
                        VaultEntry.VALUE_UNUSED));
                break;
            case REWIND:
                retVal.add(new VaultEntry(VaultEntryType.PUMP_REWIND, timestamp,
                        VaultEntry.VALUE_UNUSED));
                break;
            case SENSOR_ALERT:
                tmpEntry = extractDoubleEntry(timestamp,
                        VaultEntryType.GLUCOSE_CGM_ALERT, rawValues,
                        AMOUNT_PATTERN, creader.getValues());
                if (tmpEntry != null) {
                    // check if it is really a cgm-bg-alert
                    Matcher m = ALARM_TYPE_PATTERN.matcher(rawValues);
                    if (m.matches()) {
                        if (m.group(2).equalsIgnoreCase("102")
                                || m.group(2).equalsIgnoreCase("101")) {
                            retVal.add(tmpEntry);
                        }
                    }
                }

                break;
            case SENSOR_CAL_BG:
                tmpEntry = extractDoubleEntry(timestamp,
                        VaultEntryType.GLUCOSE_CGM_CALIBRATION, rawValues,
                        AMOUNT_PATTERN, creader.getValues());
                if (tmpEntry != null) {
                    tmpEntry.addAnnotation(new VaultEntryAnnotation(VaultEntryAnnotation.TYPE.CGM_VENDOR_MEDTRONIC));
                    retVal.add(tmpEntry);
                }

                break;
            case SENSOR_CAL_FACTOR:
                // new data format for calibration
                tmpEntry = extractDoubleEntry(timestamp,
                        VaultEntryType.GLUCOSE_CGM_CALIBRATION, rawValues,
                        CALIBRATION_BG_PATTERN, creader.getValues());
                if (tmpEntry != null) {
                    retVal.add(tmpEntry);
                }

                break;
            case SENSOR_VALUE:
                // calibrated cgm value
                tmpEntry = extractDoubleEntry(timestamp,
                        VaultEntryType.GLUCOSE_CGM, rawValues,
                        AMOUNT_PATTERN, creader.getValues());
                if (tmpEntry != null) {
                    retVal.add(tmpEntry);
                }

                // measured raw value
                tmpEntry = extractDoubleEntry(timestamp,
                        VaultEntryType.GLUCOSE_CGM_RAW, rawValues,
                        ISIG_PATTERN, creader.getValues());
                if (tmpEntry != null) {
                    retVal.add(tmpEntry);
                }

                // sensor value prediction
                tmpEntry = extractDoubleEntry(timestamp,
                        VaultEntryType.PUMP_CGM_PREDICTION, rawValues,
                        PREDICTION_PATTERN, creader.getValues());
                if (tmpEntry != null && tmpEntry.getValue() > 20.0) {
                    retVal.add(tmpEntry);
                }

                break;
            default:
                Logger.getLogger(this.getClass().getName()).severe("ASSERTION ERROR!");
                //throw new AssertionError();
        }

        return retVal;
    }

    @Override
    public List<VaultEntry> postProcessingData(List<VaultEntry> importedData) {
        return super.postProcessingData(interpret(importedData));
    }

    protected List<VaultEntry> interpret(List<VaultEntry> result) {
        List<VaultEntry> data = result;

        // sort by date
        Collections.sort(data, new SortVaultEntryByDate());

        LOG.finer("Start basal interpretation");
        data = applyTempBasalEvents(data);
        Collections.sort(data, new SortVaultEntryByDate());
        data = considerSuspendAsBasalOff(data);
        Collections.sort(data, new SortVaultEntryByDate());

        LOG.finer("Start CGM Alert interpretation");
        data = addCgmValueToCgmAltertOnMedtronicPumps(data);
        Collections.sort(data, new SortVaultEntryByDate());

        LOG.finer("Pump data interpretation finished");
        return data;
    }

    private List<VaultEntry> considerSuspendAsBasalOff(List<VaultEntry> data) {
        // suspends will stop basal rate --> add basal 0 point
        // after suspension, pump has new basal event by itselve
        // while suspension, pump does not create basal profile events :)
        if (data == null || data.isEmpty()) {
            return data;
        }

        List<VaultEntry> basalEvents = new ArrayList<>();
        List<VaultEntry> killedBasalEvents = new ArrayList<>();

        for (VaultEntry suspendItem : data) {
            if (suspendItem.getType() == VaultEntryType.PUMP_SUSPEND) {

                // find corresponding unsuspend
                VaultEntry unsuspendEvent = null;
                for (VaultEntry unspendItem : data) {
                    if (unspendItem.getType() == VaultEntryType.PUMP_UNSUSPEND
                            && unspendItem.getTimestamp().after(suspendItem.getTimestamp())) {
                        // found it
                        unsuspendEvent = unspendItem;
                        break;
                    }
                }

                // at start add basal 0
                basalEvents.add(new VaultEntry(VaultEntryType.BASAL_INTERPRETER,
                        suspendItem.getTimestamp(), 0.0));

                VaultEntry lastKnownBasalEntry = null;
                if (unsuspendEvent != null) {
                    // kill basal items between the suspension           
                    for (VaultEntry killItem : data) {
                        if ((killItem.getType() == VaultEntryType.BASAL_PROFILE
                                || killItem.getType() == VaultEntryType.BASAL_MANUAL)
                                && TimestampUtils.withinDateTimeSpan(
                                suspendItem.getTimestamp(),
                                unsuspendEvent.getTimestamp(),
                                killItem.getTimestamp())) {
                            // found basal item within suspention time span
                            killedBasalEvents.add(killItem);
                            lastKnownBasalEntry = killItem; // update last known basal entry
                        } else if ((killItem.getType() == VaultEntryType.BASAL_PROFILE
                                || killItem.getType() == VaultEntryType.BASAL_MANUAL)
                                && killItem.getTimestamp().after(unsuspendEvent.getTimestamp())) {
                            // we can stop when we exit the time span
                            break;
                        }
                    }
                } else {
                    // didn't find corresponding unsuspend item
                    LOG.log(Level.WARNING,
                            "Found no unsuspend item. "
                                    + "Cannot kill basal profile items: {0}",
                            suspendItem.toString());
                    break;
                }

                // at end set basal to old value
                if (lastKnownBasalEntry == null) {
                    // no profile elements within the suspension 
                    // --> we have to search the last known one before the suspenstion
                    for (VaultEntry basalEntry : data) {
                        if (basalEntry.getType() == VaultEntryType.BASAL_MANUAL
                                || basalEntry.getType() == VaultEntryType.BASAL_PROFILE) { // no interpreter basal items, since suspension will interrupt tmp basal
                            if (suspendItem.getTimestamp().after(basalEntry.getTimestamp())) {
                                lastKnownBasalEntry = basalEntry;
                            } else if (suspendItem.getTimestamp().before(basalEntry.getTimestamp())) { // we passed the suspension time point --> stop the search
                                break;
                            }
                        }
                    }
                }

                // add restore element
                if (lastKnownBasalEntry != null) {
                    basalEvents.add(new VaultEntry(VaultEntryType.BASAL_INTERPRETER,
                            TimestampUtils.createCleanTimestamp(unsuspendEvent.getTimestamp()),
                            lastKnownBasalEntry.getValue()));
                } else {
                    LOG.log(Level.WARNING,
                            "Cant find a basal item to restore suspension for: {0}",
                            suspendItem.toString());
                }
            }
        }
        data.removeAll(killedBasalEvents);
        data.addAll(basalEvents);
        return data;
    }

    private List<VaultEntry> applyTempBasalEvents(List<VaultEntry> data) {
//        // if tmp basal ocures, real basal rate must be calculated
//        // it is possible, that tmp basal rate events have an effect on db data <-- ?
//        if (data == null || data.isEmpty()) {
//            return data;
//        }
//        List<VaultEntry> basalEvents = new ArrayList<>();
//        List<VaultEntry> historicBasalProfileEvents = new ArrayList<>();
//        List<VaultEntry> killedBasalEvents = new ArrayList<>();
//
//        for (VaultEntry item : data) {
//            if (item.getType() == VaultEntryType.BASAL_MANUAL
//                    && item.getAnnotations().get(0).getType() == ) {
//                MedtronicAnnotatedVaultEntry basalItem
//                        = (MedtronicAnnotatedVaultEntry) item;
//
//                // get affected historic elements from this dataset
//                List<VaultEntry> affectedHistoricElements = new ArrayList<>();
//                for (int i = historicBasalProfileEvents.size() - 1; i >= 0; i--) {
//                    VaultEntry historicItem = historicBasalProfileEvents.get(i);
//
//                    if (basalItem.getDuration() > (basalItem.getTimestamp().getTime()
//                            - historicItem.getTimestamp().getTime())) {
//                        // kill event and save value for percentage calculation
//                        killedBasalEvents.add(historicItem);
//                        affectedHistoricElements.add(historicItem);
//                    } else {
//                        // yungest now available element is not affected 
//                        // --> no other remaining elements are affected
//                        // add to affected list for calculation but don't kill it
//                        affectedHistoricElements.add(historicItem);
//                        break;
//                    }
//                }
//                if (affectedHistoricElements.isEmpty()) {
//                    if ((basalItem.getRawType() == MedtronicCsvValidator.TYPE.BASAL_TMP_PERCENT
//                            && basalItem.getValue() > 0)) {
//                        LOG.log(Level.WARNING, "Could not calculate tmp basal, "
//                                + "because no profile elements are found\n{0}",
//                                basalItem.toString());
//                        killedBasalEvents.add(item); // kill the item since we cannot calculate its meaning
//                        continue;
//                    }
//                }
//
//                // apply changes
//                switch (basalItem.getRawType()) {
//                    case MedtronicCsvValidator.TYPE.BASAL_TMP_PERCENT:
//                        // calculate new rate
//                        Date startTimestamp = TimestampUtils.createCleanTimestamp(
//                                new Date((long) (basalItem.getTimestamp().getTime()
//                                        - basalItem.getDuration())));
//
//                        // calculate basal value
//                        double newBasalValue = 0;
//                        // first item need special treatment, since we need to use the start
//                        // timestamp of the tmp basal rate, not the timestamp of the profile item
//                        if (basalItem.getValue() > 0 && !affectedHistoricElements.isEmpty()) {
//                            double currentBasalValue = affectedHistoricElements.get(
//                                    affectedHistoricElements.size() - 1).getValue();
//                            newBasalValue = currentBasalValue
//                                    * basalItem.getValue() * 0.01;
//                        }
//                        // add item
//                        basalEvents.add(new VaultEntry(
//                                VaultEntryType.BASAL_MANUAL,
//                                startTimestamp,
//                                newBasalValue));
//
//                        // add the changes of basal rate within the tmp percentage timespan
//                        if (basalItem.getValue() > 0) {
//                            for (int i = 0; i < affectedHistoricElements.size() - 2; i++) {
//                                double currentBasalValue = affectedHistoricElements.get(i)
//                                        .getValue();
//                                newBasalValue = currentBasalValue
//                                        * basalItem.getValue() * 0.01;
//                                basalEvents.add(new VaultEntry(
//                                        VaultEntryType.BASAL_MANUAL,
//                                        affectedHistoricElements.get(i).getTimestamp(),
//                                        newBasalValue));
//                            }
//                        }
//
//                        // restore rate from jungest profile event afterwords
//                        if (affectedHistoricElements.size() > 0) {
//                            basalEvents.add(new VaultEntry(
//                                    VaultEntryType.BASAL_PROFILE,
//                                    basalItem.getTimestamp(),
//                                    affectedHistoricElements.get(0).getValue()));
//                        }
//                        break;
//
//                    case BASAL_TMP_RATE:
//                        // add new rate
//                        basalEvents.add(new VaultEntry(
//                                VaultEntryType.BASAL_MANUAL,
//                                new Date((long) (basalItem.getTimestamp().getTime()
//                                        - basalItem.getDuration())),
//                                basalItem.getValue()));
//
//                        // restore rate from jungest profile event afterwords
//                        if (affectedHistoricElements.size() > 0) {
//                            basalEvents.add(new VaultEntry(
//                                    VaultEntryType.BASAL_PROFILE,
//                                    basalItem.getTimestamp(),
//                                    affectedHistoricElements.get(0).getValue()));
//                        }
//                        break;
//                    default:
//                        Logger.getLogger(this.getClass().getName()).severe("ASSERTION ERROR!");
//                        throw new AssertionError();
//                }
//
//                killedBasalEvents.add(item);
//            } else if (item.getType() == VaultEntryType.BASAL_PROFILE) {
//                historicBasalProfileEvents.add(item);
//            }
//        }
//
//        data.removeAll(killedBasalEvents);
//        data.addAll(basalEvents);
        return data;
    }

    private static List<VaultEntry> addCgmValueToCgmAltertOnMedtronicPumps(List<VaultEntry> data) {
        if (data == null || data.isEmpty()) {
            return data;
        }

        double lastCgmValue = -1;
        for (VaultEntry item : data) {
            switch (item.getType()) {
                case GLUCOSE_CGM:
                    lastCgmValue = item.getValue();
                    break;
                case GLUCOSE_CGM_ALERT:
                    if (lastCgmValue > 0) {
                        item.setValue(lastCgmValue);
                    } else {
                        item.setValue(100);
                        LOG.log(Level.WARNING, "No CGM Value available for Alert: {0}", item.toString());
                    }
                    break;
                default:
                    break;

            }
        }

        return data;
    }
}
