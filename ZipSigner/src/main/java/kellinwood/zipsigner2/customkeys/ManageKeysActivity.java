/*
 * Copyright (C) 2012 Ken Ellinwood.
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
package kellinwood.zipsigner2.customkeys;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ListView;

import java.io.IOException;
import java.security.UnrecoverableKeyException;
import java.util.Enumeration;
import java.util.List;

import kellinwood.logging.Logger;
import kellinwood.logging.android.AndroidLogManager;
import kellinwood.logging.android.AndroidLogger;
import kellinwood.security.zipsigner.optional.KeyNameConflictException;
import kellinwood.security.zipsigner.optional.KeyStoreFileManager;
import kellinwood.security.zipsigner.optional.PasswordObfuscator;
import kellinwood.zipsigner2.R;
import kellinwood.zipsigner2.ZipPickerActivity;

/* Work with Keystore files, keys, and passwords.
 */
public class ManageKeysActivity extends Activity {

    // Codes used for inter-thread messaging
    static final int MESSAGE_CODE_LOAD_KEYSTORE_PASSWORD = 1;   
    static final int MESSAGE_CODE_BAD_KEYSTORE_PASSWORD = 2;   
    static final int MESSAGE_CODE_KEYSTORE_LOADED = 3;   
    static final int MESSAGE_CODE_KEYSTORE_LOAD_ERROR = 4;
    static final int MESSAGE_CODE_KEYSTORE_REMEMBER_PASSWORD = 5;
    static final int MESSAGE_CODE_ALIAS_REMEMBER_PASSWORD = 6;
    static final int MESSAGE_CODE_ALIAS_DISPLAY_NAME = 7;
    static final int MESSAGE_CODE_ALIAS_RENAME = 8;
    static final int MESSAGE_CODE_ALIAS_DELETE = 9;
    static final int MESSAGE_CODE_ALIAS_PROPERTIES = 10;

    // codes used for inter-activity messsaging
    protected static final int REQUEST_CODE_PICK_KEYSTORE_FILE = 1;
    protected static final int REQUEST_CODE_CREATE_KEYSTORE = 2;
    protected static final int REQUEST_CODE_CREATE_KEY = 3;


    private static final int MENU_ITEM_REMOVE = 42;    
    private static final int MENU_ITEM_KEYSTORE_REMEMBER_PASSWORD = 43;
    private static final int MENU_ITEM_KEYSTORE_FORGET_PASSWORD = 44;
    private static final int MENU_ITEM_KEYSTORE_CREATE_KEY = 45;

    private static final int MENU_ITEM_ALIAS_REMEMBER_PASSWORD = 55;
    private static final int MENU_ITEM_ALIAS_FORGET_PASSWORD = 56;
    private static final int MENU_ITEM_ALIAS_DISPLAY_NAME = 57;
    private static final int MENU_ITEM_ALIAS_RENAME = 58;
    private static final int MENU_ITEM_ALIAS_DELETE = 59;
    private static final int MENU_ITEM_ALIAS_PROPERTIES = 60;

    AndroidLogger logger = null;

    ExpandableListView keystoreListView = null;
    KeystoreExpandableListAdapter keystoreExpandableListAdapter = null;

    String extStorageDir = "/";

    CustomKeysDataSource customKeysDataSource = null;
    ProgressDialog keystoreLoadingDialog = null;

    boolean helpViewMode = false;

    private void showKeystoreView() {
        setContentView(R.layout.manage_keys);

        keystoreListView = (ExpandableListView)findViewById(R.id.KeystoreExpandableListView);
        keystoreExpandableListAdapter = new KeystoreExpandableListAdapter(this, customKeysDataSource.getAllKeystores());
        keystoreListView.setAdapter( keystoreExpandableListAdapter);
        keystoreListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        for(int i=0; i < keystoreExpandableListAdapter.getGroupCount(); i++) {
            keystoreListView.expandGroup(i);
        }

        keystoreListView.setOnCreateContextMenuListener( new OnCreateContextMenuListener()
        {
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

                ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
                int type = ExpandableListView.getPackedPositionType(info.packedPosition);
                int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
                int child = ExpandableListView.getPackedPositionChild(info.packedPosition);

                if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                    List<Keystore> keystoreList = customKeysDataSource.getAllKeystores();
                    Keystore keystore = keystoreList.get( group);
                    menu.setHeaderTitle(keystore.getPath());
                    menu.add(0, MENU_ITEM_REMOVE, 0, R.string.UnregisterMenuItemLabel);
                    if (keystore.rememberPassword())
                        menu.add( 0, MENU_ITEM_KEYSTORE_FORGET_PASSWORD, 0, R.string.ForgetPasswordMenuItemLabel);
                    else
                        menu.add( 0, MENU_ITEM_KEYSTORE_REMEMBER_PASSWORD, 0, R.string.RememberPasswordMenuItemLabel);
                    menu.add(0,  MENU_ITEM_KEYSTORE_CREATE_KEY, 0, R.string.CreateKeyMenuItemLabel);
                }
                else if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {

                    List<Keystore> keystoreList = customKeysDataSource.getAllKeystores();
                    Keystore keystore = keystoreList.get( group);
                    Alias alias = keystore.getAliases().get(child);
                    logger.debug("Adding context menu for alias " + alias.getName());
                    menu.setHeaderTitle(alias.getName());
                    if (alias.rememberPassword()) {
                        menu.add( 0, MENU_ITEM_ALIAS_FORGET_PASSWORD, 0, R.string.ForgetPasswordMenuItemLabel);
                    } else {
                        menu.add( 0, MENU_ITEM_ALIAS_REMEMBER_PASSWORD, 0, R.string.RememberPasswordMenuItemLabel);
                    }
                    menu.add( 0, MENU_ITEM_ALIAS_DISPLAY_NAME, 0, R.string.DisplayNameMenuItemLabel);
                    menu.add( 0, MENU_ITEM_ALIAS_RENAME, 0, R.string.RenameKeyMenuItemLabel);
                    menu.add( 0, MENU_ITEM_ALIAS_DELETE, 0, R.string.DeleteKeyMenuItemLabel);
                    menu.add( 0, MENU_ITEM_ALIAS_PROPERTIES, 0, R.string.PropertiesMenuItemLabel);
                }
            }
        });
    }
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        logger = AndroidLogManager.getAndroidLogger(ManageKeysActivity.class);
        // enable toasts for info level logging.  toasts are default for error and warnings.
        logger.setToastContext(getBaseContext());
        logger.setInfoToastEnabled(true);


        extStorageDir = Environment.getExternalStorageDirectory().toString();
        // Strip /mnt from /sdcard
        if (extStorageDir.startsWith("/mnt/sdcard")) extStorageDir = extStorageDir.substring(4);


        customKeysDataSource = new CustomKeysDataSource(getBaseContext());
        customKeysDataSource.open();

        if (customKeysDataSource.getAllKeystores().size() == 0) {
            helpViewMode = true;
            setContentView(R.layout.manage_keys_help);
            Button ok = (Button)findViewById(R.id.OkButton);
            ok.setVisibility(View.INVISIBLE);
        } else {
            showKeystoreView();
        }
    }

    /** Invoked via CreateCertFormActivity when the keystore/key creation process has been completed. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        customKeysDataSource.open();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            int requestCode = intent.getExtras().getInt(KeyParameters.REQUEST_CODE);
            String keystorePath = intent.getExtras().getString(KeyParameters.KEYSTORE_FILENAME);
            String keystorePassword = intent.getExtras().getString(KeyParameters.KEYSTORE_PASSWORD);
            String keyName = intent.getExtras().getString(KeyParameters.KEY_NAME);

            if (requestCode == REQUEST_CODE_CREATE_KEYSTORE)
                new KeystoreLoader( keystorePath, keystorePassword, false).start();
            else if (requestCode == REQUEST_CODE_CREATE_KEY) {
                Keystore keystore = keystoreExpandableListAdapter.lookupKeystoreByPath( keystorePath);
                Alias alias = new Alias();
                alias.setName(keyName);
                alias.setDisplayName(keyName);
                alias.setRememberPassword(false);
                alias.setSelected(true);
                customKeysDataSource.addKey( keystore.getId(), alias);
                keystoreExpandableListAdapter.dataChanged( customKeysDataSource.getAllKeystores());

            } else logger.error( String.format( "onNewIntent() - unknown request code %d", requestCode));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        customKeysDataSource.open();
    }

    @Override
    protected void onStop() {
        super.onStop();
        customKeysDataSource.close();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.manage_keys_menu, menu);
        return true;
    }

    /* Handles item selections */
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.MenuItemRegisterKeystore:
                launchSelectKeystoreFile();
                return true;
            case R.id.MenuItemCreateKeystore:
                Intent i = new Intent( ManageKeysActivity.this, CreateKeystoreIntroActivity.class);
                i.putExtra(KeyParameters.REQUEST_CODE, REQUEST_CODE_CREATE_KEYSTORE);
                startActivity(i);
                return true;
            case R.id.MenuItemKeysHelp:
                if (!helpViewMode) {
                    i = new Intent(this,ManageKeysHelpActivity.class);
                    startActivity(i);
                }
                return true;
            case R.id.MenuItemWebsite:
                String targetURL = getString(R.string.MyKeysWebPage);
                Intent wsi = new Intent( Intent.ACTION_VIEW, Uri.parse(targetURL));
                startActivity(wsi);
                return true;

        }
        return false;
    }

    public boolean onContextItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case MENU_ITEM_REMOVE:
            doExpListContextMenuOp( item, MENU_ITEM_REMOVE);
            break;
        case MENU_ITEM_KEYSTORE_REMEMBER_PASSWORD:
            doExpListContextMenuOp( item, MENU_ITEM_KEYSTORE_REMEMBER_PASSWORD);
            break;
        case MENU_ITEM_KEYSTORE_FORGET_PASSWORD:
            doExpListContextMenuOp( item, MENU_ITEM_KEYSTORE_FORGET_PASSWORD);
            break;
        case MENU_ITEM_KEYSTORE_CREATE_KEY:
            doExpListContextMenuOp( item, MENU_ITEM_KEYSTORE_CREATE_KEY);
            break;
        case MENU_ITEM_ALIAS_REMEMBER_PASSWORD:
            doExpListContextMenuOp( item, MENU_ITEM_ALIAS_REMEMBER_PASSWORD);
            break;
        case MENU_ITEM_ALIAS_FORGET_PASSWORD:
            doExpListContextMenuOp( item, MENU_ITEM_ALIAS_FORGET_PASSWORD);
            break;
        case MENU_ITEM_ALIAS_DISPLAY_NAME:
            doExpListContextMenuOp( item, MENU_ITEM_ALIAS_DISPLAY_NAME);
            break;
        case MENU_ITEM_ALIAS_RENAME:
            doExpListContextMenuOp( item, MENU_ITEM_ALIAS_RENAME);
            break;
        case MENU_ITEM_ALIAS_DELETE:
            doExpListContextMenuOp( item, MENU_ITEM_ALIAS_DELETE);
            break;
        case MENU_ITEM_ALIAS_PROPERTIES:
            doExpListContextMenuOp( item, MENU_ITEM_ALIAS_PROPERTIES);
            break;
        default:
            logger.error("Unknown context menu item ID: " + item.getItemId());
            break;
        }
        return false;
    }

    private void doExpListContextMenuOp( MenuItem item, int opcode) 
    {
        ExpandableListView.ExpandableListContextMenuInfo info = 
            (ExpandableListView.ExpandableListContextMenuInfo)item.getMenuInfo();
        int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
        int child = ExpandableListView.getPackedPositionChild(info.packedPosition);
        List<Keystore> keystoreList = customKeysDataSource.getAllKeystores();
        Keystore keystore = keystoreList.get( group);
        Alias alias;
        try {
            switch (opcode) {
            case MENU_ITEM_REMOVE:
                customKeysDataSource.deleteKeystore(keystore);
                keystoreExpandableListAdapter.dataChanged(customKeysDataSource.getAllKeystores());
                break;
            case MENU_ITEM_KEYSTORE_REMEMBER_PASSWORD:
                EnterPasswordDialog.show( 
                        ManageKeysActivity.this, handler, getResources().getString( R.string.EnterKeystorePassword),
                        MESSAGE_CODE_KEYSTORE_REMEMBER_PASSWORD, keystore.getPath(), keystore.getId(), true, null);
                break;
            case MENU_ITEM_KEYSTORE_FORGET_PASSWORD:
                keystore.setRememberPassword(false);
                keystore.setPassword(null);
                customKeysDataSource.updateKeystore( keystore);
                keystoreExpandableListAdapter.dataChanged(keystoreList);
                break;
            case MENU_ITEM_KEYSTORE_CREATE_KEY:
                String newKeyName = null;
                boolean unique = false;
                int num = 2;
                while (!unique) {
                    newKeyName = String.format("cert%d",num);
                    boolean ok = true;
                    for( Alias key: keystore.getAliases()) {
                        if (key.getName().equals(newKeyName)) ok = false;
                    }
                    if (ok) unique = true;
                    else num += 1;
                }
                Intent i = new Intent( this, CreateKeyFormActivity.class);
                i.putExtra( KeyParameters.REQUEST_CODE, REQUEST_CODE_CREATE_KEY);
                i.putExtra( KeyParameters.KEYSTORE_FILENAME, keystore.getPath());
                if (keystore.rememberPassword()) i.putExtra( KeyParameters.KEYSTORE_PASSWORD, keystore.getPassword());
                i.putExtra( KeyParameters.KEY_NAME, newKeyName);
                startActivity(i);
                break;
            case MENU_ITEM_ALIAS_REMEMBER_PASSWORD:
                alias = keystore.getAliases().get( child);
                EnterPasswordDialog.show(
                    ManageKeysActivity.this, handler, getResources().getString( R.string.EnterKeyPassword),
                    MESSAGE_CODE_ALIAS_REMEMBER_PASSWORD, keystore.getPath(), alias.getId(), true, alias.getName());
                break;
            case MENU_ITEM_ALIAS_FORGET_PASSWORD:
                alias = keystore.getAliases().get( child);
                alias.setRememberPassword(false);
                alias.setPassword(null);
                customKeysDataSource.updateAlias(alias);
                keystoreExpandableListAdapter.dataChanged(keystoreList);
                break;
            case MENU_ITEM_ALIAS_DISPLAY_NAME:
                alias = keystore.getAliases().get( child);
                EditDisplayNameDialog.show(this, handler, getResources().getString(R.string.DisplayNameMenuItemLabel),
                    MESSAGE_CODE_ALIAS_DISPLAY_NAME, alias.getId(), alias.getDisplayName());
                break;
            case MENU_ITEM_ALIAS_RENAME:
                alias = keystore.getAliases().get( child);
                RenameKeyDialog.show( this, handler, MESSAGE_CODE_ALIAS_RENAME, alias);
                break;
            case MENU_ITEM_ALIAS_DELETE:
                alias = keystore.getAliases().get( child);
                DeleteKeyDialog.show( this, handler, MESSAGE_CODE_ALIAS_DELETE, alias);
                break;
            case MENU_ITEM_ALIAS_PROPERTIES:
                alias = keystore.getAliases().get( child);
                if (!alias.rememberPassword()) {
                    EnterPasswordDialog.show(
                        ManageKeysActivity.this, handler, getResources().getString( R.string.EnterKeyPassword),
                        MESSAGE_CODE_ALIAS_PROPERTIES, keystore.getPath(), alias.getId(), alias.getName());
                } else handleShowKeyProperties( alias, null);
                break;
            }
        }
        catch (Exception x) { logger.error(x.getMessage(), x); }        
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
            case REQUEST_CODE_PICK_KEYSTORE_FILE:
                // obtain the filename
                uri = data == null ? null : data.getData();
                if (uri != null) {
                    EnterPasswordDialog.show( 
                            ManageKeysActivity.this, handler, getResources().getString( R.string.EnterKeystorePassword),
                            MESSAGE_CODE_LOAD_KEYSTORE_PASSWORD, uri.getPath(), 0, true, null);
                }                           
                break;                
            default:
                logger.error("onActivityResult, RESULT_OK, unknown requestCode " + requestCode);
                break;
            }
            break;
        case RESULT_CANCELED:   // operation canceled
            switch (requestCode) {
            case REQUEST_CODE_PICK_KEYSTORE_FILE:
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


    private void launchSelectKeystoreFile()
    {
        boolean debug = logger.isDebugEnabled();
        // Set sample path to the filename of the last theme loaded, or the output filename if there aren't any keystores loaded yet.
        // This will keep the starting directory of the file browser somewhat consistent.
        String samplePath = extStorageDir + "/dummy.txt";
        if (debug) logger.debug( String.format("Using sample path: %s", samplePath));
        ZipPickerActivity.launchFileBrowser(this, getResources().getString(R.string.BrowserSelectKeystore), REQUEST_CODE_PICK_KEYSTORE_FILE, samplePath);
    }

    


    // Define the Handler that receives messages from the threads and update the display
    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            
            
            switch (msg.what) {
            case ManageKeysActivity.MESSAGE_CODE_LOAD_KEYSTORE_PASSWORD:
                String encodedPassword = msg.getData().getString(EnterPasswordDialog.MSG_DATA_PASSWORD);
                String keystorePath = msg.getData().getString(EnterPasswordDialog.MSG_DATA_PATH);
                boolean rememberPassword = msg.getData().getBoolean( EnterPasswordDialog.MSG_DATA_REMEMBER_PASSWORD);
                keystoreLoadingDialog = new ProgressDialog(ManageKeysActivity.this);
                keystoreLoadingDialog.setMessage(getResources().getString(R.string.KeystoreLoadingMessage));
                keystoreLoadingDialog.show();
                new KeystoreLoader( keystorePath, encodedPassword, rememberPassword).start();
                break;
            case ManageKeysActivity.MESSAGE_CODE_BAD_KEYSTORE_PASSWORD:
                if (keystoreLoadingDialog != null) keystoreLoadingDialog.dismiss(); keystoreLoadingDialog = null;
                encodedPassword = msg.getData().getString(EnterPasswordDialog.MSG_DATA_PASSWORD);
                keystorePath = msg.getData().getString(EnterPasswordDialog.MSG_DATA_PATH);
                rememberPassword = msg.getData().getBoolean(EnterPasswordDialog.MSG_DATA_REMEMBER_PASSWORD);
                logger.error(getResources().getString(R.string.WrongKeystorePassword));
                EnterPasswordDialog.show(
                    ManageKeysActivity.this, handler, getResources().getString(R.string.EnterKeystorePassword),
                    MESSAGE_CODE_LOAD_KEYSTORE_PASSWORD, keystorePath, 0, rememberPassword, null);
                break;
            case ManageKeysActivity.MESSAGE_CODE_KEYSTORE_LOADED:
                if (keystoreLoadingDialog != null) keystoreLoadingDialog.dismiss(); keystoreLoadingDialog = null;
                logger.debug("Keystore loaded.");
                if (helpViewMode) showKeystoreView();
                else {
                    keystoreExpandableListAdapter.dataChanged(customKeysDataSource.getAllKeystores());
                    keystoreListView.expandGroup(keystoreExpandableListAdapter.getGroupCount()-1);
                }
                break;
            case ManageKeysActivity.MESSAGE_CODE_KEYSTORE_LOAD_ERROR:
                if (keystoreLoadingDialog != null) keystoreLoadingDialog.dismiss(); keystoreLoadingDialog = null;
                logger.error( msg.getData().getString( EnterPasswordDialog.MSG_DATA_MESSAGE));
                break;
            case ManageKeysActivity.MESSAGE_CODE_KEYSTORE_REMEMBER_PASSWORD:
                long keystoreId = msg.getData().getLong(EnterPasswordDialog.MSG_DATA_ID);
                encodedPassword = msg.getData().getString(EnterPasswordDialog.MSG_DATA_PASSWORD);
                keystorePath = msg.getData().getString(EnterPasswordDialog.MSG_DATA_PATH);
                rememberPassword = msg.getData().getBoolean( EnterPasswordDialog.MSG_DATA_REMEMBER_PASSWORD);     
                handleRememberKeystorePassword( keystoreId, keystorePath, encodedPassword, rememberPassword);
                break;
            case ManageKeysActivity.MESSAGE_CODE_ALIAS_REMEMBER_PASSWORD:
                long aliasId = msg.getData().getLong(EnterPasswordDialog.MSG_DATA_ID);
                encodedPassword = msg.getData().getString(EnterPasswordDialog.MSG_DATA_PASSWORD);
                keystorePath = msg.getData().getString(EnterPasswordDialog.MSG_DATA_PATH);
                rememberPassword = msg.getData().getBoolean( EnterPasswordDialog.MSG_DATA_REMEMBER_PASSWORD);
                handleRememberAliasPassword(aliasId, keystorePath, encodedPassword, rememberPassword);
                break;
            case ManageKeysActivity.MESSAGE_CODE_ALIAS_DISPLAY_NAME:
                aliasId = msg.getData().getLong(EditDisplayNameDialog.MSG_DATA_ALIAS_ID);
                String displayName = msg.getData().getString(EditDisplayNameDialog.MSG_DATA_TEXT);
                handleAliasDisplayName(aliasId, displayName);
                break;
            case EnterPasswordDialog.MESSAGE_CODE_ENTER_PASSWORD_CANCELLED:
                break; // ignore
            case ManageKeysActivity.MESSAGE_CODE_ALIAS_RENAME:
                aliasId = msg.getData().getLong(RenameKeyDialog.MSG_DATA_ALIAS_ID);
                String newKeyName = msg.getData().getString(RenameKeyDialog.MSG_DATA_KEY_NAME);
                String encodedKeystorePassword = msg.getData().getString(RenameKeyDialog.MSG_DATA_KEYSTORE_PASSWORD);
                String encodedKeyPassword = msg.getData().getString(RenameKeyDialog.MSG_DATA_KEY_PASSWORD);
                handleRenameAlias(aliasId, newKeyName, encodedKeystorePassword, encodedKeyPassword);
                break;
            case ManageKeysActivity.MESSAGE_CODE_ALIAS_DELETE:
                aliasId = msg.getData().getLong(RenameKeyDialog.MSG_DATA_ALIAS_ID);
                encodedKeystorePassword = msg.getData().getString(RenameKeyDialog.MSG_DATA_KEYSTORE_PASSWORD);
                handleDeleteAlias(aliasId, encodedKeystorePassword);
                break;
            case ManageKeysActivity.MESSAGE_CODE_ALIAS_PROPERTIES:
                aliasId = msg.getData().getLong(EnterPasswordDialog.MSG_DATA_ID);
                Alias alias = customKeysDataSource.lookupAliasById( aliasId);
                encodedPassword = msg.getData().getString(EnterPasswordDialog.MSG_DATA_PASSWORD);
                handleShowKeyProperties( alias, encodedPassword);
                break;
            default:
                logger.error("Unknown message code " + msg.what);
                break;
            }
        }
    };


    void handleRememberKeystorePassword( long keystoreId, String keystorePath, String encodedPassword, boolean rememberPassword) {
        char[] password = null;
        try {

            java.security.KeyStore ks = KeyStoreFileManager.loadKeyStore(keystorePath, encodedPassword);
            password = PasswordObfuscator.getInstance().decodeKeystorePassword( keystorePath, encodedPassword);
            Keystore keystore = customKeysDataSource.lookupKeystoreById(keystoreId);
            keystore.setPassword(encodedPassword);
            keystore.setRememberPassword(true);
            customKeysDataSource.updateKeystore(keystore);
                    
            for (Alias alias : keystore.getAliases()) {
                try {
                    ks.getKey(alias.getName(), password);
                    alias.setRememberPassword(rememberPassword);
                    String keypw = PasswordObfuscator.getInstance().encodeAliasPassword( keystorePath,alias.getName(), password);
                    alias.setPassword(keypw);
                    customKeysDataSource.updateAlias(alias);
                } catch (Exception x) {
                    logger.debug("Password for entry " + alias.getName() + " is not the same as the keystore password");
                }
            }
            keystoreExpandableListAdapter.dataChanged(customKeysDataSource.getAllKeystores());
        } catch (Exception x) {
            if (x.getMessage().indexOf("integrity check failed") >= 0) {
                logger.error(getResources().getString(R.string.WrongKeystorePassword));
                EnterPasswordDialog.show(
                        ManageKeysActivity.this, handler, getResources().getString( R.string.EnterKeystorePassword),
                        MESSAGE_CODE_KEYSTORE_REMEMBER_PASSWORD, keystorePath, keystoreId, rememberPassword, null);
            }
            else {
                logger.error("Error opening keystore file - " + x.getMessage());                    
            }
        }
        finally {
            if (password != null) PasswordObfuscator.flush( password);
        }
    }

    void handleRememberAliasPassword( long aliasId, String keystorePath, String encodedPassword, boolean rememberPassword) {
        char[] password = null;
        Alias alias = null;
        try {
            alias = customKeysDataSource.lookupAliasById(aliasId);
            java.security.KeyStore ks = KeyStoreFileManager.loadKeyStore( keystorePath, alias.getKeystore().getPassword());
            password = PasswordObfuscator.getInstance().decodeAliasPassword(alias.getKeystore().getPath(),alias.getName(), encodedPassword);
            ks.getKey(alias.getName(), password);
            alias.setRememberPassword(rememberPassword);
            alias.setPassword(encodedPassword);
            customKeysDataSource.updateAlias(alias);
            keystoreExpandableListAdapter.dataChanged(customKeysDataSource.getAllKeystores());

        } catch (UnrecoverableKeyException x) {
            logger.error(getResources().getString(R.string.WrongKeyPassword));
            EnterPasswordDialog.show(
                ManageKeysActivity.this, handler, getResources().getString( R.string.EnterKeyPassword),
                MESSAGE_CODE_ALIAS_REMEMBER_PASSWORD, keystorePath, aliasId, rememberPassword, alias.getName());
        } catch (Exception x) {
            logger.error("Error saving password - " + x.getMessage());
        }
        finally {
            if (password != null) PasswordObfuscator.flush( password);
        }
    }

    private void handleAliasDisplayName(long aliasId, String displayName) {
        try {
            Alias alias = customKeysDataSource.lookupAliasById(aliasId);
            alias.setDisplayName(displayName);
            customKeysDataSource.updateAlias(alias);
            keystoreExpandableListAdapter.dataChanged(customKeysDataSource.getAllKeystores());

        } catch (Exception x) {
            logger.error("Error saving display name - " + x.getMessage());
        }
    }

    void handleDeleteAlias( long aliasId, String storePass)
    {
        try {
            Alias alias = customKeysDataSource.lookupAliasById(aliasId);
            KeyStoreFileManager.deleteKey( alias.getKeystore().getPath(), storePass, alias.getName());
            customKeysDataSource.deleteAlias(alias);
            keystoreExpandableListAdapter.dataChanged( customKeysDataSource.getAllKeystores());
        } catch (Exception x) {
            logger.error( x.getMessage(), x);
        }
    }

    void handleRenameAlias( long aliasId, String newKeyName, String storePass, String keyPass) {
        Alias alias = customKeysDataSource.lookupAliasById(aliasId);
        char[] keyPw = null;
        if (alias.getName().equals(newKeyName)) return;
        try {
            newKeyName = KeyStoreFileManager.renameKey( alias.getKeystore().getPath(), storePass,
                alias.getName(), newKeyName, keyPass);

            if (alias.getName().equals(alias.getDisplayName())) alias.setDisplayName(newKeyName);

            if (alias.rememberPassword()) {
                keyPw = PasswordObfuscator.getInstance().decodeAliasPassword( alias.getKeystore().getPath(), alias.getName(), alias.getPassword());
                alias.setPassword(PasswordObfuscator.getInstance().encodeAliasPassword( alias.getKeystore().getPath(), newKeyName, keyPw ));
            }
            alias.setName(newKeyName);

            customKeysDataSource.updateAlias(alias);
            keystoreExpandableListAdapter.dataChanged( customKeysDataSource.getAllKeystores());

        } catch (KeyNameConflictException x) {
            logger.error( String.format(getResources().getString(R.string.NameConflictMessage), newKeyName));
        } catch (Exception x) {
            logger.error( x.getMessage(), x);
        }
        finally {
            if (keyPw != null) PasswordObfuscator.flush(keyPw);
        }
    }

    private void handleShowKeyProperties(Alias alias, String password) {
        try {
            String thePassword = alias.rememberPassword() ? alias.getPassword() : password;
            KeyStoreFileManager.validateKeyPassword(alias.getKeystore().getPath(), alias.getName(), thePassword);
            Intent i = new Intent(this, KeysPropertiesActivity.class);
            i.putExtra(KeyParameters.KEYSTORE_FILENAME, alias.getKeystore().getPath());
            i.putExtra(KeyParameters.KEY_NAME, alias.getName());
            i.putExtra(KeyParameters.KEY_PASSWORD, thePassword);
            startActivity(i);
        } catch (UnrecoverableKeyException x) {
            logger.error(getResources().getString(R.string.WrongKeyPassword));
            EnterPasswordDialog.show(
                ManageKeysActivity.this, handler, getResources().getString( R.string.EnterKeyPassword),
                MESSAGE_CODE_ALIAS_PROPERTIES, alias.getKeystore().getPath(), alias.getId(), alias.getName());
        } catch (Exception x) {
            logger.error("Error validating password - " + x.getMessage(), x);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO Auto-generated method stub
        super.onConfigurationChanged(newConfig);
    }



    class KeystoreLoader extends Thread {
        
        String keystorePath;
        String encodedPassword;
        boolean rememberPassword;
        
        Logger logger = AndroidLogManager.getAndroidLogger(KeystoreLoader.class);
        
        public KeystoreLoader( String keystorePath, String encodedPassword, boolean rememberPassword) {
            // this.customKeysDataSource = customKeysDataSource;
            this.keystorePath = keystorePath;
            this.encodedPassword = encodedPassword;
            this.rememberPassword = rememberPassword;
        }


        
        public void run() {
            char[] password = null;
            try {
                java.security.KeyStore ks = KeyStoreFileManager.loadKeyStore(keystorePath, encodedPassword);

                Keystore keystore = new Keystore();
                keystore.setPath( keystorePath);
                keystore.setRememberPassword( rememberPassword);
                keystore.setPassword( encodedPassword);

                password = PasswordObfuscator.getInstance().decodeKeystorePassword( keystorePath,encodedPassword);

                for (Enumeration<String> e = ks.aliases(); e.hasMoreElements(); ) {

                    String aliasName = e.nextElement();

                    // Ignore secret keys and trusted certs
                    if (!ks.isKeyEntry( aliasName)) return;

                    Alias alias = new Alias();
                    alias.setName(aliasName);
                    alias.setDisplayName(aliasName);
                    alias.setSelected(true);
                    try {
                        ks.getKey(aliasName, password);
                        alias.setRememberPassword(rememberPassword);
                        String keypw = PasswordObfuscator.getInstance().encodeAliasPassword( keystorePath,aliasName,password);
                        alias.setPassword(keypw);
                    } catch (Exception x) {
                        logger.debug("Password for entry " + aliasName + " is not the same as the keystore password");
                        alias.setRememberPassword(false);
                    }
                    keystore.addAlias(alias);
                }
                
                customKeysDataSource.addKeystore(keystore);
                sendMessage( MESSAGE_CODE_KEYSTORE_LOADED, null);
                
            } catch (IOException x) {
                if (x.getCause() != null)
                    logger.warn("IOException: cause="+x.getCause().getClass().getName(), x);
                else logger.warn("IOException: cause=null");
                
                if (x.getMessage() != null && x.getMessage().indexOf("integrity check failed") >= 0) {
                    sendMessage( MESSAGE_CODE_BAD_KEYSTORE_PASSWORD,null);
                } 
                else {
                    String msg = "Error opening keystore file - " + x.getMessage();
                    logger.error(msg, x);
                    sendMessage( MESSAGE_CODE_KEYSTORE_LOAD_ERROR, msg);
                    
                }
            } catch (Exception x) {
                String msg = "Error processing keystore file - " + x.getMessage();
                logger.error(msg, x);
                sendMessage( MESSAGE_CODE_KEYSTORE_LOAD_ERROR, msg);
            }
            finally {
                if (password != null) PasswordObfuscator.flush(password);
            }
        }


        void sendMessage( int msgCode, String message) {
            Message msg = new Message();
            msg.what = msgCode;
            Bundle data = new Bundle();
            data.putString( EnterPasswordDialog.MSG_DATA_MESSAGE, message);
            data.putString(EnterPasswordDialog.MSG_DATA_PASSWORD,  encodedPassword);
            data.putString(EnterPasswordDialog.MSG_DATA_PATH, keystorePath);
            data.putBoolean(EnterPasswordDialog.MSG_DATA_REMEMBER_PASSWORD, rememberPassword);
            msg.setData(data);
            handler.sendMessage(msg);            
        }
    }
}

