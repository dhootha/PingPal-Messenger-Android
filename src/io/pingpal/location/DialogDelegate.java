package io.pingpal.location;

import io.pingpal.outbox.Outbox;

import java.util.HashMap;
import java.util.Map;

import android.location.Location;
import android.os.Handler;
import android.util.Log;

class DialogDelegate implements TrackerListenerProtocol {
	
	private Tracker tracker;
	Location currentLocation;
	private double accuracyMeters;
	private double timeoutSeconds;
	private String sendTo;
	private String sendSeq;
	private Handler handler;
	private Runnable runnable;
	
	DialogDelegate(String from, String seq, double accurMeters, double durationSeconds) {
        sendTo = from;
        sendSeq = seq;
        
        if (accurMeters > 0) {
            accuracyMeters = accurMeters;
        }else{
            accuracyMeters = 100;
        }
        
        if (durationSeconds > 0) {
            timeoutSeconds = durationSeconds;
        }else{
            timeoutSeconds = 30;
        }
        
        tracker = Tracker.getSharedTracker();
        tracker.addDelegate(this);
        
        runnable = new Runnable() {
			
			@Override
			public void run() {

	        	  timeout();
			}
		};
        
        handler = new Handler();    
        handler.postDelayed(runnable, (long) (timeoutSeconds * 1000));
        
	}

	void stop() {
		handler.removeCallbacks(runnable);
        tracker = Tracker.getSharedTracker();
        tracker.removeDelegate(this);
	}

	void timeout() {
        Log.i("LocationManager", "PositionDialog timedout.");
	    
	    if (GeoUtil.isLocationOk(currentLocation)) {
	        sendLocation();
	    }
	    
	    else{
	    
	        Log.i("LocationManager", "PositionDialog timedout. No location could be obtained.");
	    }
	    
	    stop();
	}

	void sendLocation() {
		
		double latitude = currentLocation.getLatitude();
		double longitude = currentLocation.getLongitude();
		double altitude = currentLocation.getAltitude();
		float accuracy = currentLocation.getAccuracy();
		float course = currentLocation.getBearing();
		float speed = currentLocation.getSpeed();
		
		Map<String, Object> locationData = new HashMap<String, Object>();
		locationData.put("latitude", latitude);
		locationData.put("longitude", longitude);
		locationData.put("altitude", altitude);
		locationData.put("horizontalAccuracy", accuracy);
		locationData.put("verticalAccuracy", accuracy);
		locationData.put("course", course);
		locationData.put("speed", speed);
		
		long timeStamp = System.currentTimeMillis() / 1000l;
		locationData.put("timestamp", timeStamp);

		Map<String, Object> payload = new HashMap<String, Object>();
		payload.put("location", locationData);
		Map<String, Object> options = new HashMap<String, Object>();
		options.put("yourseq", sendSeq);

		Log.i("LocationManager", "Sending location.");

		Outbox.put(sendTo, payload, options);
	}

	@Override
	public void setLocation(Location location) {
		if (currentLocation == null) {
			currentLocation = location;

		} else if (location.getAccuracy() < currentLocation.getAccuracy()) {
			currentLocation = location;
		}
		
		Log.i("LocationManager", "Accuracy is: " + location.getAccuracy());

		if (currentLocation.getAccuracy() <= accuracyMeters) {
			sendLocation();
			stop();
		}
	}

}
