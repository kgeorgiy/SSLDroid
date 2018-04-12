package hu.blint.ssldroid;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hu.blint.ssldroid.db.SSLDroidDbAdapter;
import hu.blint.ssldroid.ui.Settings;
import hu.blint.ssldroid.ui.SettingsActivity;

public class SSLDroidGui extends ListActivity {
    private static final int ACTIVITY_CREATE = 0;
    private static final int ACTIVITY_EDIT = 1;
    private static final int DELETE_ID = Menu.FIRST + 1;
    private static final int CLONE_ID = Menu.FIRST + 2;
    public static final String NAME = "" + R.id.tunnelList_name;
    public static final String INFO = "" + R.id.tunnelList_info;
    public static final String[] NAMES = {NAME, INFO};
    public static final int[] IDS = {R.id.tunnelList_name, R.id.tunnelList_info};

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        final boolean running = SSLDroid.isStarted();
        menu.findItem(R.id.mainMenu_startService).setVisible(!running);
        menu.findItem(R.id.mainMenu_stopService).setVisible(running);
        menu.findItem(R.id.mainMenu_stopServiceUntilStarted).setVisible(running);

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
        case R.id.mainMenu_addTunnel:
            createTunnel();
            return true;
        case R.id.mainMenu_stopService:
            Log.d("Stopping service");
            stopService();
            return true;
        case R.id.mainMenu_stopServiceUntilStarted:
            Log.d("Stopping service until explicitly started");
            dbHelper.setStopStatus();
            stopService();
            return true;
        case R.id.mainMenu_startService:
            Log.d("Starting service");
            dbHelper.clearStopStatus();
            SSLDroid.start(this);
            invalidateOptionsMenu();
            return true;
        case R.id.mainMenu_readLogs:
            readLogs();
            return true;
        case R.id.mainMenu_settings:
            showSettings();
            return true;
        }
        return false;
    }

    private void stopService() {
        SSLDroid.stop(this);
        invalidateOptionsMenu();
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
        i.putExtra(SSLDroidTunnelDetails.ROW_ID, id);
        i.putExtra(SSLDroidTunnelDetails.DO_CLONE, true);
        startActivityForResult(i, ACTIVITY_EDIT);
    }
    
    private void readLogs() {
        startActivity(new Intent(this, SSLDroidReadLogs.class));
    }

    private void showSettings() {
        startActivityForResult(new Intent(this, SettingsActivity.class), 0);
    }

    // ListView and view (row) on which was clicked, position and
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, SSLDroidTunnelDetails.class);
        i.putExtra(SSLDroidTunnelDetails.ROW_ID, id);
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
        final List<Long> ids = new ArrayList<Long>();
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (TunnelConfig tunnel : dbHelper.fetchAllTunnels()  ) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put(NAME, tunnel.name);
            item.put(INFO, tunnel.listenPort + ":" + tunnel.targetHost + ":" + tunnel.targetPort);
            items.add(item);
            ids.add(tunnel.id);
        }
        final int visibility = Settings.isShowTunnelInfo(this) ? View.VISIBLE : View.GONE;
        setListAdapter(new SimpleAdapter(this, items, R.layout.tunnel_list_item, NAMES, IDS) {
            @Override
            public long getItemId(int position) {
                return ids.get(position);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                view.findViewById(R.id.tunnelList_info).setVisibility(visibility);
                return view;
            }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, DELETE_ID, 0, R.string.tunnelMenu_delete);
        menu.add(0, CLONE_ID, 0, R.string.tunnelMenu_clone);
    }
    
    @Override
    public void onDestroy (){
	dbHelper.close();
	super.onDestroy();
    }
}
