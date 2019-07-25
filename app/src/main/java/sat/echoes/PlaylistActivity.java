package sat.echoes;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class PlaylistActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);

        (findViewById(R.id.BackButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        try {
            ListView lstView = (ListView) findViewById(R.id.PlaylistList);
            lstView.setAdapter(new ListAdapter(this, R.layout.playlist_row, EchoService.playlists));
        }catch (Exception e) {

        }
        final ListView theListView=(ListView)findViewById(R.id.PlaylistList);
        theListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Playlist p = (Playlist) parent.getItemAtPosition(position);
                EchoService.playlist=p.getTracks();
                try {
                    MainEcho me = (MainEcho) Utils.mainEchoRef.get();
                    me.RefreshPlaylistGrid();
                    finish();
                }catch (Exception e) {
                    e.printStackTrace();
                    //Log.d("SUPER BLYAT",e.printStackTrace());
                }
                /*for(int i=0;i<parent.getChildCount();i++) {
                    parent.getChildAt(i).setBackgroundColor(0xFF181818);
                }
                view.setBackgroundColor(0x4496e6ff);*/

            }
        });
    }
}
