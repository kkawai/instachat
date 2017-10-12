package com.instachat.android.util;

import com.instachat.android.TheApp;
import com.instachat.android.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Time calculations utilities based on the handy Joda-Time java library
 * (included as a .jar)
 */
public final class TimeUtil {

    private static final String TAG = "TimeUtil";

    private TimeUtil() {
    }

    //Tue, 12 Jan 2016 13:00:06 +0000
    private static final SimpleDateFormat TOUR_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final SimpleDateFormat RSS_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss");

    public class MonthDay {

        public String mMonth;
        public String mDay;
    }

    public static Date getDateForTourDateString(final String date) throws ParseException {

        final String start = date.replace("T", " ");
        final int index = start.indexOf(".");
        if (index == -1) {
            return TOUR_DATE_FORMAT.parse(start);
        } else {
            return TOUR_DATE_FORMAT.parse(start.substring(0, index));
        }
    }

    private static final SimpleDateFormat NOW_POST_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static {
        NOW_POST_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public static String getTimeAgo(final long timeMillis) {
        // today
        Date today = new Date();

        // how much time since (ms)
        long duration = today.getTime() - timeMillis;

        int second = 1000;
        int minute = second * 60;
        int hour = minute * 60;
        int day = hour * 24;

        if (duration < (minute * 2)) {
            return TheApp.getInstance().getString(R.string.just_now);
            //return "less than 1 minute ago";
        }

        if (duration < hour) {
            int n = (int) Math.floor(duration / minute);
            return TheApp.getInstance().getString(R.string.some_minutes_ago, "" + n);
            //return n + " minutes ago";
        }

        if (duration < (hour * 2)) {
            return TheApp.getInstance().getString(R.string.one_hour_ago);
            //return "1 hour ago";
        }

        if (duration < day) {
            int n = (int) Math.floor(duration / hour);
            //return n + " hours ago";
            return TheApp.getInstance().getString(R.string.some_hours_ago, "" + n);
        }
        if (duration > day && duration < (day * 2)) {
            //return "1 day ago";
            return TheApp.getInstance().getString(R.string.one_day_ago);
        }

        int n = (int) Math.floor(duration / day);
        if (n < 365) {
            if (n > 30)
                //return (n / 30) + " month(s) ago";
                return TheApp.getInstance().getString(R.string.some_minutes_ago, "" + n);
            else
                //return n + " days ago";
                return TheApp.getInstance().getString(R.string.some_days_ago, "" + n);
        } else {
            //return ">1y";
            return TheApp.getInstance().getString(R.string.more_than_one_year_ago);
        }
    }

    public static String getTimeAgo(final String dateStr) {

        SimpleDateFormat dateFormat = RSS_DATE_FORMAT; //new SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZZZ yyyy");
        dateFormat.setLenient(true);
        Date created = null;
        try {
            created = dateFormat.parse(dateStr);
        } catch (Exception e) {
            return "";
        }

        return getTimeAgo(created.getTime());
    }

    /**
     * Returns age in years given birthdate in mm/dd/yyyy or yyyy-mm-dd format
     *
     * @param birthdate - mm/dd/yyyy or yyyy-mm-dd
     * @return - age or -1 if cannot parse given birthdate
     */
    public static int getYearsOld(final String birthdate) {
        try {
            SimpleDateFormat sdf = null;
            if (birthdate.charAt(2) == '/') {
                sdf = new SimpleDateFormat("mm/dd/yyyy");
            } else if (birthdate.charAt(2) == '-') {
                sdf = new SimpleDateFormat("mm-dd-yyyy");
            } else {
                sdf = new SimpleDateFormat("yyyy-mm-dd");
            }
            final Date bd = sdf.parse(birthdate);
            return (int) ((new Date().getTime() - bd.getTime()) / (1000 * 60 * 60 * 24)) / 365;

        } catch (final Exception e) {
            return -1;
        }
    }

    public static int getDaysOld(final String date) {
        try {
            final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            final Date d = formatter.parse(date);

            return (int) ((new Date().getTime() - d.getTime()) / (1000 * 60 * 60 * 24));
        } catch (Exception e) {
            return -1;
        }
    }

    public static String convertUTCtoLocalTime(String utcDateTime) {
        try {
            String lv_dateFormateInLocalTimeZone = "";//Will hold the final converted date
            Date lv_localDate = parseUTCTimeString(utcDateTime);
            SimpleDateFormat lv_formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
//			Log.d(TAG,"convertUTCtoLocalTime: input string " + utcDateTime);

            //Format UTC time
//			lv_formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
//			Log.d(TAG,"convertUTCtoLocalTime: The Date in the UTC time zone(UTC) " + lv_formatter.format(lv_localDate));

            //Convert the UTC date to Local timezone
            lv_formatter.setTimeZone(TimeZone.getDefault());
            lv_dateFormateInLocalTimeZone = lv_formatter.format(lv_localDate);
            MLog.d(TAG, "convertUTCtoLocalTime: The Date in the LocalTime Zone time zone " +
                    lv_formatter.format(lv_localDate));

            return lv_dateFormateInLocalTimeZone;
        } catch (Exception e) {
            return "";
        }
    }

    public static Date parseUTCTimeString(String utcDateTime) {
        //create a new Date object using the UTC timezone
        SimpleDateFormat lv_parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        lv_parser.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return lv_parser.parse(utcDateTime);
        } catch (Exception e) {
            MLog.e(TAG, "parseUTCTimeString() parse error on" + utcDateTime == null ? "<null>" :
                    utcDateTime);
            return null;
        }
    }

    public static String get12HourTimeString(String utcDateTime) {
        Date date = parseUTCTimeString(utcDateTime);
        return get12HourTimeString(date);
    }

    public static String get12HourTimeString(Date date) {
        if (date != null) {
            SimpleDateFormat format = new SimpleDateFormat("h:mma");
            format.setTimeZone(TimeZone.getDefault());
            return format.format(date);
        } else {
            return "";
        }
    }

}
