package com.zhstore.zhrecorder;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 101;
    private static final int REQUEST_CODE_SCREEN_CAPTURE = 100;
    
    private MediaProjectionManager projectionManager;
    private Button toggleButton;

    private static final String[] PERMISSIONS = { Manifest.permission.RECORD_AUDIO };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        toggleButton = findViewById(R.id.toggle_button);
        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        // Atur status tombol saat aplikasi pertama kali dibuka
        updateButtonState();

        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isServiceRunning(RecorderService.class)) {
                    // Jika service sedang berjalan, kita kirim perintah STOP
                    stopRecorderService();
                } else {
                    // Jika tidak, kita mulai proses START
                    if (checkAndRequestPermissions()) {
                        startScreenCapture();
                    }
                }
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Perbarui status tombol setiap kali pengguna kembali ke aplikasi
        updateButtonState();
    }

    private void startRecorderService(int resultCode, Intent data) {
        Intent serviceIntent = new Intent(this, RecorderService.class);
        serviceIntent.setAction(RecorderService.ACTION_START);
        serviceIntent.putExtra(RecorderService.EXTRA_RESULT_CODE, resultCode);
        serviceIntent.putExtra(RecorderService.EXTRA_DATA, data);
        startForegroundService(serviceIntent);
        updateButtonState(); // Segarkan status tombol
        finish(); // Tutup activity
    }

    private void stopRecorderService() {
        Intent serviceIntent = new Intent(this, RecorderService.class);
        serviceIntent.setAction(RecorderService.ACTION_STOP);
        startService(serviceIntent); // Cukup 'startService' untuk mengirim perintah
        Toast.makeText(this, "Perekaman Dihentikan.", Toast.LENGTH_SHORT).show();
        updateButtonState(); // Segarkan status tombol
    }
    
    // Fungsi untuk memeriksa apakah service sedang berjalan
    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    // Fungsi untuk mengubah teks dan warna tombol
    private void updateButtonState() {
        if (isServiceRunning(RecorderService.class)) {
            toggleButton.setText("Stop Recording");
            // Kamu bisa menambahkan perubahan warna di sini
            // toggleButton.setBackgroundColor(ContextCompat.getColor(this, R.color.red));
        } else {
            toggleButton.setText("Start Recording");
            // toggleButton.setBackgroundColor(ContextCompat.getColor(this, R.color.green));
        }
    }
    
    // =================================================================
    //      BAGIAN MANAJEMEN IZIN & HASIL (Tidak berubah)
    // =================================================================
    private void startScreenCapture() {
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_CAPTURE);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == RESULT_OK) {
                startRecorderService(resultCode, data);
            } else {
                Toast.makeText(this, "Izin rekam layar ditolak.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private boolean checkAndRequestPermissions() {
        java.util.List<String> listPermissionsNeeded = new java.util.ArrayList<>();
        for (String p : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), REQUEST_CODE_PERMISSIONS);
            return false;
        }
        return true;
    }
    
    
    @Overridepublic void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            // Cek apakah semua izin yang diminta telah diberikan
            boolean allPermissionsGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                // Jika pengguna memberikan izin, SEKARANG baru kita mulai proses rekam layar
                startScreenCapture();
            } else {
                Toast.makeText(this, "Izin audio dibutuhkan untuk merekam.", Toast.LENGTH_LONG).show();
            }
        }
    }
    
}