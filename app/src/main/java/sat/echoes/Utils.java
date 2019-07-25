package sat.echoes;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class Utils {

    public final static String FILENAME_STATE = "state.echo";

    public static WeakReference<Activity> mainEchoRef;
    public static void updateActivity(Activity activity) {
        mainEchoRef = new WeakReference<>(activity);
    }
    public static WeakReference<Service> echoServRef;
    public static void updateService(Service service) {
        echoServRef = new WeakReference<>(service);
    }

    public static void updateWidget(Context context) {
        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_relative_echo);
            ComponentName thisWidget = new ComponentName(context, WidgetReceiver.class);
            WidgetReceiver.UpdateComponents(remoteViews);
            appWidgetManager.updateAppWidget(thisWidget, remoteViews);
        }catch (Exception e) {}
    }

    public static boolean isServiceRunning(Context ctx, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void updateMainEchoCenterText() {
        try {
            MainEcho me = (MainEcho) Utils.mainEchoRef.get();
            me.SetCenterText(EchoService.nowPlaying.toString());
            me.UpdateTime();
        }catch (Exception e) {}
    }

    public static void updateMainEchoIcon() {
        try {
            MainEcho me = (MainEcho) mainEchoRef.get();
            me.RefreshPlayIcon();
        }catch (Exception e) {}
    }

    public static void updateMainEchoGrid() {
        try {
            MainEcho me = (MainEcho) mainEchoRef.get();
            me.RefreshPlaylistGrid();
        }catch (Exception e) {}
    }

    public static void loadPlaylistState(Context ctx) {
        File file = new File(ctx.getFilesDir(), FILENAME_STATE);

        if(!file.exists()) {
            EchoService.playlist=GetAllTracks(ctx);
            return;
        }

        try {
            BufferedReader br =new BufferedReader(new FileReader(file));
            int currentIndex=Integer.parseInt(br.readLine());
            int seekPosition=Integer.parseInt(br.readLine());
            String line;
            ArrayList<Track> playlist=new ArrayList<>();
            while((line=br.readLine())!=null) {
                if(line.length()==0) continue;
                File f=new File(line);
                Track t=new Track(line,f.getName());
                //t.getTags();
                playlist.add(t);
            }

            GetTagsForList(ctx, playlist);

            EchoService.playlist=playlist;

            Intent intent=new Intent(ctx, EchoService.class);
            intent.setAction(EchoService.ACTION_PLAYFILE);
            intent.putExtra("Track",playlist.get(currentIndex));
            intent.putExtra("Position",seekPosition);
            ctx.startService(intent);

            br.close();
        }catch (Exception e) {
            e.printStackTrace();
            Log.d("Super fail" , "\n\n\nITS RELOADING DAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMN");
            EchoService.playlist=GetAllTracks(ctx);
        }
        Toast.makeText(EchoesApp.getAppContext(),"Echoes: snapshot loaded",Toast.LENGTH_SHORT);
    }

    public static void savePlaylistState(Context ctx, int currentIndex, long seekPosition, ArrayList<Track> playlist) {
        File file = new File(ctx.getFilesDir(), FILENAME_STATE);

        if(!file.exists()) try {
            file.createNewFile();
        }catch(Exception e) {
            e.printStackTrace();
        }
        try {
            FileWriter fw=new FileWriter(file);
            fw.write(currentIndex+"\n");
            fw.write(seekPosition+"\n");
            for (Track t : playlist) {
                fw.write(t.filename+"\n");
            }
            fw.flush();
            fw.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(EchoesApp.getAppContext(),"Echoes: snapshot saved",Toast.LENGTH_SHORT);
    }

    public static void startService(Context ctx) {
        if(!EchoService.SERVICE_RUNNING) {
            Intent intent=new Intent(ctx, EchoService.class);
            intent.setAction(EchoService.ACTION_STARTSERVICE);
            EchoService.SERVICE_RUNNING=true;
            ctx.startService(intent);
        }
    }

    public static ArrayList<Playlist> GetAllPlaylists(Context ctx) {
        ArrayList<Playlist> ret=new ArrayList<>();
        ContentResolver cr = ctx.getContentResolver();
        Uri uri = MediaStore.Files.getContentUri("external");

        // every column, although that is huge waste, you probably need
        // BaseColumns.DATA (the path) only.
        String[] projection = {MediaStore.Files.FileColumns.DATA};

        // exclude media files, they would be here also.
        String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_PLAYLIST;
        String[] selectionArgs = null; // there is no ? in selection so null here

        String sortOrder = null; // unordered
        Cursor cur = cr.query(uri, projection, selection, selectionArgs, sortOrder);

        int count;
        if(cur != null)
        {
            count = cur.getCount();

            if(count > 0)
            {
                while(cur.moveToNext())
                {
                    String str=cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA));
                    if(str.toLowerCase().endsWith(".m3u")) {
                        ret.add(new Playlist(str));

                        Log.d("S", ret.get(ret.size()-1).file.getParent());
                    }
                }
            }
        }
        Log.d("SUPERTAG BLYAT", "OI BIEEEEEETCH \n\n\n"+ret.size()+"\n\n\n");
        cur.close();
        return ret;
    }

    public static Track GetTrackFromFilename(String filename, ArrayList<Track> list) throws IllegalArgumentException {
        for(Track t : list) {
            if(filename.equals(t.filename)) return t;
        }
        throw new IllegalArgumentException();
    }

    public static void GetTagsForList(Context ctx, ArrayList<Track> plist) {
        ContentResolver cr = ctx.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String projection[] = {MediaStore.Audio.Media.DATA,MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.ARTIST,MediaStore.Audio.Media.ALBUM,MediaStore.Audio.Media.DURATION};
        String selection = MediaStore.Audio.Media.IS_MUSIC+" != 0";
        /*String[] args=new String[plist.size()];
        for(int i=0;i<plist.size();i++) {
            args[i]=plist.get(i).filename;
        }*/
        Cursor cur = cr.query(uri, projection, selection, null, null);
        if(cur!=null) {
            while(cur.moveToNext()) {
                String data = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA));
                try {
                    Track t=GetTrackFromFilename(data,plist);
                    t.artist=cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    t.album=cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                    t.title=cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    t.length=cur.getLong(cur.getColumnIndex(MediaStore.Audio.Media.DURATION));
                }catch (IllegalArgumentException e) {
                    //Log.d("FAILED",data);
                }
            }
        }
    }

    public static ArrayList<Track> GetAllTracks(Context ctx) {
        ArrayList<Track>ret=new ArrayList<>();
        ContentResolver cr = ctx.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String projection[] = {MediaStore.Audio.Media.DATA,MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.ARTIST,MediaStore.Audio.Media.ALBUM,MediaStore.Audio.Media.DURATION};
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        Cursor cur = cr.query(uri, projection, selection, null, null);
        if(cur != null) {
            while (cur.moveToNext()) {
                String data = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA));
                Track t = new Track(data, cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.TITLE)));
                t.artist = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                t.album = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                t.length = cur.getLong(cur.getColumnIndex(MediaStore.Audio.Media.DURATION));
                ret.add(t);
            }
        }
        //SetCenterText(trackList.size()+"");
        /*fileList=new String[playlist.size()];
        for(int i=0;i<playlist.size();i++) {
            fileList[i]=playlist.get(i).filename;
        }*/
        cur.close();
        return ret;
    }

    public static String GetExtension(String filePath) {
        String extension = "";

        int i = filePath.lastIndexOf('.');
        if (i > 0) {
            extension = filePath.substring(i+1);
        }
        return extension;
    }
}
