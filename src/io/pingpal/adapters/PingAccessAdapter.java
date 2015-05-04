
package io.pingpal.adapters;

import io.pingpal.database.DatabaseHelper;
import io.pingpal.database.FriendsDataSource;
import io.pingpal.fragments.FacebookFragment;
import io.pingpal.messenger.R;
import io.pingpal.models.Person;

import java.util.concurrent.ExecutionException;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

public class PingAccessAdapter extends SimpleCursorAdapter {

    @SuppressWarnings("unused")
	private static final String TAG = PingAccessAdapter.class.getSimpleName();

    private LayoutInflater mInflater;

    private DisplayImageOptions mImageOptions;

    private Cursor mCursor;

    private ImageLoader mImageLoader;
    private Context mContext;

    public PingAccessAdapter(Context context, Cursor cursor) {
        super(context, 0, null, null, null, 0);
        this.mCursor = cursor;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mImageOptions = new DisplayImageOptions.Builder().cacheInMemory(true).cacheOnDisk(true)
                .displayer(new RoundedBitmapDisplayer(90)).build();
        mImageLoader = ImageLoader.getInstance();

        mContext = context;
    }

    @Override
    public int getCount() {

        return mCursor.getCount();
    }

    @Override
    public Object getItem(int position) {
        mCursor.moveToPosition(position);

        Person friend = new Person(mCursor.getString(mCursor
                .getColumnIndex(DatabaseHelper.COLUMN_FRIEND_TAG)), DatabaseHelper.COLUMN_TAG,
                mCursor.getString(mCursor.getColumnIndex(DatabaseHelper.COLUMN_NAME)));

        return friend;
    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(mCursor.getColumnIndex(DatabaseHelper.COLUMN_FRIEND_TAG));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        View rootView = convertView;
        if (rootView == null) {
            rootView = mInflater.inflate(R.layout.row_ping_access_layout, parent, false);
            holder = buildViewHolder(rootView);
        } else {
            holder = (ViewHolder)rootView.getTag();
        }

        buildView(position, holder);
        rootView.setTag(holder);

        return rootView;
    }

    /**
     * Adds the Friend data to the view stored in the ViewHolder
     *
     * @param position The position of the view
     * @param holder The ViewHolder Object that contains the view
     */
    private void buildView(int position, ViewHolder holder) {
        mCursor.moveToPosition(position);
        
        final String friendTag = mCursor.getString(mCursor.getColumnIndex(DatabaseHelper.COLUMN_FRIEND_TAG));
        
        String frindID = friendTag.replace("#", "");

        mImageLoader.displayImage(FacebookFragment.IMG_URL_START + frindID
                 + FacebookFragment.IMG_URL_END,
                holder.icon, mImageOptions);
        mCursor.moveToPosition(position);
        holder.nameTextView.setText(mCursor.getString(mCursor
                .getColumnIndex(DatabaseHelper.COLUMN_NAME)));
        
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(mContext,
                R.array.ping_options, android.R.layout.simple_spinner_item);
        
        holder.spinner.setAdapter(adapter);
        
        //Get current pingaccess
        FriendsDataSource db = new FriendsDataSource(mContext);
        db.open();
        final int pingAccess = db.getFriendPingAccess(friendTag);
        db.close();
        
        holder.spinner.setSelection(pingAccess);
        
        holder.spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				
				((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE);
				
		        FriendsDataSource db = new FriendsDataSource(mContext);
		        db.open();
		        db.setFriendPingAccess(friendTag, position);
		        db.close();				
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
				
			}
		});
    }

    /**
     * This method executes off the main thread to inflate the requested view
     * and add it to a ViewHolder Object
     *
     * @param rootView the view to be added to the viewHolder
     * @return ViewHolder constructed ViewHolder Object
     */
    private ViewHolder buildViewHolder(View rootView) {

        ViewHolder holder = null;
        AsyncTask<View, Void, ViewHolder> task = new AsyncTask<View, Void, ViewHolder>() {

            ViewHolder holder = new ViewHolder();

            @Override
            protected ViewHolder doInBackground(View... params) {
                View rootView = params[0];
                holder.icon = (ImageView)rootView.findViewById(R.id.ping_friend_imageview);
                holder.nameTextView = (TextView)rootView.findViewById(R.id.ping_friend_name);
                holder.spinner = (Spinner)rootView.findViewById(R.id.ping_spinner);
                return holder;
            }

        }.execute(rootView);

        try {
            holder = task.get();
        } catch (InterruptedException e) {

            e.printStackTrace();
        } catch (ExecutionException e) {

            e.printStackTrace();
        }

        return holder;
    }

    public static class ViewHolder {
        public ImageView icon;
        public TextView nameTextView;
        public Spinner spinner;

    }

}
