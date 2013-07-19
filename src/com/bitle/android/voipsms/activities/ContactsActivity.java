package com.bitle.android.voipsms.activities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class ContactsActivity extends ListActivity{
	private GoogleAnalyticsTracker mTracker;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mTracker = GoogleAnalyticsTracker.getInstance();
		mTracker.start("UA-23922796-1", this);
		
		try {
			List<Map<String, String>> t = getContactsCursor();
			SimpleAdapter adapter = new SimpleAdapter(getApplicationContext(),
					t,
					android.R.layout.simple_list_item_2,
					new String[] {"name", "phone"},
					new int[] {android.R.id.text1, android.R.id.text2});
		
			setListAdapter(adapter);
		} catch (Exception ex) {
			Log.e("voipsms", "error", ex);
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Map<String, String> item = (Map<String, String>) getListAdapter().getItem(position);
		
		Intent intent = new Intent();
		intent.putExtra("name", item.get("name"));
		intent.putExtra("phone", item.get("phone"));
		
		setResult(RESULT_OK, intent);
		finish();
	}

	@Override
	protected void onPause() {
		mTracker.dispatch();
		super.onPause();
	}

	@Override
	protected void onResume() {
		mTracker.trackPageView("/contactslist");
		super.onResume();
	}
	

	@Override
	protected void onDestroy() {
		mTracker.stop();
		super.onDestroy();
	}
	
	private List<Map<String, String>> getContactsCursor() {
		List<Map<String, String>> contacts = new ArrayList<Map<String, String>>();
		
		Cursor cursor = getContentResolver().query(
				ContactsContract.Contacts.CONTENT_URI, null, null, null, ContactsContract.Contacts.DISPLAY_NAME + " ASC");
		while (cursor.moveToNext()) {
			String contactId = cursor.getString(cursor
					.getColumnIndex(ContactsContract.Contacts._ID));
			
			String contactName = cursor.getString(cursor
					.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
			
			Integer hasPhone = Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)));
			if (hasPhone > 0) {
				// You know it has a number so now query it like this
				Cursor phones = getContentResolver().query(
						ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
						null,
						ContactsContract.CommonDataKinds.Phone.CONTACT_ID
								+ " = " + contactId, null, null);
				while (phones.moveToNext()) {
					String phoneNumber = phones
							.getString(phones
									.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
					Map<String, String> contact = new HashMap<String, String>();
					contact.put("name", contactName);
					contact.put("phone", phoneNumber);
					
					contacts.add(contact);
				}
				phones.close();
			}
		}
		cursor.close();
		
		return contacts;
	}
}
