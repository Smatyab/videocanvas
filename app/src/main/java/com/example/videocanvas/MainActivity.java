package com.example.videocanvas;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final int PICK_VIDEO_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private EditText etVideoPath, etStartTime, etEndTime;
    private SurfaceView surfaceView;
    private Button btnToggle;

    private MediaPlayer mediaPlayer;
    private SurfaceHolder surfaceHolder;

    private Handler playbackHandler = new Handler(Looper.getMainLooper());
    private int startTimeInMillis = 0;
    private int endTimeInMillis = -1;
    private Uri currentVideoUri = null;
    private boolean isVideoLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etVideoPath = findViewById(R.id.etVideoPath);
        etStartTime = findViewById(R.id.etStartTime);
        etEndTime = findViewById(R.id.etEndTime);
        surfaceView = findViewById(R.id.surfaceView);
        btnToggle = findViewById(R.id.btnToggle);

        Button btnBrowse = findViewById(R.id.btnBrowse);
        Button btnLoad = findViewById(R.id.btnLoad);
        Button btnRewind = findViewById(R.id.btnRewind);
        Button btnForward = findViewById(R.id.btnForward);
        Button btnNextScreen = findViewById(R.id.btnNextScreen);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        checkPermissions();

        btnNextScreen.setOnClickListener(v -> {
            releaseMediaPlayer();
            Intent intent = new Intent(MainActivity.this, MainActivity2.class);
            startActivity(intent);
        });

        btnBrowse.setOnClickListener(v -> openFilePicker());
        btnLoad.setOnClickListener(v -> prepareVideo());

        btnToggle.setOnClickListener(v -> {
            if (mediaPlayer == null || !isVideoLoaded) {
                Toast.makeText(this, "Load video first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                btnToggle.setText("▶");
            } else {
                mediaPlayer.start();
                startPlaybackMonitor();
                btnToggle.setText("⏸");
            }
        });

        btnRewind.setOnClickListener(v -> {
            if (mediaPlayer != null && isVideoLoaded) {
                int target = mediaPlayer.getCurrentPosition() - 10000;
                if (target < startTimeInMillis) target = startTimeInMillis;
                mediaPlayer.seekTo(target);
            }
        });

        btnForward.setOnClickListener(v -> {
            if (mediaPlayer != null && isVideoLoaded) {
                int target = mediaPlayer.getCurrentPosition() + 10000;
                if (endTimeInMillis != -1 && target > endTimeInMillis) target = endTimeInMillis;
                if (target > mediaPlayer.getDuration()) target = mediaPlayer.getDuration();
                mediaPlayer.seekTo(target);
            }
        });
    }

    private void prepareVideo() {
        String path = etVideoPath.getText().toString().trim();
        if (path.isEmpty()) return;

        startTimeInMillis = 0;
        try {
            startTimeInMillis = (int) (Float.parseFloat(etStartTime.getText().toString()) * 1000);
        } catch (Exception ignored) {}

        endTimeInMillis = -1;
        try {
            endTimeInMillis = (int) (Float.parseFloat(etEndTime.getText().toString()) * 1000);
        } catch (Exception ignored) {}

        releaseMediaPlayer();

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDisplay(surfaceHolder);

            if (currentVideoUri != null && path.equals(currentVideoUri.toString())) {
                mediaPlayer.setDataSource(this, currentVideoUri);
            } else {
                mediaPlayer.setDataSource(path);
            }

            mediaPlayer.setOnPreparedListener(mp -> {
                isVideoLoaded = true;

                int vW = mp.getVideoWidth();
                int vH = mp.getVideoHeight();
                float proportion = (float) vW / (float) vH;
                int sW = getResources().getDisplayMetrics().widthPixels;

                android.view.ViewGroup.LayoutParams lp = surfaceView.getLayoutParams();
                lp.width = sW;
                lp.height = (int) (sW / proportion);
                surfaceView.setLayoutParams(lp);

                if (startTimeInMillis > 0) mp.seekTo(startTimeInMillis);
                mp.start();
                btnToggle.setText("⏸");
                startPlaybackMonitor();
            });

            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startPlaybackMonitor() {
        playbackHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    if (endTimeInMillis != -1 && mediaPlayer.getCurrentPosition() >= endTimeInMillis) {
                        mediaPlayer.pause();
                        btnToggle.setText("▶");
                    } else {
                        playbackHandler.postDelayed(this, 100);
                    }
                }
            }
        }, 100);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        startActivityForResult(intent, PICK_VIDEO_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_VIDEO_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            currentVideoUri = data.getData();
            etVideoPath.setText(currentVideoUri.toString());
        }
    }

    private void releaseMediaPlayer() {
        playbackHandler.removeCallbacksAndMessages(null);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.READ_MEDIA_VIDEO},
                        PERMISSION_REQUEST_CODE
                );
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE
                );
            }
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        if (mediaPlayer != null) mediaPlayer.setDisplay(holder);
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        releaseMediaPlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
    }
}
