package com.kugou.musicplayer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.kugou.musicplayer.R;
import com.kugou.musicplayer.model.MV;

import java.util.List;

/**
 * MV列表适配器
 */
public class MVAdapter extends BaseAdapter {
    private Context context;
    private List mvList;
    private LayoutInflater inflater;
    private OnMVActionListener listener;

    public MVAdapter(Context context, List mvList) {
        this.context = context;
        this.mvList = mvList;
        this.inflater = LayoutInflater.from(context);
    }

    public void setOnMVActionListener(OnMVActionListener listener) {
        this.listener = listener;
    }

    public int getCount() {
        return mvList != null ? mvList.size() : 0;
    }

    public Object getItem(int position) {
        return mvList != null ? mvList.get(position) : null;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_mv, null);
            holder = new ViewHolder();
            holder.tvIndex = (TextView) convertView.findViewById(R.id.tvIndex);
            holder.tvMVTitle = (TextView) convertView.findViewById(R.id.tvMVTitle);
            holder.tvMVArtist = (TextView) convertView.findViewById(R.id.tvMVArtist);
            holder.btnPlayMV = (Button) convertView.findViewById(R.id.btnPlayMV);
            holder.btnDownloadMV = (Button) convertView.findViewById(R.id.btnDownloadMV);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final MV mv = (MV) mvList.get(position);
        holder.tvIndex.setText(String.valueOf(position + 1));
        holder.tvMVTitle.setText(mv.getTitle());
        holder.tvMVArtist.setText(mv.getSinger());

        holder.btnPlayMV.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (listener != null) {
                    listener.onPlayMV(mv);
                }
            }
        });

        holder.btnDownloadMV.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (listener != null) {
                    listener.onDownloadMV(mv);
                }
            }
        });

        return convertView;
    }

    static class ViewHolder {
        TextView tvIndex;
        TextView tvMVTitle;
        TextView tvMVArtist;
        Button btnPlayMV;
        Button btnDownloadMV;
    }

    public interface OnMVActionListener {
        void onPlayMV(MV mv);
        void onDownloadMV(MV mv);
    }
}
