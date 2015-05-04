package io.pingpal.location;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

class Tracker implements TrackerProtocol {
	
	private static Tracker sharedTracker;
	private static List<TrackerListenerProtocol> delegates = new ArrayList<TrackerListenerProtocol>();
	private static boolean isTracking;
	private static LocationManager locationManager;
	private final int LOCATION_REFRESH_DISTANCE = 30, LOCATION_REFRESH_TIME = 10000;
	
	static void Setup (Context context){
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		
		if(sharedTracker == null){
			sharedTracker = new Tracker();
		}
	}
	
	static Tracker getSharedTracker(){

		return sharedTracker;
	}
	
	
	private final LocationListener mLocationListener = new LocationListener() {
	    @Override
		public void onLocationChanged(final Location location) {

	    	Log.i("LocationManager", "Got location");
			if (GeoUtil.isLocationOk(location)) {
				updateDelegatesWithLocation(location);
			}
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
			
		}
	};
	
	@Override
	public void addDelegate(TrackerListenerProtocol delegate) {
		delegates.add(delegate);
		
		if (delegates.size() == 1) {
			startTracking();
		}
	}

	@Override
	public void removeDelegate(TrackerListenerProtocol delegate) {
		
	    if (delegates.contains(delegate)) {
	        delegates.remove(delegate);
	    }
	    
		if (delegates.size() == 0) {
			stopTracking();
		}
	}

	boolean isTracking() {
		return isTracking;
	}
	
	void restartWithDistance(long distance) {

	    if (isTracking) {
	        stopTracking();
	    }
	    
	    locationManager.requestLocationUpdates(locationManager.getBestProvider(new Criteria(), true), LOCATION_REFRESH_TIME, distance, mLocationListener);
		isTracking = true;
	}
	 
	@Override
	public void updateDelegatesWithLocation(Location location) {

		for (int i = 0; i < delegates.size(); i++) {
			delegates.get(i).setLocation(location);
		}
	}

	@Override
	public void stopTracking() {
		Log.i("LocationManager", "Stop tracking");
		locationManager.removeUpdates(mLocationListener);
		isTracking = false;
	}
	
	public Location getLastKnownLocation(){
		return locationManager.getLastKnownLocation(locationManager.getBestProvider(new Criteria(), false));
	}

	@Override
	public void startTracking() {
		Log.i("LocationManager", "Start tracking");

		/*if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
			
		}
		else {
			Log.e("LocationManager", "No provider is enabled.");
		}*/
		
		locationManager.requestLocationUpdates(locationManager.getBestProvider(new Criteria(), false), LOCATION_REFRESH_TIME,
				LOCATION_REFRESH_DISTANCE, mLocationListener);
		
		isTracking = true;
	}
}
