package io.pingpal.location;

import android.location.Location;

class GeoUtil {

	private final static int BAD = 500;
	@SuppressWarnings("unused")
	private final static int AGE = (1 * 60);

	static boolean isLocationOk(Location location) {

		// return location && -[location.timestamp timeIntervalSinceNow] < AGE && location.horizontalAccuracy < BAD;
		return location != null && location.getAccuracy() < BAD;
	}
}
