package dhbw.mobile2;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;


public class MainScreen extends ActionBarActivity implements ListEventsFragment.OnFragmentInteractionListener, ParticipantsListFragment.OnParticipantInteractionListener {
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    // nav drawer title
    private CharSequence mDrawerTitle;

    // used to store app title
    private CharSequence mTitle;

    // slide menu items
    private String[] navMenuTitles;
    private TypedArray navMenuIcons;

    private ArrayList<NavDrawerItem> navDrawerItems;
    private NavDrawerListAdapter adapter;

    private boolean statusParticipation = false;
    private boolean cancelOtherEvent = false;

    private boolean mapShown = true;
    private boolean listShown = false;


    public static FragmentManager fragmentManager;

    //currentUser
    ParseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        //Check if User is logged in
        if(ParseUser.getCurrentUser() == null){
            //If the user is not logged in call the loginActiviy
            Intent intent = new Intent(getApplicationContext(), LogInActivity.class);
            startActivity(intent);
        } else {
            currentUser = ParseUser.getCurrentUser();
        }


        mTitle = mDrawerTitle = getTitle();

        //Load slide menu items & icons
        navMenuTitles = getResources().getStringArray(R.array.nav_drawer_items);
        navMenuIcons = getResources().obtainTypedArray(R.array.nav_drawer_icons);


        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.list_slidermenu);

        navDrawerItems = new ArrayList<NavDrawerItem>();

        //Adding items to array, counter is deactivated:
        //0 = Profile
        //1 = Map
        //2 = Create new Event
        //3 = My event
        //4 = Settings
        //5 = Logout
        //6 = Vincents TestFragment

        navDrawerItems.add(new NavDrawerItem(navMenuTitles[0], navMenuIcons.getResourceId(0, -1)));
        navDrawerItems.add(new NavDrawerItem(navMenuTitles[1], navMenuIcons.getResourceId(0, -1)));
        navDrawerItems.add(new NavDrawerItem(navMenuTitles[2], navMenuIcons.getResourceId(2, -1)));
        navDrawerItems.add(new NavDrawerItem(navMenuTitles[3], navMenuIcons.getResourceId(3, -1)));
        navDrawerItems.add(new NavDrawerItem(navMenuTitles[4], navMenuIcons.getResourceId(4, -1)));
        navDrawerItems.add(new NavDrawerItem(navMenuTitles[5], navMenuIcons.getResourceId(5, -1)));
        navDrawerItems.add(new NavDrawerItem(navMenuTitles[6], navMenuIcons.getResourceId(5, -1)));

        //Recycle the typed array
        navMenuIcons.recycle();

        //Setting the nav drawer list adapter
        if(ParseUser.getCurrentUser()!=null){
            adapter = new NavDrawerListAdapter(getApplicationContext(), navDrawerItems);
            mDrawerList.setAdapter(adapter);
        }


        //Enabling action bar app icon and behaving it as toggle button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                R.drawable.ic_drawer, //nav menu toggle icon  war mal ic_drawer
                R.string.app_name, // nav drawer open - description for accessibility
                R.string.app_name // nav drawer close - description for accessibility
        ) {
            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(mTitle);

                //Is called on onPrepareOptionsMenu() to show action bar icons
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle(mDrawerTitle);

                //Calling onPrepareOptionsMenu() to hide action bar icons
                invalidateOptionsMenu();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (savedInstanceState == null) {
            //Setting MapFragment as default fragment
            Fragment fragment = new AppMapFragment();
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.frame_container, fragment).commit();
        }

        mDrawerList.setOnItemClickListener(new SlideMenuClickListener());
    }

    @Override
    public void onFragmentInteraction(String id) {

    }

    @Override
    public void onParticipantInteraction(String id) {

    }

    private class SlideMenuClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            displayView(position);
        }
    }

    private void displayView(int position) {

        Fragment fragment = null;

        if(position==0) {
            fragment = ProfileFragment.newInstance(ParseUser.getCurrentUser().getObjectId());
        }else if(position==1){
            fragment = new AppMapFragment();
        }else if(position==2){
            checkParticipation();
            if (statusParticipation){
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                //Yes button clicked
                                ParseUser.getCurrentUser().put("eventId", "no_event");
                                ParseUser.getCurrentUser().saveInBackground();
                                createEventFragment();
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                //No button clicked
                                break;
                        }
                    }


                };

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(
                        "You already participate in an event. " +
                                "Cancel other event to create new one?").setPositiveButton(
                        "Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
            } else {
                fragment = new CreateEventFragment();
            }

        }else if(position==3){
            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("eventId", currentUser.getString("eventId"));
            editor.commit();

            fragment = new EventDetailFragment();

        }else if(position==4){
            fragment = new SettingsFragment();
        }else if(position==5){
            fragment = new LogoutFragment();
        }else if(position == 6){
            /*SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("eventId", "l9lvvbNByv");
            editor.commit();*/

            fragment = new ListEventsFragment();
        }

        // If called Fragment is the logout fragment just add the fragment to the other instead of replacing it
        // because only the showDialog needs to be displayed and it shall be overlapping the existing fragment
        if (fragment != null && fragment.getClass() == LogoutFragment.class) {
            Fragment.instantiate(getApplicationContext(), LogoutFragment.class.getName(), new Bundle());
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().add(R.id.frame_container, fragment).commit();
        }

        if (fragment != null && fragment.getClass() != LogoutFragment.class) {

            // update selected item and title, then close the drawer
            mDrawerList.setItemChecked(position, true);
            mDrawerList.setSelection(position);
            setTitle(navMenuTitles[position]);
            mDrawerLayout.closeDrawer(mDrawerList);

            FragmentManager fragmentManager = getFragmentManager();
            //fragmentManager.beginTransaction().replace(R.id.frame_container, fragment).commit();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.frame_container, fragment);
            transaction.addToBackStack(null);
            transaction.commit();

        } else {
            //Error in creating fragment
            Log.e("MainActivity", "Error in creating fragment");
        }
    }

    private void createEventFragment(){
        Fragment fragment = new CreateEventFragment();
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.frame_container, fragment).commit();

            // update selected item and title, then close the drawer
            mDrawerList.setItemChecked(2, true);
            mDrawerList.setSelection(2);
            setTitle(navMenuTitles[2]);
            mDrawerLayout.closeDrawer(mDrawerList);

        }

    private void checkParticipation(){
        String eventIdOfUser = ParseUser.getCurrentUser().getString("eventId");
        if (eventIdOfUser != null){
            Log.d("Main", "eventId is not null");
            if (!eventIdOfUser.equals("no_event")) {
                    statusParticipation = true;

            } else {

                statusParticipation = false;

            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.main, menu);
        getMenuInflater().inflate(R.menu.menu_create_event, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Toggle nav drawer on selecting action bar app icon/title
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        //Handle action bar actions click
        switch (item.getItemId()) {
            case R.id.action_list_events:
                openList();
                return true;
            case R.id.action_map:
                openMap();
            case R.id.action_settings:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openList(){
        Fragment fragment;
                FragmentManager fragmentManager = getFragmentManager();
                fragment = new ListEventsFragment();

                 FragmentTransaction transaction = fragmentManager.beginTransaction();
                 transaction.replace(R.id.frame_container, fragment);
                 transaction.addToBackStack(null);
                 transaction.commit();
            }

    private void openMap(){
              Fragment fragment = new AppMapFragment();
                FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.frame_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
            }


    //Called when invalidateOptionsMenu() is triggered
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        /*// if nav drawer is opened, hide the action items
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        menu.findItem(R.id.action_settings).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);*/
         menu.findItem(R.id.action_list_events).setVisible(mapShown);
        menu.findItem(R.id.action_map).setVisible(listShown);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    public void setMapShown(boolean mapShown) {
        this.mapShown = mapShown;
    }

    public void setListShown(boolean listShown) {
        this.listShown = listShown;
    }

    @Override
    public void onBackPressed(){
        Log.d("Main", "onBackPressed");

        FragmentManager fragmentManager = getFragmentManager();
        if(fragmentManager.getBackStackEntryCount()!=0){
            fragmentManager.popBackStack();
        }else{
            super.onBackPressed();
        }
    }
}