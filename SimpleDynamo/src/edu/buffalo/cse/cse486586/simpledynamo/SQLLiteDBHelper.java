package edu.buffalo.cse.cse486586.simpledynamo;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SQLLiteDBHelper extends SQLiteOpenHelper {

	  public static final String TABLE_NAME = "Content_Provider_Table";
	  public static final String Key_column = "key";
	  public static final String Value_Column = "value";
	  public static final String Context_Column = "context";
	  private static final String DATABASE_NAME = "Content_Provider";
	  private static final int DATABASE_VERSION = 1;

	  /*
	   * Database creation SQL statement
	   */
	  private static final String DATABASE_CREATE = "create table "
	      + TABLE_NAME + "(" + Key_column
	      + " text primary key, " + Value_Column
	      + " text not null, " + Context_Column
	      + " number);";
	  public SQLLiteDBHelper(Context context) {
	    super(context, DATABASE_NAME, null, DATABASE_VERSION);
	  }

	  @Override
	  public void onCreate(SQLiteDatabase db) {
		  
		Log.e("Created Table", TABLE_NAME); 
	    db.execSQL(DATABASE_CREATE);
	  }

	@Override
	public void onUpgrade(SQLiteDatabase db, int arg1, int arg2) {

		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		 Log.v("Droped Table", TABLE_NAME);
	}
}
