package com.zhstore.zhrecorder;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.content.Context;
import android.content.pm.PackageManager; // Import baru
import androidx.core.app.ActivityCompat; // Import baru
import androidx.core.content.ContextCompat; // Import baru
import android.Manifest;
import android.*; // Import baru

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 101;
    private static final int REQUEST_CODE_SCREEN_CAPTURE = 100;
    private MediaProjectionManager projectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Button startButton = findViewById(R.id.btn_start_record);
        
        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // LANGKAH 1: Periksa semua izin terlebih dahulu
                if (checkAndRequestPermissions()) {
                    // Jika semua izin sudah ada, langsung minta izin rekam layar
                    startScreenCapture();
                }
            }
        });
    }

    // =================================================================
    //      FUNGSI BARU UNTUK MANAJEMEN IZIN
    // =================================================================
    private boolean checkAndRequestPermissions() {
        // Daftar semua izin yang kita butuhkan
        String[] permissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        };
        
        // Cek izin mana yang belum diberikan
        java.util.List<String> listPermissionsNeeded = new java.util.ArrayList<>();
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }

        // Jika ada izin yang belum diberikan, minta sekarang
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), REQUEST_CODE_PERMISSIONS);
            return false; // Berhenti di sini, tunggu jawaban pengguna
        }

        return true; // Semua izin sudah ada
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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
                // Jika pengguna memberikan semua izin, SEKARANG baru kita minta izin rekam layar
                startScreenCapture();
            } else {
                // Jika pengguna menolak, beri tahu mereka
                android.widget.Toast.makeText(this, "Izin penyimpanan dan audio dibutuhkan untuk merekam.", android.widget.Toast.LENGTH_LONG).show();
            }
        }
    }
    // =================================================================

    private void startScreenCapture() {
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_CAPTURE);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                Intent serviceIntent = new Intent(this, RecorderService.class);
                serviceIntent.putExtra("resultCode", resultCode);
                serviceIntent.putExtra("data", data);
                startForegroundService(serviceIntent);
                android.widget.Toast.makeText(this, "Perekaman Dimulai!", android.widget.Toast.LENGTH_SHORT).show();
                finish();
            } else {
                android.widget.Toast.makeText(this, "Izin rekam layar ditolak.", android.widget.Toast.LENGTH_SHORT).show();
            }
        }
    }
}
