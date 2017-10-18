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

public class TrackAdapter extends ArrayAdapter<Track> {
    public TrackAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }
    public TrackAdapter(Context context, int resource, List<Track> items) {
        super(context, resource, items);
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.tracklist_row, null);
        }

        Track p = getItem(position);

        if (p != null) {
            TextView tt1 = (TextView) v.findViewById(R.id.trackText);
            TextView tt2 = (TextView) v.findViewById(R.id.artistText);
            TextView tt3 = (TextView) v.findViewById(R.id.albumText);
            TextView textLength = (TextView) v.findViewById(R.id.lengthText);

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
                if(p.title!=null) tt1.setText(p.title);
                else tt1.setText(p.filename);
                if(p.artist!=null) tt2.setText(p.artist);
                else tt2.setText("");
                if(p.album!=null) tt3.setText(p.album);
                else tt3.setText("");
                textLength.setText(String.format("%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(p.length),
                        TimeUnit.MILLISECONDS.toSeconds(p.length) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(p.length))
                ));
            }
        }

        return v;
    }

}
