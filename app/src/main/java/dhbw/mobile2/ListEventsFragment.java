package dhbw.mobile2;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.List;

import dhbw.mobile2.dummy.DummyContent;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnFragmentInteractionListener}
 * interface.
 */
public class ListEventsFragment extends Fragment implements AdapterView.OnItemClickListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    ArrayList<String> titleArray;
    ArrayList<String> idArray;

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;
    View screenView;
    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private ListAdapter mAdapter;

    // TODO: Rename and change types of parameters
    public static ListEventsFragment newInstance(String param1, String param2) {
        ListEventsFragment fragment = new ListEventsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ListEventsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        getEventData();

       // mAdapter = new ArrayAdapter<DummyContent.DummyItem>(getActivity(),
            //    android.R.layout.simple_list_item_1, android.R.id.text1, DummyContent.ITEMS);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        screenView = inflater.inflate(R.layout.fragment_list_events_grid, container, false);

        // Set the adapter
        mListView = (AbsListView) screenView.findViewById(android.R.id.list);
        ((AdapterView<ListAdapter>) mListView).setAdapter(mAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        return screenView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
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
        mListener = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            mListener.onFragmentInteraction(DummyContent.ITEMS.get(position).id);
            mListener.onFragmentInteraction(titleArray.get(position));

            goToEventDetails(idArray.get(position));
        }
    }

    public void goToEventDetails(String objectId){

        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("eventId", objectId);
        editor.commit();

        Fragment fragment = new EventDetailFragment();

        if (fragment != null) {
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.frame_container, fragment).commit();

        } else {
            //Error in creating fragment
            Log.e("MainActivity", "Error in creating fragment");
        }
    }
    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mListView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(String id);
    }

    public void getEventData(){
        titleArray = new ArrayList<String>();
        idArray = new ArrayList<String>();

        //Creating ParseGeoPoint with user's current location
        LocationManager locationManager =  (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        Location userLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);;
        ParseGeoPoint point = new ParseGeoPoint(userLocation.getLatitude(), userLocation.getLongitude());
        ParseObject user = new ParseObject("User");
        user.put("location", point);

        //Preparing query
        ParseGeoPoint queryParameter = (ParseGeoPoint) user.get("location");
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Event");
        query.whereWithinKilometers("geoPoint", queryParameter, 1000000);

        Log.d("Main", "Waiting for Callback...");

        //Executing query
        List<ParseObject> list = null;
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> eventList, ParseException e) {
                Log.d("Main", "Received "+eventList.size()+" events");
                if (e == null) {
                    for (int i=0; i < eventList.size(); i++){
                        ParseObject event = eventList.get(i);
                        titleArray.add(event.getString("title"));
                        idArray.add(event.getObjectId());
                        Log.d("Main", "title: " + event.getString("title"));
                    }
                    mAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1,android.R.id.text1,titleArray);
                    mListView.setAdapter(mAdapter);
                }else {
                    Log.d("Main", "Exception: "+e);
                }
            }
        });
    }


}
