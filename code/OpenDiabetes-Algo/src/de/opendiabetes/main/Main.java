package de.opendiabetes.main;

import de.opendiabetes.algo.Algorithm;
import de.opendiabetes.algo.BruteForceAlgo;
import de.opendiabetes.main.dataprovider.AlgorithmDataProvider;
import de.opendiabetes.main.dataprovider.DemoDataProvider;
import de.opendiabetes.main.dataprovider.NightscoutDataProvider;
import de.opendiabetes.main.exception.DataProviderException;
import de.opendiabetes.vault.engine.container.VaultEntry;

import java.util.List;

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
        String dataProviderName = "demo";
        String algorithmName = "demo";
        double carbRatio = 10;
        double insulinSensitivity = 35;
        double absorptionTime = 120;
        double insulinDuration = 180;
        String configPath = "config.properties";
        boolean debug = false;
        try {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.startsWith("-")) {
                    arg = arg.substring(1);
                    switch (arg.toLowerCase()) {
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
                        case "config":
                            configPath = getValue(arg, args, i);
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
            Log.logError("Error while parsing arguments: " + e.getMessage());
            if (debug)
                e.printStackTrace();
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
                    dataProvider = new NightscoutDataProvider(configPath);
                    break;
                default:
                    Log.logError("Unknown dataprovider " + dataProviderName);
                    return;
            }
        } catch (DataProviderException e) {
            Log.logError(e.getMessage());
            if (debug)
                e.getCause().printStackTrace();
            return;
        }

        Algorithm algorithm;
        switch (algorithmName.toLowerCase()) {
            case "demo":
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
        String value = getValue(arg, args, i);
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid value for argument " + arg + ", has to be a number");
        }
    }

    public static Thread getMain() {
        return main;
    }

    public static Input getInput() {
        return input;
    }
}
