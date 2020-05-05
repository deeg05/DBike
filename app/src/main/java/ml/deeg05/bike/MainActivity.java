package ml.deeg05.bike;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    public static final String WHEEL_SIZE_PREFS_KEY = "wheel_size";
    public static final String SPEED_IN_MPH_PREFS_KEY = "speed_in_mph";
    public static final String SHARED_PREFS_NAME = "SmartBikeSettings";
    public static final String CYCLE_RECEIVER_ACTION = "speed_received";

    private static final String CURRENT_SPEED = "speed_key";
    private static final long STOP_INTERVAL = 5000;  // ms
    private int wheelSize;
    private long cycleTimestamp = 0;
    private boolean speedInMph = false;

    private TextView currentSpeedTextView;

    private BroadcastReceiver wheelCycleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(CURRENT_SPEED)) {
                float currentSpeed = intent.getFloatExtra(CURRENT_SPEED, 0.0f);
                currentSpeedTextView.setText(String.format("%.1f", currentSpeed));
            }
        }
    };

    private Timer checkStoppedTimer;

    private void registerCheckStoppedTimer() {
        long interval = 10;  // ms
        checkStoppedTimer = new Timer();
        checkStoppedTimer.schedule(new TimerTask() {
            long lastCycleTimestamp = 0;
            @Override
            public void run() {
                float currentSpeed = -1.0f;
                if (lastCycleTimestamp != cycleTimestamp) {
                    currentSpeed = SpeedCalculator.calculateSpeed(lastCycleTimestamp,
                            cycleTimestamp,
                            wheelSize,
                            speedInMph);

                    lastCycleTimestamp = cycleTimestamp;
                } else if (System.currentTimeMillis() - lastCycleTimestamp > STOP_INTERVAL) {
                    currentSpeed = 0.0f;
                    lastCycleTimestamp = 0;
                    cycleTimestamp = 0;
                }
                if (currentSpeed >= 0) {
                    Intent speedIntent = new Intent(CYCLE_RECEIVER_ACTION);
                    speedIntent.putExtra(CURRENT_SPEED, currentSpeed);
                    sendBroadcast(speedIntent);
                }
            }
        }, 0, interval);
    }

    private void unregisterCheckStoppedTimer() {
        if (checkStoppedTimer != null) {
            checkStoppedTimer.cancel();
            checkStoppedTimer = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentSpeedTextView = (TextView) findViewById(R.id.currentSpeedTextView);

        SharedPreferences preferences = getSharedPreferences(SHARED_PREFS_NAME, 0);
        wheelSize = preferences.getInt(WHEEL_SIZE_PREFS_KEY, 0);
        speedInMph = preferences.getBoolean(SPEED_IN_MPH_PREFS_KEY, false);
        if (wheelSize == 0) {
            openSettings();
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent keyevent) {
        if ((keyCode == KeyEvent.KEYCODE_HEADSETHOOK || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY)
                && keyevent.getAction() == KeyEvent.ACTION_DOWN) {
            cycleTimestamp = System.currentTimeMillis();
            return true;
        }
        if ((keyCode == KeyEvent.KEYCODE_HEADSETHOOK || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) && keyevent.getAction() == KeyEvent.KEYCODE_MEDIA_NEXT) {} // Stubs so you could peacefully listen to music
        if ((keyCode == KeyEvent.KEYCODE_HEADSETHOOK || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) && keyevent.getAction() == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {}
        return super.onKeyDown(keyCode, keyevent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter cycleFilter = new IntentFilter();
        cycleFilter.addAction(CYCLE_RECEIVER_ACTION);
        registerReceiver(wheelCycleReceiver, cycleFilter);

        registerCheckStoppedTimer();
    }

    protected void onPause() {
        super.onPause();
        unregisterCheckStoppedTimer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wheelCycleReceiver != null) {
            unregisterReceiver(wheelCycleReceiver);
        }
        unregisterCheckStoppedTimer();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            openSettings();
        }
        return super.onOptionsItemSelected(item);
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
}
