package com.example.administrator.musicplayer.tool;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.administrator.musicplayer.R;
import com.example.administrator.musicplayer.datastructure.MusicBean;

import java.util.ArrayList;

public class ListAdapter extends ArrayAdapter {
    private ArrayList<MusicBean> list = new ArrayList<>();
    private LayoutInflater layoutInflater;

    public ListAdapter(Context context, int resource) {
        super(context, resource);
        layoutInflater = LayoutInflater.from(context);
    }

    public void setList(ArrayList<MusicBean> list){
        this.list = list;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null)
            convertView = layoutInflater.inflate( R.layout.item_music_list_layout, null );
        MusicBean file = (MusicBean) getItem(position);
        TextView nameTxt = (TextView) convertView;
        nameTxt.setText( file != null ? file.getMusicName() : null );
        return convertView;
    }
}