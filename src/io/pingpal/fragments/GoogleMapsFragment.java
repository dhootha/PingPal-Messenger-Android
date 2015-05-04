package io.pingpal.fragments;

import io.pingpal.messenger.R;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * @author Robin Dahlström 22-03-2015
 */
public class GoogleMapsFragment extends Fragment {

	private Location location;
	private GoogleMap map;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        setHasOptionsMenu(true);
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_map, container, false);
        map =((SupportMapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        
    	MapsInitializer.initialize(getActivity());
    	
    	if(location != null){     	
            LatLng position = new LatLng(location.getLatitude(), location.getLongitude());

            MarkerOptions marker = new MarkerOptions().position(position);

            marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.ping_marker));
            
            map.addMarker(marker);
            
            // Move the camera to location (instantly)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
    	}

        return rootView;
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	super.onCreateOptionsMenu(menu, inflater);

    	inflater.inflate(R.menu.map_options, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

       switch(item.getItemId()){
       case R.id.normal:
    	   map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    	   return true;
       case R.id.satellite:
    	   map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
    	   return true;
       case R.id.terrain:
    	   map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
    	   return true;
       case R.id.hybrid:
    	   map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
    	   return true;
       }  
       
       return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onPrepareOptionsMenu(Menu menu) {

    	MenuItem item = menu.findItem(R.id.normal);
        MenuItem item2 = menu.findItem(R.id.satellite);
        MenuItem item3 = menu.findItem(R.id.terrain);
        MenuItem item4 = menu.findItem(R.id.hybrid);
        
        if(item != null){
            item.setVisible(true);
            item2.setVisible(true);
            item3.setVisible(true);
            item4.setVisible(true);
        
            if(map != null){
    	        switch(map.getMapType()){
    	        case GoogleMap.MAP_TYPE_NORMAL:
    	        	item.setVisible(false);
    	     	   break;
    	        case GoogleMap.MAP_TYPE_SATELLITE:
    	        	item2.setVisible(false);
    	     	   break;
    	        case GoogleMap.MAP_TYPE_TERRAIN:
    	        	item3.setVisible(false);
    	     	  break;
    	        case GoogleMap.MAP_TYPE_HYBRID:
    	        	item4.setVisible(false);
    	     	  break;
    	        }
            }
            else{
            	item.setVisible(false);
            }
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        SupportMapFragment smf = (SupportMapFragment) getFragmentManager().findFragmentById(R.id.map);
        if (smf != null){
            //getFragmentManager().beginTransaction().remove(smf).commit();
        }

    }
    
    public void setLocation(Location location){
        
    	this.location = location;	
    }

}
