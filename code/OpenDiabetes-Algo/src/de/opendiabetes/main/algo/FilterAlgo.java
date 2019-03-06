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
        long step = 5 * 60000; // 5 minutes
        long currentTime = firstTime - absorptionTime * 60000;

        while (currentTime <= lastTime) {
            times.add(currentTime);
            currentTime += step;
        }
//        long maxTime = lastTime-firstTime;
//        long minTime = firstTime + insulinDuration * 60000;
//        int startIdx = 0;
//System.out.println(firstTime/60000 +"::"+ lastTime/60000+"::"+minTime/60000+"::" + maxTime/60000+"::"+insulinDuration);
//        for (VaultEntry current : glucose) {
//
//            currentTime = current.getTimestamp().getTime();
//            
//            if (currentTime < minTime) {
//                startIdx++;
//            } else {
//                break;
//            }
//        }
//        if (glucose.size() - startIdx <= 0) {
//            return mealTreatments;
//        }

        matrix = new Array2DRowRealMatrix(glucose.size(), times.size());

        int row = 0;
        
//        resample glucose to 5 min grid?
        for (VaultEntry current : glucose) {
            currentTime = current.getTimestamp().getTime();

//            if (currentTime >= minTime) {
            currentPrediction = Predictions.predict(currentTime, mealTreatments, bolusTreatments,
                    basalTreatments, profile.getSensitivity(), insulinDuration, profile.getCarbratio(), absorptionTime);

            currentValue = current.getValue();

            deltaBg = currentValue - currentPrediction;
            nkbg = nkbg.append(deltaBg);
            for (int column = 0; column < times.size(); column++) {
                matrix.setEntry(row, column, Predictions.carbsOnBoard(currentTime - times.get(column), absorptionTime));
            }
            row++;
//            }
        }
        // COB*m = nkbg

        DecompositionSolver solver = new SingularValueDecomposition(matrix).getSolver();
        if (solver.isNonSingular()) {
            mealValues = solver.solve(nkbg);
            for (int i = 0; i < mealValues.getDimension(); i++) {
                meal = new VaultEntry(VaultEntryType.MEAL_MANUAL,
                        TimestampUtils.createCleanTimestamp(new Date(times.get(i))),
                        mealValues.getEntry(i));
                System.out.println(meal.toString());
                mealTreatments.add(meal);
            }
        }

        return mealTreatments;

    }
}
