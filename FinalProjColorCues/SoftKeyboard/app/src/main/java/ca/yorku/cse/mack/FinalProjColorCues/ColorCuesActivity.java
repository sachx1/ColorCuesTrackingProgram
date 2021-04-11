package ca.yorku.cse.mack.FinalProjColorCues;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

import ca.yorku.cse.mack.FinalProjColorCues.KeyboardPanel.OnKeystrokeListener;


public class ColorCuesActivity extends Activity implements OnKeystrokeListener
{
    private final static String MYDEBUG = "MYDEBUG"; // for Log.i messages

    private final static String APP = "SoftKeyboard";
    private final static String DATA_DIRECTORY = "/SoftKeyboardData/";
    private final static String SD2_HEADER = "App,Participant,Session,Block,Group,Condition,Layout,Scale,"
            + "Keystrokes,Characters,Time(s),Speed(wpm),ErrorRate(%),KSPC\n";

    private int numberOfPhrases;
    private boolean lowercaseOnly, showPresentedTextDuringEntry;

    private int keystrokeCount; // number of strokes in a phrase
    private int phraseCount;
    private boolean done = false;
    private boolean endOfPhrase, firstKeystrokeInPhrase;
    private StringBuilder transcribedBuffer;
    private String presentedBuffer;
    private TextView presentedText;
    private EditText transcribedText;
    private Random r = new Random();
    private ArrayList<Sample> samples;
    private long elapsedTimeForPhrase;
    private long timeStartOfPhrase;
    private String[] phrases;
    private BufferedWriter sd1, sd2;
    private File f1, f2;
    private String sd2Leader; // sd2Leader to identify conditions for data written to sd2 files.

    // compute typing speed in wpm, given text entered and time in ms
    public static float wpm(String text, long msTime)
    {
        float speed = text.length();
        speed = speed / (msTime / 1000.0f) * (60 / 5);
        return speed;
    }

    // Called when the activity is first created
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // init study parameters from shared preferences passed from setup dialog
        Bundle b = getIntent().getExtras();
        String participantCode = b.getString("participantCode");
        String sessionCode = b.getString("sessionCode");
        String keyboardLayout = b.getString("keyboardLayout");
        //numberOfPhrases = b.getInt("numberOfPhrases");
        //String phrasesFile = b.getString("phrasesFile");
        //boolean showPopupKey = b.getBoolean("showPopupKey");
        //lowercaseOnly = b.getBoolean("lowercaseOnly");
        //showPresentedTextDuringEntry = b.getBoolean("showPresented");

        //String scaleString = "Scale_" + scalingFactor;

        presentedText = (TextView)findViewById(R.id.presented);
        transcribedText = (EditText)findViewById(R.id.transcribed);

        KeyboardPanel keyboard = (KeyboardPanel)findViewById(R.id.keyboard);
        keyboard.setOnKeystrokeListener(this);
        keyboard.setVibrator((Vibrator)getSystemService(Context.VIBRATOR_SERVICE));
        //keyboard.setShowPopupKey(showPopupKey);
        //keyboard.setOffsetFromBottom(offsetFromBottom);
        //keyboard.invalidate();

        // load keyboard from resource file, based on setup choice for "Layout"
        // NOTE: String names must be the same as in the setup dialog (see setupparameters.xml)
//        if (keyboardLayout.equals("Qwerty"))
//            keyboard.loadKeyboardFromResource(R.array.qwerty, scalingFactor);
//        else if (keyboardLayout.equals("Opti"))
//            keyboard.loadKeyboardFromResource(R.array.opti, scalingFactor);
//        else if (keyboardLayout.equals("Opti II"))
//            keyboard.loadKeyboardFromResource(R.array.opti2, scalingFactor);
//        else if (keyboardLayout.equals("Fitaly"))
//            keyboard.loadKeyboardFromResource(R.array.fitaly, scalingFactor);
//        else if (keyboardLayout.equals("Lewis"))
//            keyboard.loadKeyboardFromResource(R.array.lewis, scalingFactor);
//        else if (keyboardLayout.equals("Metropolis"))
//            keyboard.loadKeyboardFromResource(R.array.metropolis, scalingFactor);

        LinearLayout keyboardContainer = (LinearLayout)findViewById(R.id.keyboardcontainer);
        keyboardContainer.setGravity(Gravity.BOTTOM);

//        // load phrases from resource file, based on setup choice for "Phrases file"
//        if (phrasesFile.equals("phrases2"))
//            phrases = getResources().getStringArray(R.array.phrases2);
//        else if (phrasesFile.equals("phrases100"))
//            phrases = getResources().getStringArray(R.array.phrases100);
//        else if (phrasesFile.equals("quickbrownfox"))
//            phrases = getResources().getStringArray(R.array.quickbrownfox);
//        else if (phrasesFile.equals("alphabet"))
//            phrases = getResources().getStringArray(R.array.alphabet);

        // get this device's default orientation
        int defaultOrientation = getDefaultDeviceOrientation();

        // force the keyboard to appear in the device's default orientation (and stay that way)
        if (defaultOrientation == Configuration.ORIENTATION_LANDSCAPE)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // use an ArrayList to store timestamp+keystroke samples
        samples = new ArrayList<Sample>();

        // ===================
        // File initialization
        // ===================

        // make a working directory (if necessary) to store data files
        File dataDirectory = new File(Environment.getExternalStorageDirectory() +
                DATA_DIRECTORY);
        if (!dataDirectory.exists() && !dataDirectory.mkdirs())
        {
            Log.e(MYDEBUG, "ERROR --> FAILED TO CREATE DIRECTORY: " + DATA_DIRECTORY);
            super.onDestroy(); // cleanup
            this.finish(); // terminate
        }

        /**
         * The following do-loop creates data files for output and a string sd2Leader to write to the sd2
         * output files.  Both the filenames and the sd2Leader are constructed by combining the setup parameters
         * so that the filenames and sd2Leader are unique and also reveal the conditions used for the block of input.
         *
         * The block code begins "B01" and is incremented on each loop iteration until an available
         * filename is found.  The goal, of course, is to ensure data files are not inadvertently overwritten.
         */
        int blockNumber = 0;
        do
        {
            ++blockNumber;
            String blockCode = String.format(Locale.CANADA, "B%02d", blockNumber);
            String baseFilename = String.format("%s-%s-%s-%s-%s-%s-%s-%s", APP, participantCode, blockCode,
                    sessionCode, keyboardLayout);
            f1 = new File(dataDirectory, baseFilename + ".sd1");
            f2 = new File(dataDirectory, baseFilename + ".sd2");

            // also make a comma-delimited leader that will begin each data line written to the sd2 file
            sd2Leader = String.format("%s,%s,%s,%s,%s,%s,%s,%s", APP, participantCode, sessionCode, blockCode, keyboardLayout);
        } while (f1.exists() || f2.exists());

        try
        {
            sd1 = new BufferedWriter(new FileWriter(f1));
            sd2 = new BufferedWriter(new FileWriter(f2));

            // output header in sd2 file
            sd2.write(SD2_HEADER, 0, SD2_HEADER.length());
            sd2.flush();

        } catch (IOException e)
        {
            Log.e(MYDEBUG, "ERROR OPENING DATA FILES! e=" + e.toString());
            super.onDestroy();
            this.finish();

        } // end file initialization

        // initialized a buffer to hold the user's input
        transcribedBuffer = new StringBuilder();

        // give focus transcribed text field so flashing I-beam appears
        transcribedText.requestFocus();

        // prevent soft keyboard from popping up
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        phraseCount = 0;
        doNewPhrase();

    } // end onCreate

    // get the default orientation of the device (affects how the tilt meter is rendered)
    public int getDefaultDeviceOrientation()
    {
        WindowManager windowManager = (WindowManager)getSystemService(WINDOW_SERVICE);
        Configuration config = getResources().getConfiguration();
        int rotation = windowManager.getDefaultDisplay().getRotation();

        if (((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) && config.orientation ==
                Configuration.ORIENTATION_LANDSCAPE)
                || ((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) && config.orientation ==
                Configuration.ORIENTATION_PORTRAIT))
            return Configuration.ORIENTATION_LANDSCAPE;
        else
            return Configuration.ORIENTATION_PORTRAIT;
    }

    // callback from keyboard when the user taps a key (process it)
    public void onKeystroke(KeyboardEvent ke)
    {
        // the follow test is now done the onClick method defined in showResultsDialog
        /*if (done)
        {
            doDone();
            return;
        }*/

        switch (ke.type)
        {
            case KeyboardEvent.TYPE_ENTER:
                // ignore ENTER if no text has been entered
                if (transcribedBuffer.length() > 0)
                    endOfPhrase = true;
                break;

            case KeyboardEvent.TYPE_BACKSPACE:
                if (transcribedBuffer.length() > 0)
                    transcribedBuffer.delete(transcribedBuffer.length() - 1, transcribedBuffer.length
                            ());
                break;

            default: // just a character
                transcribedBuffer.append((char)ke.charCode);
        }

        ++keystrokeCount;

        if (endOfPhrase)
        {
            elapsedTimeForPhrase = ke.timeStampFingerUp - timeStartOfPhrase;
            --keystrokeCount; // don't count the final (ENTER) keystroke
            doEndOfPhrase();

        } else
        {
            transcribedText.setText(transcribedBuffer);

            // move cursor to end
            transcribedText.setSelection(transcribedText.getText().length());

            if (firstKeystrokeInPhrase)
            {
                if (!showPresentedTextDuringEntry)
                    presentedText.setText("");

                timeStartOfPhrase = ke.timeStampFingerUp;
                firstKeystrokeInPhrase = false;
            }
            elapsedTimeForPhrase = ke.timeStampFingerUp - timeStartOfPhrase;
            samples.add(new Sample(elapsedTimeForPhrase, ke.raw));
        }
    }

    public void doNewPhrase()
    {
        String phrase = phrases[r.nextInt(phrases.length)];
        presentedBuffer = lowercaseOnly ? phrase.toLowerCase(Locale.US) : phrase;
        presentedText.setText(presentedBuffer);
        transcribedBuffer.setLength(0);
        transcribedText.setText(transcribedBuffer);

        keystrokeCount = 0;
        samples.clear();
        endOfPhrase = false;
        firstKeystrokeInPhrase = true;
    }

    public void doEndOfPhrase()
    {
        if (presentedBuffer == null)
            return; // "Enter" button pressed before anything entered

        String presentedPhrase = presentedBuffer.toLowerCase(Locale.getDefault()).trim();
        String transcribedPhrase = transcribedBuffer.toString().toLowerCase(Locale.getDefault())
                .trim();

        String resultsString = "Thank you!\n\n";
        resultsString += "Presented...\n   " + presentedPhrase + "\n";
        resultsString += "Transcribed...\n   " + transcribedPhrase + "\n";

        StringBuilder sd2Data = new StringBuilder(100);
        sd2Data.append(String.format("%s,", sd2Leader));

        // output number of strokes (aka keystrokes)
        sd2Data.append(String.format(Locale.CANADA, "%d,", keystrokeCount));

        // output number of characters entered
        sd2Data.append(String.format(Locale.CANADA, "%d,", transcribedPhrase.length()));

        // output time in seconds
        float d = elapsedTimeForPhrase / 1000;
        sd2Data.append(String.format(Locale.CANADA, "%.2f,", d));

        // append output time in minutes
        //d = elapsedTimeForPhrase / 1000.0f / 60.0f;

        // output speed in words per minute
        d = wpm(transcribedPhrase, elapsedTimeForPhrase);
        resultsString += String.format(Locale.CANADA, "Entry speed: %.2f wpm\n", d);
        sd2Data.append(String.format(Locale.CANADA, "%f,", d));

        // output error rate for transcribed text
        // MSD2 s1s2 = new MSD2(presentedPhrase, transcribedPhraseUncorrected);
        MSD s1s2 = new MSD(presentedPhrase, transcribedPhrase);
        d = (float)s1s2.getErrorRateNew();
        resultsString += String.format(Locale.CANADA, "Error rate: %.2f%%\n", d);
        sd2Data.append(String.format(Locale.CANADA, "%f,", d));

        // output KSPC (keystrokes per character)
        d = (float)keystrokeCount / transcribedPhrase.length();
        resultsString += String.format(Locale.CANADA, "KSPC: %.4f\n\n", d);
        resultsString += "Click OK to continue";
        sd2Data.append(String.format(Locale.CANADA, "%f\n", d)); // end of line too!

        // dump data
        StringBuilder sd1Stuff = new StringBuilder(100);
        sd1Stuff.append(String.format("%s\n", presentedPhrase));
        sd1Stuff.append(String.format("%s\n", transcribedPhrase));

        //Iterator<Sample> it = samples.iterator();
        //while (it.hasNext())
        //    sd1Stuff.append(String.format("%s\n", it.next()));

        for (Sample value : samples)
            sd1Stuff.append(String.format("%s\n", value));

        sd1Stuff.append("-----\n");

        // write to data files
        try
        {
            sd1.write(sd1Stuff.toString(), 0, sd1Stuff.length());
            sd1.flush();
            sd2.write(sd2Data.toString(), 0, sd2Data.length());
            sd2.flush();
        } catch (IOException e)
        {
            Log.e("MYDEBUG", "ERROR WRITING TO DATA FILE!\n" + e);
            super.onDestroy();
            this.finish();
        }

        // present results to user
        showResultsDialog(resultsString);

        // check if last phrase in block
        ++phraseCount;
        if (phraseCount < numberOfPhrases)
            doNewPhrase();
        else
            done = true; // will terminate on next tap (allows the user to see results from last
        // phrase)
    }

    private void showResultsDialog(String text)
    {
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.results_dialog, (ViewGroup)findViewById(R.id
                .results_layout));

        // Set text
        TextView results = (TextView)layout.findViewById(R.id.resultsArea);
        results.setText(text);

        // Initialize the dialog
        AlertDialog.Builder parameters = new AlertDialog.Builder(this);
        parameters.setView(layout).setCancelable(false).setNeutralButton("OK", new
                DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        dialog.cancel(); // close this dialog
                        if (done)
                            doDone();
                    }
                }).show();
    }

    private void doDone()
    {
        try
        {
            sd1.close();
            sd2.close();

			/*
             * Make the saved data files visible in Windows Explorer. There seems to be bug doing
			 * this with Android 4.4. I'm using the following code, instead of sendBroadcast.
			 * See...
			 * 
			 * http://code.google.com/p/android/issues/detail?id=38282
			 */
            MediaScannerConnection.scanFile(this, new String[] {f1.getAbsolutePath(), f2
                            .getAbsolutePath()}, null,
                    null);

        } catch (IOException e)
        {
            Log.e(MYDEBUG, "ERROR CLOSING DATA FILES! e=" + e);
        }

        // 24/03/2017: finish by returning to the setup dialog
        startActivity(new Intent(getApplicationContext(), ColorCuesSetup.class));
        //super.onDestroy();
        this.finish();
    }

    // -------------------------------------------------------
    // Sample - simple class to hold a timestamp and keystroke
    // -------------------------------------------------------
    private class Sample
    {
        private long time;
        private String key;

        Sample(long timeArg, String keyArg)
        {
            time = timeArg;
            key = keyArg;
        }

        public String toString()
        {
            return time + ", " + key;
        }
    }
}