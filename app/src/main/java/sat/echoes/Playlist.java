package sat.echoes;

import android.media.MediaMetadataRetriever;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import static java.lang.Long.parseLong;

/**
 * Created by Sat on 2017.10.13.
 */

public class Playlist {

    public String filename;
    public File file;
    public File directory;

    public Playlist(String filename) {
        this.filename=filename;
        file=new File(filename);
        directory=new File(file.getParent());
    }

    public ArrayList<Track> getTracks() {
        ArrayList<Track> ret=new ArrayList<>();
        try {
            BufferedReader br =new BufferedReader(new FileReader(file));
            String line;
            while((line=br.readLine())!=null) {
                if(!line.startsWith("#")) {
                    File relativeFile=new File(directory, line);
                    if(relativeFile.isFile() && relativeFile.exists()) {
                        //relative path found
                        Track t=new Track(relativeFile.getAbsolutePath(), /*metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)*/relativeFile.getName());
                        //t.getTags();
                        ret.add(t);
                    }
                }
            }
            Utils.GetTagsForList(EchoesApp.getAppContext(), ret);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }
}
