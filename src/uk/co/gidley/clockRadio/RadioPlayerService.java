package uk.co.gidley.clockRadio;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;

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

	private enum State {
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

	public void play() {

		if (mp != null && mp.isPlaying()) {
			stop();
		}

		mp = new MediaPlayer();
		try {
			mp.setDataSource("http://bbcmedia.ic.llnwd.net/stream/bbcmedia_he2_radio4_q?s=1309175586&e=1309189986&h=6a24b347bfa199ab083ae4d3582150c4");
			mp.prepare();
			mp.start();
			showNotification();
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "Unable to open stream", e);
		} catch (IllegalStateException e) {
			Log.e(TAG, "Unable to open stream", e);
		} catch (IOException e) {
			Log.e(TAG, "Unable to open stream", e);
		}
	}

	public void stop() {
		mp.stop();
		mp.release();
		mp = null;
		stopForeground(true);
	}
}
