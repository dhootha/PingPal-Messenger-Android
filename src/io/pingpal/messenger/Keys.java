package io.pingpal.messenger;

public class Keys {

    public class Payload{
        public static final String MESSAGE = "message";
        public static final String ICON = "icon";
        public static final String GROUP_MESSAGE = "groupMessage";
        public static final String GROUP_ICON = "groupIcon";
        public static final String GROUP_UPDATED = "groupUpdated";
        public static final String GROUP_REMOVED_FROM = "removedFromGroup";
        public static final String GROUP_NOTIFY_CHANGED = "groupNotifyChanged";
        public static final String MESSAGE_TYPE = "message_type";

    }

    public class Options{
        public static final String TO = "to";
        public static final String FROM = "from";
        public static final String TARGET = "to";
        public static final String ORIGIN = "from";
        public static final String TIME_TO_LIVE = "ttl";
        public static final String PUSH = "push";
        public static final String PUSH_MODE = "mode";
        public static final String PUSH_MODE_FORCE = "force";
        public static final String PUSH_MODE_FALLBACK = "fallback";
        public static final String GCM_DATA = "data";
        public static final String GCM_DATA_ALERT = "alert";
        public static final String SEQUENCE_NUMBER_YOURS = "yourseq";
        public static final String SEQUENCE_NUMBER_MINE = "myseq";

    }
    
    public class Preferences{
    	public static final String SOUND_ENABLED = "sound_enabled";
    	public static final String FIRST_CONVERSATION = "first_conversation";
    	public static final String FIRST_GROUP_INFO = "first_groupinfo";
    	public static final String USER_TAG = "user_tag";
    	public static final String USER_NAME = "user_name";
    	public static final String LOGGED_IN = "logged_in";
    }
}
