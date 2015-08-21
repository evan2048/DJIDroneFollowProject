package com.evan2048.util;

import java.text.DecimalFormat;
import java.util.StringTokenizer;

//from android source code:android4.4.2\442\frameworks\base\location\java\android\location\Location.java

public class LocationTools {
    /**
     * Constant used to specify formatting of a latitude or longitude
     * in the form "[+-]DDD.DDDDD where D indicates degrees.
     */
    public static final int FORMAT_DEGREES = 0;

    /**
     * Constant used to specify formatting of a latitude or longitude
     * in the form "[+-]DDD:MM.MMMMM" where D indicates degrees and
     * M indicates minutes of arc (1 minute = 1/60th of a degree).
     */
    public static final int FORMAT_MINUTES = 1;

    /**
     * Constant used to specify formatting of a latitude or longitude
     * in the form "DDD:MM:SS.SSSSS" where D indicates degrees, M
     * indicates minutes of arc, and S indicates seconds of arc (1
     * minute = 1/60th of a degree, 1 second = 1/3600th of a degree).
     */
    public static final int FORMAT_SECONDS = 2;
	
    /**
     * Converts a coordinate to a String representation. The outputType
     * may be one of FORMAT_DEGREES, FORMAT_MINUTES, or FORMAT_SECONDS.
     * The coordinate must be a valid double between -180.0 and 180.0.
     *
     * @throws IllegalArgumentException if coordinate is less than
     * -180.0, greater than 180.0, or is not a number.
     * @throws IllegalArgumentException if outputType is not one of
     * FORMAT_DEGREES, FORMAT_MINUTES, or FORMAT_SECONDS.
     */
    public static String convert(double coordinate, int outputType) {
        if (coordinate < -180.0 || coordinate > 180.0 ||
            Double.isNaN(coordinate)) {
            throw new IllegalArgumentException("coordinate=" + coordinate);
        }
        if ((outputType != FORMAT_DEGREES) &&
            (outputType != FORMAT_MINUTES) &&
            (outputType != FORMAT_SECONDS)) {
            throw new IllegalArgumentException("outputType=" + outputType);
        }

        StringBuilder sb = new StringBuilder();

        // Handle negative values
        if (coordinate < 0) {
            sb.append('-');
            coordinate = -coordinate;
        }

        DecimalFormat df = new DecimalFormat("###.#####");
        if (outputType == FORMAT_MINUTES || outputType == FORMAT_SECONDS) {
            int degrees = (int) Math.floor(coordinate);
            sb.append(degrees);
            sb.append(':');
            coordinate -= degrees;
            coordinate *= 60.0;
            if (outputType == FORMAT_SECONDS) {
                int minutes = (int) Math.floor(coordinate);
                sb.append(minutes);
                sb.append(':');
                coordinate -= minutes;
                coordinate *= 60.0;
            }
        }
        sb.append(df.format(coordinate));
        return sb.toString();
    }

    /**
     * Converts a String in one of the formats described by
     * FORMAT_DEGREES, FORMAT_MINUTES, or FORMAT_SECONDS into a
     * double.
     *
     * @throws NullPointerException if coordinate is null
     * @throws IllegalArgumentException if the coordinate is not
     * in one of the valid formats.
     */
    public static double convert(String coordinate) {
        // IllegalArgumentException if bad syntax
        if (coordinate == null) {
            throw new NullPointerException("coordinate");
        }

        boolean negative = false;
        if (coordinate.charAt(0) == '-') {
            coordinate = coordinate.substring(1);
            negative = true;
        }

        StringTokenizer st = new StringTokenizer(coordinate, ":");
        int tokens = st.countTokens();
        if (tokens < 1) {
            throw new IllegalArgumentException("coordinate=" + coordinate);
        }
        try {
            String degrees = st.nextToken();
            double val;
            if (tokens == 1) {
                val = Double.parseDouble(degrees);
                return negative ? -val : val;
            }

            String minutes = st.nextToken();
            int deg = Integer.parseInt(degrees);
            double min;
            double sec = 0.0;

            if (st.hasMoreTokens()) {
                min = Integer.parseInt(minutes);
                String seconds = st.nextToken();
                sec = Double.parseDouble(seconds);
            } else {
                min = Double.parseDouble(minutes);
            }

            boolean isNegative180 = negative && (deg == 180) &&
                (min == 0) && (sec == 0);

            // deg must be in [0, 179] except for the case of -180 degrees
            if ((deg < 0.0) || (deg > 179 && !isNegative180)) {
                throw new IllegalArgumentException("coordinate=" + coordinate);
            }
            if (min < 0 || min > 59) {
                throw new IllegalArgumentException("coordinate=" +
                        coordinate);
            }
            if (sec < 0 || sec > 59) {
                throw new IllegalArgumentException("coordinate=" +
                        coordinate);
            }

            val = deg*3600.0 + min*60.0 + sec;
            val /= 3600.0;
            return negative ? -val : val;
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("coordinate=" + coordinate);
        }
    }

	/*
	 * Android doc:
	   The computed distance is stored in results[0].if results has length 2 or greater,the initial bearing
	   is stored in result[1].if results has length 3 or greater,the final bearing is stored in results[2].
	 */
	//bearing:north-east-south(return 0~180);south-west-north(return -180~0)
    private static void computeDistanceAndBearing(double lat1, double lon1,
        double lat2, double lon2, float[] results) {
        // Based on http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf
        // using the "Inverse Formula" (section 4)

        int MAXITERS = 20;
        // Convert lat/long to radians
        lat1 *= Math.PI / 180.0;
        lat2 *= Math.PI / 180.0;
        lon1 *= Math.PI / 180.0;
        lon2 *= Math.PI / 180.0;

        double a = 6378137.0; // WGS84 major axis
        double b = 6356752.3142; // WGS84 semi-major axis
        double f = (a - b) / a;
        double aSqMinusBSqOverBSq = (a * a - b * b) / (b * b);

        double L = lon2 - lon1;
        double A = 0.0;
        double U1 = Math.atan((1.0 - f) * Math.tan(lat1));
        double U2 = Math.atan((1.0 - f) * Math.tan(lat2));

        double cosU1 = Math.cos(U1);
        double cosU2 = Math.cos(U2);
        double sinU1 = Math.sin(U1);
        double sinU2 = Math.sin(U2);
        double cosU1cosU2 = cosU1 * cosU2;
        double sinU1sinU2 = sinU1 * sinU2;

        double sigma = 0.0;
        double deltaSigma = 0.0;
        double cosSqAlpha = 0.0;
        double cos2SM = 0.0;
        double cosSigma = 0.0;
        double sinSigma = 0.0;
        double cosLambda = 0.0;
        double sinLambda = 0.0;

        double lambda = L; // initial guess
        for (int iter = 0; iter < MAXITERS; iter++) {
            double lambdaOrig = lambda;
            cosLambda = Math.cos(lambda);
            sinLambda = Math.sin(lambda);
            double t1 = cosU2 * sinLambda;
            double t2 = cosU1 * sinU2 - sinU1 * cosU2 * cosLambda;
            double sinSqSigma = t1 * t1 + t2 * t2; // (14)
            sinSigma = Math.sqrt(sinSqSigma);
            cosSigma = sinU1sinU2 + cosU1cosU2 * cosLambda; // (15)
            sigma = Math.atan2(sinSigma, cosSigma); // (16)
            double sinAlpha = (sinSigma == 0) ? 0.0 :
                cosU1cosU2 * sinLambda / sinSigma; // (17)
            cosSqAlpha = 1.0 - sinAlpha * sinAlpha;
            cos2SM = (cosSqAlpha == 0) ? 0.0 :
                cosSigma - 2.0 * sinU1sinU2 / cosSqAlpha; // (18)

            double uSquared = cosSqAlpha * aSqMinusBSqOverBSq; // defn
            A = 1 + (uSquared / 16384.0) * // (3)
                (4096.0 + uSquared *
                 (-768 + uSquared * (320.0 - 175.0 * uSquared)));
            double B = (uSquared / 1024.0) * // (4)
                (256.0 + uSquared *
                 (-128.0 + uSquared * (74.0 - 47.0 * uSquared)));
            double C = (f / 16.0) *
                cosSqAlpha *
                (4.0 + f * (4.0 - 3.0 * cosSqAlpha)); // (10)
            double cos2SMSq = cos2SM * cos2SM;
            deltaSigma = B * sinSigma * // (6)
                (cos2SM + (B / 4.0) *
                 (cosSigma * (-1.0 + 2.0 * cos2SMSq) -
                  (B / 6.0) * cos2SM *
                  (-3.0 + 4.0 * sinSigma * sinSigma) *
                  (-3.0 + 4.0 * cos2SMSq)));

            lambda = L +
                (1.0 - C) * f * sinAlpha *
                (sigma + C * sinSigma *
                 (cos2SM + C * cosSigma *
                  (-1.0 + 2.0 * cos2SM * cos2SM))); // (11)

            double delta = (lambda - lambdaOrig) / lambda;
            if (Math.abs(delta) < 1.0e-12) {
                break;
            }
        }

        float distance = (float) (b * A * (sigma - deltaSigma));
        results[0] = distance;
        if (results.length > 1) {
            float initialBearing = (float) Math.atan2(cosU2 * sinLambda,
                cosU1 * sinU2 - sinU1 * cosU2 * cosLambda);
            initialBearing *= 180.0 / Math.PI;
            results[1] = initialBearing;
            if (results.length > 2) {
                float finalBearing = (float) Math.atan2(cosU1 * sinLambda,
                    -sinU1 * cosU2 + cosU1 * sinU2 * cosLambda);
                finalBearing *= 180.0 / Math.PI;
                results[2] = finalBearing;
            }
        }
    }

    /**
     * Computes the approximate distance in meters between two
     * locations, and optionally the initial and final bearings of the
     * shortest path between them.  Distance and bearing are defined using the
     * WGS84 ellipsoid.
     *
     * <p> The computed distance is stored in results[0].  If results has length
     * 2 or greater, the initial bearing is stored in results[1]. If results has
     * length 3 or greater, the final bearing is stored in results[2].
     *
     * @param startLatitude the starting latitude
     * @param startLongitude the starting longitude
     * @param endLatitude the ending latitude
     * @param endLongitude the ending longitude
     * @param results an array of floats to hold the results
     *
     * @throws IllegalArgumentException if results is null or has length < 1
     */
    public static void distanceBetween(double startLatitude, double startLongitude,
        double endLatitude, double endLongitude, float[] results) {
        if (results == null || results.length < 1) {
            throw new IllegalArgumentException("results is null or has length < 1");
        }
        computeDistanceAndBearing(startLatitude, startLongitude,
            endLatitude, endLongitude, results);
    }
}
