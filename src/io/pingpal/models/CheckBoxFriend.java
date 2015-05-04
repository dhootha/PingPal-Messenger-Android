package io.pingpal.models;

/**
 * A model class used keep track of selected friends
 */
public class CheckBoxFriend{
	
	public CheckBoxFriend(String name, String tag){
		super();
		this.name = name;
		this.tag = tag;
	
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	String name, tag;
	boolean selected = false;
	public boolean isSelected() {
		return selected;
	}
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

}