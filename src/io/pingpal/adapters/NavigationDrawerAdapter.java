
package io.pingpal.adapters;

import io.pingpal.messenger.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * This class inflates view items for the NavigationDrawerFragment
 *
 * @author Paul Mallon 21-07-14
 */
public class NavigationDrawerAdapter extends ArrayAdapter<String> {

    @SuppressWarnings("unused")
    private static final String TAG = NavigationDrawerAdapter.class.getSimpleName();

    private final Context mContext;

    private final String[] mValues;

    private final int[] mImageResIds;

    public NavigationDrawerAdapter(Context context, String[] values, TypedArray imageArray) {
        super(context, R.layout.row_settings_layout, values);

        this.mContext = context;
        this.mValues = values;
        int length = imageArray.length();
        this.mImageResIds = new int[length];
        for (int i = 0; i < length; i++) {
            mImageResIds[i] = imageArray.getResourceId(i, 0);
        }
    }

    @Override
    public int getCount() {

        return mValues.length;
    }

    @Override
    public String getItem(int position) {

        return mValues[position];
    }

    @Override
    public long getItemId(int position) {

        return position;
    }

    @SuppressLint("ViewHolder")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater)mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.row_settings_layout, parent, false);

        TextView textView = (TextView)rowView.findViewById(R.id.setting_label);
        textView.setText(mValues[position]);

        ImageView imageView = (ImageView)rowView.findViewById(R.id.icon);
        imageView.setImageResource(mImageResIds[position]);

        return rowView;
    }

}
