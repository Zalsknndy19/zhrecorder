package com.zhstore.zhrecorder;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

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

        updateButtonState();

        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isServiceRunning(RecorderService.class)) {
                    stopRecorderService();
                } else {
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
        updateButtonState();
    }

    private void startScreenCapture() {
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_CAPTURE);
    }

    private void startRecorderService(int resultCode, Intent data) {
        Intent serviceIntent = new Intent(this, RecorderService.class);
        serviceIntent.setAction(RecorderService.ACTION_START);
        serviceIntent.putExtra(RecorderService.EXTRA_RESULT_CODE, resultCode);
        serviceIntent.putExtra(RecorderService.EXTRA_DATA, data);
        startForegroundService(serviceIntent);
        updateButtonState();
        finish();
    }

    private void stopRecorderService() {
        Intent serviceIntent = new Intent(this, RecorderService.class);
        serviceIntent.setAction(RecorderService.ACTION_STOP);
        startService(serviceIntent);
        updateButtonState();
    }
    
    private void updateButtonState() {
        if (isServiceRunning(RecorderService.class)) {
            toggleButton.setText("Stop Recording");
        } else {
            toggleButton.setText("Start Recording");
        }
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkAndRequestPermissions() {
        List<String> listPermissionsNeeded = new ArrayList<>();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == Activity.RESULT_OK) {
                startRecorderService(resultCode, data);
            } else {
                Toast.makeText(this, "Izin rekam layar ditolak.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            boolean allGranted = true;
            if (grantResults.length > 0) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                        break;
                    }
                }
            } else {
                allGranted = false;
            }
            
            if (allGranted) {
                startScreenCapture();
            } else {
                Toast.makeText(this, "Izin audio dibutuhkan untuk merekam.", Toast.LENGTH_LONG).show();
            }
        }
    }
}