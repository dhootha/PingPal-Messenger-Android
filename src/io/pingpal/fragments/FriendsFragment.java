
package io.pingpal.fragments;

import io.pingpal.adapters.FriendsListAdapter;
import io.pingpal.database.DatabaseCursorLoader;
import io.pingpal.database.FriendsDataSource;
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

/**
 * @author Paul Mallon & Robin Dahlström 22-07-2014
 */
public class FriendsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = FriendsFragment.class.getSimpleName();

    /**
     * The fragment argument representing the section number for this fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    /**
     * Returns a new instance of this fragment for the given section number.
     */

    private static final int FRIENDS_LOADER = 3;

    public static FriendsFragment newInstance(int sectionNumber) {
        FriendsFragment fragment = new FriendsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getLoaderManager().initLoader(FRIENDS_LOADER, null, this);

    }

    private ListView mFriendsListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_friends, container, false);
        mFriendsListView = (ListView)rootView.findViewById(R.id.friends_listview);

        mFriendsListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Log.v(TAG, "You selected the friend with the ID: " + id);
                
                String friendTag = "#" + id;

                //Create conversation if not exist
                int conversationId = ((MainActivity) getActivity()).getConversationID(friendTag);
                
                ((MainActivity)getActivity()).onFriendSelected(conversationId);
                
            }

        });

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity)activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle arg) {

        switch (id) {
            case FRIENDS_LOADER:
                String userTag = ((MainActivity)getActivity()).getUserTag();
                
                String[] params = new String[] {
                		userTag
                };
                return new DatabaseCursorLoader(getActivity(),
                        FriendsDataSource.QUERY_ACTIVE_FRIENDS, params);

            default:
                Log.v(TAG, "An invalid id was passed into onCreateLoader, createLoader failed!");
                throw new IllegalArgumentException(
                        "An invalid id was passed into onCreateLoader, createLoader failed!");
        }

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mFriendsListView.setAdapter(new FriendsListAdapter(getActivity(), cursor));

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        loader = null;

    }

    /**
     * Allows the passing of the ID of the selected friend back
     * to the activity that's displaying this fragment
     *
     * @author Paul Mallon
     */
    public interface FriendSelectedCallbacks {

		public void onFriendSelected(int friendId);
    }

}
