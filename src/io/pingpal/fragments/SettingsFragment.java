
package io.pingpal.fragments;

import io.pingpal.adapters.PingAccessAdapter;
import io.pingpal.database.DatabaseCursorLoader;
import io.pingpal.database.FriendsDataSource;
import io.pingpal.database.MessagesDataSource;
import io.pingpal.messenger.Keys;
import io.pingpal.messenger.MainActivity;
import io.pingpal.messenger.R;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * @author Robin Dahlström 04-03-15
 */
public class SettingsFragment extends Fragment implements
LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * The fragment argument representing the section number for this fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private ListView listView;
    private ToggleButton toggleButton;
    private Button clearMessagesBtn, resetTooltipsBtn;

    /**
     * Returns a new instance of this fragment for the given section number.
     */
    public static SettingsFragment newInstance(int sectionNumber) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); 

        getLoaderManager().initLoader(0, null, this); 
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_settings, container, false);
        
        listView = (ListView) rootView.findViewById(R.id.list_view);
        clearMessagesBtn = (Button) rootView.findViewById(R.id.delete_messages);
        resetTooltipsBtn = (Button) rootView.findViewById(R.id.reset_tooltips);
        
        clearMessagesBtn.setOnClickListener(getOnClickListener());
        resetTooltipsBtn.setOnClickListener(getOnClickListener());
        
        toggleButton = (ToggleButton) rootView.findViewById(R.id.toggle_button);
        
        toggleButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				
				//Save to preferences if the user has enabled sound or not
				SharedPreferences prefs = getActivity().getPreferences(Activity.MODE_PRIVATE);
				Editor edit = prefs.edit();
				
				edit.putBoolean(Keys.Preferences.SOUND_ENABLED, isChecked);
				edit.apply();
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
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {

        String userTag = ((MainActivity)getActivity()).getUserTag();
        
        String[] params = new String[] {
        		userTag
        };
        return new DatabaseCursorLoader(getActivity(), FriendsDataSource.QUERY_ACTIVE_FRIENDS, params);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		listView.setAdapter(new PingAccessAdapter(getActivity(), cursor));
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		// TODO Auto-generated method stub
        loader = null;
	}
	
	
	
	public OnClickListener getOnClickListener(){
		return new OnClickListener() {

			@Override
			public void onClick(View v) {
				switch(v.getId()){
				case R.id.delete_messages:
					MessagesDataSource db = new MessagesDataSource(getActivity());
					db.open();
					db.deleteAllMessages();
					db.close();
					Toast.makeText(getActivity(), getString(R.string.cleared_all), Toast.LENGTH_SHORT).show();
					break;
				case R.id.reset_tooltips:
					
					SharedPreferences prefs = getActivity().getPreferences(Activity.MODE_PRIVATE);
					Editor edit = prefs.edit();
					edit.putBoolean(Keys.Preferences.FIRST_CONVERSATION, true);
					edit.putBoolean(Keys.Preferences.FIRST_GROUP_INFO, true);
					edit.apply();
					Toast.makeText(getActivity(), getString(R.string.reset_done), Toast.LENGTH_SHORT).show();
					break;
				}
				
			}
			
		};
	}
}
