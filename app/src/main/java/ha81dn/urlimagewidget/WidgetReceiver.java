package ha81dn.urlimagewidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;


public class WidgetReceiver extends AppWidgetProvider {
    static AsyncTask<String, Void, Bitmap> currentTask;

    private static void prepareWidget(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            applyOnClick(context, remoteViews, widgetId);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
        //showForecast(context, appWidgetManager, appWidgetManager.getAppWidgetIds(new ComponentName(context, WidgetReceiver.class)));
    }

    private static void applyOnClick(Context context, RemoteViews remoteViews, int widgetId) {
        Intent intent = new Intent(context, WidgetReceiver.class);
        intent.setAction("com.ha81dn.urlimagewidget.UPDATE");
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, widgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.update, pendingIntent);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, WidgetReceiver.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        prepareWidget(context, appWidgetManager, allWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent.getAction().equals("com.ha81dn.urlimagewidget.UPDATE")) {
            int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (widgetId == -1) return;
            /*if (!recentlyClicked) {
                retries = 0;
                getData(context);
            }*/

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            currentTask = new HttpAsyncTask();
            ((HttpAsyncTask) currentTask).context = context;
            ((HttpAsyncTask) currentTask).appWidgetManager = appWidgetManager;
            ((HttpAsyncTask) currentTask).targetWidgetId = widgetId;
            currentTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, sharedPref.getString("source_" + widgetId, ""));
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        prepareWidget(context, appWidgetManager, appWidgetIds);
        for (int widgetId : appWidgetIds) {
            Intent intent = new Intent(context, WidgetReceiver.class);
            intent.setAction("com.ha81dn.urlimagewidget.UPDATE");
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            context.sendBroadcast(intent);
        }
    }

    private static class HttpAsyncTask extends AsyncTask<String, Void, Bitmap> {
        AppWidgetManager appWidgetManager;
        Context context;
        int targetWidgetId;
        long lastTime;

        @Override
        protected Bitmap doInBackground(String... urls) {
            try {
                if (urls[0].equals("")) return null;
                URL myFileUrl = new URL(urls[0]);
                HttpsURLConnection conn = (HttpsURLConnection) myFileUrl.openConnection();
                conn.setDoInput(true);
                conn.connect();
                InputStream is = conn.getInputStream();
                return BitmapFactory.decodeStream(is);
            } catch (Exception error) {
                Toast.makeText(context, error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... voids) {
            /*
            try {
                long now = System.currentTimeMillis();
                int idxFrom = 0, idxTo = 11;
                if (now - lastTime >= 100) {
                    lastTime = now;
                    RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
                    applyOnClick(context, remoteViews);
                    //remoteViews.setTextViewText(R.id.update, context.getString(R.string.data_fetch, dots[idx]));

                    ComponentName thisWidget = new ComponentName(context, WidgetReceiver.class);
                    int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
                    for (int widgetId : allWidgetIds) {
                        appWidgetManager.updateAppWidget(widgetId, remoteViews);
                    }
                }
            } catch (Exception ignore) {
            }
            */
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (isCancelled() || result == null) return;
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            applyOnClick(context, remoteViews, targetWidgetId);
            //remoteViews.setTextViewText(R.id.update, context.getString(R.string.data_fetch, dots[idx]));
            remoteViews.setImageViewBitmap(R.id.update, result);
            SimpleDateFormat sdf;
            Calendar now;
            sdf = new SimpleDateFormat("dd.MM.yyyy, HH:mm 'Uhr'", Locale.getDefault());
            now = Calendar.getInstance();
            remoteViews.setTextViewText(R.id.timestamp, sdf.format(now.getTime()));
            remoteViews.setTextViewText(R.id.location, sharedPref.getString("name_" + targetWidgetId, ""));

            ComponentName thisWidget = new ComponentName(context, WidgetReceiver.class);
            int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            for (int widgetId : allWidgetIds) {
                if (widgetId == targetWidgetId)
                    appWidgetManager.updateAppWidget(widgetId, remoteViews);
            }
        }
    }
}
