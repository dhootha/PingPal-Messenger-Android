
package io.pingpal.adapters;

import io.pingpal.database.DatabaseHelper;
import io.pingpal.messenger.R;
import io.pingpal.models.Person;

import java.util.concurrent.ExecutionException;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

public class GroupsAdapter extends SimpleCursorAdapter {

    @SuppressWarnings("unused")
    private static final String TAG = GroupsAdapter.class.getSimpleName();

    private LayoutInflater mInflater;

    private Cursor mCursor;

    private ImageLoader mImageLoader;

    public GroupsAdapter(Context context, Cursor cursor) {
        super(context, 0, null, null, null, 0);
        this.mCursor = cursor;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mImageLoader = ImageLoader.getInstance();

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
        return mCursor.getLong(mCursor.getColumnIndex(DatabaseHelper.COLUMN_GROUP_ID));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        View rootView = convertView;
        if (rootView == null) {
            rootView = mInflater.inflate(R.layout.row_friends_layout, parent, false);
            holder = buildViewHolder(rootView);
        } else {
            holder = (ViewHolder)rootView.getTag();
        }

        buildView(position, holder);
        rootView.setTag(holder);

        return rootView;
    }

    private void buildView(int position, ViewHolder holder) {
        mCursor.moveToPosition(position);
        mImageLoader.displayImage("drawable://" + R.drawable.ping_groups,
                holder.icon);
        mCursor.moveToPosition(position);
        holder.nameTextView.setText(mCursor.getString(mCursor
                .getColumnIndex(DatabaseHelper.COLUMN_NAME)));
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
                holder.icon = (ImageView)rootView.findViewById(R.id.icon_friend);
                holder.nameTextView = (TextView)rootView.findViewById(R.id.label_friend);
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

    }

}
