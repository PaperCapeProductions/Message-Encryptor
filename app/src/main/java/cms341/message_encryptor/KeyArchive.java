package cms341.message_encryptor;

import android.app.DialogFragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;

import static android.R.attr.key;


public class KeyArchive extends AppCompatActivity {
    private DBManager dbm;
    ListView results;
    ArrayAdapter resultsAdapter;
    private ActionMode mActionMode;
    private String password = null;
    HashMap<Integer, String> keys;
    Intent toMessage, toNFC;
    int position;
    DialogFragment login;
    private SharedPreferences prefs;
    private SharedPreferences.OnSharedPreferenceChangeListener setListener;


    @Override
    public void onRestart(){
        super.onRestart();
        getStoredKeys();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = this.getSharedPreferences("user", MODE_PRIVATE);
        password = prefs.getString("pass", "password");
        setContentView(R.layout.activity_key_archive);
        SQLiteDatabase.loadLibs(this);
        login = new LoginFragment();
        login.show(getFragmentManager(),"login");

        prefs.registerOnSharedPreferenceChangeListener(setListener);
        ArrayList<String> test = new ArrayList<>();
        results = (ListView)findViewById(R.id.keys);
        resultsAdapter = new ArrayAdapter<String>(this, R.layout.convo_item, test);
        dbm = new DBManager(this);

        results.setAdapter(resultsAdapter);


        results.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                toMessage.putExtra("key", keys.get(position));
                startActivity(toMessage);
            }
        });

        results.setOnItemLongClickListener(new android.widget.AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (mActionMode != null) {
                    return false;
                }

                mActionMode = KeyArchive.this.startActionMode(mActionModeCallback);
                view.setSelected(true);
                position = i;
                return true;
            }
        });



        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent( getApplicationContext( ),
                        KeyGenerator.class);
                startActivity(intent);
            }
        });

        setListener = new SharedPreferences.OnSharedPreferenceChangeListener(){

            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                password = prefs.getString("pass", "password");
            }
        };
    }


    public void getStoredKeys(){
            if(!password.equals("password")) {
                int id = 0;
                toMessage = new Intent(this, MainActivity.class);
                toNFC = new Intent(this, NFCManager.class);

                ArrayList<String> convos = dbm.selectAll(password);
                keys = new HashMap<Integer, String>();


                int index = 0;
                if (!resultsAdapter.isEmpty()) resultsAdapter.clear();

                while (index < convos.size()) {
                    index++;
                    resultsAdapter.add(convos.get(index));
                    Log.i("added item:", convos.get(index));
                    index++;
                    keys.put(id, convos.get(index));
                    index++;
                    id++;

                }
            }else{
                Toast.makeText(this, "Wrong password", Toast.LENGTH_LONG).show();
            }
    }

    public void shareKey(){
        System.out.print("\n\n " + results.getItemAtPosition(position).toString());
        toNFC.putExtra("conversation", results.getItemAtPosition(position).toString());
        toNFC.putExtra("key",keys.get(position));
        startActivity(toNFC);
    }

    public void deleteStoredKey(){
        dbm.delete(password,results.getItemAtPosition(position).toString());
        getStoredKeys();
    }


    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_key_archive, menu);
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_share:
                    shareKey();
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                case R.id.menu_delete:
                    deleteStoredKey();
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
        }

    };


}

