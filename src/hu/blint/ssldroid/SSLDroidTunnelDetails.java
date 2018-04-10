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

import java.security.cert.Certificate;
import javax.security.cert.CertificateExpiredException;
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
import android.database.Cursor;
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

    private final class SSLDroidTunnelHostnameChecker extends AsyncTask<String, Integer, Boolean> {

	@Override
	protected Boolean doInBackground(String... params) {
	        ConnectivityManager conMgr =  (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                String hostname = params[0];

	        if ( conMgr.getActiveNetworkInfo() != null || conMgr.getActiveNetworkInfo().isAvailable()) {
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
	        Toast.makeText(getBaseContext(), "Remote host not found, please recheck...", Toast.LENGTH_LONG).show();
            }
	}
    }

    private final class SSLDroidTunnelValidator implements View.OnClickListener {
	public void onClick(View view) {
	    if (name.getText().length() == 0) {
	        Toast.makeText(getBaseContext(), "Required tunnel name parameter not set up, skipping save", Toast.LENGTH_LONG).show();
	        return;
	    }
	    //local port validation
	    if (listenPort.getText().length() == 0) {
	        Toast.makeText(getBaseContext(), "Required local port parameter not set up, skipping save", Toast.LENGTH_LONG).show();
	        return;
	    }
	    else {
	        //local port should be between 1025-65535
	        int cPort = 0;
	        try {
	            cPort = Integer.parseInt(listenPort.getText().toString());
	        } catch (NumberFormatException e) {
	            Toast.makeText(getBaseContext(), "Local port parameter has invalid number format", Toast.LENGTH_LONG).show();
	            return;
	        }
	        if (cPort < 1025 || cPort > 65535) {
	            Toast.makeText(getBaseContext(), "Local port parameter not in valid range (1025-65535)", Toast.LENGTH_LONG).show();
	            return;
	        }
	        //check if the requested port is colliding with a port already configured for another tunnel
	        SSLDroidDbAdapter dbHelper = new SSLDroidDbAdapter(getBaseContext());
	        Cursor cursor = dbHelper.fetchAllLocalPorts();
	        startManagingCursor(cursor);
	        while (cursor.moveToNext()) {
	            String cDbName = cursor.getString(cursor.getColumnIndexOrThrow(SSLDroidDbAdapter.KEY_NAME));
	            int cDbPort = cursor.getInt(cursor.getColumnIndexOrThrow(SSLDroidDbAdapter.KEY_LOCALPORT));
	            if (cPort == cDbPort && !cDbName.contentEquals(name.getText().toString())) {
	                Toast.makeText(getBaseContext(), "Local port already configured in tunnel '"+cDbName+"', please change...", Toast.LENGTH_LONG).show();
	                return;
	            }
	        }
	    }
	    //remote host validation
	    if (targetHost.getText().length() == 0) {
	        Toast.makeText(getBaseContext(), "Required remote host parameter not set up, skipping save", Toast.LENGTH_LONG).show();
	        return;
	    }
	    else {
		//if we have interwebs access, the remote host should exist
		String hostname = targetHost.getText().toString();
		new SSLDroidTunnelHostnameChecker().execute(hostname);
	    }

	    //remote port validation
	    if (targetPort.getText().length() == 0) {
	        Toast.makeText(getBaseContext(), "Required remote port parameter not set up, skipping save", Toast.LENGTH_LONG).show();
	        return;
	    }
	    else {
	        //remote port should be between 1025-65535
	        int cPort = 0;
	        try {
	            cPort = Integer.parseInt(targetPort.getText().toString());
	        } catch (NumberFormatException e) {
	            Toast.makeText(getBaseContext(), "Remote port parameter has invalid number format", Toast.LENGTH_LONG).show();
	            return;
	        }
	        if (cPort < 1 || cPort > 65535) {
	            Toast.makeText(getBaseContext(), "Remote port parameter not in valid range (1-65535)", Toast.LENGTH_LONG).show();
	            return;
	        }
	    }
	    if (keyFile.getText().length() != 0) {
	        // try to open pkcs12 file with password
	        String cPkcsFile = keyFile.getText().toString();
	        String cPkcsPass = keyPass.getText().toString();
	        try {
	            if (checkKeys(cPkcsFile, cPkcsPass) == false) {
	                return;
	            }
	        } catch (Exception e) {
	            Toast.makeText(getBaseContext(), "PKCS12 problem: "+e.getMessage(), Toast.LENGTH_LONG).show();
	            return;
	        }
	    }
	    saveState();
	    setResult(RESULT_OK);
	    finish();
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
                        Toast.makeText(getBaseContext(), "Empty directory", Toast.LENGTH_LONG).show();
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

    public boolean checkKeys(String inCertPath, String passw) throws Exception {
        try {
            FileInputStream in_cert = new FileInputStream(inCertPath);
            KeyStore myStore = KeyStore.getInstance("PKCS12");
            myStore.load(in_cert, passw.toCharArray());
            Enumeration<String> eAliases = myStore.aliases();
            while (eAliases.hasMoreElements()) {
                String strAlias = eAliases.nextElement();
                if (myStore.isKeyEntry(strAlias)) {
                    // try to retrieve the private key part from PKCS12 certificate
                    myStore.getKey(strAlias, passw.toCharArray());
                    Certificate mycrt = myStore.getCertificate(strAlias);
                    X509Certificate mycert = X509Certificate.getInstance(mycrt.getEncoded());
                    try {
                	mycert.checkValidity();
                    } catch (CertificateExpiredException e) {
                        Toast.makeText(getBaseContext(), "PKCS12 problem: "+e.getMessage(), Toast.LENGTH_LONG).show();
                        return false;                	
                    }
                }
            }

        } catch (KeyStoreException e) {
            Toast.makeText(getBaseContext(), "PKCS12 problem: "+e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        } catch (NoSuchAlgorithmException e) {
            Toast.makeText(getBaseContext(), "PKCS12 problem: "+e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        } catch (CertificateException e) {
            Toast.makeText(getBaseContext(), "PKCS12 problem: "+e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "PKCS12 problem: "+e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        } catch (UnrecoverableKeyException e) {
            Toast.makeText(getBaseContext(), "PKCS12 problem: "+e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
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
        TunnelConfig tunnel = new TunnelConfig(
                rowId,
                name.getText().toString(),
                getPort(listenPort),
                targetHost.getText().toString(),
                getPort(targetPort),
                keyFile.getText().toString(),
                keyPass.getText().toString()
        );

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

    private int getPort(EditText port) {
        int sLocalport = 0;
        try {
            sLocalport = Integer.parseInt(port.getText().toString());
        } catch (NumberFormatException e) {
        }
        return sLocalport;
    }
}

