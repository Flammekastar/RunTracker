package com.flammekastar.locationapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
/**
 * The activity to launch settings at the user. Only consists of language settings for now, but a
 * good base for more in the future.
 *
 * @author  Alexander Maaby
 * @version 1.0
 * @since   29-09-2015
 */

public class SettingsActivity extends Activity implements OnClickListener {
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new SettingsFragment()).commit();
    }
	
    public void onClick(View view) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.action_list:
                Toast.makeText(this, "Refresh selected", Toast.LENGTH_SHORT)
                        .show();
                Intent listIntent = new Intent(this, RunListActivity.class);
                this.startActivity(listIntent);
                break;
            default:
                break;
        }

        return true;
    }
}
