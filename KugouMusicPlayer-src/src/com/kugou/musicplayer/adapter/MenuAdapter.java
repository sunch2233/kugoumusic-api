package com.kugou.musicplayer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.kugou.musicplayer.R;

/**
 * 主界面功能菜单适配器
 */
public class MenuAdapter extends BaseAdapter {
    private Context context;
    private String[] menuNames;
    private String[] menuIcons;
    private LayoutInflater inflater;

    public MenuAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.menuNames = new String[] {
            "搜索音乐", "音乐播放", "MV播放",
            "我的收藏", "下载管理", "API文档"
        };
        this.menuIcons = new String[] {
            "🔍", "♪", "▶", "★", "▼", "{}"
        };
    }

    public int getCount() {
        return menuNames.length;
    }

    public Object getItem(int position) {
        return menuNames[position];
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_menu, null);
            holder = new ViewHolder();
            holder.tvMenuIcon = (TextView) convertView.findViewById(R.id.tvMenuIcon);
            holder.tvMenuName = (TextView) convertView.findViewById(R.id.tvMenuName);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.tvMenuIcon.setText(menuIcons[position]);
        holder.tvMenuName.setText(menuNames[position]);

        return convertView;
    }

    static class ViewHolder {
        TextView tvMenuIcon;
        TextView tvMenuName;
    }
}
