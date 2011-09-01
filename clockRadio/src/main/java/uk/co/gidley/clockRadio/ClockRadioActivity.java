package uk.co.gidley.clockRadio;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;
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

public class ClockRadioActivity extends RoboActivity implements
		OnSelectStationListener {

    @InjectView(R.id.start)
    private Button playButton;
    @InjectView(R.id.stop)
    private Button stopButton;
    @InjectView(R.id.refreshStations)
    private Button refreshStations;


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
								RadioPlayerService.State state = (State) event
										.getNewValue();
								updatePlayButton(state);
							}
						}
					});
			Log.d(TAG, "Bound Service");

			runOnUiThread(new Runnable() {

				public void run() {

					updatePlayButton(mRadioPlayerService.getState());

				}
			});
		}

		public void onServiceDisconnected(ComponentName className) {
			mRadioPlayerService = null;
		}
	};

	private void updatePlayButton(
			RadioPlayerService.State state) {
		switch (state) {
		case PLAYING:
			runOnUiThread(new Runnable() {
				public void run() {
					playButton.setText(R.string.stop);
				}
			});
			break;

		case LOADING:
			runOnUiThread(new Runnable() {
				public void run() {
					playButton.setText(R.string.loading);
				}
			});
			break;

		case STOPPED:
			runOnUiThread(new Runnable() {
				public void run() {
					playButton.setText(R.string.start);
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

        playButton.setOnClickListener(mOnPlayListener);
        stopButton.setOnClickListener(mOnStopListener);
        refreshStations.setOnClickListener(mRefreshVideoListener);
	}

	public void onSelectStationListener(String stationUri) {
		this.stationUri = stationUri;
	}

}