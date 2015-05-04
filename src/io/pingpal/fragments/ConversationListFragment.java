
package io.pingpal.fragments;

import io.pingpal.adapters.ConversationListAdapter;
import io.pingpal.database.DatabaseCursorLoader;
import io.pingpal.database.DatabaseHelper;
import io.pingpal.messenger.MainActivity;
import io.pingpal.messenger.R;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class ConversationListFragment extends Fragment implements
LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = ConversationListFragment.class.getSimpleName();

    private static final int CONVERSATION_LIST_LOADER = 1;

    public static ConversationListFragment newInstance(int sectionNumber) {

        ConversationListFragment fragment = new ConversationListFragment();
        Bundle args = new Bundle();

        args.putInt(MainActivity.ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getLoaderManager().initLoader(CONVERSATION_LIST_LOADER, null, this);

    }

    private ListView mConversationsListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_conversation_list, container, false);
        mConversationsListView = (ListView)rootView.findViewById(R.id.conversations_listview);

        mConversationsListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Log.v(TAG, "You selected conversation " + id);
                
                String friendTag = "#" + id;
                
                int conversationId = ((MainActivity) getActivity()).getConversationID(friendTag);
                
                MainActivity activity = (MainActivity)getActivity();
                activity.onConversationSelected(conversationId);
            }

        });

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity)activity).onSectionAttached(getArguments().getInt(
                MainActivity.ARG_SECTION_NUMBER));

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {

            case CONVERSATION_LIST_LOADER:
                //Log.v(TAG, "Starting rawQuery");
                
                String userTag = ((MainActivity)getActivity()).getUserTag();
                
                String[] params = new String[] {
                		userTag
                };
                return new DatabaseCursorLoader(getActivity(), DatabaseHelper.QUERY_CONVERSATION_LIST, params);
            default:
                Log.v(TAG, "An invalid id was passed into onCreateLoader, createLoader failed!");
                throw new IllegalArgumentException(
                        "An invalid id was passed into onCreateLoader, createLoader failed!");
        }

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mConversationsListView.setAdapter(new ConversationListAdapter(getActivity(), cursor));

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        loader = null;

    }

    /**
     * Allows the passing of the ID of the selected conversation back
     * to the activity displaying the ConversationListFragment
     *
     * @author Paul Mallon
     */
    public interface ConversationSelectedCallbacks {

        public void onConversationSelected(int conversationId);
    }

}
