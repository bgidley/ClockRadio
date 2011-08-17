package uk.co.gidley.clockRadio;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class SleepTimerReciever extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Intent stopPlay = new Intent(context, RadioPlayerService.class);
		stopPlay.putExtra(RadioPlayerService.STOP, true);
		context.startService(stopPlay);
	}
}
