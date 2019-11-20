package kellinwood.zipsigner2.customkeys;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import kellinwood.logging.android.AndroidLogManager;
import kellinwood.logging.android.AndroidLogger;
import kellinwood.security.zipsigner.optional.KeyStoreFileManager;
import kellinwood.security.zipsigner.optional.PasswordObfuscator;
import kellinwood.zipsigner2.AlertDialogUtil;
import kellinwood.zipsigner2.R;

public class CreateKeyFormActivity extends Activity {

    AndroidLogger logger = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        logger = AndroidLogManager.getAndroidLogger(CreateKeyFormActivity.class);
        // enable toasts for info level logging.  toasts are default for error and warnings.
        logger.setToastContext(getBaseContext());
        logger.setInfoToastEnabled(true);

        setContentView(R.layout.create_key_form);

        if (getIntent().getStringExtra(KeyParameters.KEYSTORE_PASSWORD) != null)
        {
            TextView keystorePasswordLabel = (TextView)findViewById(R.id.KeystorePasswordLabel);
            keystorePasswordLabel.setVisibility(View.GONE);
            EditText keystorePasswordEditor = (EditText)findViewById(R.id.KeystorePassword);
            keystorePasswordEditor.setVisibility(View.GONE);
        }

        String newKeyName = getIntent().getStringExtra(KeyParameters.KEY_NAME);
        if (newKeyName != null) {
            EditText keyNameEditor = (EditText)findViewById(R.id.KeyName);
            keyNameEditor.setText(newKeyName);
        }

        Spinner spinner = (Spinner) findViewById(R.id.KeySize);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
            R.array.KeySizeArray, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(1);

        spinner = (Spinner) findViewById(R.id.KeyAlgorithm);
        adapter = ArrayAdapter.createFromResource(this,
            R.array.KeyAlgorithmArray, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        Button cancelButton = (Button)findViewById(R.id.CancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent( CreateKeyFormActivity.this, ManageKeysActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // go all the way back to ManageKeysActivity
                startActivity(i);
            }
        });


        Button continueButton = (Button)findViewById(R.id.ContinueButton);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // check passwords

                EditText keyNameEditor = (EditText)findViewById(R.id.KeyName);
                EditText passwordEditor = (EditText)findViewById(R.id.KeyPassword);
                EditText verifyEditor = (EditText)findViewById(R.id.VerifyPassword);

                if (!passwordEditor.getText().toString().equals( verifyEditor.getText().toString())) {
                    AlertDialogUtil.alertDialog(CreateKeyFormActivity.this, R.string.PasswordsDontMatch, 0, R.string.OkButtonLabel);
                    return;
                }
                if (passwordEditor.getText().toString().length() == 0) {
                    AlertDialogUtil.alertDialog(CreateKeyFormActivity.this, R.string.PasswordRequired, R.string.KeyPasswordRequired, R.string.OkButtonLabel);
                    return;
                }
                if (keyNameEditor.getText().toString().length() == 0) {
                    AlertDialogUtil.alertDialog(CreateKeyFormActivity.this,R.string.KeyNameRequiredTitle, R.string.KeyNameRequired, R.string.OkButtonLabel);
                    return;
                }

                String keystorePath = getIntent().getExtras().getString(KeyParameters.KEYSTORE_FILENAME);
                String keyName = keyNameEditor.getText().toString().trim();
                String storePass = null;

                if (getIntent().getExtras().getInt( KeyParameters.REQUEST_CODE) == ManageKeysActivity.REQUEST_CODE_CREATE_KEY)
                {
                    if (getIntent().getStringExtra(KeyParameters.KEYSTORE_PASSWORD) == null) {
                        EditText keystorePasswordEditor = (EditText)findViewById(R.id.KeystorePassword);
                        if (keystorePasswordEditor.getText().length() == 0) {
                            AlertDialogUtil.alertDialog(CreateKeyFormActivity.this, R.string.PasswordRequired, R.string.KeystorePasswordRequired, R.string.OkButtonLabel);
                            return;
                        }

                        storePass = PasswordObfuscator.getInstance().encodeKeystorePassword( keystorePath, keystorePasswordEditor.getText().toString());

                        try {
                            KeyStoreFileManager.validateKeystorePassword(keystorePath, storePass);
                        } catch (Exception x) {
                            AlertDialogUtil.alertDialog(CreateKeyFormActivity.this, R.string.WrongKeystorePassword, 0, R.string.OkButtonLabel);
                            return;
                        }
                    }

                    try {
                        if (KeyStoreFileManager.containsKey(keystorePath, null, keyName)) {
                            AlertDialogUtil.alertDialog(CreateKeyFormActivity.this,
                               getResources().getString( R.string.NameConflictTitle),
                               String.format(getResources().getString(R.string.NameConflictMessage), keyName));
                            return;
                        }
                    } catch (Exception x) {
                        logger.error( x.getMessage(), x);
                    }
                }

                String password = PasswordObfuscator.getInstance().encodeAliasPassword(keystorePath, keyName, passwordEditor.getText().toString());

                Spinner keySizeSpinner = (Spinner) findViewById(R.id.KeySize);
                int keySize = Integer.parseInt((String)keySizeSpinner.getSelectedItem());

                Spinner keyAlgorithmSpinner = (Spinner) findViewById(R.id.KeyAlgorithm);
                String keyAlgorithm = (String)keyAlgorithmSpinner.getSelectedItem();

                launchCreateCertFormActivity(storePass,keyNameEditor.getText().toString(), keySize, keyAlgorithm, password);
            }
        });
    }

    private void launchCreateCertFormActivity(String storePass, String keyName, int keySize, String keyAlgorithm, String keyPassword) {
        Intent i = new Intent(this, CreateCertFormActivity.class);

        i.putExtras( getIntent().getExtras());         // forward data from previous activity
        if (storePass != null) i.putExtra(KeyParameters.KEYSTORE_PASSWORD, storePass);
        i.putExtra(KeyParameters.KEY_NAME, keyName);
        i.putExtra(KeyParameters.KEY_SIZE, keySize);
        i.putExtra(KeyParameters.KEY_ALGORITHM, keyAlgorithm);
        i.putExtra(KeyParameters.KEY_PASSWORD, keyPassword);
        startActivity(i);
    }


}