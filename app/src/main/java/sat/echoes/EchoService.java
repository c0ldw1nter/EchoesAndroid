package sat.echoes;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.un4seen.bass.BASS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EchoService extends Service {
    public static final int SERVICE_ID=1;
    public static final String ACTION_STARTSERVICE="ACTION_STARTSERVICE";
    public static final String ACTION_STOPSERVICE="ACTION_STOPSERVICE";
    public static final String ACTION_PLAY="ACTION_PLAY";
    public static final String ACTION_PLAYFILE="ACTION_PLAYFILE";
    public static final String ACTION_NEXT="ACTION_NEXT";
    public static final String ACTION_PREV="ACTION_PREV";
    public static final String ACTION_STOP="ACTION_STOP";

    public static boolean SERVICE_RUNNING=false;

    public static int stream;
    public static boolean streamLoaded=false;
    public static List<String> supportedAudioTypes = Arrays.asList( "mp3", "mp2", "mp1", "wav", "ogg", "m4a" ) ;
    public static ArrayList<Track> playlist=new ArrayList<>();
    public static ArrayList<Playlist> playlists=new ArrayList<>();
    public static Track nowPlaying;

    public static BASS.SYNCPROC endSync;

    Handler handler=new Handler();
    public PowerManager.WakeLock wl;
    BroadcastReceiver headphoneReceiver;

    private Runnable automatedStop = new Runnable() {
        @Override
        public void run() {
            if(Playing()) {
                scheduleTerminationCheck();
                return;
            }
            Log.d("SUPERTAG","AUTOMATED STOP");
            stopForeground(true);
            stopSelf();
        }
    };

    void scheduleTerminationCheck() {
        handler.postDelayed(automatedStop, 10000);
    }

    /*void scheduleTermination() {
        handler.postDelayed(automatedStop, 10000);
    }

    void unscheduleTermination() {
        handler.removeCallbacksAndMessages(null);
    }*/

    @Override
    public void onCreate() {
        super.onCreate();
        Utils.updateService(this);
        try {
            if (Utils.mainEchoRef.get() == null || playlist.size() == 0) {
                Utils.loadPlaylistState(this);
            }
        }catch (Exception e) {
            Utils.loadPlaylistState(this);
        }
        if(playlist.size()==0) stopSelf();
        Utils.updateMainEchoGrid();
        GetWakeLockAndRegisterTerminator();
        scheduleTerminationCheck();
    }

    void GetWakeLockAndRegisterTerminator() {
        PowerManager pm = (PowerManager) Utils.echoServRef.get().getSystemService(Context.POWER_SERVICE);
        wl=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Echoes");
        wl.acquire();
        headphoneReceiver=new TerminatorReceiver();
        IntentFilter intentFilter=new IntentFilter();
        intentFilter.addAction(Intent.ACTION_HEADSET_PLUG);
        intentFilter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        this.registerReceiver(headphoneReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        if(playlist!=null && playlist.size()>0 && playlist.indexOf(nowPlaying)!=-1) {
            Utils.savePlaylistState(this, playlist.indexOf(nowPlaying),GetPosition(), playlist);
        }
        Utils.updateWidget(this);
        Log.d("SUPERTAG","SERVICE DESTROYED");
        wl.release();
        this.unregisterReceiver(headphoneReceiver);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {Log.d("SUPERTAG", "Intent action: "+intent.getAction()); }catch (Exception e) {}
        Utils.updateService(this);
        if(intent==null || intent.getAction()==null) {
            stopForeground(true);
            stopSelf();
            return Service.START_STICKY;
        }
        if(intent.getAction().equals(ACTION_STOPSERVICE)) {
            SERVICE_RUNNING = false;
            //unscheduleTermination();
            stopForeground(true);
            stopSelf();
        }else {
            if(!wl.isHeld()) {
                wl.acquire();
                Log.d("SUPERTAG","Had to reacquire wake lock. dafuq");
            }
            if (intent.getAction().equals(ACTION_STARTSERVICE)) {
                SERVICE_RUNNING = true;
                Utils.updateMainEchoGrid();
                /*if (!Playing()) scheduleTermination();
                else unscheduleTermination();*/
                showNotification();
            } else if (intent.getAction().equals(ACTION_PLAY)) {
                if (!streamLoaded) {
                    if (nowPlaying == null) nowPlaying = playlist.get(0);
                    LoadAudioFile(nowPlaying);
                }
                PlayPause();
                /*if (Playing()) unscheduleTermination();
                else scheduleTermination();*/
            } else if (intent.getAction().equals(ACTION_PLAYFILE)) {
                Track t = intent.getExtras().getParcelable("Track");
                LoadAudioFile(t);
                SetPosition(intent.getExtras().getLong("Position"));
                if (intent.getExtras().getBoolean("Play")) {
                    Play();
                }
                Utils.updateMainEchoGrid();
                //unscheduleTermination();
            } else if (intent.getAction().equals(ACTION_NEXT)) {
                Advance();
                //unscheduleTermination();
            } else if (intent.getAction().equals(ACTION_PREV)) {
                Previous();
                //unscheduleTermination();
            } else if (intent.getAction().equals(ACTION_STOP)) {
                Pause();
                //scheduleTermination();
            }
        }
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        //TODO for communication return IBinder implementation
        return null;
    }

    static private void showNotification() {
        Intent notificationIntent = new Intent(EchoesApp.getAppContext(), EchoService.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(EchoesApp.getAppContext(), 0,
                notificationIntent, 0);

        Bitmap icon = BitmapFactory.decodeResource(EchoesApp.getAppContext().getResources(),
                R.drawable.logo_smallicon);

        Intent prevIntent=new Intent(EchoesApp.getAppContext(), EchoService.class);
        prevIntent.setAction(EchoService.ACTION_PREV);
        PendingIntent pPrevIntent=PendingIntent.getService(EchoesApp.getAppContext(), 0, prevIntent, 0);

        Intent playIntent=new Intent(EchoesApp.getAppContext(), EchoService.class);
        playIntent.setAction(EchoService.ACTION_PLAY);
        PendingIntent pPlayIntent=PendingIntent.getService(EchoesApp.getAppContext(), 0, playIntent, 0);

        Intent nextIntent=new Intent(EchoesApp.getAppContext(), EchoService.class);
        nextIntent.setAction(EchoService.ACTION_NEXT);
        PendingIntent pNextIntent=PendingIntent.getService(EchoesApp.getAppContext(), 0, nextIntent, 0);

        String txt="<nothing>";
        if(nowPlaying!=null) txt=nowPlaying.toString();
        String state="";
        if(nowPlaying!=null) state="Paused";
        int notifPlayPauseIcon;
        String notifPlayPauseText="Play";
        if(Playing()) {
            state="Playing";
            notifPlayPauseIcon=R.drawable.pause;
            notifPlayPauseText="Pause";
        }else {
            notifPlayPauseIcon=R.drawable.play;
        }
        Notification notification = new NotificationCompat.Builder(EchoesApp.getAppContext())
                .setContentTitle("Echoes")
                .setTicker(state)
                .setSmallIcon(R.drawable.logo_smallicon)
                .setContentText(txt)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.back,"Prev",pPrevIntent)
                .addAction(notifPlayPauseIcon, notifPlayPauseText, pPlayIntent)
                .addAction(R.drawable.forward, "Next",pNextIntent)
                .setPriority(Notification.PRIORITY_MAX)
                .setOngoing(true).build();
        Utils.echoServRef.get().startForeground(SERVICE_ID, notification);
        Log.d("SUPERTAG", "notification shown: "+txt+", state: "+state);
    }

    static void InitSoundDevice() {
        BASS.BASS_Init(-1, 44100, BASS.BASS_DEVICE_DEFAULT);
    }

    public static void LoadAudioFile(Track t) {
        if (streamLoaded) BASS.BASS_ChannelStop(stream);
        BASS.BASS_Free();
        InitSoundDevice();
        if(supportedAudioTypes.indexOf(Utils.GetExtension(t.filename))==-1) {
            //SetCenterText("File type unsupported");
            return;
        }
        stream = BASS.BASS_StreamCreateFile(t.filename, 0, 0, BASS.BASS_DEVICE_DEFAULT);
        if(BASS.BASS_ErrorGetCode()!=0) {
            return;
        }

        endSync = new BASS.SYNCPROC() {
            @Override
            public void SYNCPROC(int handle, int channel, int data, Object user) {
                Controls.Advance();
            }
        };

        BASS.BASS_ChannelSetSync(stream, BASS.BASS_SYNC_END, 0, endSync, null);

        streamLoaded=true;
        nowPlaying=t;
        Utils.updateMainEchoCenterText();
        Utils.updateWidget(Utils.echoServRef.get());
    }

    public static void  PlayPause() {
        if(Playing()) {
            Pause();
        }else {
            Play();
        }
    }

    public static void  Play() {
        if(playlist==null || playlist.size()==0) return;
        if (streamLoaded && !Playing()) BASS.BASS_ChannelPlay(stream, false);
        else if(!streamLoaded && playlist!=null && playlist.size()>0) LoadAudioFile(playlist.get(0));
        showNotification();
        Utils.updateMainEchoIcon();
        Utils.updateWidget(Utils.echoServRef.get());
        Log.d("SUPERTAG","It's playing...");
    }

    public static void  Pause() {
        if (Playing()) BASS.BASS_ChannelPause(stream);
        showNotification();
        Utils.updateMainEchoIcon();
        Utils.updateWidget(Utils.echoServRef.get());
        Log.d("SUPERTAG","It's paused...");
    }

    static int GetPlayingIndex() {
        if(nowPlaying==null) return -1;
        for(Track t : playlist) {
            if(nowPlaying.filename.equals(t.filename)) return playlist.indexOf(t);
        }
        return -1;
    }

    public static void Advance() {
        if(playlist==null || playlist.size()==0) return;
        int index=GetPlayingIndex();
        if(index==-1) index=0;
        index++;
        if(index>playlist.size()-1) index=0;
        LoadAudioFile(playlist.get(index));
        Play();
    }

    public static void Previous() {
        if(playlist==null || playlist.size()==0) return;
        int index=GetPlayingIndex();
        if(index==-1) index=0;
        index--;
        if(index<0) index=playlist.size()-1;
        LoadAudioFile(playlist.get(index));
        Play();
    }

    public static boolean Playing() {
        return BASS.BASS_ChannelIsActive(stream) == BASS.BASS_ACTIVE_PLAYING;
    }

    public static void SetPosition(long pos) {
        if(!streamLoaded) return;
        if(BASS.BASS_ChannelGetLength(stream, 0)<pos) return;
        BASS.BASS_ChannelSetPosition(stream, pos,0);
    }

    public static long GetPosition() {
        if(!streamLoaded) return 0;
        return BASS.BASS_ChannelGetPosition(stream,0);
    }
}
