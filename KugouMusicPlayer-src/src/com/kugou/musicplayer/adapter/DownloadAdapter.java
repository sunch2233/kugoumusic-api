package com.kugou.musicplayer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.kugou.musicplayer.R;
import com.kugou.musicplayer.model.DownloadTask;

import java.util.List;

/**
 * 下载任务列表适配器
 */
public class DownloadAdapter extends BaseAdapter {
    private Context context;
    private List taskList;
    private LayoutInflater inflater;

    public DownloadAdapter(Context context, List taskList) {
        this.context = context;
        this.taskList = taskList;
        this.inflater = LayoutInflater.from(context);
    }

    public int getCount() {
        return taskList != null ? taskList.size() : 0;
    }

    public Object getItem(int position) {
        return taskList != null ? taskList.get(position) : null;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_download, null);
            holder = new ViewHolder();
            holder.tvDlName = (TextView) convertView.findViewById(R.id.tvDlName);
            holder.pbDownload = (ProgressBar) convertView.findViewById(R.id.pbDownload);
            holder.tvDlStatus = (TextView) convertView.findViewById(R.id.tvDlStatus);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        DownloadTask task = (DownloadTask) taskList.get(position);
        holder.tvDlName.setText(task.getName());
        holder.pbDownload.setProgress(task.getProgress());
        holder.tvDlStatus.setText(task.getStatusText());

        // 根据状态设置颜色
        if (task.getStatus() == DownloadTask.STATUS_DONE) {
            holder.tvDlStatus.setTextColor(0xFF66BB6A);
        } else if (task.getStatus() == DownloadTask.STATUS_FAILED) {
            holder.tvDlStatus.setTextColor(0xFFEF9A9A);
        } else {
            holder.tvDlStatus.setTextColor(0xFF888888);
        }

        return convertView;
    }

    static class ViewHolder {
        TextView tvDlName;
        ProgressBar pbDownload;
        TextView tvDlStatus;
    }
}
