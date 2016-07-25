package com.initech.util;


public final class DistanceUtil {
	
	public static final char UNIT_MILES = 'M', UNIT_KILOMETERS = 'K';
	
	/**
	 * Returns a floating point number for miles
	 * @param fromLatitude
	 * @param fromLongitude
	 * @param toLatitude
	 * @param toLongitude
	 * @return
	 * @throws Exception
	 */
	public static double getDistance(final double fromLatitude, final double fromLongitude, 
			final double toLatitude, final double toLongitude, 
			final char unit) throws Exception {
		return distance(Double.valueOf(fromLatitude), Double.valueOf(fromLongitude),
				Double.valueOf(toLatitude), Double.valueOf(toLongitude), unit);
	}	
	
	/**
	 * Returns a floating point number for miles
	 * @param fromLatitude
	 * @param fromLongitude
	 * @param toLatitude
	 * @param toLongitude
	 * @return
	 * @throws Exception
	 */
	public static double getDistance(final double fromLatitude, final double fromLongitude,
									 final String toLatitude, final String toLongitude,
									 final char unit) throws Exception {
		return distance(fromLatitude, fromLongitude,
				Double.valueOf(toLatitude), Double.valueOf(toLongitude), unit);
	}	
	
	private static double distance(final double lat1, final double lon1, final double lat2,
			final double lon2, final char unit) {
		final double theta = lon1 - lon2;
		double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2))
				+ Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2))
				* Math.cos(deg2rad(theta));
		dist = Math.acos(dist);
		dist = rad2deg(dist);
		dist = dist * 60 * 1.1515;
		if (unit == UNIT_KILOMETERS) {
			dist = dist * 1.609344;
		} else if (unit == UNIT_MILES) {
			dist = dist * 0.8684;
		}
		return (dist);
	}

	/**
	 * Reports human-friendly distance in terms of Miles
	 * @param distance
	 * @return
	 */
	public static String format(final double distance) {

		if (distance < 1.0d) {
			final String dis = (""+distance).substring(0,3) + " miles";
			return dis.charAt(2) == '0' ? "0.1 miles" : dis;
		} else if (distance >= 1.0d && distance < 10.0d) {
			final String dis = (""+distance).substring(0,3);
			if (dis.startsWith("1.0")) {
				return "1 mile";
			}
			if (dis.contains(".0")) {
				return dis.charAt(0) + " miles";
			}
			return dis + " miles";
		} else if (distance < 100d) {
			final String dis = ""+distance;
			final int idx = dis.indexOf('.');
			if (dis.charAt(idx+1) == '0') {
				return (int)distance+" miles";
			}
			return dis.substring(0,idx+2) + " miles";
		} else {
			return (int)distance + " miles";
		}
	}
	
	private static double deg2rad(final double deg) {
		return (deg * Math.PI / 180.0);
	}

	private static double rad2deg(final double rad) {
		return (rad / Math.PI * 180.0);
	}		
}
