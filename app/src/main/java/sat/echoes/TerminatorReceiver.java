package sat.echoes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

public class TerminatorReceiver extends BroadcastReceiver {

    public TerminatorReceiver() {
        Log.d("SUPERTAG","terminator launched");
    }

    @Override
    public void onReceive (Context context, Intent intent) {
        if(isInitialStickyBroadcast() || Utils.echoServRef==null || Utils.echoServRef.get()==null) return;
        if(intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
            int state = intent.getIntExtra("state", -1);
            if (state == 0) {
                Log.d("SUPERTAG", "Headphones unplugged event detected.");
                Controls.Stop();
                return;
            }else {
                Log.d("SUPERTAG", "Headphones plugged in event detected.");
            }
        }else {
            String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
            String str="Phone state changed.";
            if(stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                str+=" ringing, so stopping echoes";
                Controls.Stop();
            }
            Log.d("SUPERTAG", str);
        }
    }
}
