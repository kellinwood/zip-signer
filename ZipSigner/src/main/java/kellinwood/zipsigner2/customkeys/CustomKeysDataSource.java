package kellinwood.zipsigner2.customkeys;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kellinwood.logging.android.AndroidLogManager;
import kellinwood.logging.android.AndroidLogger;

public class CustomKeysDataSource {
    // Database fields
    private SQLiteDatabase database;
    private SQLiteHelper dbHelper;

    private static String[] allKeystoreColumns = {
            SQLiteHelper.KEYSTORE_COLUMN_ID, SQLiteHelper.KEYSTORE_COLUMN_PATH,
            SQLiteHelper.KEYSTORE_COLUMN_PASSWORD };

    private static String[] allAliasColumns = { SQLiteHelper.ALIAS_COLUMN_ID,
            SQLiteHelper.ALIAS_COLUMN_KEYSTORE_ID, SQLiteHelper.ALIAS_COLUMN_SELECTED, SQLiteHelper.ALIAS_COLUMN_NAME,
            SQLiteHelper.ALIAS_COLUMN_DISPLAY_NAME, SQLiteHelper.ALIAS_COLUMN_PASSWORD };

    AndroidLogger logger = null;

    public CustomKeysDataSource(Context context) {
        dbHelper = new SQLiteHelper(context);
        logger = AndroidLogManager.getAndroidLogger(CustomKeysDataSource.class);
    }

    public void open() throws SQLException {
        if (database == null) database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
        database = null;
    }

    public void addKeystore(Keystore keystore) {
        ContentValues ksValues = new ContentValues();
        ksValues.put(SQLiteHelper.KEYSTORE_COLUMN_PATH, keystore.getPath());
        if (!keystore.rememberPassword()) keystore.setPassword(null);
        ksValues.put(SQLiteHelper.KEYSTORE_COLUMN_PASSWORD, keystore.getPassword());

        long keystoreId = -1;
        
        try {
            keystoreId = database.insertOrThrow(SQLiteHelper.TABLE_KEYSTORE, null, ksValues);
        }
        catch (SQLiteConstraintException x) {
            throw new SQLiteConstraintException("keystore file already registered");
        }

        logger.debug("New keystore id="+keystoreId);
        if (keystoreId < 0) return;
        
        for (Alias alias : keystore.getAliases()) {
            ContentValues aliasValues = new ContentValues();
            aliasValues.put(SQLiteHelper.ALIAS_COLUMN_KEYSTORE_ID, keystoreId);
            aliasValues.put(SQLiteHelper.ALIAS_COLUMN_SELECTED, alias.isSelected() ? 1 : 0);
            aliasValues.put(SQLiteHelper.ALIAS_COLUMN_NAME, alias.getName());
            aliasValues.put(SQLiteHelper.ALIAS_COLUMN_DISPLAY_NAME, alias.getDisplayName());
            if (!alias.rememberPassword()) alias.setPassword(null);
            aliasValues.put(SQLiteHelper.ALIAS_COLUMN_PASSWORD, alias.getPassword());

            long aliasId = database.insertOrThrow(SQLiteHelper.TABLE_ALIAS, null,
                    aliasValues);
            alias.setId(aliasId);
        }
    }

    public void addKey( long keystoreId, Alias alias) {
        ContentValues aliasValues = new ContentValues();
        aliasValues.put(SQLiteHelper.ALIAS_COLUMN_KEYSTORE_ID, keystoreId);
        aliasValues.put(SQLiteHelper.ALIAS_COLUMN_SELECTED, alias.isSelected() ? 1 : 0);
        aliasValues.put(SQLiteHelper.ALIAS_COLUMN_NAME, alias.getName());
        aliasValues.put(SQLiteHelper.ALIAS_COLUMN_DISPLAY_NAME, alias.getDisplayName());
        if (!alias.rememberPassword()) alias.setPassword(null);
        aliasValues.put(SQLiteHelper.ALIAS_COLUMN_PASSWORD, alias.getPassword());

        long aliasId = database.insertOrThrow(SQLiteHelper.TABLE_ALIAS, null,
            aliasValues);
        alias.setId(aliasId);
    }

    public List<Keystore> getAllKeystores() {

        List<Keystore> keystores = new ArrayList<Keystore>();

        Map<Long, Keystore> keystoreMap = new HashMap<Long, Keystore>();

        Cursor cursor = database.query(SQLiteHelper.TABLE_KEYSTORE,
            allKeystoreColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Keystore keystore = cursorToKeystore(cursor);
            keystores.add(keystore);
            keystoreMap.put(keystore.getId(), keystore);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();

        cursor = database.query(SQLiteHelper.TABLE_ALIAS, allAliasColumns,
            null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            cursorToAlias(cursor, keystoreMap);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();

        return keystores;
    }

    public Keystore lookupKeystoreById( long keystoreId) {
        Cursor cursor = database.query(SQLiteHelper.TABLE_KEYSTORE,
            allKeystoreColumns, SQLiteHelper.KEYSTORE_COLUMN_ID + " = " + keystoreId, null, null, null, null);
        cursor.moveToFirst();
        Keystore keystore = cursorToKeystore(cursor);
        cursor.close();

        Map<Long, Keystore> keystoreMap = new HashMap<Long, Keystore>();
        keystoreMap.put(keystore.getId(), keystore);

        cursor = database.query(SQLiteHelper.TABLE_ALIAS, allAliasColumns,
            SQLiteHelper.ALIAS_COLUMN_KEYSTORE_ID + " = " + keystoreId, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            cursorToAlias(cursor, keystoreMap);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();

        return keystore;
    }

    public Alias lookupAliasById( long id) {
        Cursor cursor = database.query(SQLiteHelper.TABLE_ALIAS, new String[] { SQLiteHelper.ALIAS_COLUMN_KEYSTORE_ID},
            SQLiteHelper.ALIAS_COLUMN_ID + " = " + id, null, null, null, null);
        cursor.moveToFirst();
        long keystoreId = cursor.getLong(0);
        cursor.close();

        cursor = database.query(SQLiteHelper.TABLE_KEYSTORE,
            allKeystoreColumns, SQLiteHelper.KEYSTORE_COLUMN_ID + " = " + keystoreId, null, null, null, null);
        cursor.moveToFirst();
        Keystore keystore = cursorToKeystore(cursor);
        Map<Long, Keystore> keystoreMap = new HashMap<Long, Keystore>();
        keystoreMap.put(keystore.getId(), keystore);
        cursor.close();

        cursor = database.query(SQLiteHelper.TABLE_ALIAS, allAliasColumns,
            SQLiteHelper.ALIAS_COLUMN_ID + " = " + id, null, null, null, null);
        cursor.moveToFirst();
        Alias result = cursorToAlias(cursor, keystoreMap);
        cursor.close();

        return result;
    }

    private Keystore cursorToKeystore(Cursor cursor) {
        Keystore keystore = new Keystore();
        keystore.setId(cursor.getLong(0));
        keystore.setPath(cursor.getString(1));
        String pw = cursor.getString(2);
        keystore.setPassword(pw);
        keystore.setRememberPassword(pw != null && pw.length() > 0);
        return keystore;
    }

    private Alias cursorToAlias(Cursor cursor, Map<Long, Keystore> keystoreMap) {
        Alias alias = new Alias();

        alias.setId(cursor.getLong(0));
        long keystoreId = cursor.getLong(1);
        Keystore keystore = keystoreMap.get(keystoreId);
        if (keystore == null)
            return null; // todo: delete orphaned aliases
        alias.setKeystore(keystore);
        keystore.addAlias(alias);
        alias.setSelected(cursor.getLong(2) != 0);
        alias.setName(cursor.getString(3));
        alias.setDisplayName( cursor.getString(4));
        String pw = cursor.getString(5);
        alias.setPassword(pw);
        alias.setRememberPassword(pw != null && pw.length() > 0);

        return alias;
    }

    public void deleteKeystore(Keystore keystore) {

        for (Alias alias : keystore.getAliases()) {
            deleteAlias(alias);
        }
        long id = keystore.getId();
        database.delete(SQLiteHelper.TABLE_KEYSTORE,
                SQLiteHelper.KEYSTORE_COLUMN_ID + " = " + id, null);
        logger.debug("Keystore deleted with id=" + id + ", path=" + keystore.getPath());
        
    }

    public void deleteAlias(Alias alias) {
        long id = alias.getId();
        database.delete(SQLiteHelper.TABLE_ALIAS,
            SQLiteHelper.ALIAS_COLUMN_ID + " = " + id, null);
        logger.debug("Alias deleted with id=" + id + ", name = " + alias.getName());
    }

    public void updateKeystore( Keystore keystore) {
        ContentValues ksValues = new ContentValues();
        ksValues.put(SQLiteHelper.KEYSTORE_COLUMN_PATH, keystore.getPath());
        if (keystore.getPassword() == null) keystore.setRememberPassword(false);
        else if (!keystore.rememberPassword()) keystore.setPassword(null);
        ksValues.put(SQLiteHelper.KEYSTORE_COLUMN_PASSWORD, keystore.getPassword());        
        database.update (SQLiteHelper.TABLE_KEYSTORE, ksValues, SQLiteHelper.KEYSTORE_COLUMN_ID + " = " + keystore.getId(), null);
    }

    public void updateAlias(Alias alias) {
        ContentValues aliasValues = new ContentValues();
        aliasValues.put(SQLiteHelper.ALIAS_COLUMN_SELECTED, alias.isSelected() ? 1 : 0);
        aliasValues.put(SQLiteHelper.ALIAS_COLUMN_NAME, alias.getName());
        if (alias.getDisplayName() == null || alias.getDisplayName().length() == 0) alias.setDisplayName(alias.getName());
        aliasValues.put(SQLiteHelper.ALIAS_COLUMN_DISPLAY_NAME, alias.getDisplayName());
        if (alias.getPassword() == null) alias.setRememberPassword(false);
        else if (!alias.rememberPassword()) alias.setPassword(null);
        aliasValues.put(SQLiteHelper.ALIAS_COLUMN_PASSWORD, alias.getPassword());
        database.update (SQLiteHelper.TABLE_ALIAS, aliasValues, SQLiteHelper.ALIAS_COLUMN_ID + " = " + alias.getId(), null);        
    }


}
