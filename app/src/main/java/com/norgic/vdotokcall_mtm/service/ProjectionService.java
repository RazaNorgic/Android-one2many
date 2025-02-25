package com.norgic.vdotokcall_mtm.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.norgic.vdotokcall_mtm.R;

import static androidx.core.app.NotificationCompat.PRIORITY_MIN;


public class ProjectionService extends Service {

    private final IBinder mBinder = new LocalBinder();


    public ProjectionService() { }

    @Override
    public void onCreate() {
        if (Build.VERSION.SDK_INT >= 26) {
            startForeground();
        }
    }

    public class LocalBinder extends Binder {
        public com.norgic.vdotokcall_mtm.service.ProjectionService getService() {
            // Return this instance of LocalService so clients can call public methods
            return com.norgic.vdotokcall_mtm.service.ProjectionService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public MediaProjection getMediaProjection(MediaProjectionManager mediaProjectionManager, int resultCode, Intent data)
    {
        return   mediaProjectionManager.getMediaProjection(resultCode, data);
    }


    private void startForeground() {

        String channelId;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
             channelId = createNotificationChannel("networkService", "My Background Service");
        } else {
            // If earlier version channel ID is not used
            // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
            channelId= "";
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId );

        Notification notification  =notificationBuilder
                .setSmallIcon(R.mipmap.ic_launcher)
                 .setContentTitle("ProjectionService")
                .setPriority(PRIORITY_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();

        startForeground(100, notification);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(String channelId, String channelName){

        NotificationChannel channel1= new NotificationChannel(channelId,channelName, NotificationManager.IMPORTANCE_DEFAULT);
        channel1.setLightColor(Color.BLUE);
        channel1.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(channel1);
        return channelId;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

//    @Override
//    public IBinder onBind(Intent intent) {
//        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
//    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
