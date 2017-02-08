package com.swyam.remoteimageprocessing;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by Reyes Yam on 03/02/2017.
 */
public class RVAdapter  extends RecyclerView.Adapter<RVAdapter.CaptureViewHolder>{

    public static class CaptureViewHolder extends RecyclerView.ViewHolder{
        CardView cv;
        TextView capture_file_name;
        TextView capture_date;
        TextView capture_runned_on;
        ImageView capture_thumb;
        ImageButton btn_delete_capture_dialog;


        CaptureViewHolder(View itemView) {
            super(itemView);
            cv = (CardView)itemView.findViewById(R.id.cv);
            capture_file_name = (TextView)itemView.findViewById(R.id.text_view_capture_name);
            capture_date = (TextView)itemView.findViewById(R.id.text_view_capture_date);
            capture_thumb = (ImageView)itemView.findViewById(R.id.image_view_capture_thumb);
            capture_runned_on = (TextView) itemView.findViewById(R.id.text_view_runned_on);
            btn_delete_capture_dialog = (ImageButton) itemView.findViewById(R.id.btn_delete_capture_dialog);
        }
    }

    List<Capture> captures;
    Context mContext;
    private OnItemClickListener onItemClickListener;

    RVAdapter(Context mContext, List<Capture> captures){
        this.mContext = mContext;
        this.captures = captures;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public RVAdapter.CaptureViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_picture, parent, false);
        CaptureViewHolder cvh = new CaptureViewHolder(v);
        return cvh;
    }

    @Override
    public void onBindViewHolder(RVAdapter.CaptureViewHolder holder, final int position) {
        final Capture capture = captures.get(position);
        holder.capture_file_name.setText(capture.name_file);
        holder.capture_date.setText(capture.capture_date.toString());
        if(capture.thumb_path!=null) {
            holder.capture_thumb.setImageBitmap(capture.thumb_path);
        }else{
            holder.capture_thumb.setImageResource(R.drawable.ic_menu_gallery);
        }
        String runned_in ="";
        int text_color= Color.rgb(15,152,171);
        switch(capture.executed_algorithm){
            case 0:
                runned_in= mContext.getString(R.string.rv_no_execute);
                text_color = Color.RED;
                break;
            case 1:
                runned_in = mContext.getString(R.string.rv_executed_th);
                text_color = Color.rgb(14,14,68);
                break;
            case 2:
                runned_in = mContext.getString(R.string.rv_executed_ce);
                text_color = Color.rgb(102,0,102);
                break;
            default:
                runned_in = mContext.getString(R.string.rv_executed_both);
        }
        holder.capture_runned_on.setText(runned_in);
        holder.capture_runned_on.setTextColor(text_color);

        View.OnClickListener listener = new View.OnClickListener(){
            @Override
            public void onClick(View v){
                onItemClickListener.onItemClick(capture);
            }
        };

        holder.capture_thumb.setOnClickListener(listener);
        holder.capture_file_name.setOnClickListener(listener);

        View.OnLongClickListener listener2 = new View.OnLongClickListener(){
            @Override
            public boolean onLongClick(View v){
                onItemClickListener.onLongItemClick(capture,position);
                return true;
            }
        };

        holder.btn_delete_capture_dialog.setOnLongClickListener(listener2);
    }

    @Override
    public int getItemCount() {
        return captures.size();
    }

    public OnItemClickListener getOnItemClickListener() {
        return onItemClickListener;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public void removeAt(int position) {
        captures.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, captures.size());
    }


}
