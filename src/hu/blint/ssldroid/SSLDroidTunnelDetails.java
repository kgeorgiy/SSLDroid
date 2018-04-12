package hu.blint.ssldroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import hu.blint.ssldroid.db.SSLDroidDbAdapter;
import hu.blint.ssldroid.ui.ContextAsyncTask;
import hu.blint.ssldroid.ui.Settings;

//TODO: cacert + crl should be configurable for the tunnel

public class SSLDroidTunnelDetails extends Activity {
    private static final int IS_REACHABLE_TIMEOUT = 5000;

    private static final int INVALID_PORT = Integer.MIN_VALUE;
    private static final int NO_PORT = Integer.MIN_VALUE + 1;

    public static final String ROW_ID = "rowId";
    public static final String DO_CLONE = "doClone";
    public static final int NO_ROW_ID = -1;

    private Button testButton;
    private Button applyButton;

    private static class HostnameChecker extends ContextAsyncTask<SSLDroidTunnelDetails, String, Void, Boolean> {
        HostnameChecker(SSLDroidTunnelDetails details) {
            super(details);
        }

        @Override
        protected Boolean doInBackground(SSLDroidTunnelDetails details, String... params) {
            ConnectivityManager conMgr = (ConnectivityManager) details.getSystemService(Context.CONNECTIVITY_SERVICE);
            String hostname = params[0];

            if (conMgr == null || conMgr.getActiveNetworkInfo() == null || !conMgr.getActiveNetworkInfo().isAvailable()) {
                return true;
            }
            try {
                return InetAddress.getByName(hostname).isReachable(IS_REACHABLE_TIMEOUT);
            } catch (IOException e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(SSLDroidTunnelDetails details, Boolean result) {
            if (!result) {
                details.message("Remote host not found, please recheck...");
            }
        }
    }

    private static class TunnelChecker extends ContextAsyncTask<SSLDroidTunnelDetails, ActiveTunnel, Void, String> {
        TunnelChecker(SSLDroidTunnelDetails details) {
            super(details);
        }

        @Override
        protected String doInBackground(SSLDroidTunnelDetails details, ActiveTunnel... tunnels) {
            try {
                tunnels[0].connect().close();
                return "Test success";
            } catch (IOException e) {
                return "Cannot establish connection: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(SSLDroidTunnelDetails details, String error) {
            details.message(error);
            details.setTesting(false);
        }
    }

    private EditText name;
    private EditText listenPort;
    private EditText targetHost;
    private EditText targetPort;
    private EditText keyFile;
    private EditText keyPass;
    private long rowId = NO_ROW_ID;
    private Boolean doClone = false;
    private SSLDroidDbAdapter dbHelper;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        dbHelper = new SSLDroidDbAdapter(this);
        setContentView(R.layout.tunnel_details);

        applyButton = (Button) findViewById(R.id.tunnel_apply);
        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TunnelConfig tunnel = parseTunnel();
                isValid(tunnel);
                new HostnameChecker(SSLDroidTunnelDetails.this).execute(tunnel.targetHost);
                saveState();
                setResult(RESULT_OK);
                finish();
            }
        });

        testButton = (Button) findViewById(R.id.tunnel_test);
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActiveTunnel tunnel = isValid(parseTunnel());
                if (tunnel != null) {
                    setTesting(true);
                    new TunnelChecker(SSLDroidTunnelDetails.this).execute(tunnel);
                }
            }
        });

        name = findEditTextById(R.id.name);
        listenPort = findEditTextById(R.id.localport);
        targetHost = findEditTextById(R.id.remotehost);
        targetPort = findEditTextById(R.id.remoteport);
        keyFile = findEditTextById(R.id.pkcsfile);
        keyPass = findEditTextById(R.id.pkcspass);
        Button pickFile = (Button) findViewById(R.id.pickFile);

        pickFile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                pickFileSimple();
            }
        });

        Bundle extras = getIntent().getExtras();
        rowId = (bundle == null) ? NO_ROW_ID : bundle.getLong(ROW_ID, NO_ROW_ID);
        if (extras != null) {
            rowId = extras.getLong(ROW_ID, NO_ROW_ID);
            doClone = extras.getBoolean(DO_CLONE, false);
        }
        populateFields();
    }

    private void setTesting(boolean testing) {
        applyButton.setEnabled(!testing);
        testButton.setEnabled(!testing);
        testButton.setText(testing ? R.string.tunnel_testing : R.string.tunnel_test);
    }

    private EditText findEditTextById(int name) {
        return (EditText) findViewById(name);
    }

    final List<File> getFileNames(File url, File baseurl)
    {
        final List<File> names = new LinkedList<File>();
        File[] files = url.listFiles();
        if (files != null && files.length > 0) {
            for (File file : url.listFiles()) {
                if (file.getName().startsWith("."))
                    continue;
                names.add(file);
            }
        }
        return names;
    }

    private void showFiles(final List<File> names, final File baseurl) {
        final String[] namesList = new String[names.size()]; // = names.toArray(new String[] {});
        ListIterator<File> filelist = names.listIterator();
        int i = 0;
        while (filelist.hasNext()) {
            File file = filelist.next();
            if (file.isDirectory())
                namesList[i] = file.getAbsolutePath().replaceFirst(baseurl+"/", "")+" (...)";
            else
                namesList[i] = file.getAbsolutePath().replaceFirst(baseurl+"/", "");
            i++;
        }
        Log.d("Gathered file names: "+ Arrays.toString(namesList));

        // prompt user to select any file from the sdcard root
        new AlertDialog.Builder(SSLDroidTunnelDetails.this)
        .setTitle(R.string.pkcsfile_pick)
        .setItems(namesList, new OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                File name = names.get(arg1);
                if (name.isDirectory()) {
                    List<File> names_ = getFileNames(name, baseurl);
                    Collections.sort(names_);
                    if (names_.size() > 0) {
                        showFiles(names_, baseurl);
                    }
                    else
                        message("Empty directory");
                }
                if (name.isFile()) {
                    keyFile.setText(name.getAbsolutePath());
                    keyPass.requestFocus();
                }
            }
        })
        //create a Back button (shouldn't go above base URL)
        .setNeutralButton(R.string.back, new OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                if (names.size() == 0)
                    return;
                File name = names.get(0);
                if (!name.getParentFile().equals(baseurl)) {
                    List<File> names_ = getFileNames(name.getParentFile().getParentFile(), baseurl);
                    Collections.sort(names_);
                    if (names_.size() > 0) {
                        showFiles(names_, baseurl);
                    }
                    else
                        return;
                }
            }
        })
        .setNegativeButton(android.R.string.cancel, null).create().show();
    }

    //pick a file from /sdcard, courtesy of ConnectBot
    private void pickFileSimple() {
        // build list of all files in sdcard root
        final File sdcard = Environment.getExternalStorageDirectory();
        Log.d("SD Card location: "+sdcard.toString());

        // Don't show a dialog if the SD card is completely absent.
        final String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)
                && !Environment.MEDIA_MOUNTED.equals(state)) {
            new AlertDialog.Builder(SSLDroidTunnelDetails.this)
            .setMessage(R.string.alert_sdcard_absent)
            .setNegativeButton(android.R.string.cancel, null).create().show();
            return;
        }

        List<File> names = new LinkedList<File>();
        names = getFileNames(sdcard, sdcard);
        Collections.sort(names);
        showFiles(names, sdcard);
    }

    private void populateFields() {
        if (rowId != NO_ROW_ID) {
            final TunnelConfig tunnel = dbHelper.fetchTunnel(rowId);

            if(!doClone){
                name.setText(tunnel.name);
                listenPort.setText("" + tunnel.listenPort);
            }
            targetHost.setText(tunnel.targetHost);
            targetPort.setText("" + tunnel.targetPort);
            keyFile.setText(tunnel.keyFile);
            keyPass.setText(tunnel.keyPass);
        }
    }

    private void message(String message) {
        Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();
    }


    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveState();
        outState.putSerializable(ROW_ID, rowId);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //saveState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        populateFields();
    }

    private void saveState() {
        TunnelConfig tunnel = parseTunnel();

        //make sure that we have all of our values correctly set
        if (tunnel.name.length() == 0 || tunnel.listenPort == 0 || tunnel.targetHost.length() == 0 || tunnel.targetPort == 0) {
            return;
        }

        if (rowId == NO_ROW_ID || doClone) {
            long id = dbHelper.createTunnel(tunnel);
            if (id > 0) {
                rowId = id;
            }
        } else {
            dbHelper.updateTunnel(tunnel);
        }
        Log.d("Saving settings...");

        //restart the service
        stopService(new Intent(this, SSLDroid.class));
        startService(new Intent(this, SSLDroid.class));
        Log.d("Restarting service after settings save...");

    }

    private TunnelConfig parseTunnel() {
        return new TunnelConfig(
                rowId,
                name.getText().toString(),
                parsePort(listenPort.getText().toString()),
                targetHost.getText().toString(),
                parsePort(targetPort.getText().toString()),
                keyFile.getText().toString(),
                keyPass.getText().toString()
        );
    }

    private int parsePort(String value) {
        if (value.isEmpty()) {
            return NO_PORT;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return INVALID_PORT;
        }
    }

    private ActiveTunnel isValid(TunnelConfig tunnel) {
        if (tunnel.name.isEmpty()) {
            message("Required tunnel name parameter not set up, skipping save");
        } else if (tunnel.listenPort == NO_PORT) {
            message("Required local port parameter not set up, skipping save");
        } else if (tunnel.listenPort == INVALID_PORT) {
            message("Local port parameter has invalid number format");
        } else if (tunnel.listenPort < 1025 || tunnel.listenPort > 65535) {
            message("Local port parameter not in valid range (1025-65535)");
        } else if (tunnel.targetPort == NO_PORT) {
            message("Required remote host parameter not set up, skipping save");
        } else if (tunnel.targetPort == INVALID_PORT) {
            message("Remote port parameter has invalid number format");
        } else if (tunnel.targetPort < 1 || tunnel.targetPort > 65535) {
            message("Remote port parameter not in valid range (1-65535)");
        } else {
            for (TunnelConfig other : dbHelper.fetchAllTunnels()) {
                if (tunnel.listenPort == other.listenPort && tunnel.id != other.id) {
                    message("Local port already configured in tunnel '"+ other.name +"', please change...");
                    return null;
                }
            }
            try {
                return new ActiveTunnel(tunnel, Settings.getConnectionTimeout(this));
            } catch (TunnelException e) {
                message(e.getMessage());
            }
        }
        return null;
    }
}
