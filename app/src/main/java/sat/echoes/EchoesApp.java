package sat.echoes;

import android.app.Application;
import android.content.Context;

public class EchoesApp extends Application {
    private static Context context;

    public void onCreate() {
        super.onCreate();
        EchoesApp.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return EchoesApp.context;
    }
}
