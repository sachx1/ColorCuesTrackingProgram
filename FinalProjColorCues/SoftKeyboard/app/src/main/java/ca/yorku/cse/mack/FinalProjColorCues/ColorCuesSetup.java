package ca.yorku.cse.mack.FinalProjColorCues;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * ColorCuesSetup - a class that implements a setup dialog for experimental applications on Android. <p>
 *
 * @author Scott MacKenzie, 2014-2016
 */
@SuppressWarnings("unused")
public class ColorCuesSetup extends Activity implements TextWatcher
{
    final static String MYDEBUG = "MYDEBUG"; // for Log.i messages
    final static String TITLE = "ColorCuesSetup";

    /*
     * The following arrays are used to fill the spinners in the set up dialog. The first entries will be replaced by
     * corresponding values in the app's shared preferences, if any exist. In order for a value to exit as a shared
     * preference, the app must have been run at least once with the "Save" button tapped.
     */
    String[] participantCode = {"P01", "P02", "P03", "P04", "P05", "P06", "P07", "P08", "P09"};
    String[] sessionCode = {"S01", "S02", "S03", "S04", "S05", "S06", "S07", "S08", "S09"};
    String[] blockCode = {"(auto)"};
    String[] keyboardLayout = {"Qwerty", "Color Cue"};
    //String[] numberOfPhrases = {"5", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
    //String[] phrasesFileArray = {"phrases2", "phrases2", "quickbrownfox", "phrases100", "alphabet"};


    SharedPreferences sp;
    SharedPreferences.Editor spe;
    Button ok, save, exit;
    //Vibrator vib;
    private Spinner spinParticipantCode;
    private Spinner spinSessionCode, spinGroupCode, spinKeyboardLayout;
    //private Spinner spinNumberOfPhrases, spinPhrasesFile;

    // end set up parameters
    private CheckBox checkShowPresented;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.setup);

        // get a reference to a SharedPreferences object (used to store, retrieve, and save setup parameters)
        sp = this.getPreferences(MODE_PRIVATE);

        // overwrite 1st entry from shared preferences, if corresponding value exits
        participantCode[0] = sp.getString("participantCode", participantCode[0]);
        sessionCode[0] = sp.getString("sessionCode", sessionCode[0]);
        // block code initialized in main activity (based on existing filenames)
        keyboardLayout[0] = sp.getString("keyboardLayout", keyboardLayout[0]);

        // get references to widgets in setup dialog
        spinParticipantCode = (Spinner)findViewById(R.id.spinParticipantCode);
        spinSessionCode = (Spinner)findViewById(R.id.spinSessionCode);
        spinKeyboardLayout = (Spinner)findViewById(R.id.keyboardLayout);
        Spinner spinBlockCode = (Spinner)findViewById(R.id.spinBlockCode);
        //checkShowPresented = (CheckBox)findViewById(R.id.showPresentedText);

        // get references to OK, SAVE, and EXIT buttons
        ok = (Button)findViewById(R.id.ok);
        save = (Button)findViewById(R.id.save);
        exit = (Button)findViewById(R.id.exit);

        // initialise spinner adapters
        ArrayAdapter<CharSequence> adapterPC = new ArrayAdapter<CharSequence>(this, R.layout.spinnerstyle,
                participantCode);
        spinParticipantCode.setAdapter(adapterPC);

        ArrayAdapter<CharSequence> adapterSC = new ArrayAdapter<CharSequence>(this, R.layout.spinnerstyle,
                sessionCode);
        spinSessionCode.setAdapter(adapterSC);

        ArrayAdapter<CharSequence> adapterBC = new ArrayAdapter<CharSequence>(this, R.layout.spinnerstyle,
                blockCode);
        spinBlockCode.setAdapter(adapterBC);

        ArrayAdapter<CharSequence> adapterKL = new ArrayAdapter<CharSequence>(this, R.layout.spinnerstyle,
                keyboardLayout);
        spinKeyboardLayout.setAdapter(adapterKL);


        // prevent soft keyboard from popping up when activity launches
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

    }

    public void onClick(View v)
    {
        if (v == ok)
        {

            // get user's choices
            String part = participantCode[spinParticipantCode.getSelectedItemPosition()];
            String sess = sessionCode[spinSessionCode.getSelectedItemPosition()];
            //String block = blockCode[spinBlock.getSelectedItemPosition()];
            String keyLayout = keyboardLayout[spinKeyboardLayout.getSelectedItemPosition()];
            //boolean showpre = checkShowPresented.isChecked();

            // package the user's choices in a bundle
            Bundle b = new Bundle();
            b.putString("participantCode", part);
            b.putString("sessionCode", sess);
            // b.putString("blockCode", block);
            b.putString("keyboardLayout", keyLayout);
            //b.putBoolean("showPresented", showpre);

            // start experiment activity (sending the bundle with the user's choices)
            Intent i = new Intent(getApplicationContext(), ColorCuesNewActivity.class);
            i.putExtras(b);
            startActivity(i);
            //finish();

        } else if (v == save)
        {

            spe = sp.edit();
            spe.putString("participantCode", participantCode[spinParticipantCode.getSelectedItemPosition()]);
            spe.putString("sessionCode", sessionCode[spinSessionCode.getSelectedItemPosition()]);
            spe.putString("keyboardLayout", keyboardLayout[spinKeyboardLayout.getSelectedItemPosition()]);
            //spe.putBoolean("showPresented", checkShowPresented.isChecked());
            spe.apply();
            Toast.makeText(this, "Preferences saved!", Toast.LENGTH_SHORT).show();

        } else if (v == exit)
        {
            this.finish(); // terminate
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count)
    {

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after)
    {
    }

    @Override
    public void afterTextChanged(Editable s)
    {
    }

    // returns true if the passed string is parsable to float, false otherwise
    private boolean isFloat(String floatString)
    {
        try
        {
            Float.parseFloat(floatString);
        } catch (NumberFormatException e)
        {
            return false;
        }
        return true;
    }
}
