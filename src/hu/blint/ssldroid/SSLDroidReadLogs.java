package hu.blint.ssldroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SSLDroidReadLogs extends Activity {

    private ScrollView scrollView;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.read_logs, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.readLogs_menu_refresh:
                refreshLogs();
                return true;
            case R.id.readLogs_menu_share:
                shareLogs();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.read_logs);
        scrollView = (ScrollView) findViewById(R.id.readLogs_view);
        refreshLogs();
    }

    public void refreshLogs() {
        TextView logs = (TextView) findViewById(R.id.readLogs_text);
        logs.setText("");
        try {
            Process process = Runtime.getRuntime()
                    .exec(new String[]{"logcat", "-d", "-v", "time", "-b", "main", Log.TAG + ":D AndroidRuntime *:S" });

            final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            try {
                String separator = System.getProperty("line.separator");

                String line;
                while ((line = reader.readLine()) != null) {
                    logs.append(line + separator);
                }
            } catch (IOException e) {
                reader.close();
            }
        } catch (IOException e) {
            Log.d("Logcat problem: " + e.toString());
        }
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    public void shareLogs() {
        Intent sendIntent = new Intent();
        TextView logcontainer = (TextView) findViewById(R.id.readLogs_text);
        CharSequence logdata = logcontainer.getText();

        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, logdata);
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }
}
