package com.example.admin.mybledemo.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.admin.mybledemo.R;

import java.util.List;

public class ControlLogAdapter extends RecyclerView.Adapter<ControlLogAdapter.ViewHolder> {

    private Context context;
    private List<String> logs;

    public ControlLogAdapter(Context context, List<String> logs) {
        this.context = context;
        this.logs = logs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_control_log, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        viewHolder.tvLog.setText(logs.get(position));
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvLog;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLog = itemView.findViewById(R.id.tv_log);
        }
    }

}
