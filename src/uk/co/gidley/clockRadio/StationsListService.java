package uk.co.gidley.clockRadio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class StationsListService extends Service {

	private class UpdateStationsList extends AsyncTask<URI, Integer, Boolean> {

		@Override
		protected Boolean doInBackground(URI... urls) {
			Log.d(TAG, "Starting Background load");
			int count = urls.length;
			
			HttpClient httpclient = new DefaultHttpClient();

			for (int i = 0; i < count; i++) {
				// Download stations and load into SQL lite
				try {
					HttpGet httpGet = new HttpGet(urls[i]);
					HttpResponse response = httpclient.execute(httpGet);
					InputStream in = response.getEntity().getContent(); 
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(in));
					String line = "";
					while ((line = reader.readLine()) != null) {
						Log.d(TAG, "Reading line:" + line);
						if (!line.equals("Title,Url")) {
							// Not Header line
							String[] splitLine = line.split(",");
							if (splitLine.length == 2) {
								String title = splitLine[0];
								String url = splitLine[1];
								
								ContentValues item = new ContentValues();
								item.put(RadioStations.TITLE, title);
								item.put(RadioStations.URL, url);
								getContentResolver().insert(RadioStations.CONTENT_URI, item);
								Log.d(TAG, "Wrote content item:" + title);
							}
						}
					}
				} catch (IOException e) {
					return false;
				}
			}
			return true;
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		try {
			new UpdateStationsList().execute(new URI("https://spreadsheets.google.com/spreadsheet/ccc?key=0Am_QxWmAHhuudDJyN1laV09fX0twc29JenllNGo4T0E&hl=en_GB&output=csv"));
		} catch (URISyntaxException e) {
			Log.e(TAG, "unable to parse uri", e);
		}
		return START_STICKY;
	}

	private static final String TAG = "StationsListService";

	private final IBinder mBinder = new LocalBinder();

	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		StationsListService getService() {
			Log.d(TAG, "LocalBinder bound");
			return StationsListService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

}
