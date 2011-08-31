package uk.co.gidley.clockRadio;

import java.util.Calendar;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;

public class SleepTimer extends Fragment {

	private static final String SLEEPYTIME = "SLEEPYTIME";
	private static final String TAG = "SleepTimerFragment";
	private RadioPlayerService mRadioPlayerService;
	boolean mBound;

	private TimePicker timePicker;
	private final Handler sleepHandler = new Handler();
	private TextView sleepTimerDisplay;

	private int hourOfDay;
	private int minute;

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mRadioPlayerService = ((RadioPlayerService.LocalBinder) service)
					.getService();
			Log.d(TAG, "Bound Service");
		}

		public void onServiceDisconnected(ComponentName className) {
			mRadioPlayerService = null;
		}
	};

	private OnClickListener mOnClickSleepListener = new OnClickListener() {
		public void onClick(final View v) {
			new Thread(new Runnable() {

				public void run() {
					Log.d(TAG, "Started creating sleep event");
					Calendar stopTime = Calendar.getInstance();
					Calendar now = Calendar.getInstance();

					stopTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
					stopTime.set(Calendar.MINUTE, minute);
					stopTime.set(Calendar.SECOND, 0);
					if (stopTime.before(now)) {
						stopTime.add(Calendar.DATE, 1);
					}

					Log.d(TAG, "Scheduling Handler at :" + stopTime.getTime());
					sleepHandler.removeCallbacksAndMessages(SLEEPYTIME);
					sleepHandler.postAtTime(
							new Runnable() {
								public void run() {
									Log.d(TAG, "Stopping playback");
									mRadioPlayerService.stop();
									getActivity().runOnUiThread(new Runnable() {

										public void run() {
											sleepTimerDisplay
													.setVisibility(View.INVISIBLE);
										}
									});
								}
							},
							SLEEPYTIME,
							SystemClock.uptimeMillis()
									+ stopTime.getTimeInMillis()
									- now.getTimeInMillis());

					final String stopTimeDisplay = stopTime.getTime()
							.toString();

					v.post(new Runnable() {
						public void run() {

							sleepTimerDisplay.setText("Sleep Time:"
									+ stopTimeDisplay);
							sleepTimerDisplay.setVisibility(View.VISIBLE);
						}
					});
				}
			}).start();

		}
	};
	private OnTimeChangedListener onTimeChangedListener = new OnTimeChangedListener() {

		public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
			SleepTimer.this.hourOfDay = hourOfDay;
			SleepTimer.this.minute = minute;
			Log.d(TAG, "Time:" +hourOfDay +":" + minute);
		}
	};
	private OnClickListener onClickSleepCancelListener = new OnClickListener() {

		public void onClick(View v) {
			sleepHandler.removeCallbacksAndMessages(SLEEPYTIME);
			sleepTimerDisplay.setVisibility(View.INVISIBLE);
		}
	};

	@Override
	public void onStart() {
		getActivity().bindService(
				new Intent(getActivity(), RadioPlayerService.class),
				mConnection, Context.BIND_AUTO_CREATE);
		mBound = true;
		super.onStart();
	}

	@Override
	public void onStop() {
		getActivity().unbindService(mConnection);
		super.onStop();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.sleep_timer, container, false);
		Button sleepButton = (Button) view.findViewById(R.id.sleep);
		sleepButton.setOnClickListener(mOnClickSleepListener);
		timePicker = (TimePicker) view.findViewById(R.id.sleepTime);
		timePicker.setOnTimeChangedListener(onTimeChangedListener);
		this.hourOfDay = timePicker.getCurrentHour();
		this.minute = timePicker.getCurrentMinute();
		sleepTimerDisplay = (TextView) view.findViewById(R.id.sleepTimeDisplay);
		Button sleepCancelButton = (Button) view
				.findViewById(R.id.cancel_sleep);
		sleepCancelButton.setOnClickListener(onClickSleepCancelListener);
		return view;
	}

}
