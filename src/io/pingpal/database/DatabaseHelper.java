
package io.pingpal.database;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * @author Paul Mallon & Robin Dahlström 22-07-2014
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    /**
     * Table Name
     */
    public static final String TABLE_MESSAGES = "messages",
    		TABLE_FRIENDS = "friends", 
    		TABLE_USERS = "users",
    		TABLE_CONVERSATIONS = "conversations",
    		TABLE_USERS_FRIENDS = "users_friends",
    		TABLE_FACEBOOK = "facebook",
    		TABLE_GROUPS = "groups";
    
//Conversations table columns
    public static final String COLUMN_CONVERSATION_ID = "conversation_id";

//Messages table columns
    /**
     * ID of the sender of the message
     */
    public static final String COLUMN_SENDER_TAG = "sender_id";
    
    /**
     * ID of the sender of the message
     */
    public static final String COLUMN_RECEIVER_TAG = "receiver_id";
    
    /**
     * The message string
     */
    public static final String COLUMN_MESSAGE = "message";
    
    /**
     * Date and time the message was send or received
     */
    public static final String COLUMN_DATE = "date";
    
//Friends/Users table columns
    /**
     * The ID of this friend
     */
    public static final String COLUMN_FRIEND_TAG = "friend_ID";
    
    /**
     * A hashed ID representing the user, this tag is used by the Apptimate
     * servers to send/receive messages to/from users
     */
    public static final String COLUMN_TAG = "tag";
    
    /**
     * Specifies whether or not this friend/group has been deleted 1 = true
     * (deleted); 0 = false (not deleted);
     */
    public static final String COLUMN_NAME = "name";
    
    /**
     * Date and time of the users last login
     */
    public static final String COLUMN_LAST_LOGIN = "last_login";
    
    
    /**
     * Deleted, true (1) or false (0)
     */
    public static final String COLUMN_PING = "ping";
    
    /**
     * The ID of the app user
     */
    public static final String COLUMN_USER_TAG = "user_ID";

//Others
    /**
     * Boolean flag stored as integer (0 = false, 1 = true), true if the message
     * has been read, false if the message is unread
     */
    public static final String COLUMN_NEW_MESSAGE = "new_message";

    /**
     * The unique tag of the group .
     */
    public static final String COLUMN_GROUP_TAG = "group_tag";
    
    /**
     * The unique id of the group.
     */
    public static final String COLUMN_GROUP_ID = "group_id";
    
    
    
    /**
     * The unique id of the group conversation this message belongs to. Value
     * should be 0 if the message does not belong to a group conversation.
     */
    public static final String COLUMN_GROUP_NAME = "name";

    /**
     * Integer representation of Boolean true
     */
    public static final int TRUE = 1;

    /**
     * Integer representation of Boolean false
     */
    public static final int FALSE = 0;

    private static final String DATABASE_NAME = "pingpalDb";

    private static final int DATABASE_VERSION = 26;

    public static final String CREATE_TABLE_USERS = "CREATE TABLE " + TABLE_USERS + " ("
            + COLUMN_USER_TAG + " string primary key, " 
    		+ COLUMN_TAG + " string not null, " 
    		+ COLUMN_NAME + " string not null, "
    		+ COLUMN_LAST_LOGIN + " integer not null)";
    
    public static final String CREATE_TABLE_FRIENDS = "CREATE TABLE  " + TABLE_FRIENDS + " ("
    		+ "ID integer primary key autoincrement, "
    		+ COLUMN_FRIEND_TAG + " string not null, "
    		+ COLUMN_NAME + " string not null, " 
    		+ COLUMN_PING + " integer not null default 0)";
    
    public static final String CREATE_TABLE_USERS_FRIENDS = "CREATE TABLE " + TABLE_USERS_FRIENDS + " ("
    		+ COLUMN_USER_TAG + " string not null, " 
    		+ COLUMN_FRIEND_TAG + " string not null, " 
    		+ "PRIMARY KEY (" + COLUMN_USER_TAG + ", " + COLUMN_FRIEND_TAG + "))";
    
    public static final String CREATE_TABLE_CONVERSATIONS = "CREATE TABLE " + TABLE_CONVERSATIONS + " ("
            + COLUMN_CONVERSATION_ID + " integer primary key autoincrement, " 
    		+ COLUMN_USER_TAG + " string not null, " 
    		+ COLUMN_FRIEND_TAG + " string not null)";
    
    public static final String CREATE_TABLE_MESSAGES = "CREATE TABLE " + TABLE_MESSAGES + " ("
    		+ COLUMN_SENDER_TAG + " string not null, "
    		+ COLUMN_RECEIVER_TAG + " string not null, "
            + COLUMN_MESSAGE + " string not null, " 
            + COLUMN_CONVERSATION_ID + " int not null, "
            + COLUMN_GROUP_ID + " int not null, " 
            + COLUMN_DATE + " integer primary key)";
    
    public static final String CREATE_TABLE_GROUPS = "CREATE TABLE " + TABLE_GROUPS + " ("
    		+ COLUMN_GROUP_ID + " integer primary key autoincrement, "
    		+ COLUMN_GROUP_TAG + " string not null, "
            + COLUMN_GROUP_NAME + " string not null, " 
    		+ COLUMN_USER_TAG + " string not null) ";
    
    public static final String QUERY_GROUPS = "SELECT group_id, name FROM groups WHERE user_id = ?";
    
    public static final String QUERY_CONVERSATION_LIST = 
    		"SELECT " + TABLE_CONVERSATIONS + "." + COLUMN_CONVERSATION_ID + ", "
    		+ TABLE_CONVERSATIONS + "." + COLUMN_FRIEND_TAG + ", "
    		+ TABLE_FRIENDS + "." + COLUMN_NAME + ", "
    		+ COLUMN_MESSAGE + ", "
    		+ COLUMN_DATE
    		+ " FROM " + TABLE_CONVERSATIONS
    		+ " JOIN " + TABLE_FRIENDS 
    		+ " ON " + TABLE_CONVERSATIONS + "." + COLUMN_FRIEND_TAG 
    		+ " = " + TABLE_FRIENDS + "." + COLUMN_FRIEND_TAG
    		+ " JOIN " + TABLE_MESSAGES 
    		+ " ON " +  TABLE_CONVERSATIONS + "." + COLUMN_CONVERSATION_ID
    		+ " = " + TABLE_MESSAGES + "." +  COLUMN_CONVERSATION_ID
    		+ " WHERE " + COLUMN_USER_TAG + " = (?"
    			+ ") AND " + COLUMN_DATE + " = (SELECT max(date) FROM " + TABLE_MESSAGES 
    			+ " WHERE " + TABLE_MESSAGES + "." + COLUMN_CONVERSATION_ID 
    			+ " = " + TABLE_CONVERSATIONS + "." + COLUMN_CONVERSATION_ID + ")"
    		+ " GROUP BY " + TABLE_FRIENDS + "." + COLUMN_FRIEND_TAG;
    
    public static final String QUERY_CONVERSATION_BY_ID = 
    		"SELECT " + DatabaseHelper.COLUMN_SENDER_TAG + "," 
    		+ DatabaseHelper.COLUMN_MESSAGE 
    		+ " FROM " + DatabaseHelper.TABLE_MESSAGES 
    		+ " WHERE " + DatabaseHelper.COLUMN_CONVERSATION_ID + " = ?";
    
    public static final String QUERY_GROUP_CONVERSATION_BY_ID = 
    		"SELECT " + DatabaseHelper.COLUMN_SENDER_TAG + "," 
    		+ DatabaseHelper.COLUMN_MESSAGE 
    		+ " FROM " + DatabaseHelper.TABLE_MESSAGES 
    		+ " WHERE " + DatabaseHelper.COLUMN_GROUP_ID + " = ?";
    
    public static final String QUERY_CONVERSATION_COUNT = "SELECT DISTINCT "
            + DatabaseHelper.COLUMN_RECEIVER_TAG + " FROM " + DatabaseHelper.TABLE_MESSAGES;
    
    public static final String QUERY_FRIEND_BY_CONVERSATION_ID = "SELECT " 
    + DatabaseHelper.TABLE_FRIENDS + "." +  DatabaseHelper.COLUMN_FRIEND_TAG + ", "
    + DatabaseHelper.COLUMN_NAME 
    + " FROM " + DatabaseHelper.TABLE_CONVERSATIONS 
    + " JOIN " + DatabaseHelper.TABLE_FRIENDS 
    + " ON " + DatabaseHelper.TABLE_FRIENDS + "." + DatabaseHelper.COLUMN_FRIEND_TAG 
    + " = " + DatabaseHelper.TABLE_CONVERSATIONS + "." + DatabaseHelper.COLUMN_FRIEND_TAG 
    + " WHERE " + DatabaseHelper.COLUMN_CONVERSATION_ID + " = ?";
    
    public static final String QUERY_GROUP_TAGS = "SELECT " + DatabaseHelper.COLUMN_GROUP_TAG + " FROM " + DatabaseHelper.TABLE_GROUPS + "  WHERE " + DatabaseHelper.COLUMN_USER_TAG + " = ?";

    public static final long TEST_APP_USER = 10152612109244406l;

    @SuppressWarnings("unused")
	private final String TAG = this.getClass().getSimpleName();

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
        	//db.execSQL(DROP_TABLE_FRIENDS);
            db.execSQL(CREATE_TABLE_USERS);
        	db.execSQL(CREATE_TABLE_FRIENDS);
            db.execSQL(CREATE_TABLE_USERS_FRIENDS);
            db.execSQL(CREATE_TABLE_CONVERSATIONS);
            db.execSQL(CREATE_TABLE_MESSAGES);
            db.execSQL(CREATE_TABLE_GROUPS);
            //            addMessages(db);

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        //Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + "version "
        //        + newVersion + " which will destroy all old data");

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FRIENDS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONVERSATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS_FRIENDS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GROUPS);
        onCreate(db);

    }
}
