package com.bitle.android.voipsms;

import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

public class AsyncMessageTask extends AsyncTask<String, Integer, Integer> {
	public static final int OK = 0;
	public static final int SERVER_ERROR = 1;
	public static final int BAD_RESPONSE = 2;
	
	private Context mContext;
	private boolean saveInThreads = false;
	
	public AsyncMessageTask(Context context) {
		mContext = context;
	}
	
	@Override
	protected Integer doInBackground(String... arg0) {
		String request = arg0[0];
		String to = arg0[1];
		String message = arg0[2];
		
		int retCode = sendMessage(request);
		if (retCode != OK) {
			return retCode;
		}
		
		saveToSentFolder(to, message);
		return OK;
	}
	
	private int sendMessage(String request) {
		try {
			HttpGet httpGet = new HttpGet(request);
			DefaultHttpClient httpClient = new DefaultHttpClient();
			
			HttpResponse response;
			response = httpClient.execute(httpGet);
		
		
			if (response.getStatusLine().getStatusCode() != 200) {
				Log.e("voipsms", "Server returned status code: " + response.getStatusLine().getStatusCode() + " expected 200");
				return BAD_RESPONSE;
			}
			
			return parseResponse(response);
			
		} catch (ClientProtocolException e) {
			Log.e("voipsms", "Server error", e);
			return SERVER_ERROR;
		} catch (IOException e) {
			Log.e("voipsms", "Server error", e);
			return SERVER_ERROR;
		} catch (Exception ex) {
			Log.e("voipsms", "Internal error", ex);
			return SERVER_ERROR;
		}
	}
	
	private int parseResponse(HttpResponse response) throws IllegalStateException, IOException {
		InputStreamReader reader = new InputStreamReader(response.getEntity().getContent());
		
		char[] buffer = new char[255];
		
		int bytesRead = reader.read(buffer);
		if (bytesRead > 0) {
			String result = new String(buffer);
			Log.d("voipsms", "Response from the server: " + result);
			if (result.contains("success")) {
				return OK;
			}
		}
		
		return BAD_RESPONSE;
	}

	private void saveToSentFolder(String phoneNumber, String messageText) {
		if (isSaveInThreads()) {
			ContentValues values = new ContentValues();
			values.put("address", phoneNumber);
			values.put("body", messageText);
			mContext.getContentResolver().insert(Uri.parse("content://sms/sent"), values);
		}
	}

	public void setSaveInThreads(boolean saveInThreads) {
		this.saveInThreads = saveInThreads;
	}

	public boolean isSaveInThreads() {
		return saveInThreads;
	}
}
