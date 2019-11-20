package kellinwood.zipsigner2.customkeys;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import kellinwood.logging.android.AndroidLogManager;
import kellinwood.logging.android.AndroidLogger;

public class SQLiteHelper extends SQLiteOpenHelper {

    public static final String TABLE_KEYSTORE = "keystore";
    public static final String KEYSTORE_COLUMN_ID = "_id";
    public static final String KEYSTORE_COLUMN_PATH = "path";
    public static final String KEYSTORE_COLUMN_PASSWORD = "password";
    
    public static final String TABLE_ALIAS = "alias";
    public static final String ALIAS_COLUMN_ID = "_id";
    public static final String ALIAS_COLUMN_KEYSTORE_ID = "keystore_id";
    public static final String ALIAS_COLUMN_NAME = "name";
    public static final String ALIAS_COLUMN_DISPLAY_NAME = "display_name";
    public static final String ALIAS_COLUMN_SELECTED = "enabled";
    public static final String ALIAS_COLUMN_PASSWORD = "password";

    private static final String DATABASE_NAME = "custom_keys.db";
    private static final int DATABASE_VERSION = 3;
    
    AndroidLogger logger = null;

    // keystore table creation statement
    private static final String KEYSTORE_TABLE_CREATE = "create table "
        + TABLE_KEYSTORE + "(" 
        + KEYSTORE_COLUMN_ID + " integer primary key autoincrement, " 
        + KEYSTORE_COLUMN_PATH + " text not null unique,"
        + KEYSTORE_COLUMN_PASSWORD + " text default null);";
    
    // keystore table creation statement
    private static final String ALIAS_TABLE_CREATE = "create table "
        + TABLE_ALIAS + "(" 
        + ALIAS_COLUMN_ID + " integer primary key autoincrement, " 
        + ALIAS_COLUMN_KEYSTORE_ID + " integer not null, "         
        + ALIAS_COLUMN_SELECTED + " integer not null, "
        + ALIAS_COLUMN_NAME + " text not null,"
        + ALIAS_COLUMN_DISPLAY_NAME + " text not null,"
        + ALIAS_COLUMN_PASSWORD + " text default null);";    

    public SQLiteHelper(Context context) {
      super(context, DATABASE_NAME, null, DATABASE_VERSION);
      logger = AndroidLogManager.getAndroidLogger(SQLiteHelper.class);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(KEYSTORE_TABLE_CREATE);
        database.execSQL(ALIAS_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      logger.debug("Upgrading database from version " + oldVersion + " to "
              + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ALIAS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_KEYSTORE);
      onCreate(db);
    }

}
