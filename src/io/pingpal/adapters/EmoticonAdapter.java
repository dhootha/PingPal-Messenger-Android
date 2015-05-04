package io.pingpal.adapters;

import java.util.ArrayList;
import java.util.List;

import com.nostra13.universalimageloader.core.ImageLoader;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class EmoticonAdapter extends BaseAdapter {
    private Context mContext;

    public EmoticonAdapter(Context c, TypedArray imageArray) {
        mContext = c;

        for(int i = 0; i < imageArray.length(); i++){
        	thumbIds.add(imageArray.getResourceId(i,0));
        }
    }

    public int getCount() {
        return thumbIds.size();
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return thumbIds.get(position);
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {  // if it's not recycled, initialize some attributes
            imageView = new ImageView(mContext);
            int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 80, mContext.getResources().getDisplayMetrics());
            imageView.setLayoutParams(new GridView.LayoutParams(px, px));
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            
            //imageView.setPadding(24, 24, 24, 24);
        } else {
            imageView = (ImageView) convertView;
        }
        
        ImageLoader.getInstance().displayImage("drawable://" + thumbIds.get(position), imageView);

        return imageView;
    }

    // references to our images
    private List<Integer> thumbIds = new ArrayList<Integer>();
}
