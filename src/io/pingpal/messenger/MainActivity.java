
package io.pingpal.messenger;

import io.pingpal.database.FriendsDataSource;
import io.pingpal.database.MessagesDataSource;
import io.pingpal.fragments.ConversationFragment;
import io.pingpal.fragments.ConversationListFragment;
import io.pingpal.fragments.CreateGroupsFragment;
import io.pingpal.fragments.FacebookFragment;
import io.pingpal.fragments.FriendsFragment;
import io.pingpal.fragments.GoogleMapsFragment;
import io.pingpal.fragments.GroupInfoFragment;
import io.pingpal.fragments.GroupsFragment;
import io.pingpal.fragments.NavigationDrawerFragment;
import io.pingpal.fragments.SettingsFragment;
import io.pingpal.location.Manager;
import io.pingpal.outbox.Outbox;

import java.util.Map;

import android.app.ActionBar;
import android.app.ActionBar.LayoutParams;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

/**
 * @author Paul Mallon & Robin Dahlström 21-07-14
 */

public class MainActivity extends FragmentActivity implements
NavigationDrawerFragment.NavigationDrawerCallbacks,
ConversationListFragment.ConversationSelectedCallbacks,
FriendsFragment.FriendSelectedCallbacks, ActionBar.OnNavigationListener {

    public static final int CASE_CONVERSATION_LIST = 0, 
    		CASE_FRIENDS = 1, 
    		CASE_GROUP = 2, 
    		CASE_GROUP_MANAGE = 3, 
    		CASE_FACEBOOK = 4,  
    		CASE_SETTINGS = 5;

    @SuppressWarnings("unused")
	private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * The fragment argument representing the section a Fragment fragmentshould
     * be placed in
     */
    public static final String ARG_SECTION_NUMBER = "section_number";

    public static final String EXTRA_MESSAGE = "message";

    public static final String EXTRA_FROM = "from";

    public static final String PROPERTY_REG_ID = "registration_id";

    private NavigationDrawerFragment mNavigationDrawerFragment;

    private String deviceTag;

    private Communications mComms;

    private ConversationFragment mConversationFragment;

    private boolean mConversationActive = false;

    /**
     * Used to store the last screen title. For use in
     * {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    private String notificationId;

    private boolean notificationReceived = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);
        mNavigationDrawerFragment = (NavigationDrawerFragment)getSupportFragmentManager()
                .findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer,
                (DrawerLayout)findViewById(R.id.drawer_layout));
        mComms = new Communications(this);

        getFriendIdFromIntent();
        
        Manager.Setup(getApplicationContext());   
         
        Manager.AccessHandler accessHandler = new Manager.AccessHandler() {

			@Override
			public void isItOkToProced(final Map<String, Object> payload,
					final Map<String, Object> options, final Callback callback) {
				
				//Get from tag
				String fromTag = (String) options.get("from");
				
				FriendsDataSource db = new FriendsDataSource(MainActivity.this);
				int pingAccess;
				String name = "";
				
				//Check if ping is from user (group ping)
				if(fromTag.equals(getUserTag())){
					pingAccess = 2; //Don't allow/Ignore
				}
				else{
					//Get name using tag
					db.open();
					name = db.getFriend(fromTag).getName();
					
			        //Get pingaccess for friend
					pingAccess = db.getFriendPingAccess(fromTag);
				}		

				db.close();
				
				switch (pingAccess) {
				case 0: //Ask
					new AlertDialog.Builder(MainActivity.this)
					.setTitle(getString(R.string.location_request))
					.setMessage(name + " " + getString(R.string.wants_location))
					.setPositiveButton(R.string.allow,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {

									callback.call(payload, options,
											callback);
								}
							})
					.setNegativeButton(R.string.not_allow,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									// do nothing
								}
							}).setIcon(R.drawable.ic_ping_alert).show();
					break;
				case 1: //Allow
					callback.call(payload, options,
							callback);
					break;
				case 2:
					//Don't allow
					break;
				}			
			}
		};
           
        Manager.setAccesshandler(accessHandler);   
    }

    /**
     * Checks an Intent for bundled extras and extracts data to class members
     */
    private void getFriendIdFromIntent() {
        Intent intent = getIntent();
        Bundle extras = null;
        String from = null;

        if (intent != null) {
            extras = intent.getExtras();
        } else {
            return;
        }

        if (extras != null) {
            from = extras.getString(EXTRA_FROM);
        } else {
            return;
        }

        if (from != null) {
            notificationId = from;
            notificationReceived = true;
        } else {
            return;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        
        mComms.checkPlayServices();
        
        if(isLoggedIn()){
        	//Log.v(TAG, "Starting outbox");
        	mComms.setupApptimateOutbox();
        	
            SharedPreferences sp = getPreferences(Context.MODE_PRIVATE);
            
            String startTicket = sp.getString("ticket", "1");
            
            Outbox.startHistory(startTicket , new Outbox.Callback() {
                public void call(Throwable error, Object... args) {
                	
                    SharedPreferences sp = getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sp.edit();
                    
                    editor.putString("ticket", (String) args[0]);
                    editor.commit();
                }
            });
        }
        
        if (notificationReceived) {
        	
            //Create conversation if not exist
            int conversationId = getConversationID(notificationId);
        	
            onConversationSelected(conversationId);
            notificationReceived = false;
        }
        
        initializeImageLoader();
    }

    private void initializeImageLoader() {
        if (!ImageLoader.getInstance().isInited()) {

            ImageLoaderConfiguration.Builder configBuilder = new ImageLoaderConfiguration.Builder(
                    this);
            ImageLoaderConfiguration config = configBuilder.build();
            ImageLoader.getInstance().init(config);
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        switchFragment(position);
    }

    public void switchFragment(int position) {

        // update the main content by replacing fragments
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

        //Log.v(TAG, "Selected Position: " + position);
        switch (position) {

            case CASE_CONVERSATION_LIST:
                fragmentTransaction.replace(R.id.container_main,
                        ConversationListFragment.newInstance(position + 1));
                mConversationActive = false;
                break;

            case CASE_FRIENDS:
                fragmentTransaction.replace(R.id.container_main,
                        FriendsFragment.newInstance(position + 1));
                mConversationActive = false;
                break;

            case CASE_GROUP:
                fragmentTransaction.replace(R.id.container_main,
                        GroupsFragment.newInstance(position + 1));
                mConversationActive = false;
                break;

            case CASE_GROUP_MANAGE:
                fragmentTransaction.replace(R.id.container_main,
                        CreateGroupsFragment.newInstance(position + 1));
                mConversationActive = false;
                break;

            case CASE_FACEBOOK:
                fragmentTransaction.replace(R.id.container_main,
                        FacebookFragment.newInstance(position + 1));
                mConversationActive = false;
                break;

            case CASE_SETTINGS:
                fragmentTransaction.replace(R.id.container_main, 
                		SettingsFragment.newInstance(position + 1));
                mConversationActive = false;
                break;
        }

        fragmentTransaction.commit();
    }

    /**
     * Update the ActionBar title on selection of an option from the navigation
     * drawer
     *
     * @param number
     */
    public void onSectionAttached(int number) {

        switch (number) {
            // TODO: Implement constants here
            case 1:
                mTitle = getString(R.string.title_messages);
                break;
            case 2:
                mTitle = getString(R.string.title_friends);
                break;
            case 3:
                mTitle = getString(R.string.title_groups);
                break;
            case 4:
                mTitle = getString(R.string.title_groups_manage);
                break;
            case 5:
                mTitle = getString(R.string.title_facebook);
                break;
            case 6:
                mTitle = getString(R.string.title_contacts);
                break;
            case 7:
                mTitle = getString(R.string.title_settings);
                break;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (!mNavigationDrawerFragment.isDrawerOpen()) {

            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();

            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        
        //so not to change conversation name
        if(!mConversationActive){
        	actionBar.setTitle(mTitle);
        }
        
        actionBar.setDisplayShowCustomEnabled(true);
        View view = LayoutInflater.from(actionBar.getThemedContext()).inflate(
                R.layout.action_bar_custom, null);
        ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(
                ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(layoutParams);
        Button btn = (Button)view.findViewById(R.id.btn_new_msg);

        buildNewMessageMenu(btn);
        btn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                newMessageMenu.show();

            }

        });
        
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.END | Gravity.CENTER_VERTICAL);
        actionBar.setCustomView(view, lp);
        
    }
    
    PopupMenu newMessageMenu;

    private void buildNewMessageMenu(Button btn) {
        //View btnNewMsg = findViewById(R.id.btn_new_msg);
        newMessageMenu = new PopupMenu(getApplicationContext(), btn);
        MenuInflater inflater = newMessageMenu.getMenuInflater();
        inflater.inflate(R.menu.new_message_options, newMessageMenu.getMenu());
        newMessageMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
        	
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {

                    case R.id.message:
                        switchFragment(CASE_FRIENDS);
                        break;

                    case R.id.message_group:
                        switchFragment(CASE_GROUP);
                        break;

                }
                return false;
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO: Implement Options Menu
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
        	getActionBar().setTitle(getString(R.string.title_settings));
        	switchFragment(CASE_SETTINGS);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConversationSelected(int conversationId) {

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        mConversationFragment = ConversationFragment.newInstance((int) conversationId, ConversationFragment.ConversationType.SINGLE_CONVERSATION);
        fragmentTransaction.replace(R.id.container_main, mConversationFragment)
        .addToBackStack(ConversationListFragment.class.getSimpleName()).commit();
        mConversationActive = true;
    }
    
    public void onMapSelected(Location location) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        GoogleMapsFragment mapFragment = new GoogleMapsFragment();
        mapFragment.setLocation(location);
        fragmentTransaction.replace(R.id.container_main, mapFragment)
        .addToBackStack(GoogleMapsFragment.class.getSimpleName()).commit();
    }
    
    public void onGroupInfoSelected(String groupTag) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

        fragmentTransaction.replace(R.id.container_main, GroupInfoFragment.newInstance(groupTag))
        .addToBackStack(GroupInfoFragment.class.getSimpleName()).commit();
    } 
    
    public void onGroupSelected(int groupID) {
    	
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        mConversationFragment = ConversationFragment.newInstance(groupID, ConversationFragment.ConversationType.GROUP_CONVERSATION);
        fragmentTransaction.replace(R.id.container_main, mConversationFragment)
        .addToBackStack(ConversationListFragment.class.getSimpleName()).commit();
        mConversationActive = true;
    }

    public Communications getCommunications() {
        return mComms;
    }

    @Override
    public void onFriendSelected(int conversationID) {

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        mConversationFragment = ConversationFragment.newInstance(conversationID, ConversationFragment.ConversationType.SINGLE_CONVERSATION);
        fragmentTransaction.replace(R.id.container_main, mConversationFragment)
        .addToBackStack(ConversationListFragment.class.getSimpleName()).commit();
        mConversationActive = true;
    }

    public void updateConversationFragment() {
        if (mConversationActive) {
            mConversationFragment.updateMessagesListView();
        }
    }

    public String getDeviceTag() {
        return deviceTag;
    }

    public void setDeviceTag(String deviceTag) {
        this.deviceTag = deviceTag;
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        Toast.makeText(this, "You Selected: " + itemPosition, Toast.LENGTH_SHORT).show();
        return false;
    }
    
    /**
     * Gets the conversation id for the conversation between the user and the selected friend.
     * If no conversation is found, it will be created.
     * 
     * @param  friendTag ID of the friend of the conversation
     * @return The conversation ID
     */
    public int getConversationID(String friendTag){
        String userTag = getUserTag();
        MessagesDataSource db = new MessagesDataSource(this);
        db.open();
        int conversationID = db.addConversation(userTag, friendTag);      
        db.close();
        
        return conversationID;
    }
    
    public String getUserTag(){
        SharedPreferences sp = getPreferences(Context.MODE_PRIVATE);
        return  sp.getString(Keys.Preferences.USER_TAG, "");
    }
    
    /**
     * Returns whether or not user is logged in.
     * @return true if user is logged in
     */
    public boolean isLoggedIn(){
        SharedPreferences sp = getPreferences(Context.MODE_PRIVATE);
        return sp.getBoolean(Keys.Preferences.LOGGED_IN, false);
    }
    
}
