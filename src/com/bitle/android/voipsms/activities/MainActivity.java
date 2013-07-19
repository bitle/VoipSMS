package com.bitle.android.voipsms.activities;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.bitle.android.voipsms.AsyncMessageTask;
import com.bitle.android.voipsms.R;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class MainActivity extends Activity {
	private final static int SETTINGS_ID = Menu.FIRST;
	private final static int PICK_CONTACT = 0;
    private EditText phone;
	private EditText message;
	private TextView textLength;
	private GoogleAnalyticsTracker mTracker;
	private Button buttonSend;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        mTracker = GoogleAnalyticsTracker.getInstance();
        mTracker.start("UA-23922796-1", 5000, this);
        
        if (!checkUsername()) {
        	startPreferences();
        }
        
        setContentView(R.layout.main);
        
        phone = (EditText) findViewById(R.id.editNumber);
        message = (EditText) findViewById(R.id.editBody);
        textLength = (TextView) findViewById(R.id.textLength);
        buttonSend = (Button) findViewById(R.id.buttonSend);
        
        message.addTextChangedListener(createTextWatcher());
        buttonSend.setEnabled(false);
        
        loadSavedPhoneNumber();
    }
   
    @Override
	protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	outState.putString("phone", phone.getText().toString());
    	outState.putString("message", message.getText().toString());
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		phone.setText(savedInstanceState.getString("phone"));
		message.setText(savedInstanceState.getString("message"));
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == PICK_CONTACT && resultCode == RESULT_OK) {
			String phoneNumber = data.getStringExtra("phone");
			//String name = data.getStringExtra("name");
			phoneNumber = phoneNumber.replaceAll("[-)(]", "");
			phone.setText(phoneNumber);
		}
	}

	@Override
	protected void onPause() {
    	mTracker.dispatch();
		super.onPause();
	}

	@Override
	protected void onResume() {
		mTracker.trackPageView("/main");
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		mTracker.stop();
		super.onDestroy();
	}

	private TextWatcher createTextWatcher() {
		return new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				if (s.length() == 0) {
					buttonSend.setEnabled(false);
				} else if (s.length() < 150) {
					buttonSend.setEnabled(true);
					textLength.setVisibility(View.GONE);
				} else if (150 <= s.length() && s.length() <= 160) {
					buttonSend.setEnabled(true);
					String length = s.length() + "/160";
					textLength.setText(length);
					textLength.setVisibility(View.VISIBLE);
				} else {
					buttonSend.setEnabled(false);
					textLength.setText("only 160 symbols are allowed: " + s.length());
					textLength.setVisibility(View.VISIBLE);
				}
			}
			
			@Override
			public void onTextChanged(CharSequence s, int st, int b, int c) {}
			@Override
			public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
		};
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		
		menu.add(0, SETTINGS_ID, 0, "Settings");
		
		return result;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case SETTINGS_ID:
			startPreferences();
			break;
		}
		return super.onOptionsItemSelected(item);
	}


	private void loadSavedPhoneNumber() {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	String phoneNumber = prefs.getString("phoneNumberTo", null);
    	phone.setText(phoneNumber);
	}

	private void startPreferences() {
		Intent intent = new Intent();
		intent.setClass(this, UserPreferenceActivity.class);
		
		startActivity(intent);
	}

	private boolean checkUsername() {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	return prefs.getString("username", null) != null &&
    		prefs.getString("password", null) != null;
	}

	public void buttonSendClicked(View view) {
		buttonSend.setEnabled(false);
    	String phoneNumber = phone.getText().toString();
    	String messageText = message.getText().toString();
    	boolean fieldsOk = phoneNumber.length() > 0 &&
    					messageText.length() > 0 &&
    					!phoneNumber.trim().equalsIgnoreCase("") &&
    					!messageText.trim().equalsIgnoreCase("");
    	
    	if (fieldsOk) {
    		savePhone(phoneNumber);
    		
    		String request = constructRequest(phoneNumber, messageText);
    		if (request == null) {
    			return;
    		}
    		
    		AsyncMessageTask task = new AsyncMessageTask(this) {
				@Override
				protected void onPostExecute(Integer result) {
					setProgressBarIndeterminateVisibility(false);
					if (result == AsyncMessageTask.OK) {
						messageSentSuccessfully();
					} else {
						messageWasntSent(result);
					}
				}
    		};
    		
    		setProgressBarIndeterminateVisibility(true);
    		task.setSaveInThreads(getSaveInThreadsOption());
    		task.execute(new String[] {request, phoneNumber, messageText});
    	}
    }
	
	private boolean getSaveInThreadsOption() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		return prefs.getBoolean("putInThreads", false);
	}

	public void buttonContactClicked(View view) {
		Intent intent = new Intent();
		intent.setClass(this, ContactsActivity.class);
		startActivityForResult(intent, PICK_CONTACT);
	}

	private void messageSentSuccessfully() {
		buttonSend.setEnabled(true);
		message.setText(null);
		Toast.makeText(this, "Message was sent", Toast.LENGTH_LONG).show();
	}
	
	private void messageWasntSent(int errorCode) {
		buttonSend.setEnabled(false);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		switch (errorCode) {
		case AsyncMessageTask.BAD_RESPONSE:
			builder.setMessage("Bad response from the server");
			break;
		case AsyncMessageTask.SERVER_ERROR:
			builder.setMessage("Server error");
			break;
		}
		
		builder.show();
	}

	private void savePhone(String phoneNumber) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.edit().putString("phoneNumberTo", phoneNumber).commit();
	}


	private String constructRequest(String phoneNumber, String messageText) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String username = prefs.getString("username", null);
		String password = prefs.getString("password", null);
		String numberFrom = prefs.getString("phoneNumberFrom", "");
		String encodedMessage = encodeMessage(messageText);
		
		if (username == null || username.length() <= 0) {
			Toast.makeText(getApplicationContext(), "Please enter username", Toast.LENGTH_LONG);
			return null;
		}
		
		if (password == null || password.length() <= 0) {
			Toast.makeText(getApplicationContext(), "Please enter password", Toast.LENGTH_LONG);
			return null;
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("https://www.voipdiscount.com/myaccount/sendsms.php");
		sb.append("?username=").append(username);
		sb.append("&password=").append(password);
		sb.append("&from=").append(numberFrom);
		sb.append("&to=").append(phoneNumber);
		sb.append("&text=").append(encodedMessage);
		sb.append("&coding=2");
		
		return sb.toString();
	}
	
	private String encodeMessage(String url) {
		try {
			String message = URLEncoder.encode(url, "UTF-16");
			Log.d("voipsms", url + " - utf16 - > " + message);
			return message;
		} catch (UnsupportedEncodingException e) {
			Log.e("voipsms", "unsupported encoding", e);
			return url.replaceAll(" ", "%20");
		}
	}
}

