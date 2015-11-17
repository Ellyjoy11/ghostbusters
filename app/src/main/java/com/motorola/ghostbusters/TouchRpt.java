package com.motorola.ghostbusters;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;


public class TouchRpt extends Activity {
    com.motorola.ghostbusters.TouchReport myView;
    public final String TAG = "Ghostbusters";
    public static short mValues[][];
    //public int selectedGear=0;
    public boolean selectedGear[];
    public String[] gearsNames;
    static Button btnQuit;
    static Button btnGear;
    View decorView;
    int uiOptions;
    //public static boolean isBtnVis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_touch_rpt);

        decorView = getWindow().getDecorView();
        uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
        UiChangeListener();

        myView = (TouchReport) findViewById(R.id.touchReport);

        btnQuit = (Button) findViewById(R.id.quit);
        //btnGear = (Button) findViewById(R.id.gear);
        //isBtnVis = false;

    }

    public void onResume(){
        super.onResume();
        SharedPreferences userPref = PreferenceManager
                .getDefaultSharedPreferences(this);
        if (userPref.getBoolean("isAuto", true)) {
            MainActivity.mDevice.diagGearAuto(0);
        } else {
            MainActivity.mDevice.diagGearAuto(1);
        }
        getArray();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_touch_rpt, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onQuitClick(View view) {
        finish();
        Intent intent = new Intent(getApplicationContext(),
                com.motorola.ghostbusters.MainActivity.class);
        startActivity(intent);
    }

    public void onPause() {
        super.onPause();
        TouchDevice.diagGearAuto(0);
    }

    public static void getArray() {
        //TODO get Array and its dimensions

        int yD = MainActivity.mDevice.diagFrameY();
        int xD = MainActivity.mDevice.diagFrameX();
        //mValues = new short[xD][yD];

        short[] report = MainActivity.mDevice.diagDeltaFrame();

        //TODO set real dimensions and values
        TouchReport.setDimensions(xD, yD);

        //TouchReport.setValues(mValues);
        TouchReport.setValues(report);
    }

    public static void setButtons(boolean isVisible){

        if (isVisible) {
            btnQuit.setVisibility(VISIBLE);
            //btnGear.setVisibility(VISIBLE);
        } else {
            btnQuit.setVisibility(GONE);
            //btnGear.setVisibility(GONE);
        }

    }

    public void UiChangeListener()
    {
        //decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    decorView.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                    setButtons(false);
                    //myView.setBtnVisibility(!isBtnVis);
                    myView.invalidate();
                }
            }
        });

    }

}
