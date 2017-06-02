package com.davinci.advancedrecyclerview;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by Amey on 4/15/17.
 */

public class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyRecyclerViewAdapter.MyViewHolder>
{
    private List<String> messages;

    public MyRecyclerViewAdapter()
    {
        this.messages = new ArrayList<>();
    }

    public void setMessages(List<String> messages)
    {
        this.messages = messages;
        notifyDataSetChanged();
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_view, parent, false);

        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position)
    {
        holder.load(messages.get(position));
    }

    @Override
    public int getItemCount()
    {
        return messages.size();
    }

    public void removeItem(int position)
    {
        messages.remove(position);
        notifyItemRemoved(position);
    }

    //==============================================================================================
    // ViewHolder
    //==============================================================================================

    public class MyViewHolder extends RecyclerView.ViewHolder
    {
        TextView mTextView;

        public MyViewHolder(View itemView)
        {
            super(itemView);
            mTextView = (TextView) itemView.findViewById(R.id.text_view);
        }

        public void load(String message)
        {
            mTextView.setText(message);
        }
    }
}
