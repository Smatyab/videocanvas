package com.example.videocanvas;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class MainActivity2 extends AppCompatActivity {

    private PlayerView playerView;
    private ExoPlayer player;
    private Button btnNextScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        playerView = findViewById(R.id.player_view);
        btnNextScreen = findViewById(R.id.btnNextScreen2);

        btnNextScreen.setOnClickListener(v -> {
            releasePlayer();
            Intent intent = new Intent(MainActivity2.this, MainActivity3.class);
            startActivity(intent);
        });

        initializePlayer();
    }

    private void initializePlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        String videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4";
        MediaItem mediaItem = MediaItem.fromUri(videoUrl);

        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        releasePlayer();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (player == null) initializePlayer();
    }
}
