
package io.pingpal.database;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.pingpal.models.Person;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class FriendsDataSource {

    @SuppressWarnings("unused")
	private static final String TAG = FriendsDataSource.class.getSimpleName();

    public static final int CASE_APP_USER = 0;

    public static final int CASE_FACEBOOK_FRIEND = 1;

    public static final int CASE_OTHER_FRIEND = 2;

    public static final String QUERY_ACTIVE_FRIENDS = 
    		"SELECT " + DatabaseHelper.TABLE_FRIENDS + "." + DatabaseHelper.COLUMN_FRIEND_TAG + ", " 
    		+ DatabaseHelper.COLUMN_NAME 
    		+ " FROM " + DatabaseHelper.TABLE_FRIENDS 
    		+ " JOIN " + DatabaseHelper.TABLE_USERS_FRIENDS 
    		+ " ON " + DatabaseHelper.TABLE_USERS_FRIENDS + "." + DatabaseHelper.COLUMN_FRIEND_TAG 
    		+ " = " + DatabaseHelper.TABLE_FRIENDS + "." + DatabaseHelper.COLUMN_FRIEND_TAG 
    		+ " WHERE " + DatabaseHelper.COLUMN_USER_TAG + " = (?)";

    private String[] mAllFriendsColumns = {
            "ID", DatabaseHelper.COLUMN_FRIEND_TAG, DatabaseHelper.COLUMN_NAME, DatabaseHelper.COLUMN_PING
    };


    private DatabaseHelper mDbHelper;

    public FriendsDataSource(Context context) {
        mDbHelper = new DatabaseHelper(context);
    }

    private SQLiteDatabase mDb;

    public void open() throws SQLException {
        mDb = mDbHelper.getWritableDatabase();
    }

    public void close() {
        mDbHelper.close();
    }

    public Person getFriend(String friendTag) {
        String[] selectionArgs = {
                friendTag
        };
        Person friend = null;
       
        Cursor cursor = mDb.query(DatabaseHelper.TABLE_FRIENDS, mAllFriendsColumns,
                DatabaseHelper.COLUMN_FRIEND_TAG + " = ?", selectionArgs, null, null, null, null);
        if(cursor.getCount() > 0){
            cursor.moveToFirst();
            friend = new Person(cursor.getString(1), cursor.getString(2));
        }
        
        cursor.close();
        return friend;
    }
    
    public Person getFriend(int conversationID) {
        String[] selectionArgs = {
                String.valueOf(conversationID)
        };
        Person friend = null;
       
        Cursor cursor = mDb.rawQuery(DatabaseHelper.QUERY_FRIEND_BY_CONVERSATION_ID, selectionArgs);
        
        if(cursor.getCount() > 0){
        	
            cursor.moveToFirst();
            friend = new Person(cursor.getString(0), cursor.getString(1));
        }

        cursor.close();
        return friend;
    }
    
    public int getFriendPingAccess(String friendTag) {
        String[] selectionArgs = {
        		friendTag
        };
        
        String[] selection = {
                DatabaseHelper.COLUMN_PING
        };
        int pingAccess = -1;

        Cursor cursor = mDb.query(DatabaseHelper.TABLE_FRIENDS, selection,
                DatabaseHelper.COLUMN_FRIEND_TAG + " = ?", selectionArgs, null, null, null, null);
        if(cursor.getCount() > 0){
            cursor.moveToFirst();

            pingAccess = cursor.getInt(0);
        }

        cursor.close();;
        
        return pingAccess;
    }
    
    public void setFriendPingAccess(String friendTag, int pingAccess) {
        String[] selectionArgs = {
        		friendTag
        };
        
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_PING, pingAccess);
        
        mDb.update(DatabaseHelper.TABLE_FRIENDS, values, DatabaseHelper.COLUMN_FRIEND_TAG + " = ?", selectionArgs);
    }

    public String addPersonToDB(Person person, int friendCase) {
    	
        //ContentValues values;
        
        switch (friendCase) {
            case CASE_APP_USER:
            	Person user = person;
            	
            	DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
            	Date date = new Date();

                final String insertAppUser = 
                
                "INSERT INTO " + DatabaseHelper.TABLE_USERS + " ("
                + DatabaseHelper.COLUMN_USER_TAG + ", " 
                + DatabaseHelper.COLUMN_TAG + ", "
        		+ DatabaseHelper.COLUMN_NAME + ", "
        		+ DatabaseHelper.COLUMN_LAST_LOGIN + ")"
        		+ " SELECT '" 
                + user.getTag() + "', '"
                + user.getUserTag() + "', '" 
                + user.getName() + "', "
                + dateFormat.format(date)
                + " WHERE NOT EXISTS ( SELECT 1 FROM "
                + DatabaseHelper.TABLE_USERS + " WHERE " + DatabaseHelper.COLUMN_USER_TAG
                + " = '" + user.getTag() + "')";

                mDb.execSQL(insertAppUser);
                //Log.v(TAG, "Adding user if not exist: " + user.getTag() + user.getName());

                return user.getTag();

            case CASE_FACEBOOK_FRIEND:
            	
            	Person friend = person;
                final String insertFacebookFriend = 
	                "INSERT INTO " + DatabaseHelper.TABLE_FRIENDS + " (" 
	                + DatabaseHelper.COLUMN_FRIEND_TAG + ", "
	                + DatabaseHelper.COLUMN_NAME + ", "
	                + DatabaseHelper.COLUMN_PING
	                + ") SELECT '" 
	                + friend.getTag() + "', '" 
	                + friend.getName() + "', "
	                + DatabaseHelper.FALSE
	                + " WHERE NOT EXISTS ( SELECT 1 FROM "
	                + DatabaseHelper.TABLE_FRIENDS + " WHERE "
	                + DatabaseHelper.COLUMN_FRIEND_TAG + " = '" + friend.getTag() + "');";

                mDb.execSQL(insertFacebookFriend);
                
                //Log.v(TAG, "Inserted friend: " + friend.getTag() + friend.getName());
                return friend.getTag();

            default:
                throw new IllegalArgumentException(
                        "An invalid friendCase was passed into addFriend() in FriendsDataSource, addFriend() failed!");
        }
    }
    
    public void addRelation(String userTag, String friendTag){
        final String insertUserFriendRelation = 
        		"INSERT INTO " + DatabaseHelper.TABLE_USERS_FRIENDS + " SELECT '"
        		+ userTag + "', '" + friendTag
        		+ "' WHERE NOT EXISTS ( SELECT 1 FROM "
                + DatabaseHelper.TABLE_USERS_FRIENDS 
                + " WHERE " + DatabaseHelper.COLUMN_USER_TAG + " = '" + userTag 
                + "' AND " + DatabaseHelper.COLUMN_FRIEND_TAG + " = '" + friendTag + "')";
        
        mDb.execSQL(insertUserFriendRelation);
    }
}
