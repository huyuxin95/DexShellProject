package com.jju.yuxin.sourceproject;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String TAG=MainActivity.class.getSimpleName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv_content = new TextView(this);
        tv_content.setText("I am Source Apk");
        tv_content.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent(MainActivity.this, SubActivity.class);
                startActivity(intent);
            }});
        setContentView(tv_content);
        Log.i(TAG, "onCreate：app:"+getApplicationContext());
    }
}
