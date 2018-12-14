package com.d2kindia.backgroundtimer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.Locale;

/**
 * Timer service tracks the start and end time of timer; service can be placed into the
 * foreground to prevent it being killed when the activity goes away
 */
public class TimerService extends Service {

    private static final String TAG = TimerService.class.getSimpleName();

    // Start and end times in milliseconds
    private long startTime, endTime, pausedTime;

    // Is the service tracking time?
    private boolean isTimerRunning;

    // Foreground notification id
    private static final int NOTIFICATION_ID = 1;

    // Service binder
    private final IBinder serviceBinder = new RunServiceBinder();

    public class RunServiceBinder extends Binder {
        TimerService getService() {
            return TimerService.this;
        }
    }

    @Override
    public void onCreate() {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Creating service");
        }
        startTime = 0;
        endTime = 0;
        isTimerRunning = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Starting service");
        }
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Binding service");
        }
        return serviceBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Destroying service");
        }
    }

    /**
     * Starts the timer
     */
    public void startTimer() {
        if (!isTimerRunning) {
            SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
            endTime = prefs.getLong("end", System.currentTimeMillis() - endTime);

            startTime = System.currentTimeMillis() - endTime;
            isTimerRunning = true;
        } else {
            Log.e(TAG, "startTimer request for an already running timer");
        }
    }

    /**
     * Stops the timer
     */
    public void stopTimer() {
        if (isTimerRunning) {
            endTime = System.currentTimeMillis() - startTime;
            isTimerRunning = false;

            SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong("end", endTime);
            editor.putString("time",elapsedTime());
            editor.apply();

        } else {
            Log.e(TAG, "stopTimer request for a timer that isn't running");
        }
    }

    /**
     * @return whether the timer is running
     */
    public boolean isTimerRunning() {
        return isTimerRunning;
    }

    /**
     * Returns the  elapsed time
     *
     * @return the elapsed time in seconds
     */
    /*public long elapsedTime() {
        // If the timer is running, the end time will be zero
        return endTime > startTime ?
                (endTime - startTime) / 1000 :
                (System.currentTimeMillis() - startTime) / 1000;
    }*/
    public String elapsedTime() {
        pausedTime = endTime > startTime ? (endTime - startTime) / 1000 : (System.currentTimeMillis() - startTime) / 1000;
        int hours = (int) (pausedTime) / 3600;
        int minutes = (int) (pausedTime) / 60;
        int seconds = (int) (pausedTime) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Place the service into the foreground
     */
    public void foreground() {
        startForeground(NOTIFICATION_ID, createNotification());
    }

    /**
     * Return the service to the background
     */
    public void background() {
        stopForeground(true);
    }

    /**
     * Creates a notification for placing the service into the foreground
     *
     * @return a notification for interacting with the service when in the foreground
     */
    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle("Timer Active")
                .setContentText("Tap to return to the timer")
                .setSmallIcon(R.mipmap.ic_launcher);

        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(this, 0, resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);

        return builder.build();
    }
}
