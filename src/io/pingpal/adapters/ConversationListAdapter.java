
package io.pingpal.adapters;

import io.pingpal.database.DatabaseHelper;
import io.pingpal.fragments.FacebookFragment;
import io.pingpal.messenger.Communications;
import io.pingpal.messenger.R;
import io.pingpal.models.Conversation;

import java.util.Date;
import java.util.concurrent.ExecutionException;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ConversationListAdapter extends SimpleCursorAdapter {

    private static final int MESSAGE_HINT_LENGTH = 40;

    @SuppressWarnings("unused")
	private static final String TAG = ConversationListAdapter.class.getSimpleName();

    private Cursor mCursor;

    private LayoutInflater mInflater;

    private DisplayImageOptions mImageOptions;
    
    private Context mContext;

    public ConversationListAdapter(Context context, Cursor cursor) {
        super(context, 0, null, null, null, 0);
        this.mCursor = cursor;
        mContext = context;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mImageOptions = new DisplayImageOptions.Builder().cacheInMemory(true).cacheOnDisk(true)
                .displayer(new RoundedBitmapDisplayer(90)).build();
    }

    @Override
    public int getCount() {
        return mCursor.getCount();
    }

    @Override
    public Object getItem(int position) {
        mCursor.moveToPosition(position);
        Conversation conversation = new Conversation(mCursor.getLong(mCursor
                .getColumnIndex(DatabaseHelper.COLUMN_RECEIVER_TAG)), mCursor.getLong(mCursor
                        .getColumnIndex(DatabaseHelper.COLUMN_SENDER_TAG)), mCursor.getInt(mCursor
                                .getColumnIndex(DatabaseHelper.COLUMN_NEW_MESSAGE)), mCursor.getString(mCursor
                                        .getColumnIndex(DatabaseHelper.COLUMN_MESSAGE)));

        return conversation;
    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        String friendTag = mCursor.getString(mCursor.getColumnIndex(DatabaseHelper.COLUMN_FRIEND_TAG));
        return Long.parseLong(friendTag.replace("#", ""));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rootView = convertView;
        ViewHolder holder;
        if (rootView == null) {

            rootView = mInflater.inflate(R.layout.row_conversation_layout, parent, false);
            holder = buildViewHolder(rootView);

        } else {

            holder = (ViewHolder)rootView.getTag();

        }
        buildView(position, holder);
        rootView.setTag(holder);

        return rootView;
    }

    /**
     * Adds the Conversation data to the view stored in the ViewHolder
     *
     * @param position The position of the view
     * @param holder The ViewHolder Object that contains the view
     */
    private void buildView(int position, ViewHolder holder) {
        mCursor.moveToPosition(position);
        // TODO: Need policy for handling images on group chats.
        Date date = new Date(mCursor.getLong(mCursor.getColumnIndex(DatabaseHelper.COLUMN_DATE)));
        holder.dateTextView.setText(date.toString());
        
        String friendID = mCursor.getString(mCursor
                .getColumnIndex(DatabaseHelper.COLUMN_FRIEND_TAG)).replace("#", "");
        
        ImageLoader.getInstance().displayImage(
                FacebookFragment.IMG_URL_START + friendID
                        + FacebookFragment.IMG_URL_END, holder.icon, mImageOptions);
        holder.nameTextView.setText(mCursor.getString(mCursor
                .getColumnIndex(DatabaseHelper.COLUMN_NAME)));

        String message = mCursor.getString(mCursor.getColumnIndex(DatabaseHelper.COLUMN_MESSAGE));
        
        message = Communications.renameIconLocationMessage(message, mContext);
        
		if (message.length() > MESSAGE_HINT_LENGTH) {

			holder.messageHintTextView.setText(message.substring(0,
					MESSAGE_HINT_LENGTH).concat("..."));
		} else {

			holder.messageHintTextView.setText(message);
		}
		holder.newMessageTextView.setText(":-)");
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
                holder.icon = (ImageView)rootView.findViewById(R.id.conversation_imageview);
                holder.nameTextView = (TextView)rootView.findViewById(R.id.name_textview);
                holder.messageHintTextView = (TextView)rootView
                        .findViewById(R.id.message_hint_textview);
                holder.dateTextView = (TextView)rootView
                        .findViewById(R.id.coversation_date_textview);
                holder.newMessageTextView = (TextView)rootView
                        .findViewById(R.id.new_message_textview);

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
     *
     * @author Paul
     */
    public static class ViewHolder {
        public ImageView icon;

        public RelativeLayout textLayout;

        public TextView nameTextView;

        public TextView messageHintTextView;

        public TextView dateTextView;

        public TextView newMessageTextView;

    }

}
