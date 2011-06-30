package uk.co.gidley.clockRadio;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.flurry.android.FlurryAgent;

public class ClockRadioActivity extends Activity {

	@Override
	protected void onStart() {
		super.onStart();
		FlurryAgent.onStartSession(this, "AWYQTHVF81ERH8HVUP9V");
	}

	@Override
	protected void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(this);
	}

	private static final String TAG = "ClockRadioActivity";

	private RadioPlayerService mRadioPlayerService;
	boolean mBound;

	private OnClickListener mOnPlayListener = new OnClickListener() {
		public void onClick(View v) {
			final Button clicked = (Button) v;
			clicked.setText(R.string.loading);
			new Thread(new Runnable() {
				public void run() {
					mRadioPlayerService.play();
					clicked.post(new Runnable() {
						public void run() {
							clicked.setText(R.string.playing);
						}
					});
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
					v.post(new Runnable() {
						public void run() {
							Button button = (Button) findViewById(R.id.start);
							button.setText(R.string.start);
						}
					});
				}
			}).start();
		}
	};

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			mRadioPlayerService = ((RadioPlayerService.LocalBinder) service)
					.getService();

			Log.d(TAG, "Bound Service");
		}

		public void onServiceDisconnected(ComponentName className) {
			mRadioPlayerService = null;
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		doBindService();

		Button playButton = (Button) findViewById(R.id.start);
		playButton.setOnClickListener(mOnPlayListener);
		Button stopButton = (Button) findViewById(R.id.stop);
		stopButton.setOnClickListener(mOnStopListener);
		
		Button refreshStations = (Button) findViewById(R.id.refreshStations);
		refreshStations.setOnClickListener(mRefreshVideoListener);



	}

	void doBindService() {
		// Establish a connection with the service. We use an explicit
		// class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		bindService(new Intent(ClockRadioActivity.this,
				RadioPlayerService.class), mConnection,
				Context.BIND_AUTO_CREATE);
		mBound = true;
	}

	void doUnbindService() {
		if (mBound) {
			// Detach our existing connection.
			unbindService(mConnection);
			mBound = false;
		}
	}

}