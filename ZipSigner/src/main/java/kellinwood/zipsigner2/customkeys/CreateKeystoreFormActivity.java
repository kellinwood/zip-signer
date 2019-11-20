package kellinwood.zipsigner2.customkeys;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;

import kellinwood.logging.android.AndroidLogManager;
import kellinwood.logging.android.AndroidLogger;
import kellinwood.security.zipsigner.optional.PasswordObfuscator;
import kellinwood.zipsigner2.AlertDialogUtil;
import kellinwood.zipsigner2.R;
import kellinwood.zipsigner2.filebrowser.AndroidFileBrowser;

public class CreateKeystoreFormActivity extends Activity {

    AndroidLogger logger = null;
    String extStorageDir = "/";
    File keystoreFile = null;

    private static final String PREFERENCE_KEYSTORE_DIR = "/sdcard";

    private static final int REQUEST_CODE_KEYSTORE_DIR = 1;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        logger = AndroidLogManager.getAndroidLogger(CreateKeystoreFormActivity.class);
        // enable toasts for info level logging.  toasts are default for error and warnings.
        logger.setToastContext(getBaseContext());
        logger.setInfoToastEnabled(true);

        setContentView(R.layout.create_keystore_form);

        extStorageDir = Environment.getExternalStorageDirectory().toString();
        // Strip /mnt from /sdcard
        if (extStorageDir.startsWith("/mnt/sdcard")) extStorageDir = extStorageDir.substring(4);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String keystoreDir = prefs.getString(PREFERENCE_KEYSTORE_DIR, extStorageDir);

        keystoreFile = new File( keystoreDir, "keystore.jks");
        int fileNum = 1;
        while (keystoreFile.exists()) {
            fileNum += 1;
            keystoreFile = new File( keystoreDir, "keystore-"+fileNum+".jks");
        }

        TextView keystoreFileView = (TextView)findViewById(R.id.KeystoreFilename);
        keystoreFileView.setText(keystoreFile.getAbsolutePath());

        Button cancelButton = (Button)findViewById(R.id.CancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent( CreateKeystoreFormActivity.this, ManageKeysActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // go all the way back to ManageKeysActivity
                startActivity(i);
            }
        });

        Button changeDirButton = (Button)findViewById(R.id.BrowseDirButton);
        changeDirButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try
                {
                    Intent intent = new Intent("kellinwood.zipsigner.action.BROWSE_FILE");
                    intent.putExtra(AndroidFileBrowser.DATA_KEY_START_PATH, keystoreFile.getParentFile().getParent());
                    intent.putExtra(AndroidFileBrowser.DATA_KEY_REASON, getResources().getString(R.string.SelectKeystoreDir));
                    intent.putExtra(AndroidFileBrowser.DATA_KEY_DIRECTORY_SELECT_MODE, true);
                    CreateKeystoreFormActivity.this.startActivityForResult(intent, REQUEST_CODE_KEYSTORE_DIR);
                } catch (ActivityNotFoundException e) {
                    logger.error( e.getMessage(), e);
                }
            }
        });

        Button continueButton = (Button)findViewById(R.id.ContinueButton);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // check passwords

                EditText passwordEditor = (EditText)findViewById(R.id.KeystorePassword);
                EditText verifyEditor = (EditText)findViewById(R.id.VerifyPassword);

                if (passwordEditor.getText().length() == 0) {
                    AlertDialogUtil.alertDialog(CreateKeystoreFormActivity.this, R.string.PasswordRequired, R.string.KeystorePasswordRequired, R.string.OkButtonLabel);
                    return;
                }
                if (!passwordEditor.getText().toString().equals( verifyEditor.getText().toString())) {
                    AlertDialogUtil.alertDialog(CreateKeystoreFormActivity.this, R.string.PasswordsDontMatch, 0, R.string.OkButtonLabel);
                    return;
                }

                final String password = PasswordObfuscator.getInstance().encodeKeystorePassword(keystoreFile.getAbsolutePath(), passwordEditor.getText().toString());

                // check if file exists, if so display alert with verification to overwrite
                String keystoreFile = ((EditText)findViewById(R.id.KeystoreFilename)).getText().toString().trim();
                if (keystoreFile.length() == 0) {
                    logger.error("Invalid keystore filename");
                    return;
                }
                final File ksFile = new File( keystoreFile);
                if (!ksFile.getParentFile().canWrite()) {
                    logger.error("Keystore file not writeable");
                    return;
                }
                if (ksFile.exists()) {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(CreateKeystoreFormActivity.this);
                    String message = String.format( getResources().getString(R.string.OverwriteKeystoreFile), ksFile.getAbsolutePath());
                    alertDialogBuilder.setMessage(message).setTitle(R.string.OverwriteKeystoreTitle);
                    alertDialogBuilder.setPositiveButton(R.string.OkButtonLabel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                            launchCreateKeyFormActivity(ksFile, password);
                        }
                    });
                    alertDialogBuilder.setNegativeButton(R.string.CancelButtonLabel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                } else launchCreateKeyFormActivity(ksFile, password);
            }
        });
    }

    private void launchCreateKeyFormActivity(File keystoreFile, String password) {
        Intent i = new Intent(this, CreateKeyFormActivity.class);
        i.putExtra(KeyParameters.KEYSTORE_FILENAME, keystoreFile.getAbsolutePath());
        i.putExtra(KeyParameters.KEYSTORE_PASSWORD, password);
        i.putExtra(KeyParameters.KEYSTORE_OVERWRITE, true);
        // i.putExtra( KeyParameters.REQUEST_CODE, ManageKeysActivity.REQUEST_CODE_CREATE_KEYSTORE);
        i.putExtras( getIntent().getExtras());
        startActivity(i);
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
                    case REQUEST_CODE_KEYSTORE_DIR:
                        // obtain the filename
                        uri = data == null ? null : data.getData();
                        if (uri != null) {
                            try {
                                EditText keystoreFileEditor = (EditText)findViewById(R.id.KeystoreFilename);
                                keystoreFile = new File( keystoreFileEditor.getText().toString());
                                String baseName = keystoreFile.getName();
                                keystoreFile = new File( uri.getPath(), baseName);
                                keystoreFileEditor.setText( keystoreFile.getAbsolutePath());

                                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString(PREFERENCE_KEYSTORE_DIR, keystoreFile.getParent());
                                editor.commit();

                            } catch (Exception x){
                                logger.error(R.string.FailedToUpdateKeystoreDir + " - " + x.getMessage(), x);
                            }
                        }
                        break;
                    default:
                        logger.error("onActivityResult, RESULT_OK, unknown requestCode " + requestCode);
                        break;
                }
                break;
            case RESULT_CANCELED:   // signing operation canceled
                switch (requestCode) {
                    case REQUEST_CODE_KEYSTORE_DIR:
                        // logger.info(getResources().getString(R.string.FileSigningCancelled));
                        break;
                    default:
                        logger.error("onActivityResult, RESULT_CANCELED, unknown requestCode " + requestCode);
                        break;
                }
                break;
            default:
                logger.error("onActivityResult, unknown resultCode " + resultCode + ", requestCode = " + requestCode);
        }

    }
}