package hu.blint.ssldroid;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import hu.blint.ssldroid.db.SSLDroidDbAdapter;

//TODO: cacert + crl should be configurable for the tunnel
//TODO: test connection button

public class SSLDroidTunnelDetails extends Activity {
    private static final int INVALID_PORT = Integer.MIN_VALUE;
    private static final int NO_PORT = Integer.MIN_VALUE + 1;

    private final class SSLDroidTunnelHostnameChecker extends AsyncTask<String, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
            ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            String hostname = params[0];

            if (conMgr != null && conMgr.getActiveNetworkInfo() != null || conMgr.getActiveNetworkInfo().isAvailable()) {
                try {
                    InetAddress.getByName(hostname);
                } catch (UnknownHostException e) {
                    return false;
                }
            }
            return true;
        }

        protected void onPostExecute(Boolean result) {
            if (!result) {
                error("Remote host not found, please recheck...");
            }
        }
    }

    private final class SSLDroidTunnelValidator implements View.OnClickListener {
        public void onClick(View view) {
            TunnelConfig tunnel = parseTunnel();

            if (tunnel.name.isEmpty()) {
                error("Required tunnel name parameter not set up, skipping save");
            } else if (tunnel.listenPort == NO_PORT) {
                error("Required local port parameter not set up, skipping save");
            } else if (tunnel.listenPort == INVALID_PORT) {
                error("Local port parameter has invalid number format");
            } else if (tunnel.listenPort < 1025 || tunnel.listenPort > 65535) {
                error("Local port parameter not in valid range (1025-65535)");
            } else if (hasDuplicates(tunnel.listenPort)) {
                // Error
            } else if (tunnel.targetPort == NO_PORT) {
                error("Required remote host parameter not set up, skipping save");
            } else if (tunnel.targetPort == INVALID_PORT) {
                error("Remote port parameter has invalid number format");
            } else if (tunnel.targetPort < 1 || tunnel.targetPort > 65535) {
                error("Remote port parameter not in valid range (1-65535)");
            } else if (checkKeys(tunnel)) {
                new SSLDroidTunnelHostnameChecker().execute(tunnel.targetHost);
                saveState();
                setResult(RESULT_OK);
                finish();

            }
        }

        private boolean hasDuplicates(int listenPort) {
            for (TunnelConfig tunnel : dbHelper.fetchAllTunnels()) {
                if (listenPort == tunnel.listenPort) {
                    error("Local port already configured in tunnel '"+ tunnel.name +"', please change...");
                    return true;
                }
            }
            return false;
        }
    }

    private EditText name;
    private EditText listenPort;
    private EditText targetHost;
    private EditText targetPort;
    private EditText keyFile;
    private EditText keyPass;
    private Long rowId;
    private Boolean doClone = false;
    private SSLDroidDbAdapter dbHelper;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        dbHelper = new SSLDroidDbAdapter(this);
        setContentView(R.layout.tunnel_details);

        Button confirmButton = (Button) findViewById(R.id.tunnel_apply_button);
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

        rowId = null;
        Bundle extras = getIntent().getExtras();
        rowId = (bundle == null) ? null : (Long) bundle
                .getSerializable(SSLDroidDbAdapter.KEY_ROWID);
        if (extras != null) {
            rowId = extras.getLong(SSLDroidDbAdapter.KEY_ROWID);
            doClone = extras.getBoolean("doClone", false);
        }
        populateFields();
        confirmButton.setOnClickListener(new SSLDroidTunnelValidator());
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
        //Log.d("SSLDroid", "Gathered file names: "+namesList.toString());

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
                        error("Empty directory");
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
        Log.d("SSLDroid", "SD Card location: "+sdcard.toString());

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
        if (rowId != null) {
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

    public boolean checkKeys(TunnelConfig tunnel) {
        if (tunnel.keyFile.isEmpty()) {
            return true;
        }

        try {
            FileInputStream in = new FileInputStream(tunnel.keyFile);
            try {
                KeyStore store = KeyStore.getInstance("PKCS12");
                char[] password = tunnel.keyPass.toCharArray();
                store.load(in, password);

                Enumeration<String> eAliases = store.aliases();
                while (eAliases.hasMoreElements()) {
                    String alias = eAliases.nextElement();
                    if (store.isKeyEntry(alias)) {
                        // try to retrieve the private key part from PKCS12 certificate
                        store.getKey(alias, password);
                        X509Certificate.getInstance(store.getCertificate(alias).getEncoded()).checkValidity();
                    }
                }
                return true;
            } finally {
                in.close();
            }
        } catch (KeyStoreException e) {
            keyError(e);
        } catch (NoSuchAlgorithmException e) {
            keyError(e);
        } catch (CertificateException e) {
            keyError(e);
        } catch (javax.security.cert.CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            keyError(e);
        } catch (UnrecoverableKeyException e) {
            keyError(e);
        }
        return false;
    }

    private void keyError(Exception e) {
        error("PKCS12 problem: " + e.getMessage());
    }

    private void error(String message) {
        Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();
    }


    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveState();
        outState.putSerializable(SSLDroidDbAdapter.KEY_ROWID, rowId);
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

        if (rowId == null || doClone) {
            long id = dbHelper.createTunnel(tunnel);
            if (id > 0) {
                rowId = id;
            }
        } else {
            dbHelper.updateTunnel(tunnel);
        }
        Log.d("SSLDroid", "Saving settings...");

        //restart the service
        stopService(new Intent(this, SSLDroid.class));
        startService(new Intent(this, SSLDroid.class));
        Log.d("SSLDroid", "Restarting service after settings save...");

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
}

