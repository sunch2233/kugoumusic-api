package com.kugou.musicplayer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.kugou.musicplayer.R;
import com.kugou.musicplayer.model.Song;
import com.kugou.musicplayer.util.FavoriteManager;

import java.util.List;

/**
 * 歌曲列表适配器
 */
public class SongAdapter extends BaseAdapter {
    private Context context;
    private List songList;
    private LayoutInflater inflater;
    private FavoriteManager favoriteManager;
    private OnSongActionListener listener;
    private String playingHash;

    public SongAdapter(Context context, List songList) {
        this.context = context;
        this.songList = songList;
        this.inflater = LayoutInflater.from(context);
        this.favoriteManager = FavoriteManager.getInstance(context);
    }

    public void setOnSongActionListener(OnSongActionListener listener) {
        this.listener = listener;
    }

    public void setPlayingHash(String hash) {
        this.playingHash = hash;
        notifyDataSetChanged();
    }

    public int getCount() {
        return songList != null ? songList.size() : 0;
    }

    public Object getItem(int position) {
        return songList != null ? songList.get(position) : null;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_song, null);
            holder = new ViewHolder();
            holder.tvIndex = (TextView) convertView.findViewById(R.id.tvIndex);
            holder.tvSongName = (TextView) convertView.findViewById(R.id.tvSongName);
            holder.tvSongArtist = (TextView) convertView.findViewById(R.id.tvSongArtist);
            holder.btnPlay = (Button) convertView.findViewById(R.id.btnPlay);
            holder.btnFavorite = (Button) convertView.findViewById(R.id.btnFavorite);
            holder.btnDownload = (Button) convertView.findViewById(R.id.btnDownload);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final Song song = (Song) songList.get(position);
        holder.tvIndex.setText(String.valueOf(position + 1));
        holder.tvSongName.setText(song.getSongname());
        holder.tvSongArtist.setText(song.getSingername() + " · " + song.getFormatDuration());

        // 高亮正在播放的歌曲
        if (playingHash != null && playingHash.equals(song.getHash())) {
            holder.tvSongName.setTextColor(0xFF4FC3F7);
        } else {
            holder.tvSongName.setTextColor(0xFFFFFFFF);
        }

        // 收藏状态
        if (favoriteManager.isFavorite(song.getHash())) {
            holder.btnFavorite.setTextColor(0xFFE91E63);
        } else {
            holder.btnFavorite.setTextColor(0xFF4FC3F7);
        }

        holder.btnPlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (listener != null) {
                    listener.onPlay(song, position);
                }
            }
        });

        holder.btnFavorite.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (listener != null) {
                    listener.onFavorite(song);
                }
                notifyDataSetChanged();
            }
        });

        holder.btnDownload.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (listener != null) {
                    listener.onDownload(song);
                }
            }
        });

        return convertView;
    }

    static class ViewHolder {
        TextView tvIndex;
        TextView tvSongName;
        TextView tvSongArtist;
        Button btnPlay;
        Button btnFavorite;
        Button btnDownload;
    }

    /**
     * 歌曲操作回调
     */
    public interface OnSongActionListener {
        void onPlay(Song song, int position);
        void onFavorite(Song song);
        void onDownload(Song song);
    }
}
