package sat.echoes;

import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.RemoteViews;

import java.lang.ref.WeakReference;
import java.util.Random;

/**
 * Created by Sat on 2017.10.14.
 */

public class WidgetReceiver extends AppWidgetProvider {
    private static WeakReference<Activity> mainEchoRef;
    public static void updateActivity(Activity activity) {
        mainEchoRef = new WeakReference<Activity>(activity);
    }
    private static final String ACTION_CLICK_PLAY = "ACTION_CLICK_PLAY";
    private static final String ACTION_CLICK_NEXT = "ACTION_CLICK_NEXT";
    private static final String ACTION_CLICK_PREV = "ACTION_CLICK_PREV";

    @Override
    public void onEnabled(Context context) {
        ForceUpdate(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {

        // Get all ids
        ComponentName thisWidget = new ComponentName(context,
                WidgetReceiver.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int widgetId : allWidgetIds) {
            /* //create some random data
            int number = (new Random().nextInt(100));


            Log.w("WidgetExample", String.valueOf(number));
            // Set the text
            remoteViews.setTextViewText(R.id.update, String.valueOf(number));

            */

            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_relative_echo);

            Intent intent = new Intent(context, WidgetReceiver.class);
            intent.setAction(ACTION_CLICK_PLAY);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
            remoteViews.setOnClickPendingIntent(R.id.WidgetPlayButton, pendingIntent);

            intent = new Intent(context, WidgetReceiver.class);
            intent.setAction(ACTION_CLICK_NEXT);
            pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
            remoteViews.setOnClickPendingIntent(R.id.WidgetNextButton, pendingIntent);

            intent = new Intent(context, WidgetReceiver.class);
            intent.setAction(ACTION_CLICK_PREV);
            pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
            remoteViews.setOnClickPendingIntent(R.id.WidgetPreviousButton, pendingIntent);

            UpdateComponents(remoteViews);

            //remoteViews.setTextViewText(R.id.WidgetTitleText, "asdf");

            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }

    void UpdateComponents(RemoteViews rv) {
        MainEcho me=((MainEcho)mainEchoRef.get());
        if(me.Playing()) rv.setImageViewResource(R.id.WidgetPlayButton, R.drawable.pause);
        else rv.setImageViewResource(R.id.WidgetPlayButton, R.drawable.play);
        if(me.nowPlaying!=null) {
            rv.setTextViewText(R.id.WidgetTitleText, me.nowPlaying.title);
            rv.setTextViewText(R.id.WidgetArtistText, me.nowPlaying.artist);
        }else if(me.playlist!=null && me.playlist.size()>0) {
            rv.setTextViewText(R.id.WidgetTitleText, me.playlist.get(0).title);
            rv.setTextViewText(R.id.WidgetArtistText, me.playlist.get(0).artist);
        }else {
            rv.setTextViewText(R.id.WidgetTitleText, "");
            rv.setTextViewText(R.id.WidgetArtistText, "");
        }

    }

    void ForceUpdate(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context.getApplicationContext());
        ComponentName thisWidget = new ComponentName(context.getApplicationContext(), WidgetReceiver.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        if (appWidgetIds != null && appWidgetIds.length > 0) {
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent){
        super.onReceive(context, intent);
        MainEcho me=(MainEcho)mainEchoRef.get();
        if (intent.getAction().equals(ACTION_CLICK_PLAY)) {
            me.PlayPause();
        }else if(intent.getAction().equals(ACTION_CLICK_NEXT)) {
            me.Advance();
        }else if(intent.getAction().equals(ACTION_CLICK_PREV)){
            me.Previous();
        }
        ForceUpdate(context);
    }
}
