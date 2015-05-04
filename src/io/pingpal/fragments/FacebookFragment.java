
package io.pingpal.fragments;

import io.pingpal.abstractclasses.BaseActivity;
import io.pingpal.database.FriendsDataSource;
import io.pingpal.messenger.Communications;
import io.pingpal.messenger.Keys;
import io.pingpal.messenger.MainActivity;
import io.pingpal.messenger.R;
import io.pingpal.models.Person;
import io.pingpal.outbox.Outbox;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

/**
 * @author Paul Mallon & Robin Dahlström 22-07-2014
 */

public class FacebookFragment extends Fragment {

    /**
     * Use this in conjunction with IMG_URL_END to build a full URL with which
     * you can download a users profile picture. The format is IMG_URL_START +
     * Facebook ID + IMG_URL_END;
     */
    public static final String IMG_URL_START = "https://graph.facebook.com/";

    /**
     * Use this in conjunction with IMG_URL_START to build a full URL with which
     * you can download a users profile picture. The format is IMG_URL_START +
     * Facebook ID + IMG_URL_END;
     */
    public static final String IMG_URL_END = "/picture?type=normal";
    public static final String IMG_URL_END_LARGE = "/picture?type=large";

    public static final String FACEBOOK_NAMES = "facebook_names";

    public static final String FACEBOOK_IDS = "facebook_ids";

    public static final String FACEBOOK_IMAGE_URLS = "facebook_image_paths";

    public static final String IMAGE_CACHE_DIRECTORY = "AppDir/cache/images";
    
    /**
     * The fragment argument representing the section number for this fragment.
     * TODO: Move Facebook code to Facebook class, call from Communications
     * class
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    @SuppressWarnings("unused")
	private static final String TAG = FacebookFragment.class.getSimpleName();
    
    private String userTag;

    //private static final int FACEBOOK_LOADER = 2;
    
    private int viewRotation = -13;
    

    /**
     * Returns a new instance of this fragment for the given section number.
     */
    public static FacebookFragment newInstance(int sectionNumber) {
        FacebookFragment fragment = new FacebookFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    private UiLifecycleHelper uiHelper;

    private Session.StatusCallback callback = new Session.StatusCallback() {

        @Override
        public void call(Session session, SessionState state, Exception exception) {
        	
        	if(!session.isOpened()){
                onSessionStateChange(session, state, exception);
        	}
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiHelper = new UiLifecycleHelper(getActivity(), callback);
        uiHelper.onCreate(savedInstanceState);
        if(!((MainActivity)getActivity()).isLoggedIn()){
            getActivity().getActionBar().hide();
        }

        // mComms = (Communications)
        // getArguments().getSerializable(Communications.KEY_COMMUNICATIONS);
        // checkMessagesDb();

        userTag = ((MainActivity)getActivity()).getUserTag();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onResume() {
        super.onResume();
        Session session = Session.getActiveSession();

        if (session != null && (session.isOpened() || session.isClosed())) {
        	if(!((MainActivity)getActivity()).isLoggedIn()){
                onSessionStateChange(session, session.getState(), null);
        	}
        }  
        uiHelper.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    private TextView mUserNameLbl;
    
    private TextView mWelcomeText;

    private ImageView mUserImg, mUserImgShadow;
    
    private ImageView mWelcomeImage, mLayerImage;

    private static final String[] PERMISSIONS = new String[] {
        "public_profile", "email", "user_friends"
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_facebook, container, false);

        mUserImg = (ImageView)rootView.findViewById(R.id.icon_facebook_fragment);
        mUserImgShadow = (ImageView)rootView.findViewById(R.id.icon_shadow);
        mUserNameLbl = (TextView)rootView.findViewById(R.id.label_name_facebook_fragment);
        mWelcomeImage = (ImageView)rootView.findViewById(R.id.welcome_imageview);
        mLayerImage = (ImageView)rootView.findViewById(R.id.welcome_layer);
        mWelcomeText = (TextView)rootView.findViewById(R.id.welcome_text);
        
        BaseActivity.imageLoader.init(ImageLoaderConfiguration.createDefault(getActivity()
                .getBaseContext()));
        
        ImageLoader.getInstance().displayImage( "drawable://" + R.drawable.top_image,
                mWelcomeImage);
       
        if(((MainActivity)getActivity()).isLoggedIn()){
            SharedPreferences sp = getActivity().getPreferences(Context.MODE_PRIVATE);
        	String userName = sp.getString("user_name", "");
        	
        	String userID = userTag.replace("#", "");
            ImageLoader.getInstance().displayImage(IMG_URL_START + userID + IMG_URL_END_LARGE,
                    mUserImg, mImageOptions);
            ImageLoader.getInstance().displayImage("drawable://" + R.drawable.icon_back_white,
            		mUserImgShadow, mImageOptions);
            
        	mWelcomeText.setText(R.string.welcome_text);
        	mUserNameLbl.setText(getString(R.string.hello) + "\r\n" + userName + "!");
            mUserNameLbl.setRotation(viewRotation);
        	mLayerImage.setVisibility(View.GONE);
            mUserImg.setRotation(viewRotation);
        }
        else{
        	mLayerImage.setVisibility(View.VISIBLE);
            ImageLoader.getInstance().displayImage( "drawable://" + R.drawable.ping_thumbs_up_h100,
            		mUserImg);
            ImageLoader.getInstance().displayImage("drawable://" + R.drawable.icon_back_blue,
            		mUserImgShadow, mImageOptions);
            mUserImg.setRotation(viewRotation);
            ImageLoader.getInstance().displayImage( "drawable://" + R.drawable.layer_image,
            		mLayerImage);
        }  

        // Start Facebook Login
        LoginButton authButton = (LoginButton)rootView.findViewById(R.id.authButton);
        authButton.setFragment(this);
        authButton.setReadPermissions(PERMISSIONS);

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity)activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
    }
    
    private Communications mComms;

    /**
     * Verifies the Facebook is open, and saves the user profile and friends
     * list
     *
     * @param session
     * @param state
     * @param exception
     */
    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
        if (state.isOpened()) {
            saveAppUserProfile(session);        
            
            getActivity().getActionBar().show();
            mLayerImage.setVisibility(View.GONE);

            String test = getString(R.string.loggedin_text) + "\r\n" + "\u27A1 \ud83d\udc46 " ;
            mWelcomeText.setText(test);
            
            

        } else if (state.isClosed()) {
        	
            //Log.v(TAG, "Logged out...");

            SharedPreferences sp = getActivity().getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean(Keys.Preferences.LOGGED_IN, false);
            editor.commit();
            
            getActivity().getActionBar().hide();
            mLayerImage.setVisibility(View.VISIBLE);
            mWelcomeText.setText(R.string.welcome_text);
            mComms = ((MainActivity)getActivity()).getCommunications();    
            
            mComms.resetComms();
            
            ImageLoader.getInstance().displayImage( "drawable://" + R.drawable.ping_thumbs_up_h100,
            		mUserImg);
            ImageLoader.getInstance().displayImage("drawable://" + R.drawable.icon_back_blue,
            		mUserImgShadow, mImageOptions);
            mUserImg.setRotation(viewRotation);
            mUserNameLbl.setText("");
            //Temporary
            ImageLoader.getInstance().displayImage( "drawable://" + R.drawable.layer_image,
            		mLayerImage);
        }
    }

    private DisplayImageOptions mImageOptions = new DisplayImageOptions.Builder()
    .cacheInMemory(true).cacheOnDisk(true).displayer(new RoundedBitmapDisplayer(180))
    .build();

    /**
     * Requests a GraphUser object for the application user from the Facebook
     * Graph API and saves the result to the database.
     *
     * @param session
     */

    private void saveAppUserProfile(final Session session) {
        //Log.v(TAG, "Logged in...");
        //Log.v(TAG, "Requesting profile information...");
        Request.newMeRequest(session, new Request.GraphUserCallback() {

            @Override
            public void onCompleted(GraphUser user, Response response) {
                //Log.v(TAG, "USER DETAILS: ");
                //Log.v(TAG, user.getFirstName());
                //Log.v(TAG, user.getLastName());
                //Log.v(TAG, user.getId());
                
                //Save user info
                SharedPreferences sp = getActivity().getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                
                userTag = "#" + user.getId();
                
                editor.putString(Keys.Preferences.USER_TAG, userTag);
                editor.putString(Keys.Preferences.USER_NAME, user.getFirstName());
                editor.putBoolean(Keys.Preferences.LOGGED_IN, true);
                editor.commit();

                if (mComms == null) {
                    mComms = ((MainActivity)getActivity()).getCommunications();
                }
                mComms.setupApptimateOutbox();

                mUserNameLbl.setText(getString(R.string.hello) + "\r\n" + user.getFirstName() + "!");
                mUserNameLbl.setRotation(viewRotation);
                ImageLoader.getInstance().displayImage(IMG_URL_START + user.getId() + IMG_URL_END_LARGE,
                        mUserImg, mImageOptions);
                ImageLoader.getInstance().displayImage("drawable://" + R.drawable.icon_back_white,
                		mUserImgShadow, mImageOptions);
                mUserImg.setRotation(viewRotation);
                FriendsDataSource db = new FriendsDataSource(getActivity());
                db.open();
                Person mUser = new Person(userTag, Outbox.hashIfTag(userTag), user.getFirstName());
                db.addPersonToDB(mUser, FriendsDataSource.CASE_APP_USER);
                
                db.close();
                
                saveFacebookFriends(session);
            }
        }).executeAsync();
    }

    private ArrayList<String> imageURLs = new ArrayList<String>();

    private ArrayList<String> facebookIds = new ArrayList<String>();

    private ArrayList<String> names = new ArrayList<String>();

    /**
     * Gets a list of the application users Facebook friends and adds them to
     * the database or modifies their records with new data if any changes have
     * occurred since the last check
     *
     * @param session
     */
    private void saveFacebookFriends(Session session) {
        //Log.v(TAG, "requesting friend inforamtion...");
        new Request(session, "/me/friends", null, HttpMethod.GET, new Request.Callback() {

            @Override
            public void onCompleted(Response response) {

                GraphObject graphObject = response.getGraphObject();
                JSONObject dataSummary = graphObject.getInnerJSONObject();
                addFriendsToDatabase(dataSummary);

            }

            /**
             * Saves the application users friend information to the local
             * database
             *
             * @param dataSummary The data returned from the Facebook Graph API
             */
            private void addFriendsToDatabase(JSONObject dataSummary) {
                try {
                    JSONArray data = dataSummary.getJSONArray("data");

                    FriendsDataSource db = new FriendsDataSource(getActivity());

                    db.open();
                    for (int i = 0; i < data.length(); i++) {
                        String friendTag = "#" + data.getJSONObject(i).getString("id");

                        String name = data.getJSONObject(i).getString("name");
                        //Log.v(TAG, "FRIEND: ");
                        //Log.v(TAG, "String Tag: " + friendTag);
                        //Log.v(TAG, "Name: " + name);
                        //Log.v(TAG, "Friend to id: " + userTag);           
                        
                        db.addPersonToDB(new Person(friendTag, name), FriendsDataSource.CASE_FACEBOOK_FRIEND);
                        db.addRelation(userTag, friendTag);
                        
                        imageURLs.add(IMG_URL_START + data.getJSONObject(i).getString("id")
                                + IMG_URL_END);
                        facebookIds.add(data.getJSONObject(i).getString("id"));
                        names.add(data.getJSONObject(i).getString("name"));

                    }
                    db.close();
                } catch (JSONException e) {

                    e.printStackTrace();
                }
            }
            
        }).executeAsync();
    }
}
