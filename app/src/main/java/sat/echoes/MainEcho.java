package sat.echoes;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.un4seen.bass.BASS;

import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.net.Uri;
import android.provider.MediaStore;
import android.database.Cursor;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import android.content.ContentResolver;

public class MainEcho extends AppCompatActivity {
    public Handler handler;
    public int seekBarPoints=1000;
    public boolean seekBarPressed=false;
    Random rng=new Random();


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }else {
            StartupProcedure();
        }

    }

    void StartupProcedure() {
        Utils.updateActivity(this);

        Utils.startService(this);

        handler = new Handler();
        handler.postDelayed(runnable, 100);

        //this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main_echo);

        //Setting button events
        (findViewById(R.id.PlayButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(MainEcho.this, EchoService.class);
                intent.setAction(EchoService.ACTION_PLAY);
                startService(intent);
                /*if(!EchoService.streamLoaded) LoadAudioFile(EchoService.playlist.get(EchoService.nowPlayingIndex));
                PlayPause();*/
            }
        });
        (findViewById(R.id.NextTrackButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(MainEcho.this, EchoService.class);
                intent.setAction(EchoService.ACTION_NEXT);
                startService(intent);
            }
        });
        (findViewById(R.id.PrevTrackButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(MainEcho.this, EchoService.class);
                intent.setAction(EchoService.ACTION_PREV);
                startService(intent);
            }
        });
        (findViewById(R.id.ShuffleButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Shuffle();
            }
        });
        (findViewById(R.id.ListButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenPlaylistsWindow();
            }
        });

        //setting listview events
        final ListView theListView=(ListView)findViewById(R.id.TrackList);
        theListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Track t = (Track) parent.getItemAtPosition(position);

                /*for(int i=0;i<parent.getChildCount();i++) {
                    parent.getChildAt(i).setBackgroundColor(0xFF181818);
                }
                view.setBackgroundColor(0x4496e6ff);*/
                t.getTags();
                Intent intent=new Intent(MainEcho.this, EchoService.class);
                intent.putExtra("Track",t);
                intent.putExtra("Play",true);
                intent.setAction(EchoService.ACTION_PLAYFILE);
                startService(intent);
                /*LoadAudioFile(t);
                Play();*/
            }
        });

        SeekBar sb=(SeekBar)findViewById(R.id.seekBar);
        sb.setMax(seekBarPoints);
        sb.setProgress(0);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(seekBarPressed) SetPositionPercentage((float)progress/ (float)seekBar.getMax());
                UpdateTime();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seekBarPressed=true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBarPressed=false;
            }
        });

        EchoService.playlists=Utils.GetAllPlaylists(this);
        //EchoService.playlist=Utils.GetAllTracks(this);
        RefreshPlaylistGrid();
        RefreshPlayIcon();
        Utils.updateMainEchoCenterText();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(grantResults[0]==0) StartupProcedure();
        else {
            finish();
        }
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if(EchoService.Playing() && !seekBarPressed) {
                SeekBar sb=(SeekBar)findViewById(R.id.seekBar);
                sb.setProgress(GetPlayPercentage());
                UpdateTime();
            }
            handler.postDelayed(this, 100);
        }
    };

    void UpdateTime() {
        TextView tv=(TextView) findViewById(R.id.timeView);
        String txt="";
        if(!EchoService.streamLoaded) txt="00:00";
        else txt= new SimpleDateFormat("mm:ss").format(new Date((long)(BASS.BASS_ChannelBytes2Seconds(EchoService.stream,BASS.BASS_ChannelGetPosition(EchoService.stream,0))*1000)));
        tv.setText(txt);
    }

    public void ShowInfo() {
        AlertDialog.Builder infobuilder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            infobuilder = new AlertDialog.Builder(MainEcho.this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            infobuilder = new AlertDialog.Builder(MainEcho.this);
        }
        infobuilder.setTitle("Autorius")
                .setMessage("Viktoras Lukšas\nKVK I 12-2")
                .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                    }
                }).show();
    }

    public void RefreshPlaylistGrid() {
        try {
            ListView lstView = (ListView) findViewById(R.id.TrackList);
            lstView.setAdapter(new TrackAdapter(this, R.layout.tracklist_row, EchoService.playlist));
            TextView tv=(TextView)findViewById(R.id.totalTracksTxt);
            tv.setText(EchoService.playlist.size()+" tracks");
        }catch (Exception e) {

        }
    }

    void RefreshPlayIcon() {
        ImageButton playBtn=(ImageButton)findViewById(R.id.PlayButton);
        if(EchoService.Playing()) playBtn.setImageResource(R.drawable.pause);
        else playBtn.setImageResource(R.drawable.play);
    }

    void SetCenterText(String text) {
        TextView textThing = (TextView) findViewById(R.id.textThingy);
        textThing.setText(text);
    }

    void OpenPlaylistsWindow() {
        startActivity(new Intent(this, PlaylistActivity.class));
    }



    void SetPositionPercentage(float pct) {
        if(!EchoService.streamLoaded) return;
        //SeekBar sb=(SeekBar)findViewById(R.id.seekBar);
        BASS.BASS_ChannelSetPosition(EchoService.stream, (int)(BASS.BASS_ChannelGetLength(EchoService.stream,0)*pct),0);
        //sb.setProgress((int)(sb.getMax()*pct));
    }

    int GetPlayPercentage() {
        if(!EchoService.Playing()) return 0;
        float pct=(float)BASS.BASS_ChannelGetPosition(EchoService.stream, 0)/ (float)BASS.BASS_ChannelGetLength(EchoService.stream, 0);
        pct*=seekBarPoints;
        return (int)pct;
    }

    void Shuffle() {
        ArrayList<Track> newPlist=new ArrayList<>();
        while(EchoService.playlist.size()>0) {
            newPlist.add(EchoService.playlist.remove(rng.nextInt(EchoService.playlist.size())));
        }
        EchoService.playlist=newPlist;
        RefreshPlaylistGrid();
    }

}
