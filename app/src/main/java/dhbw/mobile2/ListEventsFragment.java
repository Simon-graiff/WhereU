package dhbw.mobile2;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;


import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.List;


public class ListEventsFragment extends Fragment implements AdapterView.OnItemClickListener {

    private HelperClass helperObject = new HelperClass();

    private OnFragmentInteractionListener mListener;

    ArrayList<String> idArray;

    private AbsListView mListView;
    private ListAdapter mAdapter;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ListEventsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //get the data to fill the event lines
        getEventData();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View screenView = inflater.inflate(R.layout.fragment_events_list, container, false);
        screenView.setBackgroundColor(Color.rgb(240, 240, 240));

        // Set the adapter
        mListView = (AbsListView) screenView.findViewById(android.R.id.list);
        mListView.setAdapter(mAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        return screenView;
    }

    @Override
    public void onResume() {
        super.onResume();
        //activate the ActionBar Button for the Map; reload ActionBar
        setListShown(true);

        //add eventTitle to ActionBar
        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        if (actionBar != null){
            actionBar.setTitle("List of Events");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        //deactivate the ActionBar Button for the Map; reload ActionBar
        setListShown(false);
    }

    private void setListShown(boolean isShown){
        ((MainActivity) getActivity()).setListShown(isShown);
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        //activate the Listener for the ListItems
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        //deactivate the Listener for the ListItems
        mListener = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {

            mListener.onFragmentInteraction(idArray.get(position));

            //switch to screen "EventDetail"
            //provide the eventId of the event object for the EventDetail-Screen
            helperObject.putEventIdToSharedPref(getActivity(), idArray.get(position));

            //create a new EventDetail-Fragment for the relevant event
            helperObject.switchToFragment(getFragmentManager(), new EventDetailFragment());
        }
    }


    //Listener to fetch events on event items in the list
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(String id);
    }

    public void getEventData(){
        //necessary content for eah list item
        final ArrayList<String> titleArray = new ArrayList<>();
        final ArrayList<String> categoryArray = new ArrayList<>();
        final ArrayList<Double> distanceArray = new ArrayList<>();
        final ArrayList<String> timeArray = new ArrayList<>();
        final ArrayList<String> participantCountArray = new ArrayList<>();
        idArray = new ArrayList<>();

        //Creating ParseGeoPoint with user's current location for further processing in query
        LocationManager locationManager =  (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        Location userLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        final ParseGeoPoint point = new ParseGeoPoint(userLocation.getLatitude(), userLocation.getLongitude());

        //Query: Get all events in the reach of five kilometers
        ParseQuery<ParseObject> query = ParseQuery.getQuery("FilteredEvents");
        query.fromLocalDatastore();

        //Get the List of Filtered Events from EventMap Page
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> listOfEventList, ParseException e) {
                Log.d("Main", "Received " + listOfEventList.size() + " events");

                if (e == null && listOfEventList.size() > 0) {
                    for (int j=0; j < listOfEventList.size(); j++) {
                        //get single event object from query
                        ParseObject event;
                        List<ParseObject> listEvents;
                        try {
                            listEvents = listOfEventList.get(0).fetchIfNeeded().getList("list");
                            Log.d("Main","Size: " + listEvents.size());
                            if (listEvents.size() > 0){
                            for (int i = 0; i < listEvents.size(); i++) {


                                event = listEvents.get(i).fetchIfNeeded();

                                //add title to Event List object
                                titleArray.add(event.getString("title"));
                                //add id to Event List object
                                idArray.add(event.getObjectId());
                                //add category to Event List object
                                categoryArray.add(event.getString("category"));

                                //calculate distance and add it to Event List object
                                distanceArray.add(
                                        helperObject.calculateDistance(
                                                point, event.getParseGeoPoint("geoPoint")));

                                //start: get the time of the event, create a string out of it and add it to Event List object
                                timeArray.add(helperObject.getTimeScopeString(event));
                                //end: get the time of the event, create a string out of it and add it to Event List object

                                //get a string from the current participants and maximum amount of them, add
                                //it to Event object
                                participantCountArray.add(helperObject.getParticipantsString(event));
                            }

                            } else {
                                Log.d("Main","Empty will be visible");
                            }


                        } catch (ParseException e1) {
                            e1.printStackTrace();

                        }



                    }
                } else {
                    if (getActivity().findViewById(R.id.empty) != null) {
                        getActivity().findViewById(R.id.empty).setVisibility(View.VISIBLE);
                    }
                }


                //add all event object content to the Event List
                mAdapter = new EventListAdapter(getActivity(),
                        titleArray,
                        categoryArray,
                        distanceArray,
                        timeArray,
                        participantCountArray);
                mListView.setAdapter(mAdapter);
            }
        });
    }
}
