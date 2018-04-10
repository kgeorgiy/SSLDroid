package hu.blint.ssldroid;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import hu.blint.ssldroid.db.SSLDroidDbAdapter;

public class SSLDroidGui extends ListActivity {
    private static final int ACTIVITY_CREATE = 0;
    private static final int ACTIVITY_EDIT = 1;
    private static final int DELETE_ID = Menu.FIRST + 1;
    private static final int CLONE_ID = Menu.FIRST + 2;

    private SSLDroidDbAdapter dbHelper;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tunnel_list);
        this.getListView().setDividerHeight(2);
        dbHelper = new SSLDroidDbAdapter(this);
        fillData();
        registerForContextMenu(getListView());
    }

    // Create the menu based on the XML defintion
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    // Reaction to the menu selection
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        return onSelected(item) || super.onMenuItemSelected(featureId, item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return onSelected(item) || super.onOptionsItemSelected(item);
    }

    private boolean onSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.addtunnel:
            createTunnel();
            return true;
        case R.id.stopservice:
            Log.d("SSLDroid", "Stopping service");
            stopService(new Intent(this, SSLDroid.class));
            return true;
        case R.id.stopserviceforgood:
            Log.d("SSLDroid", "Stopping service until explicitly started");
            dbHelper.setStopStatus();
            stopService(new Intent(this, SSLDroid.class));
            return true;
        case R.id.startservice:
            Log.d("SSLDroid", "Starting service");
            dbHelper.delStopStatus();
            startService(new Intent(this, SSLDroid.class));
            return true;
        case R.id.readlogs:
            readLogs();
            return true;
        }
        return false;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case DELETE_ID:
                dbHelper.deleteTunnel(info.id);
                fillData();
                return true;
            case CLONE_ID:
                cloneTunnel(info.id);
                fillData();
                return true;
        }
        return super.onContextItemSelected(item);
    }

    private void createTunnel() {
        Intent i = new Intent(this, SSLDroidTunnelDetails.class);
        startActivityForResult(i, ACTIVITY_CREATE);
    }

    public void cloneTunnel(long id) {
        Intent i = new Intent(this, SSLDroidTunnelDetails.class);
        i.putExtra(SSLDroidDbAdapter.KEY_ROWID, id);
        i.putExtra("doClone", true);
        startActivityForResult(i, ACTIVITY_EDIT);
    }
    
    private void readLogs() {
        Intent i = new Intent(this, SSLDroidReadLogs.class);
        startActivity(i);
    }

    // ListView and view (row) on which was clicked, position and
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, SSLDroidTunnelDetails.class);
        i.putExtra(SSLDroidDbAdapter.KEY_ROWID, id);
        // Activity returns an result if called with startActivityForResult
        startActivityForResult(i, ACTIVITY_EDIT);
    }

    // Called with the result of the other activity
    // requestCode was the origin request code send to the activity
    // resultCode is the return code, 0 is everything is ok
    // intend can be use to get some data from the caller
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        fillData();

    }

    private void fillData() {
        final List<String> names = new ArrayList<String>();
        final List<Long> ids = new ArrayList<Long>();
        for (TunnelConfig tunnel : dbHelper.fetchAllTunnels()) {
            names.add(tunnel.name);
            ids.add(tunnel.id);
        }

        setListAdapter(new ArrayAdapter<String>(this, R.layout.tunnel_list_item, R.id.text1, names) {
            @Override
            public long getItemId(int position) {
                return ids.get(position);
            }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, DELETE_ID, 0, R.string.menu_delete);
        menu.add(0, CLONE_ID, 0, R.string.menu_clone);
    }
    
    @Override
    public void onDestroy (){
	dbHelper.close();
	super.onDestroy();
    }
}
