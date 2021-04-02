package com.tim.test04.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.tim.test04.R;
import com.tim.test04.model.MusicMedia;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MusicPlayerActivity extends AppCompatActivity {

    private static final String TAG = MusicPlayerActivity.class.getSimpleName();

    private static final String[] PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE};

    private AppCompatSeekBar sbMusic;
    private TextView tvPlayerCurrent;
    private TextView tvPlayerDuration;
    private Button btnPlayPause;

    private Button btnRefresh;
    private RecyclerView rvMedias;
    private MediaAdapter mediaAdapter;

    private MediaPlayer mediaPlayer;
    private boolean isTracking;

    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        findViews();
        setViewsListeners();

        rvMedias.setLayoutManager(new LinearLayoutManager(this));
        mediaAdapter = new MediaAdapter();
        rvMedias.setAdapter(mediaAdapter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestRquiredPermissions();
        }

        initTime();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        releaseMediaPlayer();
        timer.cancel();
        timer = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i(TAG, "onRequestPermissionsResult: request=" + requestCode
                + ", permissions=" + Arrays.toString(permissions)
                + ", grantResults=" + Arrays.toString(grantResults));
    }

    private void findViews() {
        sbMusic = findViewById(R.id.sb_music);
        tvPlayerCurrent = findViewById(R.id.tv_player_current);
        tvPlayerDuration = findViewById(R.id.tv_player_duration);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnRefresh = findViewById(R.id.btn_refresh);
        rvMedias = findViewById(R.id.rv_medias);
    }

    private void setViewsListeners() {
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<MusicMedia> medias = queryMusicMedias();
                Log.i(TAG, "medias:" + medias);
                mediaAdapter.setMedias(medias);
                mediaAdapter.notifyDataSetChanged();
            }
        });

        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null == mediaPlayer) {
                    return;
                }
                try {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                    } else {
                        mediaPlayer.start();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    releaseMediaPlayer();
                }
            }
        });
        sbMusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isTracking = true; }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isTracking = false;
                if (null == mediaPlayer) {
                    return; }
                try {
                    long duration = mediaPlayer.getDuration();
                    int current = (int) (duration * ((float) seekBar.getProgress() / seekBar.getMax()));
                    mediaPlayer.seekTo(current);
                } catch (Exception e) {
                    e.printStackTrace(); }
            }
        });
    }

    private void requestRquiredPermissions() {
        List<String> permissions = new ArrayList<>();
        for (String permission : PERMISSIONS) {
            if (checkCallingPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(permission);
            }
        }
        if (permissions.size() == 0) {
            return;
        }
        String[] permissonArr = new String[permissions.size()];
        permissions.toArray(permissonArr);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
    }

    private void initTime() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isTracking) {
                            return;
                        }
                        if (null == mediaPlayer) {
                            return;
                        }
                        try {
                            long current = mediaPlayer.getCurrentPosition();
                            long duration = mediaPlayer.getDuration();
                            tvPlayerCurrent.setText(getTimeText(current));
                            tvPlayerDuration.setText(getTimeText(duration));
                            sbMusic.setProgress((int) (sbMusic.getMax() * ((float) current / duration)));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    private String getTimeText(long time) {
                        StringBuilder timeTime = new StringBuilder();
                        int hours = (int) (time / 1000 / 3600);
                        int minutes = (int) (time / 1000 % 3600 / 60);
                        int seconds = (int) (time / 1000 % 60);
                        timeTime.append(hours).append(":");
                        timeTime.append(minutes >= 10 ? minutes : "0" + minutes).append(":");
                        timeTime.append(seconds >= 10 ? seconds : "0" + seconds);
                        return timeTime.toString();
                    }
                });
            }
        }, 0, 100);
    }


    static class StudentViewHolder extends RecyclerView.ViewHolder {

        private TextView tvTitle;
        private TextView tvArtist;
        private TextView tvAlbum;
        private TextView tvDuration;
        private Button btnPlay;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvArtist = itemView.findViewById(R.id.tv_artist);
            tvAlbum = itemView.findViewById(R.id.tv_album);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            btnPlay = itemView.findViewById(R.id.btn_play);
        }
    }

    private class MediaAdapter extends RecyclerView.Adapter<StudentViewHolder> {

        private String TAG = MediaAdapter.class.getSimpleName();


        private List<MusicMedia> medias;

        public List<MusicMedia> getMedias() {
            return medias;
        }

        public void setMedias(List<MusicMedia> medias) {
            this.medias = medias;
        }

        @NonNull
        @Override
        public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout llStudent = (LinearLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.ll_adapter_media, parent, false);
            StudentViewHolder svh = new StudentViewHolder(llStudent);
            return svh;
        }

        @Override
        public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
            if (null == medias) {
                return;
            }
            if (position >= medias.size()) {
                Log.w(TAG, String.format("onBindViewHolder: given position:$1 is not in the range[0, $2]", position, medias.size()));
                return;
            }
            final MusicMedia media = medias.get(position);
            if (null == media) {
                return;
            }
            holder.tvTitle.setText(null == media.getTitle() ? null : "".equals(media.getTitle().trim()) ? media.getDisplayName() : media.getTitle());
            holder.tvArtist.setText(media.getArtist());
            holder.tvAlbum.setText(media.getAlbum());
            holder.tvDuration.setText(String.valueOf(media.getDuration()));
            holder.btnPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String path = media.getData();
                    try {
                        MusicPlayerActivity.this.playLocalMusic(path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return null == medias ? 0 : medias.size();
        }
    }

    private List<MusicMedia> queryMusicMedias() {
        String[] projection = null;
        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;
        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, sortOrder);
        if (null == cursor) {
            return null;
        }
        List<MusicMedia> medias = new ArrayList<>();
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
            String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
            String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            String album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
            String displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME));
            int duration = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
            String data = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
            MusicMedia media = new MusicMedia(id, title, artist, album, displayName, duration, data);
            medias.add(media);
        }
        cursor.close();
        return medias;
    }

    private void releaseMediaPlayer() {
        if (null == mediaPlayer) {
            return;
        }
        try {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playLocalMusic(String path) throws IOException {
        if (null != mediaPlayer) {
            releaseMediaPlayer();
        }
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Log.i(TAG, "onPrepared: ");
                mp.start();
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.i(TAG, "onCompletion: ");
            }
        });
        mediaPlayer.setDataSource(path);
        mediaPlayer.prepareAsync();
    }
}
