package com.android.cloudfiles.cloudfilesforandroid;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    CloudFiles cloudFiles = CloudFiles.initiate("bipbip", "5fbccf7a6f6f4371890655bb6d0f1bd7");

                    String url = cloudFiles.uploadFromUrl("http://immaceleb.com/wp-content/uploads/2015/05/bar-refaeli-9.jpg", Regions.NORTHERN_VIRGINIA, "home5", "fff");

                 //   String url = cloudFiles.uploadFromBytes("http://immaceleb.com/wp-content/uploads/2015/05/bar-refaeli-9.jpg".getBytes(), Regions.NORTHERN_VIRGINIA, "home5", "fff");


                    Log.d("dd", url);


                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
