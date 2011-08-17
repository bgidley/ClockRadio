package uk.co.gidley.clockRadio;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import uk.co.gidley.clockRadio.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class RadioPlayerService extends Service {

	private static final String TAG = "RadioPlayerService";

	public static final String STOP = "STOP";

	private final IBinder mBinder = new LocalBinder();
	private NotificationManager mNM;
	// Unique Identification Number for the Notification.
	// We use it on Notification start, and to cancel it.
	private int NOTIFICATION = R.string.local_service_started;

	private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(
			this);

	private State state;

	public State getState() {
		return state;
	}

	public void setState(State state) {
		State tmpState = this.state;
		this.state = state;
		propertyChangeSupport.firePropertyChange("State", tmpState, state);
	}

	public enum State {
		PLAYING, STOPPED, LOADING
	}

	public void addPropertyChangeListener(PropertyChangeListener pcl) {
		propertyChangeSupport.addPropertyChangeListener(pcl);
	}

	public void removePropertyChangeListener(PropertyChangeListener pcl) {
		propertyChangeSupport.removePropertyChangeListener(pcl);
	}

	private MediaPlayer mp;

	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		RadioPlayerService getService() {
			Log.d(TAG, "LocalBinder bound");
			return RadioPlayerService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

	}

	@Override
	public void onDestroy() {
		// Cancel the persistent notification.
		mNM.cancel(NOTIFICATION);
	}

	private void showNotification() {
		// In this sample, we'll use the same text for the ticker and the
		// expanded notification
		CharSequence text = getText(R.string.local_service_started);

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.icon, text,
				System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, ClockRadioActivity.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this,
				getText(R.string.local_service_label), text, contentIntent);
		startForeground(NOTIFICATION, notification);

		Log.d(TAG, "Show Notification Complete");
	}

	public void play(String playerUri) throws UnableToPlayException {
		
		setState(State.LOADING);

		if (StringUtils.isEmpty(playerUri)) {
			setState(State.STOPPED);
			throw new UnableToPlayException();
		}

		if (mp != null && mp.isPlaying()) {
			stop();
		}

		// The uri could be a playlist file OR an actual stream check based on
		// file extension (TODO review if better way of doing this)
		String audioUri;
		if (playerUri.endsWith(".pls")) {
			audioUri = parsePls(playerUri);
		} else {
			audioUri = playerUri;
		}

		if (StringUtils.isEmpty(audioUri)) {
			setState(State.STOPPED);
			throw new UnableToPlayException();
		}
		Log.d(TAG, "Audio URL:" + audioUri);

		mp = new MediaPlayer();
		try {
			mp.setDataSource(audioUri);
			mp.prepare();
			mp.start();
			setState(State.PLAYING);
			showNotification();
		} catch (IllegalArgumentException e) {
			setState(State.STOPPED);
			Log.e(TAG, "Unable to open stream", e);
		} catch (IllegalStateException e) {
			setState(State.STOPPED);
			Log.e(TAG, "Unable to open stream", e);
		} catch (IOException e) {
			setState(State.STOPPED);
			Log.e(TAG, "Unable to open stream", e);
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if (intent != null) {
			Log.d(TAG,
					"Recieved start command stop is"
							+ intent.getBooleanExtra(STOP, false));

			if (intent.getBooleanExtra(STOP, false)) {
				this.stop();
			}
		}
		return super.onStartCommand(intent, flags, startId);
	}

	private String parsePls(String playerUri) {
		HttpClient httpclient = new DefaultHttpClient();

		try {
			HttpURLConnection urlConnection;
			HttpGet httpGet = new HttpGet(playerUri);
			HttpResponse response = httpclient.execute(httpGet);
			InputStream in = response.getEntity().getContent();
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(in));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("File")) {
					// Just find the first file referenced and return it
					return line.substring(line.indexOf("=") + 1);
				}
			}
		} catch (ClientProtocolException e) {
			Log.e(TAG, "Unable to load playlist", e);
		} catch (IllegalStateException e) {
			Log.e(TAG, "Unable to load playlist", e);
		} catch (IOException e) {
			Log.e(TAG, "Unable to load playlist", e);
		}
		return null;
	}

	public void stop() {
		mp.stop();
		mp.release();
		mp = null;
		stopForeground(true);
		
		setState(State.STOPPED);
	}
}
