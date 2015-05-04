package io.pingpal.location;

import android.location.Location;

interface TrackerProtocol {

	/**
	 * Adds delegate to trackers list of delegates.
	 * @param delegate Delegate to be added
	 */
	void addDelegate(TrackerListenerProtocol delegate);

	/**
	 * Removes delegate from trackers list of delegates.
	 * @param delegate Delegate to be removed
	 */
	void removeDelegate(TrackerListenerProtocol delegate);

	void updateDelegatesWithLocation(Location location);

	void stopTracking();

	void startTracking();

}
