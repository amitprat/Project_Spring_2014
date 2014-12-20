package edu.buffalo.cse.cse486586.groupmessenger;

import java.util.HashMap;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko/amitpratapsingh
 *
 */
public class GroupMessengerProvider extends ContentProvider {
	private Long rowId;
	private  DatabaseHelper helper;
	static HashMap<String, String> BirthMap;
	final static String DBNAME = "DBSTORE";
	private SQLiteDatabase database;
	final String TAG = "DatabaseHelper";
	final static String tableName = "VALUESTORE";
	final static String CREATE_TABLE = " CREATE TABLE IF NOT EXISTS " + 
			tableName + 
			" (key TEXT PRIMARY KEY UNIQUE," +
			" value TEXT NOT NULL);";
	
	public class DatabaseHelper extends SQLiteOpenHelper{
		public DatabaseHelper(Context context, String name, CursorFactory factory,int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.d("LOG", "TABLE CREATED");
			db.execSQL(CREATE_TABLE);
		}
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.v(TAG,"Upgrading database from older version" + oldVersion +"to new version" + newVersion);
			String update = "DROP TABLE IF EXISTS" + db;
			db.execSQL(update);
			onCreate(db);
		}

	}
	@Override
	public boolean onCreate() {
	    helper = new DatabaseHelper(getContext(),
	    			DBNAME,
	    			null, 
	    			1);
	    database = helper.getWritableDatabase();
	    if(database != null)
	    	return true;
	    throw new SQLException("Table Creation Failed" + database);
	}
    @Override
    public Uri insert(Uri uri, ContentValues values) {
    	Log.v("insert", values.toString());
    	try {
			 rowId = database.insertOrThrow(tableName, null, values);
		}catch(SQLiteConstraintException e)
		{
			Log.v(TAG, "Duplicate Message insertion , Value updated");
			rowId = database.replace(tableName, null, values);
		}
    	
    	catch (Exception e) {
			Log.w(TAG, "Insert failed");
		}
    	if(rowId > 0)
    	{
    		Uri newUri =  ContentUris.withAppendedId(uri, rowId);
    		getContext().getContentResolver().notifyChange(newUri, null);
    		return newUri;
    	}
    	return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
    	database = helper.getReadableDatabase();
    	SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
    	queryBuilder.setTables(tableName);
    	queryBuilder.appendWhere("key='" +selection+"'");
    	Cursor cursor = queryBuilder.query(database,
    			projection, null, selectionArgs, null, null, sortOrder);
    	cursor.setNotificationUri(getContext().getContentResolver(), uri);
        Log.v("query", selection);
        return cursor;
    }
    @Override
    public String getType(Uri uri) {
        return null;
    }
    
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

}
