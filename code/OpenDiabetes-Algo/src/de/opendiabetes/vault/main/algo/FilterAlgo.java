package de.opendiabetes.vault.main.algo;

import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.main.dataprovider.AlgorithmDataProvider;
import de.opendiabetes.vault.main.math.Predictions;
import de.opendiabetes.vault.nsapi.NSApi;
import de.opendiabetes.vault.parser.Profile;
import de.opendiabetes.vault.util.TimestampUtils;
import org.apache.commons.math3.linear.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

public class FilterAlgo extends Algorithm {

    public FilterAlgo(long absorptionTime, long insulinDuration, Profile profile) {
        super(absorptionTime, insulinDuration, profile);
    }

    public FilterAlgo(long absorptionTime, long insulinDuration, AlgorithmDataProvider dataProvider) {
        super(absorptionTime, insulinDuration, dataProvider);
    }

    @Override
    public List<VaultEntry> calculateMeals() {
        RealMatrix matrix;
        RealVector nkbg;
        RealVector mealValues;

        ArrayList<Long> times;
        List<VaultEntry> mealTreatments;
        mealTreatments = new ArrayList<>();

        double currentPrediction;
        double currentValue;

        nkbg = new ArrayRealVector();

        times = new ArrayList<>();

        // possible discrete meal times within snippet time range each 5 Minutes.
        final long firstTime = glucose.get(0).getTimestamp().getTime() / 60000 + Math.max(absorptionTime, insulinDuration);
        long lastTime = glucose.get(glucose.size() - 1).getTimestamp().getTime() / 60000;
        long step = 5; // 5 minutes
        long currentTime = firstTime - absorptionTime;

        while (currentTime <= lastTime) {
            times.add(currentTime);
            currentTime += step;
        }
        matrix = new Array2DRowRealMatrix(glucose.size(), times.size());

        int row = 0;

//        resample glucose to 5 min grid?
        for (int i = 0; i < glucose.size(); i++) {
            VaultEntry current = glucose.get(i);
            currentTime = current.getTimestamp().getTime() / 60000;
            // skip bg values until start time
            if (currentTime < firstTime) {
                continue;
            }
            currentPrediction = Predictions.predict(current.getTimestamp().getTime(), mealTreatments, bolusTreatments,
                    basalTreatments, profile.getSensitivity(), insulinDuration, profile.getCarbratio(), absorptionTime);

//            currentValue = Filter.getMedian(glucose, i, 5, absorptionTime / 3);
            currentValue = current.getValue();

            nkbg = nkbg.append(currentValue - currentPrediction);
            for (int column = 0; column < times.size(); column++) {
                matrix.setEntry(row, column, Predictions.carbsOnBoard(currentTime - times.get(column), absorptionTime));
            }
            row++;
        }

        DecompositionSolver solver = new SingularValueDecomposition(matrix).getSolver();
        if (solver.isNonSingular()) {
            mealValues = solver.solve(nkbg);
            if (mealValues.getDimension() == times.size()) {
                for (int i = 0; i < mealValues.getDimension(); i++) {
                    mealTreatments.add(new VaultEntry(VaultEntryType.MEAL_MANUAL,
                            TimestampUtils.createCleanTimestamp(new Date(times.get(i) * 60000)), mealValues.getEntry(i)));
                }
            }
        } else {
            NSApi.LOGGER.log(Level.WARNING, "Singular");
        }

        return mealTreatments;

    }
}
