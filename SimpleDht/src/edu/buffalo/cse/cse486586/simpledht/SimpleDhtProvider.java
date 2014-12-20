package edu.buffalo.cse.cse486586.simpledht;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import edu.buffalo.cse.cse486586.simpledht.SimpleDhtActivity.ClientTask;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {
	static final String TAG = SimpleDhtProvider.class.getSimpleName();
	public static final Object monitor = new Object();
	public static boolean monitorState = false;
	private SQLLiteDBHelper database;
	SQLiteDatabase sqlDB;
	SimpleDhtActivity obj;
    @Override
    public String getType(Uri uri) {
        return null;
        
    }
    public boolean dhtInsert(ContentValues cv)
    {
    	String nodeKey = SimpleDhtActivity.nodeKey;
    	String succKey = SimpleDhtActivity.succKey;
    	String predKey = SimpleDhtActivity.predKey;
    	String succKeyValue = SimpleDhtActivity.succKeyValue;
        String key = cv.getAsString("key");
        String value = cv.getAsString("value");
        String key1 = genHash(key);
        if( (succKey.equals(nodeKey)) ||
            ( predKey.compareTo(nodeKey) > 0 && ((key1.compareTo(predKey) > 0) || (key1.compareTo(nodeKey) <= 0)) ) ||
            (((key1.compareTo(predKey) > 0)) && (key1.compareTo(nodeKey) <= 0)) 
          )
        {
          	return true; //insert in this node
        }
        else //find correct postion
        {
         	String msg = "DHTINS" +":" + key +":" + value;
          	new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "DHTINS",succKeyValue,msg);
        }
        return false;
    }
    public boolean dhtQuery(String key)
    {
    	String nodeKey = SimpleDhtActivity.nodeKey;
    	String succKey = SimpleDhtActivity.succKey;
    	String predKey = SimpleDhtActivity.predKey;
    	String succKeyValue = SimpleDhtActivity.succKeyValue;
    	String myPort = SimpleDhtActivity.myPort;
    	String key1 = genHash(key);
    	if(succKey.equals(nodeKey) || key.equals("@"))
    		return true;
    	if(!key.equals("*"))
    	{
              if(( predKey.compareTo(nodeKey) > 0 && ((key1.compareTo(predKey) > 0) || (key1.compareTo(nodeKey) <= 0)) ) ||
                (((key1.compareTo(predKey) > 0)) && (key1.compareTo(nodeKey) <= 0)) 
              )
              {
            	  return true; //insert in this node
              }
    	}
        String msg = "DHTQUERY" +":" + key +":" + myPort;
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "DHTINS",succKeyValue,msg);
		return false;
    }
    public boolean dhtDelete(String key)
    {
    	String nodeKey = SimpleDhtActivity.nodeKey;
    	String succKey = SimpleDhtActivity.succKey;
    	String predKey = SimpleDhtActivity.predKey;
    	String succKeyValue = SimpleDhtActivity.succKeyValue;
    	String key1 = genHash(key);
    	if(succKey.equals(nodeKey) || key.equals("@"))
    		return true;
    	if(!key.equals("*"))
    	{
              if(( predKey.compareTo(nodeKey) > 0 && ((key1.compareTo(predKey) > 0) || (key1.compareTo(nodeKey) <= 0)) ) ||
                (((key1.compareTo(predKey) > 0)) && (key1.compareTo(nodeKey) <= 0)) 
              )
              {
            	  return true; //delete in this node
              }
    	}
        String msg = "DHTDEL" +":" + key + ":" + succKeyValue;
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "DHTDEL",succKeyValue,msg);
		return false;
    }
    @Override
    public Uri insert(Uri uri, ContentValues values) {
    	if(dhtInsert(values) == true)
    	{
    		sqlDB.insertWithOnConflict(SQLLiteDBHelper.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    	}
        return uri;
        
    }
    @Override
    public boolean onCreate() {
    	database = new SQLLiteDBHelper(getContext());
    	sqlDB=database.getWritableDatabase();
    	obj = new SimpleDhtActivity();
        return false;
    }
	@Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
    	Log.e(TAG,"Query:"+ selection);
    	String selectArgs[]={selection};
    	Cursor cursor = null;
    	boolean result = dhtQuery(selection);
    	if(result == true)
    	{	
    		if(selection.equals("*") || selection.equals("@"))
    			cursor = sqlDB.query(SQLLiteDBHelper.TABLE_NAME, projection, null, null, null, null, null);
    		else
    			cursor = sqlDB.query(SQLLiteDBHelper.TABLE_NAME, projection, SQLLiteDBHelper.Key_column + "=?",selectArgs,null,null,sortOrder);
    	}
    	else
    	{
    		if(selection.equals("*"))
    		{
    			if(selection.equals("*") || selection.equals("@"))
        			cursor = sqlDB.query(SQLLiteDBHelper.TABLE_NAME, projection, null, null, null, null, null);
        		else
        			cursor = sqlDB.query(SQLLiteDBHelper.TABLE_NAME, projection, SQLLiteDBHelper.Key_column + "=?",selectArgs,null,null,sortOrder);
    			while(SimpleDhtActivity.mFlag != true)
    			{
    				try {
    					Thread.sleep(1000);
    				} catch (InterruptedException e) {
    					e.printStackTrace();
    				}
    			}
    			SimpleDhtActivity.resultCount = 0;
    			Cursor cursor1 = new MergeCursor(SimpleDhtActivity.mCursors);
    			cursor = new MergeCursor(new Cursor[] { cursor,cursor1 });
    		}
    		else
    		{
    			while(SimpleDhtActivity.resultCount != 1);
    			SimpleDhtActivity.resultCount = 0;
    			cursor = new MergeCursor(SimpleDhtActivity.mCursors);
    		}
    	}
        if (cursor != null)
            cursor.moveToFirst();
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
    	boolean result = dhtDelete(selection);
    	String selectArgs[]={selection};
    	int count = 0;
    	if(result == true)
    	{
    		if(selection.equals("*") || selection.equals("@"))
    			count = sqlDB.delete(SQLLiteDBHelper.TABLE_NAME, null, null);	
    		else
    			count = sqlDB.delete(SQLLiteDBHelper.TABLE_NAME, SQLLiteDBHelper.Key_column + "=?", selectArgs);
    	}
    	else if(selection.equals("*"))
    		count = sqlDB.delete(SQLLiteDBHelper.TABLE_NAME, null, null);
    	getContext().getContentResolver().notifyChange(uri, null);
    	Log.d(TAG,"Delete : entry deleted for selection : "+selection +"count"+count);
        return 1;
    }
    @SuppressWarnings("resource")
	public String genHash(String input){
    	Formatter formatter = new Formatter();
    	String str = null;
    	try
    	{
    		MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
    		byte[] sha1Hash = sha1.digest(input.getBytes());
    		for (byte b : sha1Hash) 
    		{
    			formatter.format("%02x", b);
    			str = formatter.toString();
    		}
    		
    	}
    	catch(NoSuchAlgorithmException e)
    	{
    		Log.d(TAG,"No Such Algorithm");
    	}
    	return str;
    }
}
