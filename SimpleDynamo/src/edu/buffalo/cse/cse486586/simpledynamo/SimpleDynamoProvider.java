package edu.buffalo.cse.cse486586.simpledynamo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

public class SimpleDynamoProvider extends ContentProvider {
	private SQLLiteDBHelper database;
	private SimpleDynamoActivity main;
	private SQLiteDatabase sqlDB;
	/*
	 * Parameter Declaration
	 */
	private static final String KEY_FIELD = "key";
	private static final String VALUE_FIELD = "value";
	private static final String CONTEXT_FIELD = "context";
	static final String TAG = SimpleDynamoProvider.class.getSimpleName();
	private final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
	private final Lock readLock = rw.readLock();
	private final Lock writeLock = rw.writeLock();
	ReentrantLock lock1 = new  ReentrantLock();
	private int iContext = 0;

	@Override
	public boolean onCreate() {
		database = new SQLLiteDBHelper(getContext());
		main = new SimpleDynamoActivity();
		if(database == null)
	        return false;
    	sqlDB=database.getWritableDatabase();
    	return true;
	}
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		Log.d(TAG,"insert start for key : "+values.getAsString(KEY_FIELD));
		writeLock.lock();
		Uri muri = null;
		try {
			muri = new InsertTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,uri,values).get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		writeLock.unlock();
		Log.d(TAG,"insert done for key : "+values.getAsString(KEY_FIELD));
        return muri;
	}
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		return 0;
	}
	@SuppressWarnings("static-access")
	public boolean islocalQuery(String selection)
	{
		if(selection.equals("@") == true) return true;
		if(selection.equals("*") == true) return false;
	    String key1 = genHash(selection);
	    String predKey = main.predKey;
	    String nodeKey = main.nodeKey;
	    Log.d(TAG,predKey+":"+nodeKey+":"+main.myPort);
		if( (key1.compareTo(predKey) > 0 && key1.compareTo(nodeKey) <=0) ||
				( predKey.compareTo(nodeKey) > 0 && ( (key1.compareTo(predKey) > 0) || (key1.compareTo(nodeKey) <= 0) ) )
				)
		{
			return true;
		}
		return false;
	}
	public int isKeyExists(String key)
	{
		String projection[] ={"key","value","context"};
		String selectArgs[]={key};
		database = new SQLLiteDBHelper(getContext());
		sqlDB=database.getWritableDatabase();
		Cursor resultCursor = sqlDB.query(SQLLiteDBHelper.TABLE_NAME, projection, SQLLiteDBHelper.Key_column + "=?",selectArgs,null,null,null);
		if(resultCursor == null) return -1;
		String context1 = resultCursor.getString(resultCursor.getColumnIndex("context"));
		int contextNo = Integer.parseInt(context1);
		return contextNo;
	}
	@Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		 	Log.d(TAG,"query start for key : "+selection);
		 	boolean flag = false;
		 	if(selectionArgs != null && selectionArgs.length > 0)
		 	{
		 		String select1 = selectionArgs[0];
		 		if(select1.equals("REP")) flag = true;
		 	}
		 	String selectArgs[]={selection};
		 	String[] projection1 = new String[3];
		 	if(projection == null)
		 	{
		 		projection1[0] = KEY_FIELD;
		 		projection1[1] = VALUE_FIELD;
		 	}
		 	else
		 	{
		 		projection1[0] = KEY_FIELD;
		 		projection1[1] = VALUE_FIELD;
		 		projection1[2] = CONTEXT_FIELD;
		 	}
	    	Cursor cursor = null;
	    	
	    	boolean result = islocalQuery(selection);
	    	if(flag || result == true)
	    	{
	    		readLock.lock();
	    		if(selection.equals("*") || selection.equals("@"))
	    			cursor = sqlDB.query(SQLLiteDBHelper.TABLE_NAME, projection1, null, null, null, null, null);
	    		else
	    			cursor = sqlDB.query(SQLLiteDBHelper.TABLE_NAME, projection1, SQLLiteDBHelper.Key_column + "=?",selectArgs,null,null,sortOrder);
	    		if (cursor != null)
	 	            cursor.moveToFirst();
	    		readLock.unlock();
	    	}
	    	else
	    	{
	    		lock1.lock();
	    		result = main.dhtQuery(selection,genHash(selection),"QUE");
	    		if(selection.equals("*"))
	    		{
	    			if(selection.equals("*") || selection.equals("@"))
	        			cursor = sqlDB.query(SQLLiteDBHelper.TABLE_NAME, projection1, null, null, null, null, null);
	        		else
	        			cursor = sqlDB.query(SQLLiteDBHelper.TABLE_NAME, projection1, SQLLiteDBHelper.Key_column + "=?",selectArgs,null,null,sortOrder);
	    			
	    			while(SimpleDynamoActivity.resultCount < 5);
	    			SimpleDynamoActivity.resultCount = 0;
	    			Cursor cursor1 = new MergeCursor(SimpleDynamoActivity.mCursors);
	    			cursor = new MergeCursor(new Cursor[] { cursor,cursor1 });
	    		}
	    		else
	    		{
	    			while(SimpleDynamoActivity.resultCount < 1);
	    			/*int index = -1,temp;
	    			Cursor temp1 = SimpleDynamoActivity.mCursors[0];
	    			if(SimpleDynamoActivity.resultCount > 1)
	    			{
	    				for(int i=0;i<5;i++)
	    				{
	    					Cursor c = SimpleDynamoActivity.mCursors[i];
	    					temp = Integer.parseInt(c.getString(2));
	    					if(temp > index) temp1 = c;
	    				}
	    				SimpleDynamoActivity.mCursors[0] = temp1;
	    				for(int i=1;i<5;i++) SimpleDynamoActivity.mCursors[i] = null;
	    			}*/
	    			for(int i=1;i<5;i++) SimpleDynamoActivity.mCursors[i] = null;
	    			SimpleDynamoActivity.resultCount = 0;
	    			cursor = new MergeCursor(SimpleDynamoActivity.mCursors);
	    		}
	    		 if (cursor != null)
	 	            cursor.moveToFirst();
	    		lock1.unlock();
	    	}
	    	Log.d(TAG,"query done for key : "+selection +","+cursor.getCount());
	        Log.d(TAG,"query done for key : "+selection +", value  : "+cursor.getString(1));
	        return cursor;
    }
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		String selectArgs[]={selection};
    	int count = 0;
    	count = sqlDB.delete(SQLLiteDBHelper.TABLE_NAME, SQLLiteDBHelper.Key_column + "=?", selectArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		Log.d(TAG,"Delete : entry deleted for selection : "+selection +"count"+count);
		if(selectionArgs != null && selectionArgs.length > 0)
	 	{
	 		String select1 = selectionArgs[0];
	 		if(select1.equals("DEL")) return count;
	 	}
		main.deleteRepKeys(selection);
		return count;
	}
	
	@Override
	public String getType(Uri uri) {
		return null;
	}
	public String genHash(String input){
    	@SuppressWarnings("resource")
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
	@SuppressWarnings("static-access")
	public boolean islocalInsert(ContentValues cv)
	{
		String key = cv.getAsString("key");
	    String value = cv.getAsString("value");
	    String key1 = genHash(key);
	    String predKey = main.predKey;
	    String nodeKey = main.nodeKey;
	    Log.d(TAG,predKey+":"+nodeKey+":"+main.myPort);
		if( (key1.compareTo(predKey) > 0 && key1.compareTo(nodeKey) <=0) ||
				( predKey.compareTo(nodeKey) > 0 && ( (key1.compareTo(predKey) > 0) || (key1.compareTo(nodeKey) <= 0) ) )
				)
		{
			main.replicateStore(key,value);
			return true;
		}
		main.sendRequestToDestNode(key,value,key1);
		return false;
	}
	
	public class InsertTask extends AsyncTask<Object, Void, Uri>
    {
		@Override
		protected Uri doInBackground(Object... arg0) {
			Uri uri = (Uri)arg0[0];
			ContentValues values = (ContentValues)arg0[1];
			boolean flag = false;
			if(values.size() == 3)
			{
				String key = values.getAsString("Context");
				if(key.compareTo("DUP") == 0)
				{
					key = values.getAsString(KEY_FIELD);
					String value = values.getAsString(VALUE_FIELD);
					values.clear();
					values.put(KEY_FIELD,key);
					values.put(VALUE_FIELD, value);
					flag = true;
				}
			}
			if(flag || islocalInsert(values) ==true)
			{
				Log.d(TAG,"inserted"+values.getAsString(KEY_FIELD));
				values.put(CONTEXT_FIELD,iContext+1);
				sqlDB.insertWithOnConflict(SQLLiteDBHelper.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
			}
			return uri;
		}
    }
}
