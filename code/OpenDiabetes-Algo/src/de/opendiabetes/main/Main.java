package de.opendiabetes.main;

import de.opendiabetes.algo.Algorithm;
import de.opendiabetes.algo.BruteForceAlgo;
import de.opendiabetes.algo.OpenDiabetesAlgo;
import de.opendiabetes.main.dataprovider.AlgorithmDataProvider;
import de.opendiabetes.main.dataprovider.DemoDataProvider;
import de.opendiabetes.main.dataprovider.NightscoutDataProvider;
import de.opendiabetes.main.exception.DataProviderException;
import de.opendiabetes.vault.engine.container.VaultEntry;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Properties;

public class Main {
    /**
     * The thread running the algorithm
     */
    private static Thread main;

    /**
     * The input thread parsing commands
     */
    private static Input input;

    public static void main(String[] args) {
        Properties config = null;
        String host = null;
        String secret = null;
        TemporalAccessor lastest = null;
        TemporalAccessor oldest = null;
        Integer batchSize = null;
        String dataProviderName = "demo";
        String algorithmName = "demo";
        double carbRatio = 10;
        double insulinSensitivity = 35;
        double absorptionTime = 120;
        double insulinDuration = 180;
        boolean debug = false;
        try {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.startsWith("-")) {
                    arg = arg.substring(1);
                    switch (arg.toLowerCase()) {
                        case "config":
                            config = getPropertiesFileValue(arg, args, i);
                            i++;
                            break;
                        case "host":
                            host = getValue(arg, args, i);
                            i++;
                            break;
                        case "secret":
                        case "apisecret":
                            secret = getValue(arg, args, i);
                            i++;
                            break;
                        case "latest":
                            lastest = getDateTimeValue(arg, args, i);
                            i++;
                            break;
                        case "oldest":
                            oldest = getDateTimeValue(arg, args, i);
                            i++;
                            break;
                        case "batch":
                        case "batchsize":
                            batchSize = getIntValue(arg, args, i, 1);
                            i++;
                            break;
                        case "data":
                        case "provider":
                        case "dataprovider":
                            dataProviderName = getValue(arg, args, i);
                            i++;
                            break;
                        case "algo":
                        case "algorithm":
                            algorithmName = getValue(arg, args, i);
                            i++;
                            break;
                        case "carbratio":
                            carbRatio = getDoubleValue(arg, args, i);
                            i++;
                            break;
                        case "inssens":
                        case "inssensitivity":
                        case "insulinsensitivity":
                            insulinSensitivity = getDoubleValue(arg, args, i);
                            i++;
                            break;
                        case "absorptiontime":
                            absorptionTime = getDoubleValue(arg, args, i);
                            i++;
                            break;
                        case "insduration":
                        case "insulinduration":
                            insulinDuration = getDoubleValue(arg, args, i);
                            i++;
                            break;
                        case "debug":
                            debug = true;
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown argument " + arg);
                    }
                } else throw new IllegalArgumentException("Unknown argument " + arg);
            }
        } catch (IllegalArgumentException e) {
            logException("Error while parsing arguments", e, debug);
            return;
        }

        AlgorithmDataProvider dataProvider;
        try {
            switch (dataProviderName.toLowerCase()) {
                case "demo":
                    dataProvider = new DemoDataProvider();
                    break;
                case "ns":
                case "nightscout":
                    if (config != null) {
                        if (host == null && config.getProperty("host") != null)
                            host = config.getProperty("host");
                        if (secret == null && config.getProperty("secret") != null)
                            secret = config.getProperty("secret");
                        if (lastest == null && config.getProperty("latest") != null)
                            lastest = parseDateTime("latest", config.getProperty("latest"));
                        if (oldest == null && config.getProperty("oldest") != null)
                            oldest = parseDateTime("oldest", config.getProperty("oldest"));
                        if (batchSize == null && config.getProperty("batchsize") != null)
                            batchSize = parseInt("batchsize", config.getProperty("batchsize"), 1);
                    }
                    dataProvider = new NightscoutDataProvider(host, secret, lastest, oldest, batchSize);
                    break;
                default:
                    Log.logError("Unknown dataprovider " + dataProviderName);
                    return;
            }
        } catch (DataProviderException e) {
            logException("Error in data provider", e, debug);
            return;
        } catch (IllegalArgumentException e) {
            logException("Error while reading config", e, debug);
            return;
        }

        Algorithm algorithm;
        switch (algorithmName.toLowerCase()) {
            case "demo":
                algorithm = new OpenDiabetesAlgo();
                break;
            case "bruteforce":
                algorithm = new BruteForceAlgo();
                break;
            default:
                Log.logError("Unknown algorithm " + algorithmName);
                return;
        }

        algorithm.setCarbRatio(carbRatio);
        algorithm.setInsulinSensitivity(insulinSensitivity);
        algorithm.setAbsorptionTime(absorptionTime);
        algorithm.setInsulinDuration(insulinDuration);
        algorithm.setDataProvider(dataProvider);

        main = new Thread(() -> {
            List<VaultEntry> meals = algorithm.calculateMeals();
            Log.logInfo("Calculated %d meals:", meals.size());
            meals.forEach(e -> Log.logInfo("%s: %.3f", e.getTimestamp().toString(), e.getValue()));
            dataProvider.close();
        });
        main.start();

        //TODO: necessary?
        //input = new Input();
        //input.run();
    }

    private static String getValue(String arg, String[] args, int i) {
        if (i < args.length - 1)
            return args[i + 1];
        throw new IllegalArgumentException("Missing value for argument " + arg);
    }

    private static double getDoubleValue(String arg, String[] args, int i) {
        return parseDouble(arg, getValue(arg, args, i));
    }

    private static double parseDouble(String arg, String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid value for argument " + arg + ", has to be a number", e);
        }
    }

    private static int getIntValue(String arg, String[] args, int i, int min) {
        return parseInt(arg, getValue(arg, args, i), min);
    }

    private static int parseInt(String arg, String value, int min) {
        try {
            int integer = Integer.parseInt(value);
            if (integer < min)
                throw new IllegalArgumentException("Invalid value for argument " + arg + ", minimum is " + min);
            return integer;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid value for argument " + arg + ", has to be a number", e);
        }
    }

    private static TemporalAccessor getDateTimeValue(String arg, String[] args, int i) {
        return parseDateTime(arg, getValue(arg, args, i));
    }

    private static DateTimeFormatter parser = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static TemporalAccessor parseDateTime(String arg, String value) {
        try {
            return parser.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid value for argument " + arg + ", has to be in ISO-8601 representation without timezone data", e);
        }
    }

    private static Properties getPropertiesFileValue(String arg, String[] args, int i) {
        String value = getValue(arg, args, i);
        try (InputStream input = new FileInputStream(value)) {
            Properties properties = new Properties();
            properties.load(input);
            return properties;
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Cannot find config file at " + value, e);
        } catch (IOException e) {
            throw new IllegalArgumentException("IOException while reading config file: " + e.getMessage(), e);
        }
    }

    private static void logException(String message, Exception e, boolean debug) {
        Log.logError(message + ": " + e.getMessage());
        if (debug) {
            if (e.getCause() != null)
                e.getCause().printStackTrace();
            else e.printStackTrace();
        }
    }

    public static Thread getMain() {
        return main;
    }

    public static Input getInput() {
        return input;
    }
}
