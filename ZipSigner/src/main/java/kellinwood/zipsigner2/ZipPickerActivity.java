/*
 * Copyright (C) 2010 Ken Ellinwood.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kellinwood.zipsigner2;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import kellinwood.logging.android.AndroidLogManager;
import kellinwood.logging.android.AndroidLogger;
import kellinwood.security.zipsigner.ZipSigner;


/** App for signing zip, apk, and/or jar files on an Android device. 
 *  This activity allows the input/output files to be selected and shows
 *  how to invoke the ZipSignerActivity to perform the actual work.
 *  
 */
public class ZipPickerActivity extends Activity {


    protected static final int REQUEST_CODE_PICK_FILE_TO_OPEN = 1;
    protected static final int REQUEST_CODE_PICK_FILE_TO_SAVE = 2;
    protected static final int REQUEST_CODE_PICK_INOUT_FILE = 3;

    protected static final int REQUEST_CODE_SIGN_FILE = 80701;
    protected static final int REQUEST_CODE_MANAGE_KEYS = 80702;

    private static final String PREFERENCE_IN_FILE = "input_file";
    private static final String PREFERENCE_OUT_FILE = "output_file";
    private static final String PREFERENCE_KEY_INDEX = "key_index";
    private static final String PREFERENCE_ALG_INDEX = "alg_idx";

    AndroidLogger logger = null;
    KeyListSpinnerAdapter keyModeSpinnerAdapter = null;

    Spinner algorithmSpinner = null;
    ArrayAdapter algorithmSpinnerAdapter = null;
    ArrayAdapter sha1WithRsaSpinnerAdapter = null;
    ArrayAdapter allAlgorithmsSpinnerAdapter = null;

    static {
        AndroidLogManager.overrideCategory("ZipSigner");
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.zip_picker);

        logger = AndroidLogManager.getAndroidLogger(ZipPickerActivity.class);
        // enable toasts for info level logging.  toasts are default for error and warnings.
        logger.setToastContext(getBaseContext());
        logger.setInfoToastEnabled(true);

        Button createButton = (Button)findViewById(R.id.SignButton);
        createButton.setOnClickListener( new OnClickListener() {
            public void onClick( View view) {
                invokeZipSignerActivity();
            }
        });

        String extStorageDir = Environment.getExternalStorageDirectory().toString();
        // Strip /mnt from /sdcard
        if (extStorageDir.startsWith("/mnt/sdcard")) extStorageDir = extStorageDir.substring(4);
        
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String inputFile = prefs.getString(PREFERENCE_IN_FILE, extStorageDir + "/unsigned.zip");
        String outputFile = prefs.getString(PREFERENCE_OUT_FILE, extStorageDir + "/signed.zip");
        int keyIndex = prefs.getInt(PREFERENCE_KEY_INDEX, 0);

        EditText inputText = (EditText)findViewById(R.id.InFileEditText);
        inputText.setText( inputFile);

        EditText outputText = (EditText)findViewById(R.id.OutFileEditText);
        outputText.setText( outputFile);

        Button button = (Button) findViewById(R.id.OpenPickButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                pickInputFile();
            }
        });

        button = (Button) findViewById(R.id.SaveAsPickButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                pickOutputFile();
            }
        });      
        
        button = (Button) findViewById(R.id.InOutPickButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                pickInputOutputFiles();
            }
        });        
        
        Spinner spinner = (Spinner) findViewById(R.id.KeyModeSpinner);
        keyModeSpinnerAdapter = KeyListSpinnerAdapter.createInstance(this, android.R.layout.simple_spinner_item);
        keyModeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(keyModeSpinnerAdapter);
        if (keyIndex >= keyModeSpinnerAdapter.getCount()) keyIndex = 0;
        spinner.setSelection(keyIndex);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view,
                                       int position, long id) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt( PREFERENCE_KEY_INDEX, position);
                editor.commit();
                updateAlgorithmSpinner(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapter) {}
        });

        algorithmSpinner = (Spinner) findViewById(R.id.CertSignatureAlgorithm);
        sha1WithRsaSpinnerAdapter = ArrayAdapter.createFromResource(this,
            R.array.Sha1WithRsaAlgorithmArray, android.R.layout.simple_spinner_item);
        sha1WithRsaSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        allAlgorithmsSpinnerAdapter = ArrayAdapter.createFromResource(this,
            R.array.AllShaWithRsaAlgorithmsArray, android.R.layout.simple_spinner_item);
        allAlgorithmsSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        updateAlgorithmSpinner(keyIndex);


        algorithmSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view,
                                       int position, long id) {
                if (algorithmSpinnerAdapter.getCount() > 1) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt( PREFERENCE_ALG_INDEX, position);
                    editor.commit();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapter) {}
        });
    }

    private void updateAlgorithmSpinner(int keyIndex) {
        int selection = 0;
        ArrayAdapter newAdapter = null;
        if (keyIndex < ZipSigner.SUPPORTED_KEY_MODES.length) {
            if (algorithmSpinnerAdapter != sha1WithRsaSpinnerAdapter) {
                newAdapter = sha1WithRsaSpinnerAdapter;
                selection = 0;
                TextView tv = (TextView)findViewById(R.id.SignatureAlgorithmTextView);
                tv.setVisibility(View.INVISIBLE);
                algorithmSpinner.setVisibility(View.INVISIBLE);
            }
        } else {
            if (algorithmSpinnerAdapter != allAlgorithmsSpinnerAdapter) {
                newAdapter = allAlgorithmsSpinnerAdapter;
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                selection = prefs.getInt(PREFERENCE_ALG_INDEX, 0);
                TextView tv = (TextView)findViewById(R.id.SignatureAlgorithmTextView);
                tv.setVisibility(View.VISIBLE);
                algorithmSpinner.setVisibility(View.VISIBLE);
            }
        }
        if (newAdapter != null) {
            algorithmSpinnerAdapter = newAdapter;
            algorithmSpinner.setAdapter(algorithmSpinnerAdapter);
            algorithmSpinner.setSelection(selection);
        }
    }

    private String getInputFilename() {
        return ((EditText)findViewById(R.id.InFileEditText)).getText().toString();
    }

    private String getOutputFilename() {
        return ((EditText)findViewById(R.id.OutFileEditText)).getText().toString();
    }

    private void invokeZipSignerActivity() {
        try {

            String inputFile = getInputFilename();
            String outputFile = getOutputFilename();

            // Save the input,output file names to preferences
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PREFERENCE_IN_FILE, inputFile);
            editor.putString(PREFERENCE_OUT_FILE, outputFile);            
            editor.commit();            

            // Refuse to do anything if the external storage device is not writable (external storage = /sdcard).
            if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState())) {
                logger.error( getResources().getString(R.string.ExtStorageIsReadOnly));
                return;
            }

            // Launch the ZipSignerActivity to perform the signature operation.
            Intent i = new Intent("kellinwood.zipsigner.action.SIGN_FILE");

            // Required parameters - input and output files.  The filenames must be different (e.g., 
            // you can't sign the file and save the output to itself).
            i.putExtra("inputFile", inputFile);
            i.putExtra("outputFile", outputFile);

            // Optional parameters...

            // keyMode defaults to "testkey" if not specified
            KeyEntry keyEntry = (KeyEntry)((Spinner)this.findViewById(R.id.KeyModeSpinner)).getSelectedItem();
            logger.debug(keyEntry.getDisplayName() + ", id="+keyEntry.getId());
            i.putExtra("keyMode", keyEntry.getDisplayName());

            // If "showProgressItems" is true, then the ZipSignerActivity displays the names of files in the
            // zip as they are generated/copied during the signature process.
            i.putExtra("showProgressItems", "true");

            // Set the result code used to indicate that auto-key selection failed.  This will default to
            // RESULT_FIRST_USER if not set (same code used to signal an error).
            i.putExtra("autoKeyFailRC", RESULT_FIRST_USER+1);

            // Defaults to "SHA1withRSA" if not specified, and is ignored for the built-in keys
            String signatureAlgorithm = (String)((Spinner)this.findViewById(R.id.CertSignatureAlgorithm)).getSelectedItem();
            i.putExtra("signatureAlgorithm", signatureAlgorithm);

            // If two non-builtin keys have the same name, using the ID ensures that we sign with the one selected.
            i.putExtra("customKeyId", keyEntry.getId());

            // Activity is started and the result is returned via a call to onActivityResult(), below.
            startActivityForResult(i, REQUEST_CODE_SIGN_FILE);

        }
        catch (Throwable x) {
            logger.error( x.getClass().getName() + ": " + x.getMessage(), x);
        }

    }


    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    /* Handles item selections */
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        switch (item.getItemId()) {
        case R.id.MenuItemShowHelp:
            String targetURL = getString(R.string.AboutZipSignerDocUrl);
            Intent wsi = new Intent( Intent.ACTION_VIEW, Uri.parse(targetURL));
            startActivity(wsi);
            return true;
        case R.id.MenuItemManageKeys:
            // Launch the ZipSignerActivity to perform the signature operation.
            Intent mki = new Intent("kellinwood.zipsigner.action.MANAGE_KEYS");
            // Activity is started and the result is returned via a call to onActivityResult(), below.
            startActivityForResult(mki, REQUEST_CODE_MANAGE_KEYS);            
            return true;
        case R.id.MenuItemAbout:
            AboutDialog.show(this);
            return true;
        }
        return false;
    }



    /**
     * Receives the result of other activities started with startActivityForResult(...)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Uri uri;

        switch (resultCode)
        {
        case RESULT_OK:

            switch (requestCode) {
            case REQUEST_CODE_PICK_FILE_TO_OPEN:
                // obtain the filename
                uri = data == null ? null : data.getData();
                if (uri != null) {
                    ((EditText)findViewById(R.id.InFileEditText)).setText(uri.getPath());
                }				
                break;
            case REQUEST_CODE_PICK_FILE_TO_SAVE:
                // obtain the filename
                uri = data == null ? null : data.getData();
                if (uri != null) {
                    ((EditText)findViewById(R.id.OutFileEditText)).setText(uri.getPath());
                }				
                break;
            case REQUEST_CODE_PICK_INOUT_FILE:
                // obtain the filename
                uri = data == null ? null : data.getData();
                if (uri != null) {
                    String filename = uri.getPath();
                    ((EditText)findViewById(R.id.InFileEditText)).setText(filename);
                    // auto set output file ... "input.zip" becomes "input-signed.zip"
                    int pos = filename.lastIndexOf('.');
                    if (pos > 0) {
                        filename = filename.substring(0, pos) + "-signed" + filename.substring(pos); 
                    }
                    ((EditText)findViewById(R.id.OutFileEditText)).setText(filename);
                }               
                break;                
            case REQUEST_CODE_SIGN_FILE:
                logger.info(getResources().getString( R.string.FileSigningSuccess));
                break;
            case REQUEST_CODE_MANAGE_KEYS:
                keyModeSpinnerAdapter.changeData();
                break;
            default:
                logger.error("onActivityResult, RESULT_OK, unknown requestCode " + requestCode);
                break;
            }
            break;
        case RESULT_CANCELED:   // signing operation canceled
            switch (requestCode) {
            case REQUEST_CODE_SIGN_FILE:
                logger.info(getResources().getString(R.string.FileSigningCancelled));
                break;
            case REQUEST_CODE_PICK_FILE_TO_OPEN:
                break;
            case REQUEST_CODE_PICK_FILE_TO_SAVE:
                break;
            case REQUEST_CODE_PICK_INOUT_FILE:
                break;
            case REQUEST_CODE_MANAGE_KEYS:
                keyModeSpinnerAdapter.changeData();
                break;
            default:
                logger.error("onActivityResult, RESULT_CANCELED, unknown requestCode " + requestCode);
                break;
            }
            break;
        case RESULT_FIRST_USER: // error during signing operation
            switch (requestCode) {
            case REQUEST_CODE_SIGN_FILE:
                // ZipSignerActivity displays a toast upon exiting with an error, so we probably don't need to do this.
                String errorMessage = data.getStringExtra("errorMessage");
                logger.debug("Error during file signing: " + errorMessage);
                break;
            default:
                logger.error("onActivityResult, RESULT_FIRST_USER, unknown requestCode " + requestCode);
                break;
            }
            break;
        case RESULT_FIRST_USER+1: // error with auto-key selection
            switch (requestCode) {
            case REQUEST_CODE_SIGN_FILE:
                // TODO display alert dialog?
                // String errorMessage = data.getStringExtra("errorMessage");
                String errorMessage = String.format( getResources().getString(R.string.KeySelectionMessage), getInputFilename());
                AlertDialogUtil.alertDialog(this, getResources().getString(R.string.KeySelectionError), errorMessage);
                break;
            default:
                logger.error("onActivityResult, RESULT_FIRST_USER+1, unknown requestCode " + requestCode);
                break;
            }
            break;            
        default:
            logger.error("onActivityResult, unknown resultCode " + resultCode + ", requestCode = " + requestCode);
        }

    }

    public static void launchFileBrowser( Activity parentActivity, String reason, int requestCode, String samplePath)
    {
        try
        {
            String startPath = "/";
            String inf = samplePath;
            if (inf != null && inf.length() > 0) {
                File f = new File( samplePath);
                startPath = f.getParent();
            }

            Intent intent = new Intent("kellinwood.zipsigner.action.BROWSE_FILE");
            intent.putExtra("startPath", startPath);
            intent.putExtra("reason", reason);
            parentActivity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(parentActivity, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private void pickInputFile(){
        launchFileBrowser( this, getResources().getString(R.string.BrowserSelectInput), REQUEST_CODE_PICK_FILE_TO_OPEN, getInputFilename());
    }


    private void pickOutputFile() {
        launchFileBrowser( this, getResources().getString(R.string.BrowserSelectOutput), REQUEST_CODE_PICK_FILE_TO_SAVE, getOutputFilename());
    }

    private void pickInputOutputFiles() {
        launchFileBrowser( this, getResources().getString(R.string.BrowserSelectInput), REQUEST_CODE_PICK_INOUT_FILE, getOutputFilename());
    }


}