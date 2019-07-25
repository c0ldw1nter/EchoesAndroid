package sat.echoes;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;

import com.un4seen.bass.BASS;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;
import java.nio.ByteBuffer;

/**
 * Created by Sat on 2017.10.13.
 */

public class Track implements Parcelable {

    public String filename;
    public String title;
    public String artist;
    public String album;
    public long length;

    public Track(String filename, String title) {
        if(title!=null)this.title=title;
        else this.title=filename;
        this.filename=filename;
    }

    @Override
    public String toString() {
        if(title!=null) {
            String ret=title;
            if(artist!=null) ret+=" - "+artist;
            if(album!=null) ret+= "["+album+"]";
            return ret;
        }else {
            return filename;
        }
    }

    public void getTags() {
        /*try {
            AudioFile f = AudioFileIO.read(new File(filename));
            Tag tag = f.getTag();
            AudioHeader ah=f.getAudioHeader();
            length=ah.getTrackLength()*1000;
            artist=tag.getFirst(FieldKey.ARTIST);
            album=tag.getFirst(FieldKey.ALBUM);
            title=tag.getFirst(FieldKey.TITLE);
        }catch (Exception e) {
            e.printStackTrace();
        }*/

        ContentResolver cr = EchoesApp.getAppContext().getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String projection[] = {MediaStore.Audio.Media.DATA,MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.ARTIST,MediaStore.Audio.Media.ALBUM,MediaStore.Audio.Media.DURATION};
        String selection = MediaStore.Audio.Media.DATA+" like ? ";
        String[] args={filename};
        Cursor cur = cr.query(uri, projection, selection, args, null);
        if(cur!=null) {
            cur.moveToNext();
            try {
                artist = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                album = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                title = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.TITLE));
                length = cur.getLong(cur.getColumnIndex(MediaStore.Audio.Media.DURATION));
            } catch (IllegalArgumentException e) {
                Log.d("FAILED", filename);
            }
        }

    }

    public int describeContents(){
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[]{this.filename,
                this.title,
                this.artist,
                this.album});
        dest.writeLong(length);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Track createFromParcel(Parcel in) {
            return new Track(in);
        }

        public Track[] newArray(int size) {
            return new Track[size];
        }
    };

    public Track(Parcel in){
        String[] data = new String[4];

        in.readStringArray(data);
        // the order needs to be the same as in writeToParcel() method
        this.filename = data[0];
        this.title = data[1];
        this.artist = data[2];
        this.album=data[3];
        this.length=in.readLong();
    }
}
