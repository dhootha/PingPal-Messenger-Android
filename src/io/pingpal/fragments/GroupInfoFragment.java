package io.pingpal.fragments;

import io.pingpal.adapters.GroupFriendsAdapter;
import io.pingpal.adapters.GroupMembersAdapter;
import io.pingpal.database.DatabaseCursorLoader;
import io.pingpal.database.DatabaseHelper;
import io.pingpal.database.FriendsDataSource;
import io.pingpal.database.MessagesDataSource;
import io.pingpal.messenger.Keys;
import io.pingpal.messenger.MainActivity;
import io.pingpal.messenger.R;
import io.pingpal.models.CheckBoxFriend;
import io.pingpal.models.Person;
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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Robin Dahlström 03-03-2015
 */
public class GroupInfoFragment extends Fragment implements
LoaderManager.LoaderCallbacks<Cursor>, OnClickListener{

	private ListView membersListView, friendAddListView;
	private Button applyButton;
	private Context context;
	private String groupTag;
	private Cursor cursor;
	private boolean userHasQuit;
	//Keeps track of which friends/members have been selected
	private List<String> membersTags = new ArrayList<String>(), friendsTags = new ArrayList<String>();	
	//Data for GroupMembersAdapter
	List<CheckBoxFriend> membersList = new ArrayList<CheckBoxFriend>();
	
	public static GroupInfoFragment newInstance(String groupTag) {
		GroupInfoFragment fragment = new GroupInfoFragment();
        Bundle args = new Bundle();
        args.putString("group_tag", groupTag);
        fragment.setArguments(args);
		
		return fragment;
	}
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View rootView = inflater.inflate(R.layout.fragment_group_info, container, false);
    	
    	membersListView = (ListView) rootView.findViewById(R.id.group_members_listview);
    	friendAddListView = (ListView) rootView.findViewById(R.id.group_addfriends_listview);
    	applyButton = (Button) rootView.findViewById(R.id.group_removemember_btn);
    	
    	applyButton.setOnClickListener(new OnClickListener() {
			
			@SuppressWarnings("unchecked")
			@Override
			public void onClick(View v) {
				
				//Remove chosen members from group
				new RemoveMembersAsync().execute(membersTags);
				membersTags = new ArrayList<String>();
				
				//Add chosen friends to group
				new AddMembersAsync().execute(friendsTags);
				friendsTags = new ArrayList<String>();
			}
		});
    	
    	context = getActivity().getApplicationContext();	
    	
    	groupTag = getArguments().getString("group_tag");
    	
    	new GetGroupMembers().execute(groupTag);
    	
    	getLoaderManager().initLoader(0, null, this);
    	
    	//Firstimetooltip
		final SharedPreferences prefs = getActivity().getPreferences(Activity.MODE_PRIVATE);
        boolean isFirstTime = prefs.getBoolean(Keys.Preferences.FIRST_GROUP_INFO, true);
    	
        if(isFirstTime){
            
            new AlertDialog.Builder(getActivity())
            .setTitle(getString(R.string.tips))
            .setMessage(getString(R.string.groupinfo_tip))
            .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) { 
                	
                    //Now user har seen tooltip
                    Editor edit = prefs.edit();       
                    edit.putBoolean(Keys.Preferences.FIRST_GROUP_INFO, false);
                    edit.apply();
                }
             })
            .setIcon(R.drawable.ic_ping_alert)
             .show();
        }
        
		return rootView;	
    }

    public class GetGroupMembers extends AsyncTask<String, Void, List<String>> {
		
		@Override
		protected List<String> doInBackground(String... params) {
			String groupTag = params[0];

			groupTag = CreateGroupsFragment.encodeToUTF8(groupTag);
			
			String URI = CreateGroupsFragment.getMembers;
			URI = URI.replace("thetag", groupTag);

			List<String> members = new ArrayList<String>();
			
			HttpClient httpclient = new DefaultHttpClient();
			HttpResponse response;
			
			try {
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
			        	
			        	String memberTag = list.get(i).get("name");
			        	members.add(memberTag);
					}
			        
			        return members;
				}
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
			return null;
		}
		
		@Override
		protected void onPostExecute(List<String> result) {
			super.onPostExecute(result);

			//add result (membertags) into list along with names
			if (result.size() > 0) {
				

				String userTag = ((MainActivity) getActivity()).getUserTag();

				for (String memberTag : result) {

					String name = "";

					// check if user
					if (userTag.equals(memberTag)) {
						SharedPreferences sp = getActivity().getPreferences(
								Context.MODE_PRIVATE);
						String userName = sp.getString("user_name", "");
						name = userName;
					} else {
						// Get name of member from db
						FriendsDataSource db = new FriendsDataSource(context);
						db.open();
						Person friend = db.getFriend(memberTag);
						db.close();
						if (friend != null) {
							name = friend.getName();
						}
					}
					membersList.add(new CheckBoxFriend(name, memberTag));
				}

				membersListView.setAdapter(new GroupMembersAdapter(context,
						membersList, GroupInfoFragment.this));
			}
		}

    	
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
		friendAddListView.setAdapter(new GroupFriendsAdapter(context, arg1, this));

	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		arg0 = null;
	}
	
	class RemoveMembersAsync extends AsyncTask<List<String>, String, Integer>{

		@Override
		protected Integer doInBackground(List<String>... params) {
			List<String> memberTags = params[0];

			String URI;	

			HttpClient httpclient = new DefaultHttpClient();
			HttpResponse response;
			
			//Loop through members, remove each from group
			for (int i = 0; i < memberTags.size(); i++) {
				URI = CreateGroupsFragment.removeMember;
				
				String encodedMemberTag = CreateGroupsFragment.encodeToUTF8(memberTags.get(i));
				String encodedGroupTag = CreateGroupsFragment.encodeToUTF8(groupTag);
				URI = URI.replace("thename", encodedMemberTag);
				URI = URI.replace("thetag", encodedGroupTag);

				try {
					response = httpclient.execute(new HttpGet(URI));
					StatusLine statusLine = response.getStatusLine();
					response.getEntity().consumeContent();
					
				    if(statusLine.getStatusCode() == HttpStatus.SC_OK){
				    	//Let friend know that he is removed from the group (Mohaha)
						HashMap<String, Object> payload = new HashMap<String, Object>();
						payload.put("removedFromGroup", groupTag);
						Outbox.put(memberTags.get(i), payload);
						
						payload = new HashMap<String, Object>();
						payload.put("groupUpdated", groupTag);
						Outbox.put(groupTag, payload);
				    }
				    else {

				        //Close connection
				        response.getEntity().getContent().close();
				        throw new IOException(statusLine.getReasonPhrase());
					}
				    
				    //If user is removing self from group, remove group from db
				    String userTag = ((MainActivity)getActivity()).getUserTag();
				    
				    if (memberTags.get(i).equals(userTag)) {
				    	MessagesDataSource mMessageDb = new MessagesDataSource(context);
				    	mMessageDb.open();
				    	mMessageDb.removeGroup(groupTag);
				    	mMessageDb.close();
				    	
				    	//stop listening 
				    	Outbox.unsubscribeTag(userTag, groupTag);

				    	userHasQuit = true;
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
	
	class AddMembersAsync extends AsyncTask<List<String>, String, Void>{

		@Override
		protected Void doInBackground(List<String>... params) {
			List<String> friendTags = params[0];
			
			String URI;	
			HttpClient httpclient = new DefaultHttpClient();
			HttpResponse response;
				
			//Loop through members, add each to group
			for (int i = 0; i < friendTags.size(); i++) {

				String memberTag = friendTags.get(i);
				String encodedMemberTag = CreateGroupsFragment.encodeToUTF8(memberTag);
				String encGroupTag = CreateGroupsFragment.encodeToUTF8(groupTag);
				
				URI = CreateGroupsFragment.addMember;
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
				    else {
						HashMap<String, Object> payload = new HashMap<String, Object>();
						payload.put("groupNotifyChanged", groupTag);
						payload.put("member", memberTag);
						payload.put("changedTo", "notify");
						
						Outbox.put(memberTag, payload);
					}
				    
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}	

			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
				
			if(userHasQuit){			
		    	((MainActivity)getActivity()).switchFragment(MainActivity.CASE_GROUP);
			}
			else {
				((MainActivity)getActivity()).onGroupInfoSelected(groupTag);
			}
		}
	}

	@Override
	public void onClick(View v) {
		int pos = -1;
		
		//Gets the listview that the checkbox is in
		ListView listView = (ListView) v.getParent().getParent();
		
		//Gets the positon of the checkbox in the listview
		pos = listView.getPositionForView(v);
			
			if(pos != ListView.INVALID_POSITION){

				CheckBox checkBox = (CheckBox) v;
	        	
				/*
				 * The membersListView uses memberslist in its adapter
				 */
	        	if(listView.equals(membersListView)){
	        		
	        		CheckBoxFriend checkboxfriend = membersList.get(pos);
	        		
	        		if(checkBox.isChecked()){
		        		membersTags.add(checkboxfriend.getTag());
	        		}
	        		else{
	        			membersTags.remove(checkboxfriend.getTag());
	        		}
	        	}
				/*
				 * The friendAddListView uses cursor in its adapter
				 */
	        	else if(listView.equals(friendAddListView)){
	        		
					//Find the row with the correct position in the cursor
					cursor.moveToFirst();
					while (cursor.isAfterLast() == false) {
						if(cursor.getPosition() == pos){
							break;
						}
					  cursor.moveToNext();
					}
        	
		        	String tag = cursor.getString(cursor
		                    .getColumnIndex(DatabaseHelper.COLUMN_FRIEND_TAG));
	        		
	        		if(checkBox.isChecked()){
		        		friendsTags.add(tag);
	        		}
	        		else{
	        			friendsTags.remove(tag);
	        		}
	        	}	
			}
	
	}
}
