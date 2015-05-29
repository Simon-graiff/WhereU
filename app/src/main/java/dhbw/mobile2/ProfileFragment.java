package dhbw.mobile2;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.GetCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import static dhbw.mobile2.R.*;

public class ProfileFragment extends Fragment implements View.OnClickListener {

    private String userID;
    private String username;
    private String gender;
    private String aboutMe;
    private ParseFile profilepictureFile;
    private Bitmap profilepictureFileBitmap;
    private View rootView = null;
    private ProgressDialog progressDialog;


    public static ProfileFragment newInstance(String UserID) {
        ProfileFragment f = new ProfileFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putString("UserID", UserID);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onPause() {
        EditText aboutMeTextField = (EditText) rootView.findViewById(id.editText_about);
        String newAboutMe = aboutMeTextField.getText().toString();

        if(!aboutMe.equals(newAboutMe)){
            ParseUser.getCurrentUser().put("aboutMe", newAboutMe);
            ParseUser.getCurrentUser().saveInBackground();
            Toast.makeText(getActivity().getWindow().getContext(), "Your changes are saved!", Toast.LENGTH_LONG).show();
        }

        super.onPause();
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        progressDialog = ProgressDialog.show(getActivity(), "Loading", "Please wait.."); //Show loading dialog until data has been pulled from parse
        rootView = inflater.inflate(layout.fragment_profile, container, false);
        userID= getArguments().getString("UserID");


        //Add OnClickListener to the Profile data Change Entry
        rootView.findViewById(id.imageView_change).setOnClickListener(this);
        rootView.findViewById(id.textView_change).setOnClickListener(this);
        rootView.findViewById(id.textView_reloadFacebookData).setOnClickListener(this);



        if(userID.equals(ParseUser.getCurrentUser().getObjectId())){
            username = ParseUser.getCurrentUser().getUsername();
            gender= ParseUser.getCurrentUser().getString("gender");
            aboutMe =ParseUser.getCurrentUser().getString("aboutMe");
            profilepictureFile = ParseUser.getCurrentUser().getParseFile("profilepicture");
            profilepictureFileBitmap = getRoundedCornerBitmap();
            updateLayout();
        }else{
            pullUserDataFromParse();
        }


        return rootView;
    }

    private void updateLayout() {

        //Update Profile Picutre
        ImageView imageView = (ImageView) rootView.findViewById(id.imageView_ProfilePicuture);
        // Get the Pixesl of the Screen to Scale the ProfilePicture according to the Screen Size
        int heightPixels = getActivity().getApplicationContext().getResources().getDisplayMetrics().heightPixels;
        //Set the ProfilePicuture to the ImageView and scale it to the screen size
        imageView.setImageBitmap(Bitmap.createScaledBitmap(profilepictureFileBitmap, ((int) (heightPixels * 0.2)), ((int) (heightPixels * 0.2)), false));


        //Set user information
        getActivity().setTitle(username); //Change ActionBar title to the username's title
        TextView usernameView = (TextView) rootView.findViewById(id.editText_username);
        usernameView.setText(username);

        TextView aboutMeView = (TextView) rootView.findViewById(id.editText_about);
        aboutMeView.setText(aboutMe);

        //Only show about me if any text is entered
            if (!aboutMe.equals("")){
                rootView.findViewById(id.editText_about).setVisibility(View.VISIBLE);
            }




        if(gender.equalsIgnoreCase("male")){
            //Disable female Button
            ImageButton femaleButton =(ImageButton) rootView.findViewById(id.imageButton_female);
            femaleButton.setImageResource(drawable.ic_female_disabled);
        } else if (gender.equalsIgnoreCase("female")){
            //Dissable male BUtton
            ImageButton maleButton =(ImageButton) rootView.findViewById(id.imageButton_male);
            maleButton.setImageResource(drawable.ic_male_disabled);
        }

        //Adjust ProfileLayout if called user profile is the own profle
        if(userID.equals(ParseUser.getCurrentUser().getObjectId())){
            //Show the Change Profile data Fields
            rootView.findViewById(id.layout_change).setVisibility(View.VISIBLE);
        }



        //Update Done! Close Loading Dialog
        progressDialog.dismiss();
    }

    private void pullUserDataFromParse(){
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo("objectId", userID);
        query.getFirstInBackground(new GetCallback<ParseUser>() {
            @Override
            public void done(ParseUser parseUser, ParseException e) {

                if (e == null) {
                    username = parseUser.getUsername();
                    gender = parseUser.getString("gender");
                    aboutMe =parseUser.getString("aboutMe");
                    profilepictureFile = parseUser.getParseFile("profilepicture");
                    profilepictureFileBitmap = getRoundedCornerBitmap(); //Transfer ParseFile to Bitmap
                    updateLayout();
                } else {
                    Log.e("ParseUser", "Cannot retrive User with UserID= " + userID + " from Parse");
                }

            }
        });

    }




    /**
     * This Function returns a Bitmap that thats cut to a circle
     * @return
     */

    private Bitmap getRoundedCornerBitmap() {
        byte [] data = new byte[0];
        try {
            data = profilepictureFile.getData();
        } catch (ParseException e) {
            e.printStackTrace();
        }


        Bitmap bitmap= BitmapFactory.decodeByteArray(data, 0, data.length);
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = 100;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;

    }


    public void changeProfile(View view){
        Log.d("Profile", "Go to changes");
    }


    @Override
    public void onClick(View v) {

        if(v.getId()!= id.textView_reloadFacebookData) {
            //Hide Change Buttons
            rootView.findViewById(id.layout_change).setVisibility(View.GONE);
            rootView.findViewById(id.editText_about).setVisibility(View.VISIBLE);
            rootView.findViewById(id.editText_about).setFocusableInTouchMode(true);
            rootView.findViewById(id.editText_about).setFocusable(true);
            rootView.findViewById(id.textView_reloadFacebookData).setVisibility(View.VISIBLE);

        }
        else{
            new LogInActivity().retriveFacebookData();
            Toast.makeText(getActivity().getWindow().getContext(), "Your profile was updated", Toast.LENGTH_LONG).show();
            //Save aboutMe before reloading Layout
            EditText aboutMeTextField = (EditText) rootView.findViewById(id.editText_about);
            String aboutMe = aboutMeTextField.getText().toString();
            if(aboutMe!=null){
                ParseUser.getCurrentUser().put("aboutMe", aboutMe);
                ParseUser.getCurrentUser().saveInBackground();
            }
            FragmentManager fragmentManager = getFragmentManager();
            Fragment fragment = ProfileFragment.newInstance(ParseUser.getCurrentUser().getObjectId());
            fragmentManager.beginTransaction().replace(R.id.frame_container, fragment).commit();
              }

    }
}
