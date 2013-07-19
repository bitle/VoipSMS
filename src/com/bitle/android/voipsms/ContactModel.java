package com.bitle.android.voipsms;

import java.util.ArrayList;
import java.util.List;

public class ContactModel {
	private String name;
	private List<String> phoneNumber = new ArrayList<String>();
	
	public ContactModel(String name) {
		setName(name);
	}
	
	public ContactModel(String name, String phoneNumber) {
		setName(name);
		addPhoneNumber(phoneNumber);
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<String> getPhoneNumber() {
		return phoneNumber;
	}
	protected void setPhoneNumber(List<String> phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	public void addPhoneNumber(String phoneNumber) {
		getPhoneNumber().add(phoneNumber);
	}
}
