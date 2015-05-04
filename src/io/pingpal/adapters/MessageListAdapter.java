
package io.pingpal.adapters;

import io.pingpal.database.DatabaseHelper;
import io.pingpal.fragments.FacebookFragment;
import io.pingpal.messenger.MainActivity;
import io.pingpal.messenger.R;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

/**
 * This class is responsible for inflating messages from a conversation into
 * views and adding them to the chat_listview in the MessagesFragment.
 *
 * @author Paul Mallon & Robin Dahlström 23-07-14
 */
public class MessageListAdapter extends SimpleCursorAdapter {

    @SuppressWarnings("unused")
	private static final String TAG = MessageListAdapter.class.getSimpleName();

    private LayoutInflater mInflater;

    private DisplayImageOptions mImageOptions, mImageOptions2;

    private Cursor mCursor;
    
    private Context mContext;

    private ImageLoader mImageLoader;
    
    /*
     * Keeps track of colors of the senders in a group chat
     */
    private Map<String, Integer> senderColors  = new HashMap<String, Integer>();

    public MessageListAdapter(Context context, Cursor cursor) {
        super(context, 0, null, null, null, 0);
        mContext = context;

        this.mCursor = cursor;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mImageOptions = new DisplayImageOptions.Builder().cacheInMemory(true).cacheOnDisk(true)
                .displayer(new RoundedBitmapDisplayer(90)).build();
        mImageOptions2 = new DisplayImageOptions.Builder().cacheInMemory(true).cacheOnDisk(true)
                .build();
        mImageLoader = ImageLoader.getInstance();

    }

    public MessageListAdapter(Context context) {
        super(context, 0, null, null, null, 0);
        
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mImageLoader = ImageLoader.getInstance();

    }

    @Override
    public int getCount() {

        return mCursor.getCount();
    }

    @Override
    public String getItem(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getString(mCursor.getColumnIndex(DatabaseHelper.COLUMN_MESSAGE));
    }

    @Override
    public long getItemId(int position) {

        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder = null;
        View messageView = convertView;

        if (messageView == null) {
        	View chatView = mInflater.inflate(R.layout.fragment_conversation, parent, false);
            holder = new ViewHolder();
            holder.msgListView = (ListView)chatView.findViewById(R.id.messages_listview);
            holder.message = (TextView)holder.msgListView.findViewById(R.id.message_hint_textview);
            holder.icon = (ImageView)holder.msgListView.findViewById(R.id.icon);
            holder.miniMap = (ImageView)holder.msgListView.findViewById(R.id.miniMap);
            
        } else {
            holder = (ViewHolder)messageView.getTag();
        }

        messageView = buildMessageView(mInflater, holder.msgListView, position);
        messageView.setTag(holder);
        return messageView;
    }

    private View buildMessageView(LayoutInflater inflater, ListView listView, int position) {
        View messageView;
        mCursor.moveToPosition(position);
        int layout, idTextView, idImageView;
        
        String senderTag = mCursor.getString(mCursor.getColumnIndex(DatabaseHelper.COLUMN_SENDER_TAG));

        int colorResource;
        String userTag = ((MainActivity)mContext).getUserTag();
        
        if (senderTag.equals(userTag)) {
        	colorResource = R.drawable.rounded_message_sent;
        	layout = R.layout.message_sent_layout;
        	idTextView = R.id.sent_textview;
        	idImageView = R.id.sent_imageview;

        } else {
        	colorResource = selectSenderColor(senderTag);
        	layout = R.layout.message_received_layout;
        	idTextView = R.id.received_textview;
        	idImageView = R.id.received_imageview;
        }
	
        messageView = inflater.inflate(layout, listView, false);
        TextView textView = (TextView)messageView.findViewById(idTextView);
        ImageView imageView = (ImageView)messageView.findViewById(idImageView);       
        
        textView.setBackgroundResource(colorResource);

        textView.setText(mCursor.getString(mCursor
                .getColumnIndex(DatabaseHelper.COLUMN_MESSAGE)));
               
        checkIfEmoticon(textView, layout);
        checkIfLocationData(textView, messageView);
        
        String senderID = senderTag.replace("#", "");
        
        mImageLoader.displayImage(
                FacebookFragment.IMG_URL_START + senderID + FacebookFragment.IMG_URL_END,
                imageView, mImageOptions);
        
        return messageView;
    }
    
    /**
     * Determines the color of the senders message
     */
    private int selectSenderColor(String senderID){

		if (!senderColors.containsKey(senderID)) {
			
			int nbr = senderColors.size();
			int background;
			
	        TypedArray typedArr = ((Activity)mContext).getResources().obtainTypedArray(R.array.group_colors);
	        
			background = typedArr.getResourceId(nbr, R.drawable.rounded_message_received);
			typedArr.recycle();
			
			senderColors.put(senderID, background);
		}
		
		return senderColors.get(senderID);
    }
    
    /**
     * Checks if this message is actually location data and if so, creates a map 
     */
    private void checkIfLocationData(TextView textView, View messageView){
    	String text = textView.getText().toString();
    	
    	if(text.startsWith("@locData:")){
    		String [] locationData = text.split(":");
    		
    		if(locationData.length != 7){
    			return;
    		}
    		
    		final String latitude = locationData[1];
    		final String longitude = locationData[2];
    		final String altitude = locationData[3];
    		final String accuracy = locationData[4];
    		final String speed = locationData[5];
    		final String course = locationData[6];

    		ImageView mapView = (ImageView)messageView.findViewById(R.id.miniMap);
    		TextView textView2 = (TextView)messageView.findViewById(R.id.received_textview);
    		
    		String penquinMarker = "-Link to your small map marker icon here-";
    		String staticMap = "https://maps.googleapis.com/maps/api/staticmap?center=" + latitude + "," + longitude + "&zoom=14&size=900x350&markers=icon:" + penquinMarker + "|" + latitude +"," + longitude + "&key=" + mContext.getString(R.string.maps_key);

    		mapView.setVisibility(View.VISIBLE);
    		mapView.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					
					Location location = new Location(LocationManager.GPS_PROVIDER);
					location.setLatitude(Double.parseDouble(latitude));
					location.setLongitude(Double.parseDouble(longitude));
					location.setAltitude(Double.parseDouble(altitude));
					location.setBearing(Float.parseFloat(course));
					location.setSpeed(Float.parseFloat(speed));
					location.setAccuracy(Float.parseFloat(accuracy));
					
					((MainActivity)mContext).onMapSelected(location);
				}
			});
    		textView2.setVisibility(View.INVISIBLE);

            mImageLoader.displayImage(staticMap,
                    mapView,mImageOptions2);  
    	}

    }

    /**
     * 
     * @param textView
     * Checks if this message is actually an emoticon and if so, adds the emoticon to the textview.
     */
	private void checkIfEmoticon(TextView textView, int layout) {
		String [] emoticonName = 
        	{"Ping_angry_h100.png", "Ping_happy_h100.png", "Ping_sad_h100.png",
        		"Ping_scared_h100.png" ,"Ping_sleepy_h100.png", "Ping_OMG_h100.png", 
        		"Ping_WTF_h100.png", "Ping_thumbs_up_h100.png", "Ping_blush_h100.png", 
        		"Ping_awkward_h100.png", "Ping_h100.png"};

        for(int i = 0; i < emoticonName.length; i++){
        	
            if(textView.getText().toString().equals(emoticonName[i])){	
            	//get the name for the emoticon in drawable folder (android image names are lowercase)
            	String iconName = emoticonName[i].toLowerCase(Locale.getDefault()).replace(".png", "");
            	
            	//get resource id for the selected emoticon
            	int iconId = mContext.getApplicationContext().getResources().getIdentifier(iconName, "drawable", mContext.getPackageName());
            	
            	//put image in textview
            	textView.setText("");
            	textView.setTextSize(0);
            	textView.setBackgroundColor(Color.WHITE);
            	
            	
            	DisplayMetrics dm = new DisplayMetrics();
            	  WindowManager windowManager = (WindowManager) mContext
                          .getSystemService(Context.WINDOW_SERVICE);
            	  windowManager.getDefaultDisplay().getMetrics(dm);
            	float densityScale = dm.density;
            	
            	BitmapDrawable bd=(BitmapDrawable) mContext.getResources().getDrawable(iconId);
            	int height=bd.getBitmap().getHeight()/3;
            	int width=bd.getBitmap().getWidth()/3;

            	float scaledWidth = (width/2) * densityScale;
            	float scaledHeight = (height/2) * densityScale;
            	
            	Drawable dr = mContext.getResources().getDrawable(iconId);
            	Bitmap bitmap = ((BitmapDrawable) dr).getBitmap();
            	Drawable d = new BitmapDrawable(mContext.getResources(),Bitmap.createScaledBitmap(bitmap, (int)scaledWidth, (int)scaledHeight, true));
            	
            	switch(layout){
            	case R.layout.message_sent_layout: 	
                	textView.setCompoundDrawablesWithIntrinsicBounds(null, null,d,null);
                	
            		break;
            	case R.layout.message_received_layout:
                	textView.setCompoundDrawablesWithIntrinsicBounds(d, null, null, null);
            		break;
            	}
            	break;
            }
        }
	}

    @Override
    public void changeCursor(Cursor cursor) {
        mCursor.close();
        mCursor = cursor;
        super.changeCursor(null);
    }

    /**
     * This static class is used to recycle old message views
     *
     * @author Paul Mallon 23-07-14
     */
    public static class ViewHolder {

        public ListView msgListView;

        public TextView message;

        public ImageView icon;
        
        public ImageView miniMap;
    }

    /**
     * Gets the app user ID in string format as required by Apptimate Services
     *
     * @param context
     * @return A String of variable length in the form of "#1234567890"
     */
    /*private String getAppUserId(Context context) {
        SharedPreferences sp = ((Activity)context).getPreferences(Context.MODE_PRIVATE);
        String appUser = sp.getString(FacebookFragment.KEY_USER_TAG, null);
        return appUser;
    }*/
}
