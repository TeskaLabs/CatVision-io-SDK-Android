package com.teskalabs.cvio.message;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;

public class SeaCatJSONMessageTrigger extends SeaCatMessageTrigger {

	private static final String TAG = SeaCatMessageTrigger.class.getName();
	protected final JSONObject json = new JSONObject();

	public SeaCatJSONMessageTrigger(String eventName) throws IOException {
		super(eventName);
	}

	public SeaCatJSONMessageTrigger(String URLBase, String eventName) throws IOException {
		super(URLBase, eventName);
	}


	public SeaCatJSONMessageTrigger put(String name, String value) throws JSONException {
		json.put(name, value);
		return this;
	}


	@Override
	protected String getMessageContentType() {
		return "application/json; charset=utf-8";
	}

	@Override
	protected void writeContent(DataOutputStream outputStream) throws IOException {
		outputStream.writeBytes(json.toString());
		outputStream.flush();
	}

}
