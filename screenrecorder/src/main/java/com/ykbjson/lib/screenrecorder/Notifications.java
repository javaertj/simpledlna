package com.ykbjson.lib.screenrecorder;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.text.format.DateUtils;

import static android.os.Build.VERSION_CODES.O;

/**
 * Description：通知帮助类，录屏属于隐私操作，需要用户时刻注意到
 * <BR/>
 * Creator：yankebin
 * <BR/>
 * CreatedAt：2019-07-30
 */
public class Notifications extends ContextWrapper {

    public static final String ACTION_STOP = BuildConfig.APPLICATION_ID + ".action.STOP";

    protected long mLastFiredTime = 0;
    protected NotificationManager mManager;
    protected Notification.Action mStopAction;
    protected Notification.Builder mBuilder;

    public Notifications(Context context) {
        super(context);
        if (Build.VERSION.SDK_INT >= O) {
            createNotificationChannel();
        }
    }

    public void recording(long timeMs) {
        if (SystemClock.elapsedRealtime() - mLastFiredTime < 1000) {
            return;
        }
        Notification notification = getBuilder()
                .setContentText("Length: " + DateUtils.formatElapsedTime(timeMs / 1000))
                .build();
        getNotificationManager().notify(getId(), notification);
        mLastFiredTime = SystemClock.elapsedRealtime();
    }

    protected Notification.Builder getBuilder() {
        if (mBuilder == null) {
            Notification.Builder builder = new Notification.Builder(this)
                    .setContentTitle("Recording...")
                    .setOngoing(true)
                    .setLocalOnly(true)
                    .setOnlyAlertOnce(true)
                    .addAction(stopAction())
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.ic_stat_recording);
            if (Build.VERSION.SDK_INT >= O) {
                builder.setChannelId(getChannelId())
                        .setUsesChronometer(true);
            }
            mBuilder = builder;
        }
        return mBuilder;
    }

    @TargetApi(O)
    protected void createNotificationChannel() {
        NotificationChannel channel =
                new NotificationChannel(getChannelId(), getChannelName(), NotificationManager.IMPORTANCE_LOW);
        channel.setShowBadge(false);
        getNotificationManager().createNotificationChannel(channel);
    }

    protected Notification.Action stopAction() {
        if (mStopAction == null) {
            Intent intent = new Intent(ACTION_STOP);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1,
                    intent, PendingIntent.FLAG_ONE_SHOT);
            mStopAction = new Notification.Action(android.R.drawable.ic_media_pause, "Stop", pendingIntent);
        }
        return mStopAction;
    }

    public void clear() {
        mLastFiredTime = 0;
        mBuilder = null;
        mStopAction = null;
        getNotificationManager().cancelAll();
    }

    protected NotificationManager getNotificationManager() {
        if (mManager == null) {
            mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return mManager;
    }

    protected int getId(){
        return 0x1fff;
    }

    protected String getChannelId(){
        return "Recording";
    }

    protected String getChannelName(){
        return "Screen Recorder Notifications";
    }
}
