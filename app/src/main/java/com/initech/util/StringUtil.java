package com.initech.util;

import com.initech.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {

    // private static final Pattern emojiRegexp =
    private static final Pattern hashRegexp = Pattern.compile("(#\\w+)", 2);
    private static final Pattern mentionRegexp = Pattern.compile("(^|[^a-zA-Z0-9_]+)(@([a-zA-Z0-9_]+))", 2);
    private static final Pattern whiteSpacePattern = Pattern.compile("\\s+");

    /*
     * public static final SimpleDateFormat YEAR_FORMAT = new
     * SimpleDateFormat("yyyy"); public static final SimpleDateFormat
     * DATE_FORMAT = new SimpleDateFormat("MM-dd-yyyy"); public static final
     * SimpleDateFormat HOUR_FORMAT = new SimpleDateFormat("hh:mm:ss");
     */
    public static final SimpleDateFormat DATE_AND_HOUR_FORMAT_2 = new SimpleDateFormat("EEE, d MMM hh:mm:ss a");
    public static final SimpleDateFormat HOUR_FORMAT = new SimpleDateFormat("hh:mm aa");

    public static String getCleanText(final CharSequence s) {
        String trimmed = s.toString().trim();
        return whiteSpacePattern.matcher(trimmed).replaceAll(" ");
    }

    public static boolean isNullOrEmpty(String s) {
        boolean isNullOrEmpty;
        if (s != null && s.length() != 0) {
            isNullOrEmpty = false;
        } else {
            isNullOrEmpty = true;
        }

        return isNullOrEmpty;
    }

    public static Matcher mentionMatcher(final String s) {
        return mentionRegexp.matcher(s);
    }

    public static String stripNewLines(String s) {
        return s.replaceAll("\r\n", "");
    }

    public static boolean isDigits(final String str) {
        if (isEmpty(str)) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isEmpty(final String s) {
        return s == null || s.length() == 0;
    }

    public static boolean isNotEmpty(final String s) {
        return !isEmpty(s);
    }

    public static String notNull(final String str) {
        if (str == null) {
            return "";
        } else {
            return str;
        }
    }

    public static String alternateCase(final String str) {

        if (isEmpty(str)) {
            return str;
        }

        final StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            final Character c = str.charAt(i);

            if (i % 2 == 1) {
                sb.append(Character.toUpperCase(c));
            } else {
                sb.append(str.charAt(i));
            }
        }
        return sb.toString();
    }

    /**
     * Strips string after the given character including it
     *
     * @param str
     * @param stripChar
     * @return
     */
    public static String stripString(final String str, final char stripChar) {
        final int i = str.indexOf(stripChar);
        return i != -1 ? str.substring(0, i) : str;
    }

    /**
     * gets the last part of the url, so if http://www.aaa.com/blah/some.jpg it
     * should return some.jpg
     *
     * @param url
     * @return
     */
    public static String getFileFromUrl(final String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }

    /**
     * only returns the alphabetical characters in this string
     *
     * @param s
     * @return
     */
    public static String onlyAlpha(final String s) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (Character.isLetter(s.charAt(i))) {
                sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }

    public static String stripOutNumbers(final String str) {
        if (isEmpty(str)) {
            return str;
        }
        final StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            if (Character.isDigit(str.charAt(i))) {
                continue;
            } else {
                sb.append(str.charAt(i));
            }
        }
        return sb.toString();
    }

    public static boolean isValidUsername(final String username) {
        if (username == null || username.trim().length() <= Constants.MIN_USERNAME_LENGTH || username.trim().length() > Constants.MAX_USERNAME_LENGTH) {
            return false;
        }
        return username.matches("[A-Za-z0-9_]+");
    }

    public static boolean isValidPassword(final String password) {
        return password.trim().length() >= Constants.MIN_PASSWORD_LENGTH && password.trim().length() <= Constants.MAX_PASSWORD_LENGTH;
    }

    public static String getHour(final long time) {
        final String timeStr = StringUtil.HOUR_FORMAT.format(new Date(time));
        if (timeStr.charAt(0) == '0') {
            return timeStr.substring(1);
        } else {
            return timeStr;
        }
    }
}
