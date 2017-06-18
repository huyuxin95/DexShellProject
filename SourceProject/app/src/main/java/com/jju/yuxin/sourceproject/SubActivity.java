package com.jju.yuxin.sourceproject;


import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class SubActivity extends Activity {
    private static final String TAG=SubActivity.class.getSimpleName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv_content = new TextView(this);
        tv_content.setText("I am SubActivity");
        setContentView(tv_content);
        Log.i(TAG, "SubActivity：app:"+getApplicationContext());

    }
}
