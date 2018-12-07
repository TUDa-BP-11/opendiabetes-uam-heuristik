package de.opendiabetes.parser;

import de.opendiabetes.vault.engine.container.VaultEntry;
import de.opendiabetes.vault.engine.container.VaultEntryType;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import java.util.List;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Main {
    private static final long ONE_HOUR = 3600000;
    public static void main(String[] args){
//        VaultEntryParser parser = new VaultEntryParser();

        
        VaultEntryType entryType = VaultEntryType.GLUCOSE_CGM;
        String s = "2017-10-29T01:51:22.000Z";
        long e = 1509241882000L;
        Date date = makeDate(e,s);
        VaultEntry ve = new VaultEntry(entryType,date,2.0);
            System.out.println(ve.toString());
        
        s = "2017-10-29T03:01:22.000Z";
        e = 1509246082000L;
        date = makeDate(e,s);
        
        ve = new VaultEntry(entryType,date,2.0);
            System.out.println(ve.toString());
//        String path = "/home/anna/Daten/Uni/14. Semester/BP/Dataset_Small/00390014/direct-sharing-31/entries_2017-07-10_to_2017-11-08.json";
//        List<VaultEntry> entries = parser.parseFile(path);
//        System.out.println("Entries:");
//        for (VaultEntry entry : entries){
//            System.out.println(entry.toString());
//        }
//
//        String path2 = "/home/anna/Daten/Uni/14. Semester/BP/Dataset_Small/00390014/direct-sharing-31/treatments_2017-07-10_to_2017-11-08.json";
//        List<VaultEntry> treatments = parser.parseFile(path2);
//    //    System.out.println("Treatments:");
//        for (VaultEntry entry : treatments){
//            System.out.println(entry.toString());
//        }
//
//        System.out.println("Timezone: GMT "+parser.getTimezone());
    }
    private static Date makeDate(long epoch, String dateString){
        Date date = new Date(epoch);
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Date tmp = new Date(0);
        try{
            tmp = formatter.parse(dateString);

        }catch (ParseException e){
            System.out.println(e.getMessage());

        }
        TimeZone localeTimezone = TimeZone.getDefault();
        long offset = 0;
        if (localeTimezone.inDaylightTime(tmp))
                offset = ONE_HOUR;
        long timezone = (localeTimezone.getRawOffset() + offset - date.getTime()  + tmp.getTime())/ONE_HOUR;
        System.out.println("Local Timezone: "+localeTimezone.getRawOffset()/ONE_HOUR);
        System.out.println("DST: "+offset/ONE_HOUR);
        System.out.println("Timezone: "+timezone);
        
        DateFormat formatter2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS ZZZZ");
        String[] ids = TimeZone.getAvailableIDs((int)(timezone*ONE_HOUR));
        TimeZone tz = TimeZone.getTimeZone(ids[0]);
        System.out.println(formatter2.format(date) + " " + formatter2.getTimeZone().getDisplayName());
        System.out.println(formatter2.format(tmp) + " " + formatter2.getTimeZone().getDisplayName());
        formatter2.setTimeZone(tz);
        
        System.out.println(formatter2.format(date) + " " + formatter2.getTimeZone().getDisplayName());
        System.out.println(formatter2.format(tmp) + " " + formatter2.getTimeZone().getDisplayName());
        return date;
    }
       
    
}
