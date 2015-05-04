
package io.pingpal.models;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Paul Mallon & Robin Dahlström 22-07-14
 */
public class Conversation {

	@SuppressWarnings("unused")
	private static final String TAG = Person.class.getSimpleName();
	
    private static final int HINT_LENGTH = 50;

    private static final String TBC = "...";

    private ArrayList<Message> messages;

    public Conversation() {

        messages = new ArrayList<Message>();
    }

    private long mFriendId;

    public Conversation(int friendId) {

        this.mFriendId = friendId;
        messages = new ArrayList<Message>();

    }

    public Conversation(int friendId, Message message) {

        this.mFriendId = friendId;
        messages = new ArrayList<Message>();

        messages.add(message);

    }

    /**
     * A substring of the full message to provide a hint to the reader
     * when looking at the conversation list.
     */
    private String messageHint;

    private long mConversationId;

    private boolean newMessage;

    public Conversation(long conversationId, long friendId, int newMessage, String message) {

        this.mConversationId = conversationId;
        this.mFriendId = friendId;
        this.newMessage = (newMessage == 1) ? true : true;
        this.messageHint = (message.length() > HINT_LENGTH) ? message.substring(0, HINT_LENGTH)
                + TBC : message + TBC;
    }

    public long getConversationId() {
        return mConversationId;
    }

    public void setConversationId(long conversationId) {
        this.mConversationId = conversationId;
    }

    public void setMessages(ArrayList<Message> messages) {
        this.messages = messages;
    }

    public String getMessageHint() {
        return messageHint;
    }

    public void setMessageHint(String messageHint) {
        this.messageHint = messageHint;
    }

    public boolean isNewMessage() {
        return newMessage;
    }

    public void setNewMessage(boolean unreadMessage) {
        this.newMessage = unreadMessage;
    }

    public long getFriendId() {
        return mFriendId;
    }

    private int mUnreadMessageCount;

    public ArrayList<Message> getMessages() {
        mUnreadMessageCount = 0;
        return messages;
    }

    public void addMessage(Message message) {
        this.messages.add(message);
        mUnreadMessageCount++;
    }

    public int getMessageCount() {
        return messages.size();
    }

    public int getUnreadMessageCount() {
        return mUnreadMessageCount;
    }

    public void setMessages(List<Message> friendMessages) {

        messages = (ArrayList<Message>)friendMessages;

        countUnreadMessages(messages);
    }

    private void countUnreadMessages(ArrayList<Message> messages) {

        mUnreadMessageCount = 0;

        for (Message message : messages) {
            if (!message.isMessageRead()) {
                mUnreadMessageCount++;
            }
        }

    }

    public int size() {

        return messages.size();
    }

}
