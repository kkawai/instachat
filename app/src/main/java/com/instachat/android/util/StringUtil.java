package com.instachat.android.util;

import com.instachat.android.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {

    // private static final Pattern emojiRegexp =
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
        return s.replaceAll("\r\n", " ").replaceAll("\n", " ");
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

    public static boolean isValidEmail(final CharSequence target) {
        if (target.length() < Constants.MIN_EMAIL_LENGTH || target.length() > Constants.MAX_EMAIL_LENGTH) {
            return false;
        } else {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
        }
    }

    public static boolean isValidUsername(final String username) {
        if (username == null || username.trim().length() <= Constants.MIN_USERNAME_LENGTH || username.trim().length() > Constants.MAX_USERNAME_LENGTH) {
            return false;
        }
        return username.matches("[A-Za-z0-9._-]+");
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

    /**
     * Strips surrounding single quotes from a string greater than
     * 2 length.  Also, converts any sequences of 2 quotes to 1.  This method
     * is the converse of android.database.DatabaseUtils.sqlEscapeString().
     * <p>
     * Example: ''the fox''s tail was red'' -> 'the fox's tail was red'
     * 'the fox''s tail was red' -> the fox's tail was red
     * stacy''s hair is green -> stacy's hair is green
     * stacy's hair is green -> stacy's hair is green
     * my car is blue -> my car is blue
     *
     * @param s
     * @return
     */
    public static String unescapeQuotes(String s) {
        if (s == null)
            return s;
        s = s.replace("''", "'");
        if (s.startsWith("'") && s.endsWith("'") && s.length() > 2) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    /**
     * Attempts to 'correct' a given url.  If the url starts with Http:// instead of
     * http:// or Https://, it will return http:// and https:// respectively
     * <p>
     * The android VIEW intent on any such url can cause a crash since it won't be able to
     * find a matching activity since their mapping is case sensitive.
     *
     * @param url
     * @return
     */
    public static String correctUrl(final String url) {
        try {
            String s = url.substring(0, 8).toLowerCase();
            if (s.startsWith("http://")) {
                return "http://" + url.substring(7);
            } else if (s.startsWith("https://")) {
                return "http://" + url.substring(8);
            } else {
                return url;
            }
        } catch (Exception e) {
            return url;
        }
    }
}
