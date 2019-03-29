package de.opendiabetes.vault.main.algo;

import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.main.math.Predictions;
import de.opendiabetes.vault.parser.Profile;

import de.opendiabetes.vault.util.TimestampUtils;

import java.time.LocalTime;
import java.time.ZoneId;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestAlgorithms {

    private List<VaultEntry> entries;
    private List<VaultEntry> basals;
    private List<VaultEntry> boli;
    private List<VaultEntry> testMeals;
    private Profile profile;
    private final int absTime = 120;
    private final int insDur = 180;
    private final double peak = 55;

    @BeforeEach
    public void init() {
        entries = new ArrayList<>();
        basals = new ArrayList<>();
        boli = new ArrayList<>();
        testMeals = new ArrayList<>();
        profile = new Profile(ZoneId.of("Zulu"), 35, 10, Collections.singletonList(new Profile.BasalProfile(LocalTime.of(0, 0), 0.2)));
    }

    @Test
    public void lineTest() {
        for (int i = 0; i < 200; i++) {
            entries.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(i * 5 * 60 * 1000), 100));
        }
        
        Algorithm algorithm;
        List<VaultEntry> resultMeals;
        algorithm = new LMAlgo(absTime, insDur, peak, profile, entries, boli, basals);
        resultMeals = algorithm.calculateMeals();
        assertEquals(0, resultMeals.size());

        algorithm = new OldLMAlgo(absTime, insDur, peak, profile, entries, boli, basals);
        resultMeals = algorithm.calculateMeals();
        assertEquals(0, resultMeals.size());

        algorithm = new MinimumAlgo(absTime, insDur, peak, profile, entries, boli, basals);
        resultMeals = algorithm.calculateMeals();
        assertEquals(0, resultMeals.size());

        algorithm = new PolyCurveFitterAlgo(absTime, insDur, peak, profile, entries, boli, basals);
        resultMeals = algorithm.calculateMeals();
        assertEquals(0, resultMeals.size());

        algorithm = new QRAlgo(absTime, insDur, peak, profile, entries, boli, basals);
        resultMeals = algorithm.calculateMeals();
        assertEquals(0, resultMeals.size());
    }

    @Test
    public void fallingGraphTest() {
        for (int i = 0; i < 200; i++) {
            entries.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(i * 5 * 60 * 1000), 200 - ((i * i) / 300.0)));
        }

        Algorithm algorithm;
        List<VaultEntry> resultMeals;
        algorithm = new LMAlgo(absTime, insDur, peak, profile, entries, boli, basals);
        resultMeals = algorithm.calculateMeals();
        assertEquals(0, resultMeals.size());

        algorithm = new OldLMAlgo(absTime, insDur, peak, profile, entries, boli, basals);
        resultMeals = algorithm.calculateMeals();
        assertEquals(0, resultMeals.size());

        algorithm = new MinimumAlgo(absTime, insDur, peak, profile, entries, boli, basals);
        resultMeals = algorithm.calculateMeals();
        assertEquals(0, resultMeals.size());

        algorithm = new PolyCurveFitterAlgo(absTime, insDur, peak, profile, entries, boli, basals);
        resultMeals = algorithm.calculateMeals();
        assertEquals(0, resultMeals.size());

        algorithm = new QRAlgo(absTime, insDur, peak, profile, entries, boli, basals);
        resultMeals = algorithm.calculateMeals();
        assertEquals(0, resultMeals.size());
    }

    @Test
    public void oneMealTest() {
        int timeDelta = 5 * 60 * 1000;
        int valueDelta = 5;
        int timestamp = 15 * 60 * 1000;
        int value = 50;

        testMeals.add(new VaultEntry(VaultEntryType.MEAL_MANUAL, TimestampUtils.createCleanTimestamp(new Date(timestamp)), value));
        int startValue = 100;
        for (int i = -30; i < 50; i++) {
            double d = Predictions.predict(i * 5 * 60 * 1000, testMeals, boli, basals, profile.getSensitivity(), insDur, profile.getCarbratio(), absTime, peak);

//            entries.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, TimestampUtils.createCleanTimestamp(new Date(i * 5 * 60 * 1000)), d + startValue));

            entries.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(i * 5 * 60 * 1000), d + startValue));
        }
        double result;
        long resTime;

        Algorithm algorithm;
        List<VaultEntry> resultMeals;
        algorithm = new LMAlgo(absTime, insDur, peak, profile, entries, boli, basals);
        resultMeals = algorithm.calculateMeals();
        result = 0;
        resTime = 0;
        System.out.println("oneMealTest #meals:"+ resultMeals.size());
        System.out.println("oneMealTest time:"+ resultMeals.get(0).getTimestamp().toString());
        System.out.println("oneMealTest value:"+ resultMeals.get(0).getValue());
        System.out.println("oneMealTest time:"+ testMeals.get(0).getTimestamp().toString());
        System.out.println("oneMealTest value:"+ testMeals.get(0).getValue());

        for (int i = 0; i < resultMeals.size(); i++) {
            result += resultMeals.get(i).getValue();
            resTime += resultMeals.get(i).getTimestamp().getTime();
        }
        resTime /= resultMeals.size();
        assertEquals(timestamp, resTime, timeDelta);
        assertEquals(value, result, valueDelta);

        algorithm = new OldLMAlgo(absTime, insDur, peak, profile, entries, boli, basals);
        resultMeals = algorithm.calculateMeals();
        result = 0;
        resTime = 0;
        for (int i = 0; i < resultMeals.size(); i++) {
            result += resultMeals.get(i).getValue();
            resTime += resultMeals.get(i).getTimestamp().getTime();
        }
        resTime /= resultMeals.size();
        assertEquals(timestamp, resTime, timeDelta);
        assertEquals(value, result, valueDelta);

        algorithm = new MinimumAlgo(absTime, insDur, peak, profile, entries, boli, basals);
        resultMeals = algorithm.calculateMeals();
        result = 0;
        resTime = 0;
        for (int i = 0; i < resultMeals.size(); i++) {
            result += resultMeals.get(i).getValue();
            resTime += resultMeals.get(i).getTimestamp().getTime();
        }
        resTime /= resultMeals.size();
        assertEquals(timestamp, resTime, timeDelta);
        assertEquals(value, result, valueDelta);

        algorithm = new PolyCurveFitterAlgo(absTime, insDur, peak, profile, entries, boli, basals); //remove?
        resultMeals = algorithm.calculateMeals();
        result = 0;
        resTime = 0;
        for (int i = 0; i < resultMeals.size(); i++) {
            result += resultMeals.get(i).getValue();
            resTime += resultMeals.get(i).getTimestamp().getTime();
        }
        resTime /= resultMeals.size();
        //assertEquals(timestamp, resTime, timeDelta);
        //assertEquals(value, result, valueDelta);

        algorithm = new QRAlgo(absTime, insDur, peak, profile, entries, boli, basals); //remove?
        resultMeals = algorithm.calculateMeals();
        result = 0;
        resTime = 0;
        for (int i = 0; i < resultMeals.size(); i++) {
            result += resultMeals.get(i).getValue();
            resTime += resultMeals.get(i).getTimestamp().getTime();
        }
        System.out.println(resultMeals);
        resTime /= resultMeals.size();
        //assertEquals(timestamp, resTime, timeDelta);
        //assertEquals(value, result, valueDelta);
    }

    @Test
    public void randomizedCurveTest() {
        Random random = new Random();
        int timeDelta = 10 * 60 * 1000;
        int valueDelta = 5;
        int firstMealTime = 10 * 60 * 1000;
        int secondMealTime = 250 * 60 * 1000;
        int firstValue = 10 + random.nextInt(50);
        int secondValue = 10 + random.nextInt(50);

        testMeals.add(new VaultEntry(VaultEntryType.MEAL_MANUAL, new Date(firstMealTime), firstValue));
        testMeals.add(new VaultEntry(VaultEntryType.MEAL_MANUAL, new Date(secondMealTime), secondValue));
        for (int i = 0; i < 2; i++) {
            boli.add(new VaultEntry(VaultEntryType.BOLUS_NORMAL, new Date((i + 1) * 40 * 60 * 1000), random.nextDouble() * 2));
        }
        int startValue = 100;
        for (int i = -30; i < 120; i++) {
            double d = Predictions.predict(i * 5 * 60 * 1000, testMeals, boli, basals, profile.getSensitivity(), insDur, profile.getCarbratio(), absTime, peak);
            entries.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(i * 5 * 60 * 1000), d + startValue));
        }
        Algorithm algorithm;
        List<VaultEntry> resultMeals;
        algorithm = new LMAlgo(absTime, insDur, peak, profile, entries, boli, basals);
        resultMeals = algorithm.calculateMeals();

        
        System.out.println("randomizedCurveTest #meals:"+ resultMeals.size());
        System.out.println("randomizedCurveTest time:"+ resultMeals.get(0).getTimestamp().toString());
        System.out.println("randomizedCurveTest value:"+ resultMeals.get(0).getValue());
        System.out.println("randomizedCurveTest time:"+ testMeals.get(0).getTimestamp().toString());
        System.out.println("randomizedCurveTest value:"+ testMeals.get(0).getValue());
        
//        checkMeals(timeDelta, valueDelta, resultMeals);

        algorithm = new OldLMAlgo(absTime, insDur, peak, profile, entries, boli, basals);
        resultMeals = algorithm.calculateMeals();
        checkMeals(timeDelta, valueDelta, resultMeals);

        algorithm = new MinimumAlgo(absTime, insDur, peak, profile, entries, boli, basals);
        resultMeals = algorithm.calculateMeals();
        checkMeals(timeDelta, valueDelta, resultMeals);

        algorithm = new PolyCurveFitterAlgo(absTime, insDur, peak, profile, entries, boli, basals);
        resultMeals = algorithm.calculateMeals();
        for (VaultEntry meal : testMeals) {
            //checkMealsAround(timeDelta, valueDelta, resultMeals, meal);
        }

        algorithm = new QRAlgo(absTime, insDur, peak, profile, entries, boli, basals);
        resultMeals = algorithm.calculateMeals();
        for (VaultEntry meal : testMeals) {
            //checkMealsAround(timeDelta, valueDelta, resultMeals, meal);
        }

    }

    @Test
    public void disturbedDataTest() {
        Random random = new Random();
        int timeDelta = 12 * 60 * 1000;
        int valueDelta = 5;
        int firstMealTime = 10 * 60 * 1000;
        int secondMealTime = 250 * 60 * 1000;
        int firstValue = 10 + random.nextInt(50);
        int secondValue = 10 + random.nextInt(50);
        List<VaultEntry> disturbedEntries = new ArrayList<>();

        testMeals.add(new VaultEntry(VaultEntryType.MEAL_MANUAL, new Date(firstMealTime), firstValue));
        testMeals.add(new VaultEntry(VaultEntryType.MEAL_MANUAL, new Date(secondMealTime), secondValue));
        for (int i = 0; i < 2; i++) {
            boli.add(new VaultEntry(VaultEntryType.BOLUS_NORMAL, new Date((i + 1) * 40 * 60 * 1000), random.nextDouble() * 3));
        }
        int startValue = 100;
        for (int i = -30; i < 120; i++) {
            double d = Predictions.predict(i * 5 * 60 * 1000, testMeals, boli, basals, profile.getSensitivity(), insDur, profile.getCarbratio(), absTime, peak);
            entries.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(i * 5 * 60 * 1000), d + startValue));
            disturbedEntries.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(i * 5 * 60 * 1000), d + startValue - 6 + random.nextInt(13)));
        }

        Algorithm algorithm;
        List<VaultEntry> resultMeals, disturbedMeals;
        algorithm = new LMAlgo(absTime, insDur, peak, profile, entries, boli, basals);
        resultMeals = algorithm.calculateMeals();
        algorithm = new LMAlgo(absTime, insDur, peak, profile, disturbedEntries, boli, basals);
        disturbedMeals = algorithm.calculateMeals();
        for (VaultEntry meal : resultMeals) {
            checkMealsAround(timeDelta, valueDelta, disturbedMeals, meal);
        }

        algorithm = new MinimumAlgo(absTime, insDur, peak, profile, entries, boli, basals);
        resultMeals = algorithm.calculateMeals();
        algorithm = new MinimumAlgo(absTime, insDur, peak, profile, disturbedEntries, boli, basals);
        disturbedMeals = algorithm.calculateMeals();
        for (VaultEntry meal : resultMeals) {
            //checkMealsAround(timeDelta, valueDelta, disturbedMeals, meal);
        }

        algorithm = new OldLMAlgo(absTime, insDur, peak, profile, entries, boli, basals);
        resultMeals = algorithm.calculateMeals();
        algorithm = new OldLMAlgo(absTime, insDur, peak, profile, disturbedEntries, boli, basals);
        disturbedMeals = algorithm.calculateMeals();
        for (VaultEntry meal : resultMeals) {
            checkMealsAround(timeDelta, valueDelta, disturbedMeals, meal);
        }

        algorithm = new PolyCurveFitterAlgo(absTime, insDur, peak, profile, entries, boli, basals);
        resultMeals = algorithm.calculateMeals();
        algorithm = new PolyCurveFitterAlgo(absTime, insDur, peak, profile, disturbedEntries, boli, basals);
        disturbedMeals = algorithm.calculateMeals();
        for (VaultEntry meal : resultMeals) {
            //checkMealsAround(timeDelta, valueDelta, disturbedMeals, meal);
        }

        algorithm = new QRAlgo(absTime, insDur, peak, profile, entries, boli, basals);
        resultMeals = algorithm.calculateMeals();
        algorithm = new QRAlgo(absTime, insDur, peak, profile, disturbedEntries, boli, basals);
        disturbedMeals = algorithm.calculateMeals();
        for (VaultEntry meal : resultMeals) {
            //checkMealsAround(timeDelta, valueDelta, disturbedMeals, meal);
        }

    }

    private void checkMealsAround(int timeDelta, int valueDelta, List<VaultEntry> resultMeals, VaultEntry meal) {
        double sum = resultMeals.stream().filter(e -> (e.getTimestamp().getTime() < meal.getTimestamp().getTime() + timeDelta
                && e.getTimestamp().getTime() > meal.getTimestamp().getTime() - timeDelta)).
                mapToDouble(VaultEntry::getValue).sum();
        assertEquals(meal.getValue(), sum, valueDelta);
    }

    private void checkMeals(int timeDelta, int valueDelta, List<VaultEntry> resultMeals) {
        assertEquals(testMeals.size(), resultMeals.size());
        for (int i = 0; i < testMeals.size(); i++) {
            VaultEntry testMeal = testMeals.get(i);
            VaultEntry resultMeal = resultMeals.get(i);
            assertEquals(testMeal.getValue(), resultMeal.getValue(), valueDelta);
            assertEquals(testMeal.getTimestamp().getTime(), resultMeal.getTimestamp().getTime(), timeDelta);
        }
    }
}
