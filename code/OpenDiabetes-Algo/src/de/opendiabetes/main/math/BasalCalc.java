package de.opendiabetes.main.math;

import de.opendiabetes.main.algo.TempBasal;
import de.opendiabetes.parser.Profile;
import de.opendiabetes.vault.engine.container.VaultEntry;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class BasalCalc {

    private final Profile profile;
    private List<TempBasal> tempBasals;

    public BasalCalc(Profile profile) {
        this.profile = profile;
    }

    public List<TempBasal> calculateBasal(List<VaultEntry> basalTreatments) {
        tempBasals = new ArrayList<>();
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
        return tempBasals;
    }

    private void maketmpBasal(double value, long duration, Date date) {
        if (duration <= 0) {
            return;
        }

        List<Profile.BasalProfile> list = profile.getBasalProfiles();
        long treatmentTime = (date.getTime() / 60000) % (60 * 24); //Time in min from 00:00
        
        Profile.BasalProfile next;
        Iterator<Profile.BasalProfile> it = list.iterator();
        Profile.BasalProfile prev = it.next();
        while (it.hasNext()){
            next = it.next();
//        for (int i = 0; i < list.size() - 1; i++) {
//            prev = list.get(i);
//            next = list.get(i + 1);
            long profileTime1 = prev.getStart().getHour() * 60 + prev.getStart().getMinute(); //Time in min from 00:00
            long profileTime2 = next.getStart().getHour() * 60 + next.getStart().getMinute();

            if (profileTime1 <= treatmentTime && treatmentTime < profileTime2) {
                if (treatmentTime + duration <= profileTime2) {
                    tempBasals.add(new TempBasal(value - (prev.getValue() * duration / 60), duration, date));
                    return;
                } else {
                    long deltadur = profileTime2 - treatmentTime;
                    double deltaValue = value * deltadur / duration;

                    tempBasals.add(new TempBasal(deltaValue - (prev.getValue() * deltadur / 60), deltadur, date));
                    maketmpBasal(value - deltaValue, duration - deltadur, new Date(date.getTime() + deltadur * 60000));
                    return;
                }
            }
            prev = next;
        }

        //last case 24:00
        long profileTime = 24 * 60;

        if (treatmentTime + duration <= profileTime) {
            tempBasals.add(new TempBasal(value - (prev.getValue() * duration / 60), duration, date));
        } else {
            long deltadur = profileTime - treatmentTime;
            double deltaValue = value * deltadur / duration;

            tempBasals.add(new TempBasal(deltaValue - (prev.getValue() * deltadur / 60), deltadur, date));
            maketmpBasal(value - deltaValue, duration - deltadur, new Date(date.getTime() + deltadur * 60000));
        }
    }

}
