
package io.pingpal.database;

import java.util.ArrayList;
import java.util.List;

import io.pingpal.fragments.ConversationFragment;
import io.pingpal.models.Message;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class MessagesDataSource {

    @SuppressWarnings("unused")
	private static final String TAG = MessagesDataSource.class.getSimpleName();

    private SQLiteDatabase mDb;

    private DatabaseHelper mDbHelper;

    public static final String[] CONVERSATION_CURSOR_MAP = {
        DatabaseHelper.COLUMN_DATE,
        DatabaseHelper.COLUMN_RECEIVER_TAG, DatabaseHelper.COLUMN_SENDER_TAG,
        DatabaseHelper.COLUMN_MESSAGE, DatabaseHelper.COLUMN_NAME
    };

    public MessagesDataSource(Context context) {
        mDbHelper = new DatabaseHelper(context);
    }

    public void open() throws SQLException {
        mDb = mDbHelper.getWritableDatabase();
    }

    public void close() {
        mDbHelper.close();
    }
    
    /**
     * Add conversation do db if not exist
     * @param userTag
     * @param friendTag
     * @return Returns conversation ID
     */
    public int addConversation(String userTag, String friendTag){

        final String newConversation =
        		"INSERT INTO " + DatabaseHelper.TABLE_CONVERSATIONS 
        		+ " ( " + DatabaseHelper.COLUMN_USER_TAG + ", " + DatabaseHelper.COLUMN_FRIEND_TAG + ")"
        		+ " SELECT '"
        		+ userTag + "', '" + friendTag
        		+ "' WHERE NOT EXISTS ( SELECT 1 FROM "
                + DatabaseHelper.TABLE_CONVERSATIONS 
                + " WHERE " + DatabaseHelper.COLUMN_USER_TAG + " = '" + userTag
                + "' AND " + DatabaseHelper.COLUMN_FRIEND_TAG + " = '" + friendTag + "')";

        mDb.execSQL(newConversation);

        Cursor cursor = mDb.rawQuery("SELECT " + DatabaseHelper.COLUMN_CONVERSATION_ID 
        		+ " FROM " + DatabaseHelper.TABLE_CONVERSATIONS 
        		+" WHERE " + DatabaseHelper.COLUMN_USER_TAG + " = '" + userTag 
        		+ "' AND " + DatabaseHelper.COLUMN_FRIEND_TAG + " = '" + friendTag + "'", null);
        cursor.moveToFirst();
        int conversationId = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_CONVERSATION_ID));
        cursor.close();
        
        return conversationId;
    }
    
    public void addGroup(String groupTag, String groupName, String userTag){

        final String newGroup =
        		"INSERT INTO " + DatabaseHelper.TABLE_GROUPS 
        		+ " ( " + DatabaseHelper.COLUMN_GROUP_TAG + ", " + DatabaseHelper.COLUMN_GROUP_NAME + ", " + DatabaseHelper.COLUMN_USER_TAG + ")"
        		+ " SELECT '"
        		+ groupTag + "', '" + groupName + "', '" + userTag
        		+ "' WHERE NOT EXISTS ( SELECT 1 FROM "
                + DatabaseHelper.TABLE_GROUPS
                + " WHERE " + DatabaseHelper.COLUMN_USER_TAG + " = '" + userTag
                + "' AND " + DatabaseHelper.COLUMN_GROUP_TAG + " = '" + groupTag + "')";

        mDb.execSQL(newGroup);
        
        /*Cursor cursor = mDb.rawQuery("SELECT " + DatabaseHelper.COLUMN_GROUP_ID + " FROM " + DatabaseHelper.TABLE_GROUPS +" WHERE " + DatabaseHelper.COLUMN_USER_ID + " = " + userID, null);
        cursor.moveToFirst();
        
        int groupID = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_CONVERSATION_ID));
        
        return groupID;*/
    }

    public long addMessage(Message message) {
    	
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_SENDER_TAG, message.getsenderID());
        values.put(DatabaseHelper.COLUMN_MESSAGE, message.getMessage());
        values.put(DatabaseHelper.COLUMN_DATE, message.getDate().getTime());
        values.put(DatabaseHelper.COLUMN_CONVERSATION_ID, message.getConversationId());
        values.put(DatabaseHelper.COLUMN_GROUP_ID, message.getGroupId()); 
        values.put(DatabaseHelper.COLUMN_RECEIVER_TAG, message.getreceiverID());
        long insertId;
        
        try {
            insertId = mDb.insert(DatabaseHelper.TABLE_MESSAGES, null, values);
            message.setId(insertId);
            
        } catch (Exception e) {
            return -1;
        }

        return insertId;
    }

    /**
     * @param conversationId The conversation ID
     * @return Returns cursor with conversation data
     */
    public Cursor getConversation(long conversationId, ConversationFragment.ConversationType conversationType) {
    	//Redo to use more sql?
        String[] params = new String[] {
        		String.valueOf(conversationId)
        };
        
        String query = null;
        switch(conversationType){
		case SINGLE_CONVERSATION:
        	query = DatabaseHelper.QUERY_CONVERSATION_BY_ID;
			break;
		case GROUP_CONVERSATION:
        	query = DatabaseHelper.QUERY_GROUP_CONVERSATION_BY_ID;
			break;
        
        }

    	Cursor cursor = mDb.rawQuery(query, params);
        //Log.v(TAG, "Retreiving conversation with ID: " + conversationId);
        //Log.v(TAG, "Cursor size: " + cursor.getCount());
        return cursor;
    }
    
    /**
     * @param groupID The group ID
     * @return Returns group tag
     */
    public String getGroupTag(int groupID){
    	
    	String[] selectColumn = {
                DatabaseHelper.COLUMN_GROUP_TAG
        };

        String[] selectionArgs = {
                String.valueOf(groupID)
        };
        String groupTag = null;
        
        Cursor cursor = mDb.query(DatabaseHelper.TABLE_GROUPS, selectColumn,
                DatabaseHelper.COLUMN_GROUP_ID + " = ? ", selectionArgs, null, null, null, null);

        if(cursor.getCount() > 0){
        	cursor.moveToFirst();
        	groupTag = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_GROUP_TAG));
        }
        
        cursor.close();       
        return groupTag;
    }
    
    /**
     * @param groupTag The group tag
     * @return Returns group ID
     */
    public int getGroupID(String groupTag){
    	
    	String[] selectColumn = {
                DatabaseHelper.COLUMN_GROUP_ID
        };

        String[] selectionArgs = {
                groupTag
        };
        int groupID = -1;

        Cursor cursor = mDb.query(DatabaseHelper.TABLE_GROUPS, selectColumn,
                DatabaseHelper.COLUMN_GROUP_TAG + " = ? ", selectionArgs, null, null, null, null);

        if(cursor.getCount() > 0){      	
        	cursor.moveToFirst();        
        	groupID = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_GROUP_ID));
        }
        
        cursor.close();
        return groupID;
    }
    
    /**
     * @param groupTag The group ID
     * @return Returns group name
     */
    public String getGroupName(int conversationID){
    	
    	String[] selectColumn = {
                DatabaseHelper.COLUMN_GROUP_NAME
        };

        String[] selectionArgs = {
                String.valueOf(conversationID)
        };
        String groupName = null;

        Cursor cursor = mDb.query(DatabaseHelper.TABLE_GROUPS, selectColumn,
                DatabaseHelper.COLUMN_GROUP_ID + " = ? ", selectionArgs, null, null, null, null);

        if(cursor.getCount() > 0){ 
            cursor.moveToFirst();        
            groupName = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_GROUP_NAME));
        }

        cursor.close();

        return groupName;
    }
    
    
    /**
     * Removes the given group from DB
     * @param groupID The group ID
     */
    public void removeGroup(int groupID){
        final String removeGroup =
        		"DELETE FROM " + DatabaseHelper.TABLE_GROUPS + " WHERE " + DatabaseHelper.COLUMN_GROUP_ID + " = " + groupID;

        mDb.execSQL(removeGroup);
    }
    
    /**
     * Removes the given group from DB
     * @param groupID The group ID
     */
    public void removeGroup(String groupTag){
        final String removeGroup =
        		"DELETE FROM " + DatabaseHelper.TABLE_GROUPS + " WHERE " + DatabaseHelper.COLUMN_GROUP_TAG + " = '" + groupTag + "'";

        mDb.execSQL(removeGroup);
    }
    /**
     * Get tags for all groups the user is in
     * @return 
     */
    public List<String> getAllGroupTags(String userTag){
    	
        String[] selectionArgs = {
                userTag
        };
        List<String> groupTags = new ArrayList<String>();
    	Cursor cursor = mDb.rawQuery(DatabaseHelper.QUERY_GROUP_TAGS, selectionArgs);

        if(cursor.getCount() > 0){ 
            cursor.moveToFirst();
                  
            for (int i = 0; i < cursor.getCount(); i++) {
    			if(!cursor.isAfterLast()){
    				groupTags.add(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_GROUP_TAG)));
    				cursor.moveToNext();
    			}
    		}
        }
        
        cursor.close();
        return groupTags;
    }
    
    /**
     * Deletes ALL messages from database
     */
    public void deleteAllMessages(){
        final String deleteMessages =
        		"DELETE FROM " + DatabaseHelper.TABLE_MESSAGES;
        mDb.execSQL(deleteMessages);
    }
}
