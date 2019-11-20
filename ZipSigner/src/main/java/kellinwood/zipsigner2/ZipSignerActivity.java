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
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.security.UnrecoverableKeyException;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import kellinwood.logging.android.AndroidLogManager;
import kellinwood.logging.android.AndroidLogger;
import kellinwood.security.zipsigner.AutoKeyException;
import kellinwood.security.zipsigner.ProgressEvent;
import kellinwood.security.zipsigner.ProgressListener;
import kellinwood.security.zipsigner.ZipSigner;
import kellinwood.security.zipsigner.optional.CustomKeySigner;
import kellinwood.security.zipsigner.optional.PasswordObfuscator;
import kellinwood.zipsigner2.customkeys.Alias;
import kellinwood.zipsigner2.customkeys.CustomKeysDataSource;
import kellinwood.zipsigner2.customkeys.EnterPasswordDialog;
import kellinwood.zipsigner2.customkeys.Keystore;


/** Demo app for signing zip, apk, and/or jar files on an Android device. */
public class ZipSignerActivity extends Activity {


    AndroidLogger logger = null;
    ProgressBar progressBar = null;
    TextView currentItemView = null;
    SignerThread signerThread = null;

    private static final int MESSAGE_TYPE_PERCENT_DONE = 1;
    private static final int MESSAGE_TYPE_SIGNING_COMPLETE = 2;
    private static final int MESSAGE_TYPE_SIGNING_CANCELED = 3;
    private static final int MESSAGE_TYPE_SIGNING_ERROR = 4;    
    private static final int MESSAGE_TYPE_ANNOUNCE_KEY = 5;
    private static final int MESSAGE_TYPE_AUTO_KEY_FAIL = 6;
    private static final int MESSAGE_TYPE_BAD_PASSWORD = 7;
    private static final int MESSAGE_TYPE_KEY_PASSWORD = 8;

    private static final String MESSAGE_KEY = "message";

    private int AUTO_KEY_FAIL_RESULT = RESULT_FIRST_USER;

    String inputFile;
    String outputFile;
    String keyMode;
    String keyAlias;
    String keystorePath;
    String keystorePassword;
    String keyPassword;
    String signatureAlgorithm = "SHA1withRSA";
    boolean showProgressItems;
    boolean builtInKey;

    private String getStringExtra( Intent i, String extraName, String defaultValue) {

        String value = i.getStringExtra( extraName);
        if (value == null) return defaultValue;
        return value;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.zip_signer);

        logger = AndroidLogManager.getAndroidLogger(ZipSignerActivity.class);
        logger.setToastContext(getBaseContext());
        logger.setInfoToastEnabled(true);
        
        currentItemView = (TextView)findViewById(R.id.SigningZipItemTextView);

        progressBar = (ProgressBar)findViewById(R.id.SigningZipProgressBar);
        progressBar.setIndeterminate(false);
        progressBar.setMax(100);
        progressBar.setProgress(0);

        Button cancelButton = (Button)findViewById(R.id.SigningZipCancelButton);
        cancelButton.setOnClickListener( new OnClickListener() {
            public void onClick(View arg0) {
                if (signerThread != null) signerThread.cancel();
            }

        });

        Intent intent = getIntent();
        AUTO_KEY_FAIL_RESULT = intent.getExtras().getInt("autoKeyFailRC", RESULT_FIRST_USER);
        showProgressItems = Boolean.valueOf( getStringExtra(intent, "showProgressItems", "true"));
        inputFile = intent.getStringExtra("inputFile");
        outputFile = intent.getStringExtra("outputFile");

        builtInKey = false;
        keyMode = intent.getStringExtra("keyMode");
        if (keyMode == null) keyMode = "testkey"; // backwards compatible.

        for (String builtInKeyName : ZipSigner.SUPPORTED_KEY_MODES){
            if (keyMode.equals( builtInKeyName)) {
                builtInKey = true;
                break;
            }
        }

        String alg = intent.getStringExtra("signatureAlgorithm");
        if (alg != null) signatureAlgorithm = alg;

        Alias selectedAlias = null;
        if (!builtInKey) {
            long customKeyId = intent.getLongExtra("customKeyId",-1L);
            CustomKeysDataSource customKeysDataSource = new CustomKeysDataSource(ZipSignerActivity.this);
            customKeysDataSource.open();
            List<Keystore> keystoreList = customKeysDataSource.getAllKeystores();
            customKeysDataSource.close();

            boolean useIdMatch = (customKeyId >= 0);

            for (Keystore keystore : keystoreList) {
                for (Alias alias : keystore.getAliases()) {
                    if (useIdMatch) {
                        if (alias.getId() == customKeyId) {
                            selectedAlias = alias;
                        }
                    } else {
                        if (selectedAlias == null && keyMode.equals(alias.getDisplayName()))
                            selectedAlias = alias;
                    }
                }
            }
            if (selectedAlias == null) {
                // TODO: return error
                return;
            }
            keyAlias = selectedAlias.getName();

            keyPassword = selectedAlias.getPassword();
            keystorePassword = selectedAlias.getKeystore().getPassword();
            keystorePath = selectedAlias.getKeystore().getPath();
        }

        if (builtInKey || keyPassword != null) {
            signerThread = new SignerThread( handler);
            signerThread.start();
        } else {
            EnterPasswordDialog.show( this, handler, getResources().getString(R.string.EnterKeyPassword), MESSAGE_TYPE_KEY_PASSWORD,
                keystorePath, 0, keyAlias);
        }
    }

    // Define the Handler that receives messages from the thread and update the progress
    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            // int msgType = msg.getData().getInt(MESSAGE_TYPE_NAME);
            switch (msg.what) {
            case MESSAGE_TYPE_PERCENT_DONE:
                int percentDone = msg.arg1; 
                if (percentDone > 100) percentDone = 100;
                progressBar.setProgress(percentDone);
                Bundle data = msg.getData();
                if (data != null) {
                    String currentItem = data.getString( MESSAGE_KEY);
                    currentItemView.setText( currentItem);
                }
                break;
            case MESSAGE_TYPE_SIGNING_COMPLETE:
                progressBar.setProgress(100);
                setResult( RESULT_OK);
                finish();
                break;
            case MESSAGE_TYPE_SIGNING_CANCELED:
                setResult( RESULT_CANCELED);
                finish();
                break;
            case MESSAGE_TYPE_SIGNING_ERROR:
                String msgText = msg.getData().getString( MESSAGE_KEY);
                logger.error( msgText);
                Intent i = new Intent();
                i.putExtra( "errorMessage", msgText);
                setResult( RESULT_FIRST_USER, i);
                finish();
                break;
            case MESSAGE_TYPE_ANNOUNCE_KEY:
                msgText = msg.getData().getString( MESSAGE_KEY);
                logger.info(getResources().getString(R.string.SigningWithKey) + msgText);
                break;
            case MESSAGE_TYPE_AUTO_KEY_FAIL:
                msgText = msg.getData().getString( MESSAGE_KEY);
                if (AUTO_KEY_FAIL_RESULT == RESULT_FIRST_USER) logger.error( msgText);
                i = new Intent();
                i.putExtra( "errorMessage", msgText);
                setResult( AUTO_KEY_FAIL_RESULT, i);
                finish();
                break;
            case MESSAGE_TYPE_BAD_PASSWORD:
                msgText = getResources().getString(R.string.WrongKeyPassword);
                logger.error( msgText);
                EnterPasswordDialog.show( ZipSignerActivity.this, handler, getResources().getString(R.string.EnterKeyPassword), MESSAGE_TYPE_KEY_PASSWORD,
                    keystorePath, 0, keyAlias);
                break;
            case MESSAGE_TYPE_KEY_PASSWORD:
                keyPassword = msg.getData().getString(EnterPasswordDialog.MSG_DATA_PASSWORD);
                logger.debug("password: " + keyPassword);
                signerThread = new SignerThread( handler);
                signerThread.start();
                break;
            case EnterPasswordDialog.MESSAGE_CODE_ENTER_PASSWORD_CANCELLED:
                setResult( RESULT_CANCELED);
                finish();
                break;
            default:
                logger.error("Unknown message code: " + msg.what);
            }



        }
    };

    class SignerThread extends Thread implements ProgressListener, Observer
    {
        AndroidLogger logger = AndroidLogManager.getAndroidLogger(SignerThread.class);
        ZipSigner zipSigner = null;
        Handler mHandler;
        long lastProgressTime = 0;

        SignerThread(Handler h)
        {
            mHandler = h;
        }


        public void cancel() {
            if (zipSigner != null) zipSigner.cancel();
        }

        public void run()
        {
            char[] keystorePw = null;
            char[] aliasPw = null;
            try {
                if (inputFile == null) throw new IllegalArgumentException("Parameter inputFile is null");
                if (outputFile == null) throw new IllegalArgumentException("Parameter outputFile is null");

                zipSigner = new ZipSigner();
                zipSigner.addAutoKeyObserver(this);
                zipSigner.addProgressListener( this);
                zipSigner.setResourceAdapter( new ZipSignerAppResourceAdapter( getResources()));

                if (builtInKey) {
                    zipSigner.setKeymode(keyMode);
                    zipSigner.signZip( inputFile, outputFile);
                } else {

                    File keystoreFile = new File( keystorePath);

                    if (keystorePassword != null) {
                        keystorePw = PasswordObfuscator.getInstance().decodeKeystorePassword( keystorePath, keystorePassword);
                    }
                    aliasPw = PasswordObfuscator.getInstance().decodeAliasPassword(keystorePath, keyAlias, keyPassword);

                    CustomKeySigner.signZip(zipSigner, keystoreFile.getAbsolutePath(), keystorePw,
                        keyAlias, aliasPw, signatureAlgorithm, inputFile, outputFile);
                }

                if (zipSigner.isCanceled()) 
                    sendMessage( MESSAGE_TYPE_SIGNING_CANCELED, 0, null, null);
                else
                    sendMessage( MESSAGE_TYPE_SIGNING_COMPLETE, 0, null, null);

            }
            catch (AutoKeyException x) {
                sendMessage( MESSAGE_TYPE_AUTO_KEY_FAIL, 0, MESSAGE_KEY, x.getMessage());
            }
            catch (UnrecoverableKeyException x) {
                sendMessage( MESSAGE_TYPE_BAD_PASSWORD, 0, MESSAGE_KEY, x.getMessage());
            }
            catch (Throwable t) {

                logger.error( t.getMessage(), t);
                
                String tname = t.getClass().getName();
                int pos = tname.lastIndexOf('.');
                if (pos >= 0) tname = tname.substring(pos+1);

                sendMessage( MESSAGE_TYPE_SIGNING_ERROR, 0, MESSAGE_KEY, tname + ": " + t.getMessage());
            }
            finally {
                if (keystorePw != null) PasswordObfuscator.flush(keystorePw);
                if (aliasPw != null) PasswordObfuscator.flush(aliasPw);
            }
        }

        private void sendMessage( int messageType, int arg1, String str1Name, String str1Value) {
            Message msg = mHandler.obtainMessage();

            msg.what = messageType;
            msg.arg1 = arg1;
            if (str1Name != null) {
                Bundle b = new Bundle();
                b.putString(str1Name, str1Value);
                msg.setData(b);
            }
            mHandler.sendMessage(msg);
        }


        /** Called to notify the listener that progress has been made during
            the zip signing operation.
            @param event object containing progress info
         */
        public void onProgress( ProgressEvent event)
        {
            long currentTime = System.currentTimeMillis();

            // Update progress at most twice a second but always display 100%.
            if (event.getPercentDone() == 100 || event.getPriority() > ProgressEvent.PRORITY_NORMAL || (currentTime - lastProgressTime) >= 500)
            {
                if (showProgressItems)
                    sendMessage( MESSAGE_TYPE_PERCENT_DONE, event.getPercentDone(), MESSAGE_KEY, event.getMessage());
                else
                    sendMessage( MESSAGE_TYPE_PERCENT_DONE, event.getPercentDone(), null, null);

                lastProgressTime = currentTime;
            }
        }

        // Called when the key is automatically determined
        @Override
        public void update(Observable o, Object arg) {
            logger.debug("observer update: " + arg);
            sendMessage( MESSAGE_TYPE_ANNOUNCE_KEY, 0, MESSAGE_KEY, (String)arg);
            
        }
    }

}