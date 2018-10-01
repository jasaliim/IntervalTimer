package com.example.android.intervaltimer;

import android.content.Context;
import android.graphics.Color;

//TODO Maybe a more general name? End, empty and cycles types are not really timers.
public class MyTimer {

    //The different types of timers this application supports.
    static final int PREP_TYPE = 0;
    static final int WORK_TYPE = 1;
    static final int REST_TYPE = 2;
    static final int END_TYPE = 3;
    static final int EMPTY_TYPE = 4;
    static final int CYCLES_TYPE = 5;

    //TODO move later to settings page
    private static final int PREP_COLOR = Color.GREEN;
    private static final int WORK_COLOR = Color.RED;
    private static final int REST_COLOR = Color.BLUE;
    private static final int END_COLOR = Color.YELLOW;

    private int type;
    private String typeAsString;
    private int time;
    private int color;
    //private Context context;

    /**
     * @return Integer representation of the type
     */
    public int getType() {
        return type;
    }

    /**
     * @return String representation of the type
     */
    String getTypeAsString() {
        return typeAsString;
    }

    /**
     * @return This timers time
     */
    public int getTime() {
        return time;
    }

    /**
     * @return Preferred background color for this timer
     */
    public int getColor() {
        return color;
    }

    /**
     * @param time Time to be set
     */
    public void setTime(int time) {
        this.time = time;
    }

    /**
     * Constructor for MyTimer class.
     * @param type Desired type of MyTimer. Should be given as MyTimer.*_TYPE
     * @param time Desired time of MyTimer as seconds.
     * @param context Context so the string representations can be gotten from resources.
     */
    public MyTimer(int type, int time, Context context) {
        //Empty and cycles type + the default case are not optimal.
        //TODO Maybe a better solution.
        switch(type){
            case PREP_TYPE:
                this.typeAsString = context.getResources().getString(R.string.prep);
                this.color = PREP_COLOR;
                break;
            case WORK_TYPE:
                this.typeAsString = context.getResources().getString(R.string.work);
                this.color = WORK_COLOR;
                break;
            case REST_TYPE:
                this.typeAsString = context.getResources().getString(R.string.rest);
                this.color = REST_COLOR;
                break;
            case END_TYPE:
                this.typeAsString = context.getResources().getString(R.string.end);
                this.color = END_COLOR;
                break;
            case EMPTY_TYPE:
                this.typeAsString = "";
                this.color = END_COLOR;
                break;
            case CYCLES_TYPE:
                this.typeAsString = "";
                this.color = END_COLOR;
            default:
                this.typeAsString = "ERROR";
                this.color = END_COLOR;
                break;
        }
        this.type = type;
        this.time = time;
        //this.context = context;
    }
}
