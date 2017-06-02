package com.davinci.advancedrecyclerview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.davinci.advancedrecyclertouchlistener.RecyclerTouchListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements RecyclerTouchListener.OnRowClickListener
{

    List<String> messages = new ArrayList<>();
    private RecyclerTouchListener onTouchListener;
    private MyRecyclerViewAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RecyclerView mRecyclerView =
                (RecyclerView) findViewById(R.id.my_recycler_view);
        mAdapter = new MyRecyclerViewAdapter();
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);

        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(mLayoutManager);

        messages.add("Test 1");
        messages.add("Test 2");
        messages.add("Test 3");
        messages.add("Test 4");
        messages.add("Test 5");

        mAdapter.setMessages(messages);

        onTouchListener = new RecyclerTouchListener(this, mRecyclerView);
        onTouchListener.setRightCartItemOptionViews(R.id.first_right_button,
                R.id.second_right_button, R.id.third_right_button)
                .setForeground(R.id.item_view)
                .setBackground(R.id.right_options_view, R.id.left_options_view)
                .setRowClickListener(this);

        mRecyclerView.addOnItemTouchListener(onTouchListener);

    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    //==============================================================================================
    // RowClickListener implementation
    //==============================================================================================

    @Override
    public void onSwipedRight(int position)
    {
        mAdapter.removeItem(position);
    }

    @Override
    public void onSwipedLeft(int position)
    {
        mAdapter.removeItem(position);
    }

    @Override
    public void onOptionClicked(int optionID) {

    }

    @Override
    public void onRowClicked(int position) {

    }

}
