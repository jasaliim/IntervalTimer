package com.example.android.intervaltimer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private WorkoutAdapter mAdapter;

    private TextView totalTimeTextView;

    private static final int DEFAULT_PREP_TIME = 5;  //TODO move these later to the settings page
    private static final int DEFAULT_WORK_TIME = 4;
    private static final int DEFAULT_REST_TIME = 4;
    private static final int DEFAULT_CYCLES = 10;

    private static LinkedList<MyTimer> times = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeTimes();

        Button addSetsButton = findViewById(R.id.add_sets_button);
        addSetsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                times = mAdapter.getmDataset();
                addOneSet();
                mAdapter.notifyItemRangeInserted(mAdapter.getItemCount(), 3);
                updateTimes(times);
            }
        });

        Button startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, WorkoutActivity.class);
                startActivity(intent);
            }
        });

        totalTimeTextView = findViewById(R.id.total_time_text_view);

        RecyclerView recyclerView = findViewById(R.id.main_recycler_view);
        recyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        mAdapter = new WorkoutAdapter(times, this);
        recyclerView.setAdapter(mAdapter);

        calculateTotalTime();
    }

    /**
     * Adds one set to the workout, i.e work/rest intervals times cycles.
     */
    private void addOneSet() {
        times.add(new MyTimer(MyTimer.WORK_TYPE, DEFAULT_WORK_TIME, this));
        times.add(new MyTimer(MyTimer.REST_TYPE, DEFAULT_REST_TIME, this));
        times.add(new MyTimer(MyTimer.CYCLES_TYPE, DEFAULT_CYCLES, this));
    }

    /**
     * Initialize the times by adding the prep time plus one set.
     */
    private void initializeTimes() {
        times.add(new MyTimer(MyTimer.PREP_TYPE, DEFAULT_PREP_TIME, this));
        addOneSet();
    }


    /**
     * Updates the list of times and calculates the new total time and updates it.
     *
     * @param list List that replaces the current one.
     */
    public void updateTimes(LinkedList<MyTimer> list) {
        times = list;
        calculateTotalTime();
    }

    /**
     * Calculates the total time of the workout.
     * Namely Prep time + each ((Work time + Rest time) * Cycles) from the list.
     */
    private void calculateTotalTime() {
        //Prep time only once
        int total = times.get(0).getTime();

        //We always have all three of work/rest/cycles, so we can jump three indexes at a time
        //and count from there. If this specification changes, this calculation has to change.
        for (int i = 3; i < times.size(); i += 3) {
            total += (times.get(i).getTime() * (times.get(i - 1).getTime() + times.get(i - 2).getTime()));
        }

        int minutes = total / 60;
        int seconds = total % 60;

        totalTimeTextView.setText(String.format(Locale.getDefault(), "Total time: %02d:%02d", minutes, seconds));
    }

    /**
     * @return List of workout set times in format [Prep, Work, Rest, Cycles, Work, Rest, Cycles...]
     */
    public static LinkedList<MyTimer> getTimes() {
        return times;
    }

}
