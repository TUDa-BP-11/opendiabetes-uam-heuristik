package de.opendiabetes.vault.main.algo;

import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.main.dataprovider.AlgorithmDataProvider;
import de.opendiabetes.vault.main.math.Predictions;
import de.opendiabetes.vault.parser.Profile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAlgorithms {
    private List<VaultEntry> entries;
    private List<VaultEntry> basals;
    private List<VaultEntry> boli;
    private List<VaultEntry> testMeals;
    private Profile profile;
    private final int absTime = 120;
    private final int insDur = 180;


    @BeforeEach
    public void init() {
        entries = new ArrayList<>();
        basals = new ArrayList<>();
        boli = new ArrayList<>();
        testMeals = new ArrayList<>();
        profile = new Profile(ZoneId.of("Zulu"), 35, 10, null);
    }


    @Test
    public void lineTest() {
        for (int i = 0; i < 50; i++) {
            entries.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(i * 5 * 60 * 1000), 100));
        }
        TestDataProvider testDataProvider = new TestDataProvider(entries, basals, boli, profile);

        Algorithm algorithm;
        List<VaultEntry> resultMeals;
        algorithm = new LMAlgo(absTime, insDur, testDataProvider);
        resultMeals = algorithm.calculateMeals();
        assertEquals(0, resultMeals.size());

        algorithm = new OldLMAlgo(absTime, insDur, testDataProvider);
        resultMeals = algorithm.calculateMeals();
        assertEquals(0, resultMeals.size());

        algorithm = new MinimumAlgo(absTime, insDur, testDataProvider);
        resultMeals = algorithm.calculateMeals();
        assertEquals(0, resultMeals.size());

        algorithm = new PolyCurveFitterAlgo(absTime, insDur, testDataProvider);
        resultMeals = algorithm.calculateMeals();
        assertEquals(0, resultMeals.size());

        algorithm = new QRAlgo(absTime, insDur, testDataProvider);
        resultMeals = algorithm.calculateMeals();
        //assertEquals(0, resultMeals.size());
    }

    @Test
    public void fallingGraphTest() {
        for (int i = 0; i < 20; i++) {
            entries.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(i * 5 * 60 * 1000), 200 - ((i * i) / 2)));
        }
        TestDataProvider testDataProvider = new TestDataProvider(entries, basals, boli, profile);

        Algorithm algorithm;
        List<VaultEntry> resultMeals;
        algorithm = new LMAlgo(absTime, insDur, testDataProvider);
        resultMeals = algorithm.calculateMeals();
        //assertEquals(0, resultMeals.size());

        algorithm = new OldLMAlgo(absTime, insDur, testDataProvider);
        resultMeals = algorithm.calculateMeals();
        //assertEquals(0, resultMeals.size());

        algorithm = new MinimumAlgo(absTime, insDur, testDataProvider);
        resultMeals = algorithm.calculateMeals();
        assertEquals(0, resultMeals.size());

        algorithm = new PolyCurveFitterAlgo(absTime, insDur, testDataProvider);
        resultMeals = algorithm.calculateMeals();
        assertEquals(0, resultMeals.size());

        algorithm = new QRAlgo(absTime, insDur, testDataProvider);
        resultMeals = algorithm.calculateMeals();
        assertEquals(0, resultMeals.size());
    }

    @Test
    public void oneMealTest() {
        int timeDelta = 5 * 60 * 1000;
        int valueDelta = 5;
        int timestamp = 15 * 60 * 1000;
        int value = 50;

        testMeals.add(new VaultEntry(VaultEntryType.MEAL_MANUAL, new Date(timestamp), value));
        int startValue = 100;
        for (int i = 0; i < 50; i++) {
            double d = Predictions.predict(i * 5 * 60 * 1000, testMeals, boli, basals, profile.getSensitivity(), insDur, profile.getCarbratio(), absTime);
            entries.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(i * 5 * 60 * 1000), d + startValue));
        }
        TestDataProvider testDataProvider = new TestDataProvider(entries, basals, boli, profile);
        double result;
        long resTime;

        Algorithm algorithm;
        List<VaultEntry> resultMeals;
        algorithm = new LMAlgo(absTime, insDur, testDataProvider);
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

        algorithm = new OldLMAlgo(absTime, insDur, testDataProvider);
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

        algorithm = new MinimumAlgo(absTime, insDur, testDataProvider);
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

        algorithm = new PolyCurveFitterAlgo(absTime, insDur, testDataProvider); //remove?
        resultMeals = algorithm.calculateMeals();
        result = 0;
        resTime = 0;
        for (int i = 0; i < resultMeals.size(); i++) {
            result += resultMeals.get(i).getValue();
            resTime += resultMeals.get(i).getTimestamp().getTime();
        }
        //resTime /= resultMeals.size();
        //assertEquals(timestamp, resTime, timeDelta);
        //assertEquals(value, result, valueDelta);

        algorithm = new QRAlgo(absTime, insDur, testDataProvider); //remove?
        resultMeals = algorithm.calculateMeals();
        result = 0;
        resTime = 0;
        for (int i = 0; i < resultMeals.size(); i++) {
            result += resultMeals.get(i).getValue();
            resTime += resultMeals.get(i).getTimestamp().getTime();
        }
        //resTime /= resultMeals.size();
        //assertEquals(timestamp, resTime, timeDelta);
        //assertEquals(value, result, valueDelta);
    }

    @Test
    public void randomizedCurveTest(){
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
            boli.add(new VaultEntry(VaultEntryType.BOLUS_NORMAL, new Date((i + 1) * 40 * 60 * 1000), random.nextDouble() * 3));
        }
        for (int i = 0; i < 10; i++) {
            basals.add(new VaultEntry(VaultEntryType.BASAL_PROFILE, new Date((i) * 45 * 60 * 1000), (random.nextDouble() - 0.5) * 0.1));
        }
        int startValue = 100;
        for (int i = 0; i < 120; i++) {
            double d = Predictions.predict(i * 5 * 60 * 1000, testMeals, boli, basals, profile.getSensitivity(), insDur, profile.getCarbratio(), absTime);
            entries.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(i * 5 * 60 * 1000), d + startValue));
        }
        TestDataProvider testDataProvider = new TestDataProvider(entries, basals, boli, profile);

        Algorithm algorithm;
        List<VaultEntry> resultMeals;
        algorithm = new LMAlgo(absTime, insDur, testDataProvider);
        resultMeals = algorithm.calculateMeals();
        checkMeals(timeDelta, valueDelta, resultMeals);

        algorithm = new OldLMAlgo(absTime, insDur, testDataProvider);
        resultMeals = algorithm.calculateMeals();
        checkMeals(timeDelta, valueDelta, resultMeals);

        algorithm = new MinimumAlgo(absTime, insDur, testDataProvider);
        resultMeals = algorithm.calculateMeals();
        checkMeals(timeDelta, valueDelta, resultMeals);

        algorithm = new PolyCurveFitterAlgo(absTime, insDur, testDataProvider);
        resultMeals = algorithm.calculateMeals();
        for (VaultEntry meal: testMeals){
            //checkMealsAround(timeDelta, valueDelta, resultMeals, meal);
        }

        algorithm = new QRAlgo(absTime, insDur, testDataProvider);
        resultMeals = algorithm.calculateMeals();
        for (VaultEntry meal: testMeals){
            //checkMealsAround(timeDelta, valueDelta, resultMeals, meal);
        }

    }

    @Test
    public void disturbedDataTest(){
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
        for (int i = 0; i < 10; i++) {
            basals.add(new VaultEntry(VaultEntryType.BASAL_PROFILE, new Date((i) * 45 * 60 * 1000), (random.nextDouble() - 0.5) * 0.1));
        }
        int startValue = 100;
        for (int i = 0; i < 120; i++) {
            double d = Predictions.predict(i * 5 * 60 * 1000, testMeals, boli, basals, profile.getSensitivity(), insDur, profile.getCarbratio(), absTime);
            entries.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(i * 5 * 60 * 1000), d + startValue));
            disturbedEntries.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(i * 5 * 60 * 1000), d + startValue - 6 + random.nextInt(13)));
        }
        TestDataProvider testDataProvider = new TestDataProvider(entries, basals, boli, profile);
        TestDataProvider distDataProvider = new TestDataProvider(disturbedEntries, basals, boli, profile);

        Algorithm algorithm;
        List<VaultEntry> resultMeals, disturbedMeals;
        algorithm = new LMAlgo(absTime, insDur, testDataProvider);
        resultMeals = algorithm.calculateMeals();
        algorithm = new LMAlgo(absTime, insDur, distDataProvider);
        disturbedMeals = algorithm.calculateMeals();
        for (VaultEntry meal: resultMeals){
            checkMealsAround(timeDelta, valueDelta, disturbedMeals, meal);
        }

        algorithm = new MinimumAlgo(absTime, insDur, testDataProvider);
        resultMeals = algorithm.calculateMeals();
        algorithm = new MinimumAlgo(absTime, insDur, distDataProvider);
        disturbedMeals = algorithm.calculateMeals();
        for (VaultEntry meal: resultMeals){
            //checkMealsAround(timeDelta, valueDelta, disturbedMeals, meal);
        }

        algorithm = new OldLMAlgo(absTime, insDur, testDataProvider);
        resultMeals = algorithm.calculateMeals();
        algorithm = new OldLMAlgo(absTime, insDur, distDataProvider);
        disturbedMeals = algorithm.calculateMeals();
        for (VaultEntry meal: resultMeals){
            checkMealsAround(timeDelta, valueDelta, disturbedMeals, meal);
        }

        algorithm = new PolyCurveFitterAlgo(absTime, insDur, testDataProvider);
        resultMeals = algorithm.calculateMeals();
        algorithm = new PolyCurveFitterAlgo(absTime, insDur, distDataProvider);
        disturbedMeals = algorithm.calculateMeals();
        for (VaultEntry meal: resultMeals){
            //checkMealsAround(timeDelta, valueDelta, disturbedMeals, meal);
        }

        algorithm = new QRAlgo(absTime, insDur, testDataProvider);
        resultMeals = algorithm.calculateMeals();
        algorithm = new QRAlgo(absTime, insDur, distDataProvider);
        disturbedMeals = algorithm.calculateMeals();
        for (VaultEntry meal: resultMeals){
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
            assertEquals(testMeal.getValue() , resultMeal.getValue(), valueDelta);
            assertEquals(testMeal.getTimestamp().getTime() , resultMeal.getTimestamp().getTime(), timeDelta);
        }
    }

    private static class TestDataProvider implements AlgorithmDataProvider {
        private final List<VaultEntry> entries, basals, boli;
        private final Profile profile;

        public TestDataProvider(List<VaultEntry> entries, List<VaultEntry> basals, List<VaultEntry> boli, Profile profile) {
            this.entries = entries;
            this.basals = basals;
            this.boli = boli;
            this.profile = profile;
        }

        @Override
        public List<VaultEntry> getGlucoseMeasurements() {
            return entries;
        }

        @Override
        public List<VaultEntry> getBolusTreatments() {
            return boli;
        }

        @Override
        public List<VaultEntry> getRawBasalTreatments() {
            return null;
        }

        @Override
        public List<VaultEntry> getBasalDifferences() {
            return basals;
        }

        @Override
        public Profile getProfile() {
            return profile;
        }
    }

}
