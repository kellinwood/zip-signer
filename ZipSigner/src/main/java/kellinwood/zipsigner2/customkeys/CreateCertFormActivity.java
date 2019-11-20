package kellinwood.zipsigner2.customkeys;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import kellinwood.logging.Logger;
import kellinwood.logging.android.AndroidLogManager;
import kellinwood.logging.android.AndroidLogger;
import kellinwood.security.zipsigner.optional.CertCreator;
import kellinwood.security.zipsigner.optional.DistinguishedNameValues;
import kellinwood.security.zipsigner.optional.PasswordObfuscator;
import kellinwood.zipsigner2.AlertDialogUtil;
import kellinwood.zipsigner2.R;

public class CreateCertFormActivity extends Activity {

    AndroidLogger logger = null;

    Params params = new Params();
    ProgressDialog creatingKeyProgressDialog = null;

    private static final String MSG_DATA_MESSAGE = "message";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        logger = AndroidLogManager.getAndroidLogger(CreateCertFormActivity.class);
        // enable toasts for info level logging.  toasts are default for error and warnings.
        logger.setToastContext(getBaseContext());
        logger.setInfoToastEnabled(true);

        setContentView(R.layout.create_cert_form);

        Spinner spinner = (Spinner) findViewById(R.id.CertSignatureAlgorithm);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
            R.array.AllShaWithRsaAlgorithmsArray, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0);


        Button cancelButton = (Button)findViewById(R.id.CancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent( CreateCertFormActivity.this, ManageKeysActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // go all the way back to ManageKeysActivity
                startActivity(i);
            }
        });


        Button finishButton = (Button)findViewById(R.id.FinishButton);
        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createKeystoreAndKey();
            }
        });
    }

    private void createKeystoreAndKey() {

        params = new Params();

        String validityYearsStr = ((EditText)findViewById(R.id.CertValidityYears)).getText().toString();
        params.certValidityYears = 30;
        try {
            params.certValidityYears = Integer.parseInt(validityYearsStr);
        } catch (Exception x) {
            AlertDialogUtil.alertDialog(CreateCertFormActivity.this, R.string.InvalidValidity, R.string.InvalidValidityMessage, R.id.OkButton);
            return;
        }

        if (params.certValidityYears <= 0) {
            AlertDialogUtil.alertDialog(CreateCertFormActivity.this, R.string.InvalidValidity, R.string.InvalidValidityMessage, R.id.OkButton);
            return;
        }

        Spinner certAlgSpinner = (Spinner) findViewById(R.id.CertSignatureAlgorithm);
        params.certSignatureAlgorithm =  (String)certAlgSpinner.getSelectedItem();

        params.distinguishedNameValues.setCountry(((EditText)findViewById(R.id.Country)).getText().toString());
        params.distinguishedNameValues.setState(((EditText)findViewById(R.id.State)).getText().toString());
        params.distinguishedNameValues.setLocality(((EditText)findViewById(R.id.Locality)).getText().toString());
        params.distinguishedNameValues.setStreet(((EditText)findViewById(R.id.Street)).getText().toString());
        params.distinguishedNameValues.setOrganization(((EditText)findViewById(R.id.Organization)).getText().toString());
        params.distinguishedNameValues.setOrganizationalUnit(((EditText)findViewById(R.id.OrganizationalUnit)).getText().toString());
        params.distinguishedNameValues.setCommonName(((EditText)findViewById(R.id.CommonName)).getText().toString());

        Bundle intentExtras = getIntent().getExtras();
        params.requestCode = intentExtras.getInt(KeyParameters.REQUEST_CODE);
        params.storePath = intentExtras.getString(KeyParameters.KEYSTORE_FILENAME);
        params.storePass = intentExtras.getString( KeyParameters.KEYSTORE_PASSWORD);
        params.keyAlgorithm = intentExtras.getString(KeyParameters.KEY_ALGORITHM);
        params.keySize = intentExtras.getInt(KeyParameters.KEY_SIZE);
        params.keyName = intentExtras.getString(KeyParameters.KEY_NAME);
        params.keyPass  = intentExtras.getString(KeyParameters.KEY_PASSWORD);

        if (params.distinguishedNameValues.size() == 0) {
            AlertDialogUtil.alertDialog(CreateCertFormActivity.this ,R.string.MissingCertInfoTitle, R.string.MissingCertInfoMessage, R.string.OkButtonLabel);
            return;
        }

        if (params.certValidityYears < 25) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage(R.string.ShortCertValidityMessage).setTitle(R.string.ShortCertValidityTitle);
            alertDialogBuilder.setPositiveButton(R.string.ContinueAnywayButtonLabel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                    doCreateKeystoreAndKey();
                }
            });
            alertDialogBuilder.setNegativeButton(R.string.CancelButtonLabel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
            return;
        }

        doCreateKeystoreAndKey();
    }

    private void doCreateKeystoreAndKey() {

        creatingKeyProgressDialog = new ProgressDialog(this);
        creatingKeyProgressDialog.setMessage(getResources().getString(R.string.CreatingKeyProgressMessage));
        creatingKeyProgressDialog.show();

        new Thread() {
            Logger logger =  AndroidLogManager.getAndroidLogger(CreateCertFormActivity.class);
            public void run() {
                char[] storePass = PasswordObfuscator.getInstance().decodeKeystorePassword( params.storePath, params.storePass);
                char[] keyPass = PasswordObfuscator.getInstance().decodeAliasPassword ( params.storePath, params.keyName, params.keyPass);
                try {

                    if (params.requestCode == ManageKeysActivity.REQUEST_CODE_CREATE_KEYSTORE) {
                        CertCreator.createKeystoreAndKey(params.storePath, storePass,
                            params.keyAlgorithm, params.keySize, params.keyName, keyPass,
                            params.certSignatureAlgorithm, params.certValidityYears, params.distinguishedNameValues);
                    } else if (params.requestCode == ManageKeysActivity.REQUEST_CODE_CREATE_KEY) {
                        CertCreator.createKey(params.storePath, storePass,
                            params.keyAlgorithm, params.keySize, params.keyName, keyPass,
                            params.certSignatureAlgorithm, params.certValidityYears, params.distinguishedNameValues);
                    }

                    sendMessage(CreateCertFormActivity.MESSAGE_CODE_SUCCESS, getResources().getString( R.string.SuccessMessage));

                } catch (Exception x) {
                    logger.error( x.getMessage(), x);
                    sendMessage(CreateCertFormActivity.MESSAGE_CODE_FAILURE, x.getMessage());
                } finally {
                    PasswordObfuscator.flush(storePass);
                    PasswordObfuscator.flush(keyPass);
                }
            }

            void sendMessage( int msgCode, String message) {
                Message msg = new Message();
                msg.what = msgCode;
                Bundle data = new Bundle();
                data.putString( CreateCertFormActivity.MSG_DATA_MESSAGE, message);
                msg.setData(data);
                handler.sendMessage(msg);
            }

        }.start();
    }


    private static final int MESSAGE_CODE_SUCCESS = 1;
    private static final int MESSAGE_CODE_FAILURE = 2;

    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CreateCertFormActivity.MESSAGE_CODE_SUCCESS:
                    creatingKeyProgressDialog.dismiss();
                    logger.info( msg.getData().getString(CreateCertFormActivity.MSG_DATA_MESSAGE));
                    // back to MyKeys activity
                    Intent i = new Intent( CreateCertFormActivity.this, ManageKeysActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    i.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    i.putExtra(KeyParameters.REQUEST_CODE, params.requestCode);
                    i.putExtra( KeyParameters.KEYSTORE_FILENAME, params.storePath);
                    i.putExtra( KeyParameters.KEYSTORE_PASSWORD, params.storePass);
                    i.putExtra( KeyParameters.KEY_NAME, params.keyName);
                    startActivity(i);
                    break;
                case CreateCertFormActivity.MESSAGE_CODE_FAILURE:
                    creatingKeyProgressDialog.dismiss();
                    logger.error(msg.getData().getString(CreateCertFormActivity.MSG_DATA_MESSAGE));
                    break;
            }
        }
    };



    class Params {
        int requestCode;
        String storePath;
        String storePass;
        String keyName;
        String keyAlgorithm;
        int keySize;
        String keyPass;
        int certValidityYears;
        String certSignatureAlgorithm;
        DistinguishedNameValues distinguishedNameValues = new DistinguishedNameValues();


    }
}