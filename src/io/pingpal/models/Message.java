
package io.pingpal.models;

import java.util.Date;

/**
 * @author Paul Mallon & Robin Dahlström 22-07-14
 */
public class Message {

    private String mSenderTag, mReceiverTag;

    private String mMessage;
    

    private Date mTimeStamp;

    /**
     * AppUser should be false if this message was received from a friend Set to
     * true if message was sent by this apps user.
     */

    private int mConversationId = 0;

    /**
     * @param senderTag the facebookId of the contact
     * @param message the message text
     * @param timeStamp the time of arrival as a long, use Date.getTime()
     * @param conversationId the id of the conversation, currently the
     *            facebookId, will need to change when suppport for groups is
     *            added
     */
    public Message(String senderTag, String message, long timeStamp, int conversationId, long groupId, String receiverTag) {

        this.mSenderTag = senderTag;
        this.mMessage = message;
        this.mTimeStamp = new Date(timeStamp);
        this.mReceiverTag = receiverTag;
        this.mConversationId = conversationId;
        this.mGroupId = groupId;
    }

    private long mMessageId;

    public long getId() {
        return mMessageId;
    }

    public void setId(long id) {
        this.mMessageId = id;
    }

    public Date getDate() {
        return mTimeStamp;
    }

    public long getDateAsLong() {
        return mTimeStamp.getTime();
    }

    public String getDateAsString() {

        return mTimeStamp.toString();
    }

    public String getMessage() {
        return mMessage;
    }

    private boolean mMessageRead = false;

    public boolean isMessageRead() {
        return mMessageRead;
    }

    public void setMessageRead(boolean messageRead) {
        this.mMessageRead = messageRead;
    }

    public String getsenderID() {
        return mSenderTag;
    }
    
    public String getreceiverID(){
    	return mReceiverTag;
    }


    /**
     * Group ID set to -1 if the message is not part of a group conversation
     */
    private long mGroupId = -1;

    public long getGroupId() {
        return mGroupId;
    }

    public int getConversationId() {
        return mConversationId;
    }

    public void setConversationId(int conversationId) {
        this.mConversationId = conversationId;
    }

    private String mImageIconName;

    public String getImageIconName() {
        return mImageIconName;
    }

    public void setImageIconName(String imageIconName) {
        this.mImageIconName = imageIconName;
    }

}
