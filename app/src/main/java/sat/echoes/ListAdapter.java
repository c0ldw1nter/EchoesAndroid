package sat.echoes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Sat on 2017.10.13.
 */

public class ListAdapter extends ArrayAdapter<Playlist> {
    public ListAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }
    public ListAdapter(Context context, int resource, List<Playlist> items) {
        super(context, resource, items);
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.playlist_row, null);
        }

        Playlist p = getItem(position);

        if (p != null) {
            TextView tt1 = (TextView) v.findViewById(R.id.playlistName);

            if (tt1 != null) {
                /*String composedText=new String();
                if(p.title!=null){
                    composedText+=p.title;
                    if(p.artist!=null) {
                        composedText+=" - "+p.artist;
                    }
                }else {
                    composedText=p.filename;
                }*/
                tt1.setText(p.file.getName());
            }
        }

        return v;
    }

}
