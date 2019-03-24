package de.opendiabetes.vault.main.algo;

import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.main.dataprovider.AlgorithmDataProvider;
import de.opendiabetes.vault.parser.Profile;
import org.junit.Before;
import org.junit.Test;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAlgorithms {
    private List<VaultEntry> entries;
    private List<VaultEntry> basals;
    private List<VaultEntry> boli;
    private List<VaultEntry> testMeals;
    private Profile profile;
    private final int absTime = 120;
    private final int insDur = 180;


    @Before
    public void init() {
        entries = new ArrayList<>();
        basals = new ArrayList<>();
        boli = new ArrayList<>();
        testMeals = new ArrayList<>();
        profile = new Profile(ZoneId.of("Zulu"), 5, 5, null);
    }


    @Test
    public void testLine() {
        for (int i = 0; i < 50; i++) {
            entries.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(i * 5 * 60 * 1000), 100));
        }
        TestDataProvider testDataProvider = new TestDataProvider(entries, basals, boli, profile);

        Algorithm algorithm;
        List<VaultEntry> resultMeals;
//        algorithm = new LMAlgo(absTime, insDur, testDataProvider);
//        resultMeals = algorithm.calculateMeals();
        //assertEquals(0, resultMeals.size());
        algorithm = new FilterAlgo(absTime, insDur, testDataProvider);
        resultMeals = algorithm.calculateMeals();
        assertEquals(0, resultMeals.size());
        algorithm = new MinimumAlgo(absTime, insDur, testDataProvider);
        resultMeals = algorithm.calculateMeals();
        assertEquals(0, resultMeals.size());
        algorithm = new PolyCurveFitterAlgo(absTime, insDur, testDataProvider);
        resultMeals = algorithm.calculateMeals();
        assertEquals(0,resultMeals.size());
//        algorithm = new QRAlgo(absTime, insDur, testDataProvider);
//        resultMeals = algorithm.calculateMeals();
        //assertEquals(0,resultMeals.size());
        algorithm = new QRDiffAlgo(absTime, insDur, testDataProvider);
        resultMeals = algorithm.calculateMeals();
        assertEquals(0, resultMeals.size());
    }

    @Test
    public void testFallingGraph() {
        for (int i = 0; i < 20; i++) {
            entries.add(new VaultEntry(VaultEntryType.GLUCOSE_CGM, new Date(i * 5 * 60 * 1000), 200 - ((i * i) / 2)));
        }
        TestDataProvider testDataProvider = new TestDataProvider(entries, basals, boli, profile);

        Algorithm algorithm;
        List<VaultEntry> resultMeals;
//        algorithm = new LMAlgo(absTime, insDur, testDataProvider);
//        resultMeals = algorithm.calculateMeals();
        //assertEquals(0, resultMeals.size());
        algorithm = new FilterAlgo(absTime, insDur, testDataProvider);
        resultMeals = algorithm.calculateMeals();
        assertEquals(0, resultMeals.size());
        algorithm = new MinimumAlgo(absTime, insDur, testDataProvider);
        resultMeals = algorithm.calculateMeals();
        assertEquals(0, resultMeals.size());
//        algorithm = new PolyCurveFitterAlgo(absTime, insDur, testDataProvider);
//        resultMeals = algorithm.calculateMeals();
        //assertEquals(0,resultMeals.size());
        algorithm = new QRAlgo(absTime, insDur, testDataProvider);
        resultMeals = algorithm.calculateMeals();
        assertEquals(0,resultMeals.size());
        algorithm = new QRDiffAlgo(absTime, insDur, testDataProvider);
        resultMeals = algorithm.calculateMeals();
        assertEquals(0, resultMeals.size());
    }

    @Test
    public void testMealCalculation(){

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
