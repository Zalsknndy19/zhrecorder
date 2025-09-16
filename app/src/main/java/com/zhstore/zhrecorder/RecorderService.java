package com.zhstore.zhrecorder;

// TAMBAHKAN IMPORT BARU INI
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.media.MediaRecorder;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import java.io.IOException;
import android.*;
// ... (import lain tetap sama)

public class RecorderService extends Service {

    // ... (variabel lain tetap sama)
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private MediaRecorder mediaRecorder;
    private VirtualDisplay virtualDisplay;

    // Variabel baru untuk notifikasi
    private static final String CHANNEL_ID = "ZHRecorderChannel";
    private static final int NOTIFICATION_ID = 123;

    @Override
    public void onCreate() {
        super.onCreate();
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Buat dan tampilkan notifikasi SEGERA
        Notification notification = buildNotification();
        startForeground(NOTIFICATION_ID, notification);

        // Ambil "kunci" izin (tidak berubah)
        int resultCode = intent.getIntExtra("resultCode", -1);
        Intent data = intent.getParcelableExtra("data");

        mediaProjection = projectionManager.getMediaProjection(resultCode, data);
        
        initRecorder();
        startRecording();
        
        return START_NOT_STICKY;
    }

    private void initRecorder() {
        mediaRecorder = new MediaRecorder();
        
        // ================== PERUBAHAN UTAMA DI SINI ==================
        // UNTUK TES PERTAMA, KITA GUNAKAN MIKROFON. Ini dijamin berhasil.
        // Jika ini berhasil, baru kita coba lagi dengan REMOTE_SUBMIX.
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        // ============================================================

        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        
        // ... (sisa pengaturan video & audio tidak berubah)
        // Pengaturan Video
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        
        mediaRecorder.setVideoSize(metrics.widthPixels, metrics.heightPixels);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setVideoEncodingBitRate(8 * 1024 * 1024); // 8 Mbps
        mediaRecorder.setVideoFrameRate(30);

        // Pengaturan Audio
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioEncodingBitRate(192 * 1024); // 192 kbps
        mediaRecorder.setAudioSamplingRate(44100);
        
        
        mediaRecorder.setOutputFile(getExternalFilesDir(null).getAbsolutePath() + "/ZHRecorder_Test.mp4");

        // ... (try-catch mediaRecorder.prepare() tidak berubah)
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // ... (startRecording(), onDestroy(), onBind() tidak berubah)

    private void startRecording() {
        if (mediaRecorder == null || mediaProjection == null) return;
        
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);

        // Buat "layar virtual" yang akan ditangkap oleh MediaRecorder
        virtualDisplay = mediaProjection.createVirtualDisplay("ZHRecorder",
                metrics.widthPixels, metrics.heightPixels, metrics.densityDpi,
                android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder.getSurface(), null, null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Hentikan dan bersihkan semuanya
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // ================== FUNGSI BARU UNTUK NOTIFIKASI ==================
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
    // ==================================================================
}
