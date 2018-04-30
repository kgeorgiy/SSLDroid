package hu.blint.ssldroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

import hu.blint.ssldroid.db.SSLDroidDbAdapter;
import hu.blint.ssldroid.ui.ContextAsyncTask;
import hu.blint.ssldroid.ui.FileChooser;
import hu.blint.ssldroid.ui.Settings;

public class SSLDroidTunnelDetails extends Activity {
    private static final int IS_REACHABLE_TIMEOUT = 5000;

    private static final int INVALID_PORT = Integer.MIN_VALUE;
    private static final int NO_PORT = Integer.MIN_VALUE + 1;

    public static final String ROW_ID = "rowId";
    public static final String DO_CLONE = "doClone";
    public static final int NO_ROW_ID = -1;

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
    private EditText trustFile;
    private EditText trustPass;
    private Spinner trustType;
    private Button testButton;
    private Button applyButton;
    private View trustFileRow;
    private View trustPassRow;

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

        name = findEditTextById(R.id.tunnel_name);
        listenPort = findEditTextById(R.id.tunnel_localPort);
        targetHost = findEditTextById(R.id.tunnel_remoteHost);
        targetPort = findEditTextById(R.id.tunnel_remotePort);
        keyFile = findEditTextById(R.id.tunnel_certFile);
        keyPass = findEditTextById(R.id.tunnel_certPass);
        trustType = (Spinner) findViewById(R.id.tunnel_trustType);
        trustFile = findEditTextById(R.id.tunnel_trustFile);
        trustPass = findEditTextById(R.id.tunnel_trustPass);


        trustFileRow = findViewById(R.id.tunnel_trustFile_row);
        trustPassRow = findViewById(R.id.tunnel_trustPass_row);

        setFileChooser(R.id.tunnel_pickKeyFile, keyFile, keyPass);
        setFileChooser(R.id.tunnel_pickTrustFile, trustFile, trustPass);

        ArrayAdapter<String> trustTypes = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        trustTypes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (TrustType type : TrustType.values()) {
            trustTypes.add(getString(type.getId()));
        }
        trustType.setAdapter(trustTypes);
        trustType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateVisibility();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Ignore
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

    private void setFileChooser(int id, final EditText value, final EditText focus) {
        final FileChooser fileChooser = new FileChooser(getString(R.string.tunnel_pickFile_dialogTitle), this, new FileChooser.Listener() {
            @Override
            public void fileChosen(File file) {
                value.setText(file.getAbsolutePath());
                focus.requestFocus();
            }
        });

        findViewById(id).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                fileChooser.pickFileSimple();
            }
        });
    }

    private void setTesting(boolean testing) {
        applyButton.setEnabled(!testing);
        testButton.setEnabled(!testing);
        testButton.setText(testing ? R.string.tunnel_testing : R.string.tunnel_test);
    }

    private EditText findEditTextById(int name) {
        return (EditText) findViewById(name);
    }

    private void populateFields() {
        if (rowId != NO_ROW_ID) {
            final TunnelConfig tunnel = dbHelper.fetchTunnel(rowId);

            if(!doClone){
                name.setText(tunnel.name);
                listenPort.setText(getString(R.string.tunnel_port_format, tunnel.listenPort));
            }
            targetHost.setText(tunnel.targetHost);
            targetPort.setText(getString(R.string.tunnel_port_format, tunnel.targetPort));
            keyFile.setText(tunnel.keyFile);
            keyPass.setText(tunnel.keyPass);

            trustType.setSelection(tunnel.trustType.ordinal());
            trustFile.setText(tunnel.trustFile);
            trustPass.setText(tunnel.trustPass);
        } else {
            trustType.setSelection(0);
        }
        updateVisibility();
    }

    private void updateVisibility() {
        final int trustVisibility = trustType.getSelectedItemPosition() == TrustType.CUSTOM.ordinal() ? View.VISIBLE : View.GONE;
        trustFileRow.setVisibility(trustVisibility);
        trustPassRow.setVisibility(trustVisibility);
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
                keyPass.getText().toString(),
                TrustType.values()[trustType.getSelectedItemPosition()],
                trustFile.getText().toString(),
                trustPass.getText().toString()
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
