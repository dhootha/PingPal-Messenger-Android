
package io.pingpal.fragments;

import io.pingpal.adapters.GroupFriendsAdapter;
import io.pingpal.database.DatabaseCursorLoader;
import io.pingpal.database.DatabaseHelper;
import io.pingpal.database.FriendsDataSource;
import io.pingpal.messenger.MainActivity;
import io.pingpal.messenger.R;
import io.pingpal.outbox.Outbox;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

/**
 * @author Robin Dahlström 18-02-2015
 */
public class CreateGroupsFragment extends Fragment implements
LoaderManager.LoaderCallbacks<Cursor>, OnClickListener{

	private ListView listView;
	private Button createGroupButton;
	private EditText editText;
	private String userTag;
	private List<String> members = new ArrayList<String>();
	private Cursor cursor;
	
	/*
	 * Server http request links. Responses will be in Json format. 
	 */
	public static final String createGroup = "http://ppmapi.com/ppmess/rest/msggroup/creategroup?name=thename&tag=thetag";
	public static final String setGroupName = "http://ppmapi.com/ppmess/rest/msggroup/setgroupname?name=thename&tag=thetag";
	public static final String removeMember = "http://ppmapi.com/ppmess/rest/msgmember/removemember?name=thename&tag=thetag";
	public static final String getGroupName = "http://ppmapi.com/ppmess/rest/msggroup/getgroupname?tag=thetag";
	public static final String getGroups = "http://ppmapi.com/ppmess/rest/msgmember/getgroups?name=thename";
	public static final String getMembers = "http://ppmapi.com/ppmess/rest/msggroup/getmembers?tag=thetag";
	public static final String addMember = "http://ppmapi.com/ppmess/rest/msgmember/addmember?name=thename&tag=thetag";
	
    @SuppressWarnings("unused")
	private static final String TAG = CreateGroupsFragment.class.getSimpleName();
    
    /**
     * The fragment argument representing the section number for this fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    /**
     * Returns a new instance of this fragment for the given section number.
     */

    public static CreateGroupsFragment newInstance(int sectionNumber) {
        CreateGroupsFragment fragment = new CreateGroupsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_groups_manage, container, false);
        listView = (ListView) rootView.findViewById(R.id.group_friends_listview);
        createGroupButton = (Button) rootView.findViewById(R.id.create_group_button);
        editText = (EditText) rootView.findViewById(R.id.group_editText_name);
        
        editText.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId,
                                          KeyEvent event) {
                if (event != null&& (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    InputMethodManager in = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);

                    //Hide vertical keyboard
                    in.hideSoftInputFromWindow(v
                                    .getApplicationWindowToken(),
                            InputMethodManager.HIDE_NOT_ALWAYS);

                    return true;

                }
                return false;
            }
        });
        
        createGroupButton.setOnClickListener(new OnClickListener() {
        	
			@SuppressWarnings("unchecked")
			@Override
			public void onClick(View v) {
				
				String groupName = editText.getText().toString();
				
				if(groupName.length() > 0){
					Map<String, String> groupParams = new HashMap<String, String>();
					groupParams.put("group_name", groupName);

					new GroupCreateAsync().execute(groupParams);
				}
			}
		});
        
        return rootView;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userTag = ((MainActivity)getActivity()).getUserTag();
        getLoaderManager().initLoader(0, null, this);
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
	public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
		cursor = arg1;
		listView.setAdapter(new GroupFriendsAdapter(getActivity(), arg1, this));
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		arg0 = null;
		
	}
	
	class GroupCreateAsync extends AsyncTask<Map<String,String>, String, String>{

		@Override
		protected String doInBackground(Map<String,String>... params) {
			Map<String,String> createParams = params[0];
			
			String groupName = createParams.get("group_name");
			String groupTag = Outbox.createUniqueTag();
			
			groupName = CreateGroupsFragment.encodeToUTF8(groupName);
			String encGroupTag = CreateGroupsFragment.encodeToUTF8(groupTag);
			
			String URI = createGroup;	
			URI = URI.replace("thename", groupName);
			URI = URI.replace("thetag", encGroupTag);
			
			
			HttpClient httpclient = new DefaultHttpClient();
			HttpResponse response;
				
			try {
				response = httpclient.execute(new HttpGet(URI));
				StatusLine statusLine = response.getStatusLine();
				
			    if(statusLine.getStatusCode() != HttpStatus.SC_OK){
					
			        //Close connection
			        response.getEntity().getContent().close();
			        throw new IOException(statusLine.getReasonPhrase());
			    }

			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return groupTag;
		}

		@Override
		protected void onPostExecute(String theGroupTag) {
			super.onPostExecute(theGroupTag);

			Toast.makeText(getActivity(), getString(R.string.create_success), Toast.LENGTH_SHORT).show();

			//Add user to members list
			members.add(userTag);
			
			resetViews();
			new AddMembersAsync().execute(theGroupTag);
		}
	}
	
	class AddMembersAsync extends AsyncTask<String, String, String>{

		@Override
		protected String doInBackground(String... params) {
			String groupTag = params[0];
			
			String URI;	
			HttpClient httpclient = new DefaultHttpClient();
			HttpResponse response;
				
			//Loop through members, add each to group
			for (int i = 0; i < members.size(); i++) {

				String memberTag = members.get(i);
				String encodedMemberTag = CreateGroupsFragment.encodeToUTF8(memberTag);
				String encGroupTag = CreateGroupsFragment.encodeToUTF8(groupTag);
				
				URI = addMember;
				URI = URI.replace("thename", encodedMemberTag);
				URI = URI.replace("thetag", encGroupTag);
				
				try {
					response = httpclient.execute(new HttpGet(URI));
					StatusLine statusLine = response.getStatusLine();
					response.getEntity().consumeContent();
					
				    if(statusLine.getStatusCode() != HttpStatus.SC_OK){
				        //Close connection
				        response.getEntity().getContent().close();
				        throw new IOException(statusLine.getReasonPhrase());
				    }
				    else{

						HashMap<String, Object> payload = new HashMap<String, Object>();
						payload.put("groupNotifyChanged", groupTag);
						payload.put("member", members.get(i));
						payload.put("changedTo", "notify");
						
						Outbox.put(members.get(i), payload);
				    }
				    
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}	

			return null;
		}
		
	}
	
	/**
	 * Encode a string into UTF-8 format
	 * @param string 
	 * @return encoded string
	 */
    public static String encodeToUTF8(String string){
		try {
			string = URLEncoder.encode(string, "UTF-8");
			return string;
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return string;
    }
    
    private void resetViews(){
    	listView.setItemChecked(-1, false);
    	editText.setText("");
    }

	@Override
	public void onClick(View v) {
		int pos = listView.getPositionForView(v);
		
		if(pos != ListView.INVALID_POSITION){

			//Find the row with the correct position in the cursor
			cursor.moveToFirst();
			while (cursor.isAfterLast() == false) {
				int position = cursor.getPosition();
				if(position == pos){
					break;
				}
			  cursor.moveToNext();
			}

        	String tag = cursor.getString(cursor
                    .getColumnIndex(DatabaseHelper.COLUMN_FRIEND_TAG));
        	
        	members.add(tag);
		}
	}
}
