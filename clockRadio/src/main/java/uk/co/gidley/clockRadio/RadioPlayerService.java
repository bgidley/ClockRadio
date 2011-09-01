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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import com.google.inject.Inject;
import de.akquinet.android.androlog.Log;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import roboguice.service.RoboService;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class RadioPlayerService extends RoboService {

    private static final String TAG = "RadioPlayerService";

    private final IBinder mBinder = new LocalBinder();

    @Inject
    private PowerManager pm;
    @Inject
    private NotificationManager notificationManager;

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

    private WakeLock wl;

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
        super.onCreate();
        state = State.STOPPED;

    }

    @Override
    public void onDestroy() {
        releaseWakeLock();
        hideNotification();
        super.onDestroy();
    }

    private void hideNotification() {
        // Cancel the persistent notification.
        notificationManager.cancel(NOTIFICATION);
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
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
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

        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wl.acquire();
    }

    private String parsePls(String playerUri) {
        HttpClient httpclient = new DefaultHttpClient();

        try {
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
        if (mp != null) {
            mp.stop();
            mp.release();
            mp = null;
            stopForeground(true);
            hideNotification();
            releaseWakeLock();

            setState(State.STOPPED);
        }
    }

    private void releaseWakeLock() {
        if (wl != null) {
            if (wl.isHeld()) {
                Log.d(TAG, "Wake Lock Released");
                wl.release();
            }
            wl = null;
        }
    }
}
