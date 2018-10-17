package com.example.android.intervaltimer;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.Locale;

public class WorkoutActivity extends AppCompatActivity {

    private TextView remainingTimeTextView;
    private TextView currentTypeTextView;
    private TextView nextIntervalTextView;
    private TextView totalTimeLeftTextView;
    private TextView roundsLeftTextView;
    private ConstraintLayout constraintLayout;
    private Button prevButton;
    private Button pauseButton; //TODO pause functionality
    private Button nextButton;
    private Button soundOnOffButton;
    private Button stopButton;

    private final LinkedList<MyTimer> expandedTimers = new LinkedList<>();
    private int totalTime = 0;
    private int totalRounds = 0;
    private int totalTimeLeft = 0;
    private int roundsLeft = 0;
    private int currentPosition = 0;

    private boolean isTimerRunning = false;
    private long millisToCountTo = 0;

    private MediaPlayer mMediaPlayer;
    private int lastSecondPlayed = 0;
    private boolean soundOn = true;

    //Handler and runnable for timers.
    private final Handler timerHandler = new Handler();
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long currentTime = SystemClock.uptimeMillis();
            long millisRemaining = millisToCountTo - currentTime;

            //When the timer gets to 0.
            if (millisRemaining <= 0) {
                try {
                    moveList(true);
                } catch (IndexOutOfBoundsException e) {
                    stopHandler();
                    return;
                }
                //After the countdown beeps there is a longer beep at the start of next timer.
                if (currentPosition < expandedTimers.size() - 2) {
                    if (mMediaPlayer != null && soundOn) {
                        mMediaPlayer.release();
                        mMediaPlayer = MediaPlayer.create(WorkoutActivity.this, R.raw.start_fast);
                        mMediaPlayer.start();
                    }
                }
                currentTime = SystemClock.uptimeMillis();
                millisRemaining = millisToCountTo - currentTime;
            }

            //Got to the last timer, so stop the handler.
            if (currentPosition >= expandedTimers.size() - 2) {
                stopHandler();
                return;
            }

            //Round the remaining time up because of inaccuracy in the handlers clock.
            double toRound = Math.ceil(millisRemaining / 1000.0);
            int seconds = (int) toRound;

            if (seconds <= 3 && seconds != lastSecondPlayed && soundOn && mMediaPlayer != null) {
                if (lastSecondPlayed == 0) {
                    mMediaPlayer.release();
                    mMediaPlayer = MediaPlayer.create(WorkoutActivity.this, R.raw.beep_fast);
                }
                mMediaPlayer.start();
                lastSecondPlayed = seconds;
            }

            remainingTimeTextView.setText(String.format(Locale.getDefault(), "%d", seconds));

            int remainingTotalTime = totalTimeLeft - (expandedTimers.get(currentPosition).getTime() - seconds);
            int minutes = remainingTotalTime / 60;
            seconds = remainingTotalTime % 60;

            totalTimeLeftTextView.setText(String.format(Locale.getDefault(), "Total time left: %02d:%02d", minutes, seconds));

            //Call this function again after the given time. Should be low enough that seconds won't
            //"stretch" but not too low for performance reasons.
            long millisRemainingThisSecond = millisRemaining % 1000;
            long timeToCallback;
            if (millisRemainingThisSecond <= 50) {
                timeToCallback = 10;
            } else {
                timeToCallback = millisRemainingThisSecond - 40;
            }

            timerHandler.postDelayed(this, timeToCallback);
            isTimerRunning = true;
        }
    };

    /**
     * Rewinds the media player by pausing it and seeking to 0 ms.
     */
    private void rewindMediaPlayer() {
        if (mMediaPlayer != null && soundOn) {
            mMediaPlayer.pause();
            mMediaPlayer.seekTo(0);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        assignViews();
        initializeTimesAndRounds();
        initializeViewsAndButtons();
        updateMillisToCountTo();

        mMediaPlayer = MediaPlayer.create(this, R.raw.beep_fast);
        //Rewind the audio on completion
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                rewindMediaPlayer();
            }
        });
        /*TODO change this bubblegum fix to a bug that happens when the first prep is skipped.
        In that case the moveList tries to pause the mediaPlayer which is still in the 'prepared'
        state and that state can't be paused. So now we just start the media player and immediately
        rewind and pause it, so it will be in the 'started' state, which then can be paused.
        */
        if (soundOn) {
            mMediaPlayer.start();
            rewindMediaPlayer();
        }

        startTimer();
    }

    /**
     * Find the views and assign them to the member variables.
     */
    private void assignViews() {
        remainingTimeTextView = findViewById(R.id.remaining_time_text_view);
        currentTypeTextView = findViewById(R.id.current_type_text_view);
        nextIntervalTextView = findViewById(R.id.next_interval_text_view);
        totalTimeLeftTextView = findViewById(R.id.total_time_left_text_view);
        roundsLeftTextView = findViewById(R.id.rounds_left_text_view);
        constraintLayout = findViewById(R.id.constraint_layout);
        prevButton = findViewById(R.id.prev_button);
        pauseButton = findViewById(R.id.pause_button);
        nextButton = findViewById(R.id.next_button);
        soundOnOffButton = findViewById(R.id.sound_button);
        stopButton = findViewById(R.id.stop_button);
    }

    /**
     * Initializes the times and rounds.
     * Expands the timer list gotten from main activity to a list with no 'cycles-timers',
     * just prep/work/rest/work/rest/work/rest/..../end/empty. End and empty are somewhat of a
     * 'help'-timers, as they make the updating near the end of the list easier and simpler.
     */
    private void initializeTimesAndRounds() {
        LinkedList<MyTimer> timers = MainActivity.getTimes();

        //Add prep time
        expandedTimers.add(timers.getFirst());
        totalTime += timers.getFirst().getTime();

        //Expand and add all the 'work/rest'-cycles
        for (int i = 3; i < timers.size(); i += 3) {
            for (int j = 0; j < timers.get(i).getTime(); j++) {
                expandedTimers.add(timers.get(i - 2));
                expandedTimers.add(timers.get(i - 1));
                totalTime += (timers.get(i - 2).getTime() + timers.get(i - 1).getTime());
            }
            //One work/rest-cycle is one round.
            totalRounds += timers.get(i).getTime();
        }

        expandedTimers.add(new MyTimer(MyTimer.END_TYPE, 0, this));
        expandedTimers.add(new MyTimer(MyTimer.EMPTY_TYPE, 0, this));

        totalTimeLeft = totalTime;
        roundsLeft = totalRounds;
    }

    /**
     * Initializes the views and buttons, i.e sets their texts and colors to the initial values.
     * Also adds the OnClickListeners to the buttons.
     */
    private void initializeViewsAndButtons() {
        int minutes = totalTime / 60;
        int seconds = totalTime % 60;

        MyTimer firstTimer = expandedTimers.get(0);
        MyTimer secondTimer = expandedTimers.get(1);

        currentTypeTextView.setText(firstTimer.getTypeAsString());
        constraintLayout.setBackgroundColor(firstTimer.getColor());
        remainingTimeTextView.setText(String.format(Locale.getDefault(), "%d", firstTimer.getTime()));
        nextIntervalTextView.setText(getString(R.string.next_type_time, secondTimer.getTypeAsString(), secondTimer.getTime()));
        nextIntervalTextView.setBackgroundColor(secondTimer.getColor());
        totalTimeLeftTextView.setText(String.format(Locale.getDefault(), "Total time left: %02d:%02d", minutes, seconds));
        roundsLeftTextView.setText(getString(R.string.rounds_left, roundsLeft));

        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveList(false);
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveList(true);
            }
        });

        soundOnOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                soundOn = !soundOn;

                //If sound is now off, release the media player. Else initialize it again.
                if (!soundOn && mMediaPlayer != null) {
                    mMediaPlayer.release();
                } else {
                    if (lastSecondPlayed == 1) {
                        mMediaPlayer = MediaPlayer.create(WorkoutActivity.this, R.raw.start_fast);
                    } else {
                        mMediaPlayer = MediaPlayer.create(WorkoutActivity.this, R.raw.beep_fast);
                    }

                }
                //Set the correct text to the button.
                if (soundOn){
                    soundOnOffButton.setText(R.string.on);
                }else{
                    soundOnOffButton.setText(R.string.off);
                }

            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMediaPlayer != null){
                    mMediaPlayer.release();
                }
                finish();
            }
        });
    }

    /**
     * Moves the list to the next or previous timer (if it exists) and updates the UI.
     *
     * @param moveForward If true, the list moves forwards; if false, backwards.
     */
    private void moveList(boolean moveForward) {
        rewindMediaPlayer();
        lastSecondPlayed = 0;

        int change = moveForward ? 1 : -1;

        int newPosition = currentPosition + change;

        //We only ever want to move to the second last position, as the last 'timer' is only
        //an empty one and should never be the 'active' one.
        if (newPosition < 0 || newPosition > expandedTimers.size() - 2) {
            return;
        }

        MyTimer newPositionTimer = expandedTimers.get(newPosition);
        MyTimer nextFromNewTimer = expandedTimers.get(newPosition + 1);

        currentTypeTextView.setText(newPositionTimer.getTypeAsString());
        constraintLayout.setBackgroundColor(newPositionTimer.getColor());
        nextIntervalTextView.setText(getString(R.string.next_type_time, nextFromNewTimer.getTypeAsString(), nextFromNewTimer.getTime()));
        nextIntervalTextView.setBackgroundColor(nextFromNewTimer.getColor());
        remainingTimeTextView.setText(String.format(Locale.getDefault(), "%d", newPositionTimer.getTime()));

        if (moveForward) {
            totalTimeLeft -= expandedTimers.get(currentPosition).getTime();
            //RoundsLeft only decrements if we have moved forwards to a work-timer or a end-'timer'.
            //Also the new position must not be 1, as the first move from prep to work should not
            //decrement rounds.
            if ((newPositionTimer.getType() == MyTimer.WORK_TYPE || newPositionTimer.getType() == MyTimer.END_TYPE) && newPosition != 1) {
                roundsLeft--;
                roundsLeftTextView.setText(getString(R.string.rounds_left, roundsLeft));
            }
            //Set the text views specifically for the end of the list, so the next interval text
            //view won't show the text "Next: 0" and the remaining time is also empty.
            if (newPositionTimer.getType() == MyTimer.END_TYPE) {
                nextIntervalTextView.setText("");
                remainingTimeTextView.setText("");
            }
        }

        if (!moveForward) {
            totalTimeLeft += newPositionTimer.getTime();
            //RoundsLeft only increments if we have moved backwards to a Rest-timer.
            if (newPositionTimer.getType() == MyTimer.REST_TYPE) {
                roundsLeft++;
                roundsLeftTextView.setText(getString(R.string.rounds_left, roundsLeft));
            }
        }

        //So the next interval text view doesn't show "Next: END! 0", only "END!".
        if (expandedTimers.get(newPosition + 1).getType() == MyTimer.END_TYPE) {
            nextIntervalTextView.setText(getString(R.string.end));
        }

        currentPosition = newPosition;

        updateTotalTimeLeft();
        startTimer();
        updateMillisToCountTo();
    }

    /**
     * Stops the handler from running the timers.
     */
    private void stopHandler() {
        timerHandler.removeCallbacks(timerRunnable);
        isTimerRunning = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopHandler();
        mMediaPlayer.release();
    }

    /**
     * Updates the milliseconds long which the timer is counting to.
     */
    private void updateMillisToCountTo() {
        long countdownTime = expandedTimers.get(currentPosition).getTime() * 1000;
        long startTime = SystemClock.uptimeMillis();
        millisToCountTo = startTime + countdownTime;
    }

    /**
     * Updates the total time left text view.
     */
    private void updateTotalTimeLeft() {
        int minutes = totalTimeLeft / 60;
        int seconds = totalTimeLeft % 60;
        totalTimeLeftTextView.setText(String.format(Locale.getDefault(), "Total time left: %02d:%02d", minutes, seconds));
    }

    /**
     * Starts the timer if it is not running.
     */
    private void startTimer() {
        if (!isTimerRunning) {
            timerHandler.postDelayed(timerRunnable, 0);
        }
    }
}

//TODO add sound icon to sound button