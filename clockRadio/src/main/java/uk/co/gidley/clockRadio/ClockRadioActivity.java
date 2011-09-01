/*
 * Copyright 2011 Ben Gidley
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package uk.co.gidley.clockRadio;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import com.flurry.android.FlurryAgent;
import de.akquinet.android.androlog.Log;
import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;
import uk.co.gidley.clockRadio.RadioPlayerService.State;
import uk.co.gidley.clockRadio.RadioStationsList.OnSelectStationListener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

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

    /**
     * Called when the activity is first created.
     */
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