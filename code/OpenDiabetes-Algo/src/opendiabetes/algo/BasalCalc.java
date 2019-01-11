package opendiabetes.algo;

import de.opendiabetes.parser.Profile;
import de.opendiabetes.vault.engine.container.VaultEntry;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BasalCalc {

    private Profile profile;
    private List<tmpBasal> tmpBasals;

    public BasalCalc(Profile profile) {
        this.profile = profile;
    }

    public List<tmpBasal> calculateBasal(List<VaultEntry> basalTreatments) {
        tmpBasals = new ArrayList<>();
        VaultEntry current;

        if (!basalTreatments.isEmpty()) {
            current = basalTreatments.remove(0);

            while (!basalTreatments.isEmpty()) {
                VaultEntry next = basalTreatments.remove(0);
                long deltaTime = Math.round((next.getTimestamp().getTime() - current.getTimestamp().getTime()) / 60000.0);

                if (deltaTime < current.getValue2()) {
                    double value = current.getValue() * deltaTime / current.getValue2();
                    maketmpBasal(value, deltaTime, current.getTimestamp());
                } else {
                    maketmpBasal(current.getValue(), Math.round(current.getValue2()), current.getTimestamp());
                }

                current = next;
            }
            //last one
            maketmpBasal(current.getValue(), Math.round(current.getValue2()), current.getTimestamp());
        }
        return tmpBasals;
    }

    private void maketmpBasal(double value, long duration, Date date) {
        if (duration <= 0) return;

        List<Profile.BasalProfile> list = profile.getBasalProfiles();
        long treatmentTime = (date.getTime() / 60000) % (60 * 24); //Time in min from 00:00

        for (int i = 0; i < list.size() - 1; i++) {
            long profileTime1 = list.get(i).getStart().getHour() * 60 + list.get(i).getStart().getMinute(); //Time in min from 00:00
            long profileTime2 = list.get(i + 1).getStart().getHour() * 60 + list.get(i + 1).getStart().getMinute();

            if (profileTime1 <= treatmentTime && treatmentTime < profileTime2) {
                if (treatmentTime + duration <= profileTime2) {
                    tmpBasals.add(new tmpBasal(value - (list.get(i).getValue() * duration / 60), duration, date));
                    return;
                } else {

                    long deltadur = profileTime2 - treatmentTime;
                    double deltaValue = value * deltadur / duration;

                    tmpBasals.add(new tmpBasal(deltaValue - (list.get(i).getValue() * deltadur / 60), deltadur, date));
                    maketmpBasal(value - deltaValue, duration - deltadur, new Date(date.getTime() + deltadur * 60000));
                    return;
                }
            }
        }

        //last case 24:00
        long profileTime = 24 * 60;

        if (treatmentTime + duration <= profileTime) {
            tmpBasals.add(new tmpBasal(value - (list.get(list.size() - 1).getValue() * duration / 60), duration, date));
        } else {
            long deltadur = profileTime - treatmentTime;
            double deltaValue = value * deltadur / duration;

            tmpBasals.add(new tmpBasal(deltaValue - (list.get(list.size() - 1).getValue() * deltadur / 60), deltadur, date));
            maketmpBasal(value - deltaValue, duration - deltadur, new Date(date.getTime() + deltadur * 60000));
        }
    }

}
