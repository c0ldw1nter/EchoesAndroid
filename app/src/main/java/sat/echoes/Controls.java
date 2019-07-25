package sat.echoes;

import android.content.Intent;

public class Controls {
    public static void PlayPause() {
        DoAction(EchoService.ACTION_PLAY);
    }
    public static void Advance() {
        DoAction(EchoService.ACTION_NEXT);
    }
    public static void Previous() {
        DoAction(EchoService.ACTION_PREV);
    }
    public static void Stop() {
        DoAction(EchoService.ACTION_STOP);
    }
    public static void Terminate() {
        DoAction(EchoService.ACTION_STOPSERVICE);
    }
    private static void DoAction(String action) {
        Intent intent=new Intent(EchoesApp.getAppContext(), EchoService.class);
        intent.setAction(action);
        EchoesApp.getAppContext().startService(intent);
    }
}
