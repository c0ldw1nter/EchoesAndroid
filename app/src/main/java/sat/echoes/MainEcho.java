package sat.echoes;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.un4seen.bass.BASS;
import java.io.File;

import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.net.Uri;
import android.provider.MediaStore;
import android.database.Cursor;
import android.util.Log;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;
import java.util.HashMap;
import java.util.List;
import java.io.FilenameFilter;
import android.content.ContentResolver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;

import android.content.pm.PackageManager;

public class MainEcho extends AppCompatActivity {
    int stream;
    int nowPlayingIndex=0;
    boolean streamLoaded=false;
    public List<String> supportedAudioTypes = Arrays.asList( "mp3", "mp2", "mp1", "wav", "ogg", "m4a" ) ;
    public List<Track> playlist;
    public String[] fileList;
    public Track nowPlaying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WidgetReceiver.updateActivity(this);

        //this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main_echo);

        //Setting button events
        (findViewById(R.id.PlayButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!streamLoaded) LoadAudioFile(playlist.get(nowPlayingIndex));
                PlayPause();
            }
        });
        (findViewById(R.id.NextTrackButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Advance();
            }
        });
        (findViewById(R.id.PrevTrackButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Previous();
            }
        });

        //setting listview events
        final ListView theListView=(ListView)findViewById(R.id.TrackList);
        theListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Track t = (Track) parent.getItemAtPosition(position);
                LoadAudioFile(t);
                Play();
            }
        });
        GetAllFiles();

    }

    public boolean Playing() {
        return BASS.BASS_ChannelIsActive(stream) == BASS.BASS_ACTIVE_PLAYING;
    }

    void RefreshPlaylistGrid() {
        try {
            ListView lstView = (ListView) findViewById(R.id.TrackList);
            lstView.setAdapter(new TrackAdapter(this, R.layout.tracklist_row, playlist));
        }catch (Exception e) {

        }
    }

    void RefreshPlayIcon() {
        ImageButton playBtn=(ImageButton)findViewById(R.id.PlayButton);
        if(Playing()) playBtn.setImageResource(R.drawable.pause);
        else playBtn.setImageResource(R.drawable.play);
    }

    void GetAllFiles() {
        playlist=new ArrayList<>();
        ContentResolver cr = getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cur = cr.query(uri, null, selection, null, sortOrder);
        int count;
        if(cur != null)
        {
            count = cur.getCount();
            if(count > 0)
            {
                while(cur.moveToNext())
                {
                    String data = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA));
                    Track t=new Track(data, cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.TITLE)));
                    t.artist=cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    t.album=cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                    t.length=cur.getLong(cur.getColumnIndex(MediaStore.Audio.Media.DURATION));
                    playlist.add(t);
                }
            }
        }
        //SetCenterText(trackList.size()+"");
        fileList=new String[playlist.size()];
        for(int i=0;i<playlist.size();i++) {
            fileList[i]=playlist.get(i).filename;
        }
        cur.close();
        RefreshPlaylistGrid();
    }

    boolean checkWriteExternalPermission()
    {
        String permission = "android.permission.READ_EXTERNAL_STORAGE";
        int res = MainEcho.this.getBaseContext().checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    void InitSoundDevice() {
        BASS.BASS_Init(-1, 44100, BASS.BASS_DEVICE_DEFAULT);
    }

    void SetCenterText(String text) {
        TextView textThing = (TextView) findViewById(R.id.textThingy);
        textThing.setText(text);
    }
    void AddCenterText(String text) {
        TextView textThing = (TextView) findViewById(R.id.textThingy);
        textThing.setText(textThing.getText()+text);
    }

    void LoadAudioFile(Track t) {
        if (streamLoaded) BASS.BASS_ChannelStop(stream);
        BASS.BASS_Free();
        InitSoundDevice();
        if(supportedAudioTypes.indexOf(GetExtension(t.filename))==-1) {
            SetCenterText("File type unsupported");
            return;
        }
        stream = BASS.BASS_StreamCreateFile(t.filename, 0, 0, BASS.BASS_DEVICE_DEFAULT);
        if(BASS.BASS_ErrorGetCode()!=0) {
            return;
        }
        streamLoaded=true;
        SetCenterText(t.toString());
        nowPlaying=t;
    }

    void PlayPause() {
        if(Playing()) {
            Pause();
        }else {
            Play();
        }
    }

    void Play() {
        if (streamLoaded && !Playing()) BASS.BASS_ChannelPlay(stream, false);
        else if(!streamLoaded && playlist!=null && playlist.size()>0) LoadAudioFile(playlist.get(0));
        RefreshPlayIcon();
    }

    void Pause() {
        if (Playing()) BASS.BASS_ChannelPause(stream);
        RefreshPlayIcon();
    }

    void Advance() {
        nowPlayingIndex++;
        if(nowPlayingIndex>playlist.size()-1) nowPlayingIndex=0;
        LoadAudioFile(playlist.get(nowPlayingIndex));
        Play();
    }

    void Previous() {
        nowPlayingIndex--;
        if(nowPlayingIndex<0) nowPlayingIndex=playlist.size()-1;
        LoadAudioFile(playlist.get(nowPlayingIndex));
        Play();
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
