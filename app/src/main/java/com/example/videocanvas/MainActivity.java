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

    // Define your Default URL here
    private static final String DEFAULT_VIDEO_URL = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";

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

        // Navigate to Screen 2
        btnNextScreen.setOnClickListener(v -> {
            releaseMediaPlayer();
            // Ensure you have created MainActivity2.java as discussed before
            Intent intent = new Intent(MainActivity.this, MainActivity2.class);
            startActivity(intent);
        });

        btnBrowse.setOnClickListener(v -> openFilePicker());

        // This button triggers the logic to choose between Default or Selected video
        btnLoad.setOnClickListener(v -> prepareVideo());

        btnToggle.setOnClickListener(v -> {
            if (mediaPlayer == null || !isVideoLoaded) {
                // If not loaded, try to load (it will pick default if empty)
                prepareVideo();
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
        // 1. Check what is in the text box
        String inputPath = etVideoPath.getText().toString().trim();
        String finalPathToPlay;

        // 2. Logic: Default vs User Input
        if (inputPath.isEmpty()) {
            // Case: Input is empty -> Play Default Video
            finalPathToPlay = DEFAULT_VIDEO_URL;
            // Clear local URI reference so we don't accidentally try to play a file that isn't selected
            currentVideoUri = null;
            Toast.makeText(this, "Input empty. Playing Default Video.", Toast.LENGTH_SHORT).show();
        } else {
            // Case: Input has text -> Play User Selection
            finalPathToPlay = inputPath;
        }

        startTimeInMillis = 0;
        try {
            if (!etStartTime.getText().toString().isEmpty()) {
                startTimeInMillis = (int) (Float.parseFloat(etStartTime.getText().toString()) * 1000);
            }
        } catch (Exception ignored) {}

        endTimeInMillis = -1;
        try {
            if (!etEndTime.getText().toString().isEmpty()) {
                endTimeInMillis = (int) (Float.parseFloat(etEndTime.getText().toString()) * 1000);
            }
        } catch (Exception ignored) {}

        releaseMediaPlayer();

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDisplay(surfaceHolder);

            // 3. Set Data Source
            // If we have a local URI and the text matches it, play from local storage
            if (currentVideoUri != null && finalPathToPlay.equals(currentVideoUri.toString())) {
                mediaPlayer.setDataSource(this, currentVideoUri);
            } else {
                // Otherwise play from URL (either the default one or the user typed one)
                mediaPlayer.setDataSource(finalPathToPlay);
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

            // Add error listener to catch bad URLs
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(MainActivity.this, "Error playing video", Toast.LENGTH_SHORT).show();
                return false;
            });

            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
            if (currentVideoUri != null) {
                etVideoPath.setText(currentVideoUri.toString());
            }
        }
    }

    private void releaseMediaPlayer() {
        playbackHandler.removeCallbacksAndMessages(null);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isVideoLoaded = false;
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