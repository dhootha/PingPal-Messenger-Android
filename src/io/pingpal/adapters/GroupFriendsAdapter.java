package io.pingpal.adapters;

import io.pingpal.database.DatabaseHelper;
import io.pingpal.fragments.FacebookFragment;
import io.pingpal.messenger.R;
import io.pingpal.models.CheckBoxFriend;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

public class GroupFriendsAdapter extends SimpleCursorAdapter{
	
    @SuppressWarnings("unused")
	private static final String TAG = GroupFriendsAdapter.class.getSimpleName();
    
    private Cursor mCursor;
    
    private LayoutInflater mInflater;

    private DisplayImageOptions mImageOptions;
    
    private Fragment fragment;
    
    //Used to keep track of the selected friends
    private List<CheckBoxFriend> checkedFriends = new ArrayList<CheckBoxFriend>();

    public GroupFriendsAdapter(Context context, Cursor cursor, Fragment fragment) {
        super(context, 0, null, null, null, 0);
        
        this.mCursor = cursor;
        this.fragment = fragment;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mImageOptions = new DisplayImageOptions.Builder().cacheInMemory(true).cacheOnDisk(true)
                .displayer(new RoundedBitmapDisplayer(90)).build();
        
        //Save cursor data to checkedFriends list
    	cursor.moveToFirst();
        for (int i = 0; i < cursor.getCount(); i++) {
        	String name = mCursor.getString(mCursor
                    .getColumnIndex(DatabaseHelper.COLUMN_NAME));
        	
        	String tag = mCursor.getString(mCursor
                    .getColumnIndex(DatabaseHelper.COLUMN_FRIEND_TAG));
        	
        	checkedFriends.add(new CheckBoxFriend(name, tag));
        	cursor.moveToNext();
		}
    }
    
    @Override
    public int getCount() {
        return mCursor.getCount();
    }
    
    /**
     * Adds the Conversation data to the view stored in the ViewHolder
     *
     * @param position The position of the view
     * @param holder The ViewHolder Object that contains the view
     */
    private void buildView(final int position, ViewHolder holder) {
        mCursor.moveToPosition(position);
        
        CheckBoxFriend friendCheck = checkedFriends.get(position);
        
        
        String id = friendCheck.getTag().replace("#", "");

        ImageLoader.getInstance().displayImage(
                FacebookFragment.IMG_URL_START + id
                        + FacebookFragment.IMG_URL_END, holder.icon, mImageOptions);

        holder.nameTextView.setText(friendCheck.getName());
        
        holder.checkBox.setOnClickListener((OnClickListener) fragment);
        
        holder.checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				checkedFriends.get(position).setSelected(isChecked);
			}
		});
        holder.checkBox.setChecked(friendCheck.isSelected());
        holder.checkBox.setTag(friendCheck);
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;
        if (convertView == null) {

        	convertView = mInflater.inflate(R.layout.row_group_friend_layout, parent, false);
            holder = buildViewHolder(convertView);
        } else {

            holder = (ViewHolder)convertView.getTag();
        }
        buildView(position, holder);
        convertView.setTag(holder);

        return convertView;
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

            @Override
            protected ViewHolder doInBackground(View... params) {

                ViewHolder holder = new ViewHolder();
                View rootView = params[0];
                holder.icon = (ImageView)rootView.findViewById(R.id.group_friend_imageview);
                holder.nameTextView = (TextView)rootView.findViewById(R.id.group_friend_name);
                holder.checkBox = (CheckBox)rootView.findViewById(R.id.group_friend_checkbox);
                
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
    
    /**
     * A container class used to store inflated views.
     */
    public static class ViewHolder {
        public ImageView icon;

        public TextView nameTextView;

        public CheckBox checkBox;
    }
}
