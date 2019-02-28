package de.opendiabetes.main.algo;

import de.opendiabetes.main.dataprovider.AlgorithmDataProvider;
import de.opendiabetes.main.math.Predictions;
import de.opendiabetes.parser.Profile;
import de.opendiabetes.vault.container.VaultEntry;
import de.opendiabetes.vault.container.VaultEntryType;
import de.opendiabetes.vault.util.TimestampUtils;
import org.apache.commons.math3.linear.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class FilterAlgo extends Algorithm {

    public FilterAlgo(double absorptionTime, double insulinDuration, Profile profile) {
        super(absorptionTime, insulinDuration, profile);
    }

    public FilterAlgo(double absorptionTime, double insulinDuration, AlgorithmDataProvider dataProvider) {
        super(absorptionTime, insulinDuration, dataProvider);
    }

    @Override
    public List<VaultEntry> calculateMeals() {
        RealMatrix matrix;
        RealVector nkbg;
        RealVector mealValues;
        
        ArrayList<Long> times;

        List<VaultEntry> mealTreatments;
        VaultEntry meal;

        double currentPrediction;
        double currentValue;
        double deltaBg;

        nkbg = new ArrayRealVector();
        mealTreatments = new ArrayList<>();
        times = new ArrayList<>();

        // possible discrete meal times within snippet time range each 5 Minutes.
        long firstTime = glucose.get(0).getTimestamp().getTime();
        long lastTime = glucose.get(glucose.size() - 1).getTimestamp().getTime();
        long step = 5 * 60 * 1000; // 5 minutes
        long currentTime = firstTime;
        while (currentTime <= lastTime) {
            times.add(currentTime);
            currentTime += step;
        }
        matrix = new Array2DRowRealMatrix(glucose.size(), times.size()); //3

        int row = 0;
        for (VaultEntry current : glucose) {
            currentTime = current.getTimestamp().getTime();

            currentPrediction = Predictions.predict(currentTime, mealTreatments, bolusTreatments,
                    basalTreatments, profile.getSensitivity(), insulinDuration, profile.getCarbratio(), absorptionTime);

            currentValue = current.getValue();

            deltaBg = currentValue - currentPrediction;
            nkbg = nkbg.append(deltaBg);

            for (int column = 0; column < times.size(); column++) {
                long dt = currentTime - times.get(column);
                if (dt < 0) {
                    dt = 0;
                }

                matrix.setEntry(row, column, Predictions.carbsOnBoard(dt, absorptionTime));

            }
            row++;
        }
        // COB*m = nkbg
        DecompositionSolver solver = new QRDecomposition(matrix).getSolver();
        mealValues = solver.solve(nkbg);
        for (int i = 0; i < mealValues.getDimension(); i++) {
            meal = new VaultEntry(VaultEntryType.MEAL_MANUAL,
                    TimestampUtils.createCleanTimestamp(new Date(times.get(i))),
                    mealValues.getEntry(i));
            System.out.println(meal.toString());
            mealTreatments.add(meal);
        }

        return mealTreatments;

    }
}
