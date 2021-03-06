package com.example.android.intervaltimer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.Locale;

/**
 * Adapter for showing the sets in the main activity's recyclerview.
 */
public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder> {
    private LinkedList<MyTimer> mDataset;
    private final Context mContext;

    class WorkoutViewHolder extends RecyclerView.ViewHolder {
        final TextView descTextView;
        final Button minusButton;
        final EditText timeEditText;
        final Button plusButton;
        final Button deleteButton;

        private int mSeconds;

        void setSeconds(int seconds) {
            mSeconds = seconds;
        }

        WorkoutViewHolder(View itemView) {
            super(itemView);

            descTextView = itemView.findViewById(R.id.desc_tv);
            minusButton = itemView.findViewById(R.id.minus_button);
            timeEditText = itemView.findViewById(R.id.time_et);
            plusButton = itemView.findViewById(R.id.plus_button);
            deleteButton = itemView.findViewById(R.id.delete_button);

            minusButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSeconds--;
                    updateTimeTextView();
                    updateDatasetsWithNewSeconds(WorkoutViewHolder.this, mSeconds);
                }
            });

            plusButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSeconds++;
                    updateTimeTextView();
                    updateDatasetsWithNewSeconds(WorkoutViewHolder.this, mSeconds);
                }
            });

            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteGivenSet(WorkoutViewHolder.this);
                }
            });

            timeEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        EditText ev = (EditText) v;
                        String nmbString = ev.getText().toString();
                        if (TextUtils.isEmpty(nmbString)) {
                            ev.setText(String.format(Locale.getDefault(), "%d", mSeconds));
                        }
                        int seconds;
                        try {
                            seconds = Integer.parseInt(nmbString);
                        } catch (NumberFormatException nfe){
                            seconds = mSeconds;
                        }
                        mSeconds = seconds;
                        updateTimeTextView();
                        updateDatasetsWithNewSeconds(WorkoutViewHolder.this, mSeconds);
                    }
                }
            });
        }

        private void updateTimeTextView() {
            if (mSeconds < 0) {
                mSeconds = 0;
                return;
            }
            timeEditText.setText(String.format(Locale.getDefault(), "%d", mSeconds));
        }
    }

    /**
     * Deletes the set, which the given view holder is part of. (Or more specifically, is the cycles
     * item of.)
     *
     * @param holder The cycles item view to be deleted, along with its preceding work and rest items.
     */
    private void deleteGivenSet(WorkoutViewHolder holder) {
        //Don't allow deleting if there is currently only one set.
        if (getItemCount() <= 4) {
            return;
        }
        int position = holder.getAdapterPosition();
        mDataset.remove(position);
        mDataset.remove(position - 1);
        mDataset.remove(position - 2);
        notifyItemRangeRemoved(position - 2, 3);
        updateMainActivityDataset();
    }

    WorkoutAdapter(LinkedList<MyTimer> dataset, Context context) {
        mDataset = dataset;
        mContext = context;
    }

    @NonNull
    @Override
    public WorkoutAdapter.WorkoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_layout, parent, false);

        return new WorkoutViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkoutViewHolder holder, int position) {
        holder.deleteButton.setVisibility(View.INVISIBLE);
        if (position == 0) holder.descTextView.setText(mContext.getString(R.string.prep));
        else if (position % 3 == 1) holder.descTextView.setText(mContext.getString(R.string.work));
        else if (position % 3 == 2) holder.descTextView.setText(mContext.getString(R.string.rest));
        else {
            holder.descTextView.setText(mContext.getString(R.string.cycles));
            holder.deleteButton.setVisibility(View.VISIBLE);
        }

        int secs = mDataset.get(position).getTime();
        holder.setSeconds(secs);
        holder.updateTimeTextView();
    }

    /**
     * Updates this adapters dataset list and the main activity's time list.
     * Assumes that the view holder and its corresponding MyTimer are at the same index.
     *
     * @param workoutViewHolder The view holder which time was changed.
     * @param newSeconds        The new time.
     */
    private void updateDatasetsWithNewSeconds(WorkoutViewHolder workoutViewHolder, int newSeconds) {
        mDataset.get(workoutViewHolder.getAdapterPosition()).setTime(newSeconds);
        updateMainActivityDataset();
    }


    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    LinkedList<MyTimer> getmDataset() {
        return mDataset;
    }

    private void updateMainActivityDataset() {
        if (mContext instanceof MainActivity) {
            ((MainActivity) mContext).updateTimes(mDataset);
        }
    }
}

//TODO add delete icon to delete button