package com.teskalabs.cvio.message;

import com.teskalabs.seacat.android.client.SeaCatClient;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

abstract public class SeaCatMessageTrigger implements Runnable {

	private static final String TAG = SeaCatMessageTrigger.class.getName();
	private final URL url;

	protected int responseCode = -1;
	protected String responseMessage = null;
	protected final ByteArrayOutputStream responseBody;

	private static final String eventsAPIUrlBase = "https://api.seacat/event/trigger/";

	public SeaCatMessageTrigger(String eventName) throws IOException {
		this(eventsAPIUrlBase, eventName);
	}

	public SeaCatMessageTrigger(String URLBase, String eventName) throws IOException {
		url = new URL(URLBase +  eventName);
		responseBody = new ByteArrayOutputStream();
	}

	@Override
	public void run() {
		onPreExecute();

		try {
			HttpURLConnection conn = SeaCatClient.open(url);

			conn.setRequestMethod("PUT");
			conn.setRequestProperty("Content-Type", getMessageContentType());
			conn.setDoOutput(true);

			DataOutputStream outputStream = new DataOutputStream(conn.getOutputStream());
			writeContent(outputStream);
			outputStream.close();

			InputStream is = conn.getInputStream();
			int nRead;
			byte[] data = new byte[1024];
			while ((nRead = is.read(data, 0, data.length)) != -1) {
				responseBody.write(data, 0, nRead);
			}
			responseBody.flush();

			responseCode = conn.getResponseCode();
			responseMessage = conn.getResponseMessage();

			onPostExecute();
		} catch (IOException e) {
			onError(e);
		}
	}

	/* Override me ! */
	public void onPreExecute() {
	}

	/* Override me ! */
	public void onPostExecute() {
	}

	/* Override me ! */
	public void onError(IOException e) {
	}


	public int getResponseCode() {
		return responseCode;
	}

	public String getResponseMessage() {
		return responseMessage;
	}

	public ByteArrayOutputStream getResponseBody() {
		return responseBody;
	}


	// Pure virtual methods
	protected abstract String getMessageContentType();
	protected abstract void writeContent(DataOutputStream outputStream) throws IOException;
}
