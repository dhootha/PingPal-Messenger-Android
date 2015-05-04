
package io.pingpal.fragments;

import io.pingpal.adapters.GroupsAdapter;
import io.pingpal.database.DatabaseCursorLoader;
import io.pingpal.database.DatabaseHelper;
import io.pingpal.database.MessagesDataSource;
import io.pingpal.messenger.MainActivity;
import io.pingpal.messenger.R;
import io.pingpal.outbox.Outbox;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Paul Mallon & Robin Dahlström 22-07-2014
 */
public class GroupsFragment extends Fragment implements
LoaderManager.LoaderCallbacks<Cursor> {
	
    @SuppressWarnings("unused")
	private static final String TAG = GroupsFragment.class.getSimpleName();
    private ListView listView;
    private ProgressBar progressBar;
    private String userTag;
    private Context context;
    /**
     * The fragment argument representing the section number for this fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    /**
     * Returns a new instance of this fragment for the given section number.
     */

    public static GroupsFragment newInstance(int sectionNumber) {
        GroupsFragment fragment = new GroupsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_groups, container, false);
        progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
        listView = (ListView) rootView.findViewById(R.id.groups_listview);

        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Log.v(TAG, "You selected conversation " + id);
                MainActivity activity = (MainActivity)getActivity();
                activity.onGroupSelected((int) id);
            }

        });
        
        context = getActivity().getApplicationContext();	
        
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity)activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); 

        userTag = ((MainActivity)getActivity()).getUserTag();
        new SaveGroupsFromServerToDB().execute();
    }

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        //Log.v(TAG, "Starting rawQuery");

        String[] params = new String[] {
        		userTag
        };
        return new DatabaseCursorLoader(getActivity(), DatabaseHelper.QUERY_GROUPS, params);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		listView.setAdapter(new GroupsAdapter(context, cursor));
		
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
        loader = null;
		
	}

    public class SaveGroupsFromServerToDB extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			
			//First, get all groups info for this user
			String URI = CreateGroupsFragment.getGroups;
			
			String encodedUserTag = CreateGroupsFragment.encodeToUTF8(userTag);
			URI = URI.replace("thename", encodedUserTag);
			
			HttpClient httpclient = new DefaultHttpClient();
			HttpResponse response;
	        
			try {
		        ArrayList<String> groupTags = new ArrayList<String>();
				response = httpclient.execute(new HttpGet(URI));
				StatusLine statusLine = response.getStatusLine();
				
			    if(statusLine.getStatusCode() == HttpStatus.SC_OK){
			        ByteArrayOutputStream out = new ByteArrayOutputStream();
			        response.getEntity().writeTo(out);
			        String responseString = out.toString();
			        
			        //Nested hashmap
			        @SuppressWarnings("unchecked")
			        Map<String, ArrayList<Map<String, String>>> result =
			                new ObjectMapper().readValue(responseString, HashMap.class);
			        
			        ArrayList<Map<String, String>> list = result.get("response");
        
			        for (int i = 0; i < list.size(); i++) {
				        groupTags.add(list.get(i).get("tag"));
					}

			        out.close();

			    } else{
			        //Close connection
			        response.getEntity().getContent().close();
			        throw new IOException(statusLine.getReasonPhrase());
			    }
			    
			    //Use grouptags to get groupnames
			    String [] groupNames = new String[groupTags.size()];
			    
			    for (int i = 0; i < groupTags.size(); i++) {
			    	URI = CreateGroupsFragment.getGroupName;
			    	
			    	String groupTag = groupTags.get(i);
			    	
			    	//Subscribe to groupTag    	
			    	Outbox.subscribeTag(userTag, groupTag);
			    	
			    	groupTag = CreateGroupsFragment.encodeToUTF8(groupTag);
			    	
			    	URI = URI.replace("thetag", groupTag);

			    	response = httpclient.execute(new HttpGet(URI));
			    	
				    if(statusLine.getStatusCode() == HttpStatus.SC_OK){
				        ByteArrayOutputStream out = new ByteArrayOutputStream();
				        response.getEntity().writeTo(out);
				        String responseString = out.toString();
				        
				        //Nested hashmap
				        @SuppressWarnings("unchecked")
				        Map<String, Map<String, String>> result =
				                new ObjectMapper().readValue(responseString, HashMap.class);	        
				        groupNames[i] = result.get("response").get("name");
				        //Log.v(TAG, "Name is: " + result.get("response").get("name"));
				        
				        out.close();
				        response.getEntity().consumeContent();

				    } else{
				        //Close connection
				        response.getEntity().getContent().close();
				        throw new IOException(statusLine.getReasonPhrase());
				    }
				}
			    
			    saveAndDeleteGroupsToFromDB(groupTags, groupNames);           
			    		    
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return null;
		}

		private void saveAndDeleteGroupsToFromDB(ArrayList<String> groupTags,
				String[] groupNames) {
			//Add all groups info to db
			MessagesDataSource mdb = new MessagesDataSource(getActivity());
			mdb.open();
			for (int i = 0; i < groupNames.length; i++) {
				String groupTag = groupTags.get(i);
				
			    mdb.addGroup(groupTag, groupNames[i], userTag);
			    Outbox.subscribeTag(userTag, groupTag);
			}
			
			//Remove groups from db that are not on server         
			List<String> dbGroupTags = mdb.getAllGroupTags(userTag);

			//first, remove from dbGroupTags list tags that exist on server
			for (String groupTag : groupTags) {
				dbGroupTags.remove(groupTag);
			}
			
			//remove the grouptags still remaining in dbGroupTags from database
			for (String dbGroupTag : dbGroupTags) {
				mdb.removeGroup(dbGroupTag);
				Outbox.unsubscribeTag(userTag, dbGroupTag);
			}
			
			mdb.close();
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			
			if(progressBar != null){
				progressBar.setVisibility(View.GONE);
			}

	        getLoaderManager().initLoader(0, null, GroupsFragment.this); 
		}
    }
}
