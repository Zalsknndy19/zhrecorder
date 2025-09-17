package com.zhstore.zhrecorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@RequiresApi(api = Build.VERSION_CODES.Q)
public class RecorderService extends Service {

    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE";
    public static final String EXTRA_DATA = "EXTRA_DATA";

    private static final String CHANNEL_ID = "RecorderChannel";
    private static final int NOTIFICATION_ID = 12345;
    private static final String TAG = "RecorderService";

    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private MediaRecorder mediaRecorder;
    private VirtualDisplay virtualDisplay;

    private MediaProjection.Callback projectionCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_START.equals(intent.getAction())) {
            startRecording(intent);
        } else if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopRecording();
        }
        return START_NOT_STICKY;
    }

    private void startRecording(Intent intent) {
        startForeground(NOTIFICATION_ID, buildNotification());

        int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1);
        // Perbaikan: Gunakan cara yang benar untuk mengambil Parcelable di Android versi baru
        Intent data = intent.getParcelableExtra(EXTRA_DATA);

        mediaProjection = projectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection == null) {
            Log.e(TAG, "MediaProjection tidak bisa didapatkan.");
            stopSelf();
            return;
        }

        // Siapkan "pendengar" untuk saat sesi perekaman dihentikan (misal dari notifikasi)
        projectionCallback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                // Jika perekaman dihentikan dari luar, pastikan service kita juga berhenti
                if (mediaRecorder != null) {
                    stopRecording();
                }
            }
        };
        mediaProjection.registerCallback(projectionCallback, null);

        try {
            initRecorder();
            createVirtualDisplay();
            mediaRecorder.start();
            Toast.makeText(this, "Perekaman Dimulai!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Gagal memulai perekaman", e);
            stopRecording();
        }
    }

    private void stopRecording() {
        if (mediaProjection != null) {
            mediaProjection.unregisterCallback(projectionCallback);
            mediaProjection.stop();
            mediaProjection = null;
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (RuntimeException stopException) {
                // Abaikan jika sudah dihentikan
            }
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
        }
        stopForeground(true);
        stopSelf();
    }
    
    private void initRecorder() throws IOException {
        mediaRecorder = new MediaRecorder();

        // Menggunakan sumber audio REMOTE_SUBMIX yang lebih modern
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.REMOTE_SUBMIX);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mediaRecorder.setVideoSize(metrics.widthPixels, metrics.heightPixels);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setVideoEncodingBitRate(8 * 1024 * 1024);
        mediaRecorder.setVideoFrameRate(30);

        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioEncodingBitRate(192 * 1024);
        mediaRecorder.setAudioSamplingRate(44100);

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        String filePath = dir.getAbsolutePath() + "/ZHRec_" + timestamp + ".mp4";
        mediaRecorder.setOutputFile(filePath);

        mediaRecorder.prepare();
    }

    private void createVirtualDisplay() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        virtualDisplay = mediaProjection.createVirtualDisplay("ZHRecorder",
                metrics.widthPixels, metrics.heightPixels, metrics.densityDpi,
                android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder.getSurface(), null, null);
    }
    
    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("ZHRecorder Aktif")
                .setContentText("Merekam layar...")
                .setSmallIcon(R.drawable.ic_launcher)
                .build();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "ZHRecorder Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
