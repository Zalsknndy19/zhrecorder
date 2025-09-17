package com.zhstore.zhrecorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.VirtualDisplay;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import androidx.core.app.NotificationCompat;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecorderService extends Service {

    // Kunci untuk komunikasi antar komponen
    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE";
    public static final String EXTRA_DATA = "EXTRA_DATA";

    private static final String CHANNEL_ID = "RecorderChannel";
    private static final int NOTIFICATION_ID = 12345;

    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private MediaRecorder mediaRecorder;
    private VirtualDisplay virtualDisplay;

    @Override
    public void onCreate() {
        super.onCreate();
        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_START.equals(action)) {
                startRecording(intent);
            } else if (ACTION_STOP.equals(action)) {
                stopRecording();
            }
        }
        return START_NOT_STICKY;
    }

    private void startRecording(Intent intent) {
        // Tampilkan notifikasi wajib untuk foreground service
        startForeground(NOTIFICATION_ID, buildNotification());

        int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1);
        Intent data = intent.getParcelableExtra(EXTRA_DATA);

        mediaProjection = projectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection == null) {
            stopSelf();
            return;
        }

        initRecorder();
        createVirtualDisplay();
        mediaRecorder.start();
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (RuntimeException e) {
                // Abaikan error jika stop dipanggil setelah error lain
            }
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        stopForeground(true);
        stopSelf();
    }
    
    private void initRecorder() {
        mediaRecorder = new MediaRecorder();

        // Mengatur sumber audio ke audio internal menggunakan API modern
        AudioPlaybackCaptureConfiguration audioConfig = new AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA) // Tangkap audio dari pemutar media
                .addMatchingUsage(AudioAttributes.USAGE_GAME) // Tangkap audio dari game
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_PERFORMANCE); // Sumber dummy
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mediaRecorder.setAudioPlaybackCaptureConfig(audioConfig);
        }

        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        // Pengaturan Video
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mediaRecorder.setVideoSize(metrics.widthPixels, metrics.heightPixels);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setVideoEncodingBitRate(8 * 1024 * 1024); // 8 Mbps
        mediaRecorder.setVideoFrameRate(30);

        // Pengaturan Audio
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioEncodingBitRate(1920 * 1024);
        mediaRecorder.setAudioSamplingRate(44100);

        // Atur lokasi file output
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        mediaRecorder.setOutputFile(dir.getAbsolutePath() + "/ZHRec_" + timestamp + ".mp4");

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            stopRecording();
        }
    }

    private void createVirtualDisplay() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        virtualDisplay = mediaProjection.createVirtualDisplay("ZHRecorder",
                metrics.widthPixels, metrics.heightPixels, metrics.densityDpi,
                android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder.getSurface(), null, null);
    }
    
    private Notification buildNotification() {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle("ZHRecorder Aktif")
                .setContentText("Merekam layar...")
                .setSmallIcon(R.drawable.ic_launcher); // Pastikan kamu punya ikon ini

        return builder.build();
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "ZHRecorder Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
    

    @Override
    public IBinder onBind(Intent intent) { return null; }
}