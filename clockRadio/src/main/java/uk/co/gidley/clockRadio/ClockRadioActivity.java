package uk.co.gidley.clockRadio;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import uk.co.gidley.clockRadio.RadioPlayerService.State;
import uk.co.gidley.clockRadio.RadioStationsList.OnSelectStationListener;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import de.akquinet.android.androlog.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;

public class ClockRadioActivity extends Activity implements
		OnSelectStationListener {

	@Override
	protected void onStart() {

        Log.init(this);
		bindService(new Intent(ClockRadioActivity.this,
				RadioPlayerService.class), mConnection,
				Context.BIND_AUTO_CREATE);
		mBound = true;
		FlurryAgent.onStartSession(this, "AWYQTHVF81ERH8HVUP9V");
		super.onStart();
	}

	@Override
	protected void onStop() {
		FlurryAgent.onEndSession(this);
		unbindService(mConnection);
		super.onStop();
	}

	private static final String TAG = "ClockRadioActivity";
	private RadioPlayerService mRadioPlayerService;
	boolean mBound;

	private OnClickListener mOnPlayListener = new OnClickListener() {
		public void onClick(View v) {
			new Thread(new Runnable() {
				public void run() {
					try {
						mRadioPlayerService.play(stationUri);
					} catch (UnableToPlayException e) {
						runOnUiThread(new Runnable() {
							public void run() {
								Toast.makeText(getBaseContext(),
										"Unable to play", Toast.LENGTH_SHORT);
							}
						});
					}
				}
			}).start();
		}
	};

	private OnClickListener mRefreshVideoListener = new OnClickListener() {
		public void onClick(View v) {
			startService(new Intent(getBaseContext(), StationsListService.class));
			Log.d(TAG, "Requested Stations list");
		}
	};

	private OnClickListener mOnStopListener = new OnClickListener() {
		public void onClick(final View v) {
			new Thread(new Runnable() {
				public void run() {
					mRadioPlayerService.stop();
				}
			}).start();
		}
	};

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mRadioPlayerService = ((RadioPlayerService.LocalBinder) service)
					.getService();

			mRadioPlayerService
					.addPropertyChangeListener(new PropertyChangeListener() {
						public void propertyChange(PropertyChangeEvent event) {
							if (event.getPropertyName().equals("State")) {
								final Button button = (Button) findViewById(R.id.start);

								RadioPlayerService.State state = (State) event
										.getNewValue();
								updatePlayButton(button, state);
							}
						}
					});
			Log.d(TAG, "Bound Service");

			runOnUiThread(new Runnable() {

				public void run() {

					Button playButton = (Button) findViewById(R.id.start);
					updatePlayButton(playButton, mRadioPlayerService.getState());

				}
			});
		}

		public void onServiceDisconnected(ComponentName className) {
			mRadioPlayerService = null;
		}
	};

	private void updatePlayButton(final Button button,
			RadioPlayerService.State state) {
		switch (state) {
		case PLAYING:
			runOnUiThread(new Runnable() {
				public void run() {
					button.setText(R.string.stop);
				}
			});
			break;

		case LOADING:
			runOnUiThread(new Runnable() {
				public void run() {
					button.setText(R.string.loading);
				}
			});
			break;

		case STOPPED:
			runOnUiThread(new Runnable() {
				public void run() {
					button.setText(R.string.start);
				}
			});
			break;
		}
	}

	private String stationUri;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Button playButton = (Button) findViewById(R.id.start);
		playButton.setOnClickListener(mOnPlayListener);
		Button stopButton = (Button) findViewById(R.id.stop);
		stopButton.setOnClickListener(mOnStopListener);
		Button refreshStations = (Button) findViewById(R.id.refreshStations);
		refreshStations.setOnClickListener(mRefreshVideoListener);
	}

	public void onSelectStationListener(String stationUri) {
		this.stationUri = stationUri;
	}

}