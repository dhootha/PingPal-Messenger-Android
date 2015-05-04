
package io.pingpal.fragments;

import io.pingpal.adapters.EmoticonAdapter;
import io.pingpal.adapters.MessageListAdapter;
import io.pingpal.database.DatabaseCursorLoader;
import io.pingpal.database.DatabaseHelper;
import io.pingpal.database.FriendsDataSource;
import io.pingpal.database.MessagesDataSource;
import io.pingpal.location.Manager;
import io.pingpal.messenger.Keys;
import io.pingpal.messenger.MainActivity;
import io.pingpal.messenger.R;
import io.pingpal.models.Message;
import io.pingpal.models.Person;
import io.pingpal.outbox.Outbox;
import io.pingpal.outbox.Outbox.Filter;
import io.pingpal.outbox.Outbox.Inbox;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
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
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Paul Mallon & Robin Dahlström 22-07-2014
 */
public class ConversationFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = ConversationFragment.class.getSimpleName();

    public static final String KEY_CONVERSATION_TYPE = "conversation_type";
    public static final String KEY_CONVERSATION_ID = "conversation_id";
    
	public enum ConversationType {
		SINGLE_CONVERSATION, GROUP_CONVERSATION
	}
    
    private static final int MESSAGE_LOADER = 0;

    /**
     * Returns a new instance of this fragment for the given section number -
     * Conversation ID can also be the group ID
     */

    public static ConversationFragment newInstance(int conversationID, ConversationType type) {

        ConversationFragment fragment = new ConversationFragment();
        Bundle args = new Bundle();

        args.putInt(ConversationFragment.KEY_CONVERSATION_ID, conversationID);
        args.putSerializable(ConversationFragment.KEY_CONVERSATION_TYPE, type);
        fragment.setArguments(args);

        return fragment;
    }

    /**
     * The tag messages are sent to in the conversation.
     */
    private String sendToTag;

    private Context mContext;

    private MessagesDataSource mMessageDb;

    private FriendsDataSource mFriendsDb;
    
    private String userTag;
    
    private AnimationDrawable msgReceiveAnim;
    
    /**
     * The friend the user is talking to in the conversation.
     */
    private Person convFriend; 
    
    private ConversationType conversationType;
    
    private int conversationID;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getActivity().getApplicationContext();
        mMessageDb = new MessagesDataSource(mContext);
        mFriendsDb = new FriendsDataSource(mContext);
        conversationID = this.getArguments().getInt(ConversationFragment.KEY_CONVERSATION_ID);
        conversationType = (ConversationType) this.getArguments().get(ConversationFragment.KEY_CONVERSATION_TYPE);
        
		switch (conversationType) {
		case SINGLE_CONVERSATION:
			// get friends ID & name
			mFriendsDb.open();
			convFriend = mFriendsDb.getFriend(conversationID);
			mFriendsDb.close();
			sendToTag = convFriend.getTag();
			break;
		case GROUP_CONVERSATION:
			// get group tag
			mMessageDb.open();
			sendToTag = mMessageDb.getGroupTag(conversationID);
			mMessageDb.close();
			break;
		}
            
        Bundle bundle = new Bundle();
        bundle.putLong(ConversationFragment.KEY_CONVERSATION_ID, conversationID);
        getLoaderManager().initLoader(MESSAGE_LOADER, bundle, this);
        
        setHasOptionsMenu(true);
        
        userTag = ((MainActivity)getActivity()).getUserTag();
    }

    private ListView mMessagesListView;
    
    private GridView emoticonGridView;

    private RelativeLayout mSendMessageLayout;

    private EditText mMessageTxt;

    private Button mSendBtn;

    private Button mPenguinBtn;

    private MessageListAdapter mAdapter;
    
    private ImageView recieveMsgImage;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_conversation, container, false);
        mSendMessageLayout = (RelativeLayout)rootView.findViewById(R.id.send_message_layout);
        mMessagesListView = (ListView)rootView.findViewById(R.id.messages_listview);
        mMessageTxt = (EditText)mSendMessageLayout.findViewById(R.id.message_edittext);
        
        mMessageTxt.setOnEditorActionListener(getEditorActionListener());
        mMessageTxt.addTextChangedListener(getTextWatcher());
        
        mSendBtn = (Button)mSendMessageLayout.findViewById(R.id.button_send);
        mPenguinBtn = (Button)mSendMessageLayout.findViewById(R.id.button_penguins);
        mPenguinBtn.setOnClickListener(getOnClickListener());
        mSendBtn.setOnClickListener(getOnClickListener());
        emoticonGridView = (GridView) rootView.findViewById(R.id.emoticons_gridview);
        
        recieveMsgImage = (ImageView) rootView.findViewById(R.id.msg_receive_image);    
        recieveMsgImage.setBackgroundResource(R.drawable.animation_receive_msg);
        msgReceiveAnim = (AnimationDrawable) recieveMsgImage.getBackground();
        
        TypedArray typedArr = getActivity().getResources().obtainTypedArray(R.array.emoticons);
        getActivity().getResources().obtainTypedArray(R.array.emoticons).recycle();
        emoticonGridView.setAdapter(new EmoticonAdapter(mContext, typedArr));

        emoticonGridView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                sendEmoticon((int) id);
            }
        });
        
        setHasOptionsMenu(true);
        
        //Set name of conversation (friend name or group name)
        editConversationTitle();
        
        //Tell user about options menu the first time  
        firstTimeToolTip();
        
        //Listen to when friend is writing
        listenToWriting();
        
        return rootView;
    }

	private void listenToWriting() {
		Filter writingFilter = new Filter() {
			
			@Override
			public boolean call(Map<String, Object> payload, Map<String, Object> options) {

				//Only listen to payload with "writing" as key, and if it's from the friend in this conversation
				return payload.containsKey("writing") && options.get("from").equals(sendToTag);
			}
		};
		
        Inbox writingInbox = new Inbox() {
			
			@Override
			public void call(Map<String, Object> payload, Map<String, Object> options,
					Outbox arg2) {
				// TODO Auto-generated method stub
				boolean isWriting = (Boolean) payload.get("writing");
				
				if (isWriting) {
			        msgReceiveAnim.start();
					recieveMsgImage.setVisibility(View.VISIBLE);
					
				}else {
			        msgReceiveAnim.stop();
					recieveMsgImage.setVisibility(View.GONE);
				}
			}
		};
		
		Outbox.attachInbox(writingInbox, writingFilter);
	}

	private void firstTimeToolTip() {
		final SharedPreferences prefs = getActivity().getPreferences(Activity.MODE_PRIVATE);
        boolean isFirstTime = prefs.getBoolean(Keys.Preferences.FIRST_CONVERSATION, true);
        
        
        if (isFirstTime) {
            getActivity().openOptionsMenu();
                
            String theTip = getString(R.string.conv_tip);
            
            switch (conversationType) {
			case SINGLE_CONVERSATION:
	            //Get first name of friend
	            String friendName = (String) getActivity().getActionBar().getTitle();            
	            String arr[] = friendName.split(" ", 2);
	            friendName = arr[0];
	            //Insert friends first name into the tip text
	            theTip = theTip.replace("(name)", friendName);
				break; 
			case GROUP_CONVERSATION:
	            //Group specific for tip text
	            theTip = theTip.replace("(name)", getString(R.string.the_member));
				break;
            }
 
            new AlertDialog.Builder(getActivity())
            .setTitle(getString(R.string.tips))
            .setMessage(theTip)
            .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) { 
                	
                    //Now user har seen tooltip
                    Editor edit = prefs.edit();       
                    edit.putBoolean(Keys.Preferences.FIRST_CONVERSATION, false);
                    edit.apply();
                }
             })
            .setIcon(R.drawable.ic_ping_alert)
             .show();
		}
	}
    
	private void sendEmoticon(int iconId) {
    	String iconName = getResources().getResourceEntryName(iconId);
    	
    	//modify iconName for sending to Outbox
    	iconName += ".png";
    	iconName = Character.toUpperCase(iconName.charAt(0)) + iconName.substring(1);
    	
    	//Quick, dirty way for now, for compatibility with iOS version
    	iconName = iconName.replace("omg", "OMG");
    	iconName = iconName.replace("wtf", "WTF");
        
        Message msg = null; 
        String messageType = null;
        
		switch (conversationType) {
		case SINGLE_CONVERSATION:
			msg = new Message(userTag, iconName, new Date().getTime(),
					conversationID, -1, sendToTag);
			messageType = Keys.Payload.ICON;
			break;
		case GROUP_CONVERSATION:
			msg = new Message(userTag, iconName, new Date().getTime(), -1,
					conversationID, sendToTag);
			messageType = Keys.Payload.GROUP_ICON;
			break;
		}

		Map<String, Object> options = createOptions();

        Log.v(TAG, "Options = " + options);

        HashMap<String, Object> payload = new HashMap<String, Object>();
        payload.put(messageType, iconName);
        //Log.v(TAG, "Sending to " + sendToTag + ": " + payload);

        Outbox.put(sendToTag, payload, options);

        updateDatabaseAndCursor(msg);
        mMessagesListView.setSelection(mMessagesListView.getCount() - 1);
	}

    
	private void editConversationTitle() {
 
		switch (conversationType) {
		case SINGLE_CONVERSATION:
			//Get friends name
	        String name = convFriend.getName();
	        
	        //Set title
	        getActivity().getActionBar().setTitle(name);
			break;
		case GROUP_CONVERSATION:

			mMessageDb.open();
			String groupName = mMessageDb.getGroupName(conversationID);
			mMessageDb.close();
			getActivity().getActionBar().setTitle(groupName);

			break;
		}
	}

    public OnClickListener getOnClickListener() {
        return new OnClickListener() {

            @Override
            public void onClick(View v) {
            	switch(v.getId()){
            	case R.id.button_penguins:
            		
            		if(emoticonGridView.getVisibility() == View.GONE){
                		emoticonGridView.setVisibility(View.VISIBLE);
                		mPenguinBtn.setTextColor(getResources().getColor(R.color.PingPal_Grey));
            		}
            		else{
                		emoticonGridView.setVisibility(View.GONE);
                		mPenguinBtn.setTextColor(getResources().getColor(R.color.PingPal_Green));
            		}
            		
            		break;
            	case R.id.button_send:
                    processMessage();
            		break;
            	}
            }

        };
    }

    /**
     * builds a Listener to handle the return button press on the soft keyboard
     *
     * @return OnEditorActionListener
     */
    private OnEditorActionListener getEditorActionListener() {
        OnEditorActionListener listener = new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND
                        || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    processMessage();
                    handled = true;
                }

                return handled;
            }

        };
        return listener;
    }
    
    private TextWatcher getTextWatcher(){
    	TextWatcher textWatcher = new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				
				HashMap<String, Object> payload = new HashMap<String, Object>();
				
				//Tell friend when the user is writing
				if (s.length() > 0) {
					payload.put("writing", true);
				}
				else {		   
					payload.put("writing", false);
				}

				 Outbox.put(sendToTag, payload);
			}
			
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void afterTextChanged(Editable arg0) {
				// TODO Auto-generated method stub
				
			}
		};
		return textWatcher;
    	
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        String conversationId = String.valueOf(args.getLong(KEY_CONVERSATION_ID));

        switch (id) {

            case MESSAGE_LOADER:
                //Log.v(TAG, "Attempting to load conversation number " + conversationId);
                String[] params = new String[] {
                		String.valueOf(conversationId)
                };

        		switch (conversationType) {
				case SINGLE_CONVERSATION:
                    return new DatabaseCursorLoader(getActivity(),
                            DatabaseHelper.QUERY_CONVERSATION_BY_ID, params);
				case GROUP_CONVERSATION:
                    return new DatabaseCursorLoader(getActivity(),
                            DatabaseHelper.QUERY_GROUP_CONVERSATION_BY_ID, params);
        		
        		}

            default:
                Log.v(TAG, "An invalid id was passed into onCreateLoader, createLoader failed!");
                throw new IllegalArgumentException(
                        "An invalid id was passed into onCreateLoader, createLoader failed!");
        }

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

    	if(!data.isClosed()){
            mAdapter = new MessageListAdapter(getActivity(), data);
            mMessagesListView.setAdapter(mAdapter);
            mMessagesListView.setSelection(mMessagesListView.getCount() - 1);
    	}
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        loader = null;

    }

	protected void processMessage() {

    	String message = mMessageTxt.getText().toString();
	        
        Message msg = null;
			
        String messageType = null;
        
		switch (conversationType) {
		case SINGLE_CONVERSATION:
			messageType = Keys.Payload.MESSAGE;
			msg = new Message(userTag, message, new Date().getTime(),
					conversationID, -1, sendToTag);
			break;
		case GROUP_CONVERSATION:
			messageType = Keys.Payload.GROUP_MESSAGE;
			msg = new Message(userTag, message, new Date().getTime(), -1,
					conversationID, sendToTag);
			break;
		}
  
        if (!(message == null) && !(message.length() < 1)) {
            
        	Map<String, Object> options = createOptions();

            HashMap<String, Object> payload = new HashMap<String, Object>();
            payload.put(messageType, message);
            //Log.v(TAG, "Options is: " + options + " sendto:" + sendToTag);
            
			Outbox.put(sendToTag, payload, options);

            updateDatabaseAndCursor(msg);
            mMessagesListView.setSelection(mMessagesListView.getCount() - 1);
            mMessageTxt.setText("");
        }

    }

    private void updateDatabaseAndCursor(Message msg) {
        try {
            mMessageDb.open();
            mMessageDb.addMessage(msg);
            mMessageDb.close();
            updateMessagesListView();

        } catch (Exception e) {
            e.printStackTrace();
            Log.v(TAG, "An error occurred");
        }
    }

    public void updateMessagesListView() {
        mMessageDb.open();
        Cursor cursor = mMessageDb.getConversation(conversationID, conversationType);
        mAdapter.changeCursor(cursor);
        mAdapter.notifyDataSetChanged();
        mMessageDb.close();
        msgReceiveAnim.stop();
        recieveMsgImage.setVisibility(View.GONE);
        mMessagesListView.setSelection(mMessagesListView.getCount() - 1);
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        
		switch (conversationType) {
		case SINGLE_CONVERSATION:
			inflater.inflate(R.menu.conversation_options, menu);
			break;
		case GROUP_CONVERSATION:
			inflater.inflate(R.menu.group_options, menu);
			break;
		default:
			break;
		}
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

       switch (item.getItemId()) {
          case R.id.quit_group:
        	 new QuitFromGroup().execute();
             return true;
          case R.id.group_members:
        	 ((MainActivity)getActivity()).onGroupInfoSelected(sendToTag);
             return true;
          case R.id.ping:
        	  sendPing();
        	  return true;
          default:
             return super.onOptionsItemSelected(item);
       }
    }

    private void sendPing() {
    	
    	Map<String, Object> options = createOptions();
        
        Outbox.Inbox pingInbox = new Outbox.Inbox() {
			
			@SuppressWarnings("unchecked")
			@Override
			public void call(Map<String, Object> payload, Map<String, Object> options,
					Outbox outbox) {
				
				Map<String, Object> location = (Map<String, Object>) payload.get("location");
				
				double latitude = (Double) location.get("latitude");
				double longitude = (Double) location.get("longitude");
				double altitude = (Double) location.get("altitude");
				double accuracy = (Double) location.get("horizontalAccuracy");
				double speed = (Double) location.get("speed");
				double course = (Double) location.get("course");				
				String fromTag = (String) options.get("from");
				
				String locationString = "@locData:" +  latitude + ":" + longitude + ":" + altitude + ":" + accuracy + ":" + speed + ":" + course;
				
				Message message = null;
				switch (conversationType) {
				case SINGLE_CONVERSATION:

					message = new Message(fromTag, locationString,
							new Date().getTime(), conversationID, -1, userTag);
					break;
				case GROUP_CONVERSATION:

					message = new Message(fromTag, locationString,
							new Date().getTime(), -1, conversationID, userTag);
					break;
				default:
					break;
				}
				
	            updateDatabaseAndCursor(message);
	            mMessagesListView.setSelection(mMessagesListView.getCount() - 1);
			}
		};
        
    	Manager.getDevicePosition(sendToTag, 0, 30, pingInbox, options);
    	Toast.makeText(mContext, getString(R.string.ping_sent), Toast.LENGTH_SHORT).show();
    }
    
    private class QuitFromGroup extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			
			String URI = CreateGroupsFragment.removeMember;		

			String encodedUserTag = CreateGroupsFragment.encodeToUTF8(userTag);
			String encGroupTag = CreateGroupsFragment.encodeToUTF8(sendToTag);
			
			URI = URI.replace("thename", encodedUserTag);
			URI = URI.replace("thetag", encGroupTag);

			HttpClient httpclient = new DefaultHttpClient();
			HttpResponse response;
			
			try {
				response = httpclient.execute(new HttpGet(URI));
				
				StatusLine statusLine = response.getStatusLine();
				
			    if(statusLine.getStatusCode() == HttpStatus.SC_OK){
			    	
			    	//stop listening 
			    	Outbox.unsubscribeTag(userTag, sendToTag);
			    }
			    
		    	//also remove group from db
		    	mMessageDb.open();
		    	mMessageDb.removeGroup(sendToTag);
		    	mMessageDb.close();
		    	
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
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			
			((MainActivity)getActivity()).switchFragment(MainActivity.CASE_GROUP);
		}
    }
    
    @SuppressWarnings("unchecked")
	private Map<String, Object> createOptions(){
    	
    	ObjectMapper mapper = new ObjectMapper();
    	
    	String pushOptions = "{\"apns\":{\"data\":{\"aps\":{\"alert\":\"Message!\"}}},\"gcm\":{\"data\":{\"alert\":\"Message!\"}},\"mode\":\"fallback\"}";
    	
    	Map<String, Object> optionsMapped = null;
        try {
			optionsMapped = mapper.readValue(pushOptions, HashMap.class);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
        Map<String, Object> options = new HashMap<String, Object>();
        options.put(Keys.Options.PUSH, optionsMapped);
        options.put(Keys.Options.TO, sendToTag);
        options.put(Keys.Options.FROM, userTag);
        
        return options;
    }
}



