package com.ynsuper.slideshowver1.adapter;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.ynsuper.slideshowver1.R;
import com.ynsuper.slideshowver1.model.AudioModel;
import com.ynsuper.slideshowver1.model.MusicModel;
import com.ynsuper.slideshowver1.util.Constants;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MusicAdapter extends BaseAdapter {
    private List<AudioModel> listAudio;
    private List<MusicModel> listMusic;
    private final Context context;
    private OnSongClickListener onSongClickListener;
    private boolean isLocalMusic;

    @Override
    public int getCount() {
        if (isLocalMusic) {
            return listAudio.size();
        }
        return listMusic.size();
    }

    @Override
    public Object getItem(int position) {
        if (isLocalMusic) {
            return listAudio.get(position);
        }
        return listMusic.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return getCount();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View viewProduct;
        if (convertView == null) {
            viewProduct = View.inflate(parent.getContext(), R.layout.item_music, null);
        } else viewProduct = convertView;
        if (isLocalMusic) {
            ((TextView) viewProduct.findViewById(R.id.txt_music_name)).setText(listAudio.get(position).getName());
            ((TextView) viewProduct.findViewById(R.id.txt_artists)).setText(listAudio.get(position).getArtist());
//            viewProduct.findViewById(R.id.btn_down_save).setVisibility(View.GONE);
            ProgressBar progressBar = viewProduct.findViewById(R.id.progress_download);
            Glide.with(context).load(R.drawable.ic_check_circle_black_24dp).into(((ImageView) viewProduct.findViewById(R.id.btn_down_save)));
            ImageView imgDownUse = viewProduct.findViewById(R.id.btn_down_save);
            imgDownUse.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSongClickListener.onSongDownloadClick(listAudio.get(position).getPath(), listAudio.get(position).getName(), progressBar, imgDownUse, false);
                }
            });
            ((ConstraintLayout) viewProduct.findViewById(R.id.constraint_item_music)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    onSongClickListener.onSongClick(listPath.get(position).get("file_path"), listPath.get(position).get("title"));
                    onSongClickListener.onSongClick(listAudio.get(position).getPath(), listAudio.get(position).getName());
                }
            });
        } else {
            if (viewProduct != null) {
                MusicModel song = listMusic.get(position);
                TextView txtSongName = viewProduct.findViewById(R.id.txt_music_name);
                txtSongName.setText(song.getName());
                ((TextView) viewProduct.findViewById(R.id.txt_artists)).setText(song.getArtist());
                ((ConstraintLayout) viewProduct.findViewById(R.id.constraint_item_music)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onSongClickListener.onSongClick(song);
                    }
                });
                ProgressBar progressBar = viewProduct.findViewById(R.id.progress_download);
                ImageView imgDownUse = viewProduct.findViewById(R.id.btn_down_save);
                imgDownUse.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onSongClickListener.onSongDownloadClick(song.getAudio(), song.getName(), progressBar, imgDownUse, true);
                    }
                });
                String fileName = song.getName() + ".mp3";
                // will create file in global Music directory, can be any other directory, just don't forget to handle permissions
                File folder = new File(Constants.PATH_DOWNLOAD_MUSIC_FROM_CLOUD);
                folder.mkdirs();
                ArrayList<String> fileNames = new ArrayList<String>(Arrays.asList(folder.list()));
                Log.d("TESTADAPTER", "song name: " + song.getName() + " - " + fileNames.contains(fileName) + " txtSong: " + txtSongName.getText());

                if(fileNames.contains(fileName) && txtSongName.getText().equals(song.getName())) {
                    imgDownUse.setImageResource(R.drawable.ic_check_circle_black_24dp);
                }
            }

        }

        return viewProduct;
    }

    private String createTimeLabel(int time){
        String timeLabel = "";
        int  min = time / 1000 / 60;
        if (min < 10) timeLabel += "0";
        int sec = time / 1000 % 60;
        timeLabel += "$min:";
        if (sec < 10) timeLabel += "0";
        timeLabel += sec;
        return timeLabel;
    }

    public MusicAdapter(List<MusicModel> listMusic, Context context, OnSongClickListener onSongClickListener) {
//        this.isLocalMusic = isLocalMusic;
        this.listMusic = listMusic;
        this.context = context;
        this.onSongClickListener = onSongClickListener;
    }

    public MusicAdapter(List<AudioModel> listAudio, Context context, OnSongClickListener onSongClickListener, boolean isLocalMusic) {
        this.isLocalMusic = isLocalMusic;
        this.listAudio = listAudio;
        this.context = context;
        this.onSongClickListener = onSongClickListener;

    }

    public interface OnSongClickListener {

        default void onSongClick(MusicModel musicModel) {

        }

        default void onSongClick(String filePath, String fileName) {

        }

        default void onSongDownloadClick(String url, String name, ProgressBar progressBar, ImageView imgDownUse, boolean isInternetMusic) {

        }

    }
}
