
package io.pingpal.models;

/**
 * @author Paul Mallon & Robin Dahlström 23-07-14
 */
public class Person {

    @SuppressWarnings("unused")
	private static final String TAG = Person.class.getSimpleName();

    private String mName;

    private int mImageResourceId;
    
    private String mTag;
    private String mUserTag = "testTag";
    /**
     * Use this constructor for user data
     * @param facebookId
     * @param userTag
     * @param name
     * @param imageURL
     */
    public Person(String tag, String userTag, String name) {


        this.mTag = tag;
        this.mUserTag = userTag;
        this.mName = name;

    }
    
    /**
     * Use this constructor for friend data
     * @param facebookId
     * @param userTag
     * @param name
     * @param imageURL
     */
    public Person(String tag, String name) {

        this.mTag = tag;
        this.mName = name;
    }

    public String getTag() {
        return mTag;
    }


    public int getImageResourceId() {
        return mImageResourceId;
    }

    public void setImageResourceId(int imageResourceId) {
        this.mImageResourceId = imageResourceId;
    }

    public String getName() {

        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getUserTag() {
        return mUserTag;
    }

    public void setmUserTag(String mUserTag) {
        this.mUserTag = mUserTag;
    }

}
