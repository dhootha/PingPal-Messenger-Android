package io.pingpal.adapters;


import io.pingpal.fragments.FacebookFragment;
import io.pingpal.messenger.R;
import io.pingpal.models.CheckBoxFriend;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;


public class GroupMembersAdapter extends BaseAdapter {
	
    private LayoutInflater mInflater;
    private ImageLoader mImageLoader;
    private DisplayImageOptions mImageOptions;
    private Fragment fragment;
    
    //Used to keep track of the selected friends
    private List<CheckBoxFriend> checkedFriends = new ArrayList<CheckBoxFriend>();
    
    public GroupMembersAdapter(Context context, List<CheckBoxFriend> groupMembers, Fragment fragment) {   	

        this.fragment = fragment;
        checkedFriends = groupMembers;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mImageOptions = new DisplayImageOptions.Builder().cacheInMemory(true).cacheOnDisk(true)
                .displayer(new RoundedBitmapDisplayer(90)).build();
        mImageLoader = ImageLoader.getInstance();
    }

    public int getCount() {
        return checkedFriends.size();
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
		return position;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
    	
        ViewHolder holder = null;
        View rootView = convertView;
        if (rootView == null) {
            rootView = mInflater.inflate(R.layout.row_group_member_layout, parent, false);
            holder = buildViewHolder(rootView);
        } else {
            holder = (ViewHolder)rootView.getTag();
        }

        buildView(position, holder);
        rootView.setTag(holder);

        return rootView;
    }
    
    private void buildView(final int position, ViewHolder holder) {

    	CheckBoxFriend friendCheck = checkedFriends.get(position);
        String friendID = friendCheck.getTag().replace("#", "");
        
        mImageLoader.displayImage(FacebookFragment.IMG_URL_START + friendID
                 + FacebookFragment.IMG_URL_END,
                holder.icon, mImageOptions);
        
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
    
    private ViewHolder buildViewHolder(View rootView) {

        ViewHolder holder = null;
        AsyncTask<View, Void, ViewHolder> task = new AsyncTask<View, Void, ViewHolder>() {

            ViewHolder holder = new ViewHolder();

            @Override
            protected ViewHolder doInBackground(View... params) {
                View rootView = params[0];
                holder.icon = (ImageView)rootView.findViewById(R.id.group_member_imageview);
                holder.nameTextView = (TextView)rootView.findViewById(R.id.group_member_name);
                holder.checkBox = (CheckBox) rootView.findViewById(R.id.group_removemember_checkbox);
                
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
