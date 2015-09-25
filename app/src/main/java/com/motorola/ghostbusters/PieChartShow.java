package com.motorola.ghostbusters;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

public class PieChartShow extends Activity {
    TextView stView4, stView5;
    MyPie pieView;
    TextView touchInfo;
    TextView oopsText;
    ImageView oopsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pie_chart_show);
        pieView = (MyPie) findViewById(R.id.myPie);
    }

    @Override
    public void onResume() {
        super.onResume();

        //pieView.arrangeArrays(MainActivity.mDevice.diagGearCount);

        String stats[] = MainActivity.mDevice.diagStats();
        String value1 = "";
        String value2 = "";
        //value += "Statistics: \n";
        for(int i=0; i<stats.length; i++) {
            if (stats[i].startsWith("H")) {
                value2 += stats[i];
            } else {
                value1 += stats[i];
            }
            //Log.d(TAG, "add stat: " + stats[i]);
        }

        /////////////End of read stats////////
        if (!value1.isEmpty()) {
            TextView stView4 = (TextView) findViewById(R.id.stat1);
            stView4.setMovementMethod(new ScrollingMovementMethod());
            stView4.setText(value1);
            TextView stView5 = (TextView) findViewById(R.id.stat2);
            stView5.setMovementMethod(new ScrollingMovementMethod());
            stView5.setText(value2);
        } else {
            TextView oopsText = (TextView) findViewById(R.id.oopsText);
            ImageView oopsView = (ImageView) findViewById(R.id.oopsView);

            //TODO set image and text randomly
            oopsText.setText("Oops, there are no statistics data for touch in this build...");
            oopsView.setImageResource(R.drawable.no_stats_1);
        }

        touchInfo = (TextView) findViewById(R.id.infoText);
        String textToShowAtBottom = "Product: " + MainActivity.productInfo + "  Config: " + MainActivity.getTouchCfg();
        if (!MainActivity.panel.isEmpty()) {
            textToShowAtBottom += " Panel: " + MainActivity.panel;
        }
        touchInfo.setText(textToShowAtBottom);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_pie_chart_show, menu);
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
}
