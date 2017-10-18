package sat.echoes;

/**
 * Created by Sat on 2017.10.13.
 */

public class Track {

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
}
