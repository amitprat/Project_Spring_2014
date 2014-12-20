package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class SimpleDhtActivity extends Activity {
	SimpleDhtProvider dhtProvider;
	OnTestClickListener obj;
	
	static final String TAG = SimpleDhtActivity.class.getSimpleName();
	static final int TOTAL_COUNT = 5;
	static final int SERVER_PORT = 10000;
	static final String joinerNodePortNo = "11108";
	static final String JOINERNODE = "5554";
	
	static String nodeKey;
	static String predKey;
	static String succKey;
	static String predKeyValue;
	static String succKeyValue;
	static String myPort;
	static int resultCount;
	
	TextView localTextView;
	TextView remoteTextView;
	int currNodeCount;
	String remotePort;
	String portStr;
	static MatrixCursor matrixcursor;
	String[] columnNames;
	static Cursor[] mCursors;
	static boolean mFlag;
	private Uri uri;
    @SuppressWarnings("deprecation")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_dht_main);
        obj = new OnTestClickListener(localTextView, getContentResolver());
    	dhtProvider = new SimpleDhtProvider();
    	columnNames = new String[]  {"key","value"};
    	matrixcursor = new MatrixCursor(columnNames);
 		startManagingCursor(matrixcursor);
 		mCursors = new Cursor[5];
 		mFlag = false;
 		uri = Uri.parse("content://edu.buffalo.cse.cse486586.simpledht.provider");
 		TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
 		portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        findViewById(R.id.button3).setOnClickListener(
                new OnTestClickListener(tv, getContentResolver()));
        
        broadcaseIdentity(myPort);
        
        try {
			ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
			new serverTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
		} catch (IOException e) {
			Log.e(TAG, "Can't create a ServerSocket");
			return;
		}
        
        
    }
    public void broadcaseIdentity(String myPort)
    {
    	nodeKey = dhtProvider.genHash(portStr);
    	succKey = nodeKey; succKeyValue = myPort;
    	predKey = null; predKeyValue = null;
    	String msg = "B"+":"+nodeKey+ ":" + myPort;
    	if(!(myPort.equals(joinerNodePortNo))) //if it is joiner node 
    	{
    		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "B",joinerNodePortNo,msg);
    	}
    }
    class serverTask extends AsyncTask<ServerSocket,String, Void>
    {
    	@Override
    	protected Void doInBackground(ServerSocket... params) {
    		try
    		{
    			ServerSocket sock = params[0];
    			while(true)
    			{
    				Socket cl_socket = sock.accept();
    				BufferedReader in = new BufferedReader(new InputStreamReader(cl_socket.getInputStream()));
    				String line = in.readLine();
    				processLine(line);
    				cl_socket.close();
    			}
    		}catch(Exception e)
    		{
    			Log.e(TAG, "Accept Failed!!"+e);
    		}
    		return null;
    	}
    	@SuppressWarnings("deprecation")
		void processLine(String line)
    	{
    		String[] splitArray = line.split(":");
    		if(splitArray[0].equals("B"))
    		{
    			updateJoinerSuccessorNode(splitArray);
    		}
    		else if( splitArray[0].equals("R"))
    		{
    			updateMySuccPredValues(splitArray);
    		}
    		else if(splitArray[0].equals("DHTINS"))
    		{
    				dhtInsertUtil(splitArray);
    		}
    		else if(splitArray[0].equals("DHTQUERY"))
    		{
    			Log.d(TAG,"DHTQUERY : Search for key :"+splitArray[1]);
    			dhtQueryUtil(splitArray);
    		}
    		else if(splitArray[0].equals("DHTQUERYRES"))
    		{
    			Log.d(TAG,"DHTQUERYRES :"+line+"search type:"+splitArray[1]);
    			if(splitArray[1].equals("END"))
    			{
    				mCursors[resultCount] = matrixcursor;
    				matrixcursor = new MatrixCursor(columnNames);
    				startManagingCursor(matrixcursor);
    				resultCount++;
    				if(splitArray[5].equals("*") && splitArray[4].equals(myPort))
    					mFlag = true;
    			}
    			else
    				matrixcursor.addRow(new Object[] {splitArray[2],splitArray[3]});
    		}
    		else if(splitArray[0].equals("DHTDEL"))
    		{
    			Log.d(TAG,"DHTDEL : Delete Key-Value Pair :"+splitArray[1]);
    			obj.deleteHelper(splitArray[1]);
    		}
    	}
    	@Override
    	protected void onProgressUpdate(String... values) {
    		super.onProgressUpdate(values);
    		String predKey = "Pred";
    		String succKey = "Succ";
    		String nodeKey = "Node";
    		String nodeValue = obj.queryPair(nodeKey);
    		String value = obj.queryPair(predKey);
    		if( (nodeValue.compareTo(values[1]) > 0) &&
    				(value.compareTo(values[1]) < 0) )	
    		{		
    			obj.insertPair(predKey,values[1]);
    		}
    		value = obj.queryPair(succKey);
    		if( (nodeValue.compareTo(values[1]) < 0) &&
    				(value.compareTo(values[1]) > 0) )	
    		{		
    			obj.insertPair(succKey,values[1]);
    		}
        }
    }
    public static class ClientTask extends AsyncTask<String, Void, Void> 
    {
        @Override
        protected Void doInBackground(String... msgs) {
            try {
            		 String remotePort = msgs[1];
                     Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                             Integer.parseInt(remotePort));
                     String msgToSend = msgs[2];
                     BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                     out.write(msgToSend);
                     out.flush();
                     socket.close();
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }
    }
    public void dhtInsertUtil(String splitArray[])
    {
    	String key = dhtProvider.genHash(splitArray[1]);
        if( ( predKey.compareTo(nodeKey) > 0 && ((key.compareTo(predKey) > 0) || (key.compareTo(nodeKey) <= 0)) ) ||
        		((key.compareTo(predKey) > 0)) && (key.compareTo(nodeKey) <= 0) )
        {
        	obj.insertPair(splitArray[1], splitArray[2]);
        }
        else
        {
        	String msg = "DHTINS" +":" + splitArray[1] +":" + splitArray[2];
        	new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "DHTINS",succKeyValue,msg);
        }
    }
    public void dhtQueryUtil(String splitArray[])
    {
    	Log.d(TAG,"dhtQueryUtil : "+splitArray[1]);
    	String key = dhtProvider.genHash(splitArray[1]);
    	if(splitArray[1].equals("*")) //do both
    	{
    		Cursor cursor = obj.queryCursor("@");
    		sendResults(cursor,splitArray[2],"*");
        	if(!(succKeyValue.equals(splitArray[2])))
        	{
        		String msg = "DHTQUERY" +":" + splitArray[1] +":" + splitArray[2];
        		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "DHTQUERY",succKeyValue,msg);
        	}
    	}
    	else if( ( predKey.compareTo(nodeKey) > 0 && ((key.compareTo(predKey) > 0) || (key.compareTo(nodeKey) <= 0)) ) ||
        	((key.compareTo(predKey) > 0)) && (key.compareTo(nodeKey) <= 0) )
        { //reply to sender
        	Cursor cursor = obj.queryCursor(splitArray[1]);
        	sendResults(cursor,splitArray[2],"N");
        }
        else //forward query
        {
        	String msg = "DHTQUERY" +":" + splitArray[1] +":" + splitArray[2];
        	new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "DHTQUERY",succKeyValue,msg);
        }
    }
    void sendResults(Cursor cursor,String sender,String type)
    {
    	if(cursor != null)
		{
    		cursor.moveToPosition(-1);
    		while (cursor.moveToNext()){
    			String key = cursor.getString(0);
    			String value = cursor.getString(1);
    			String msg = "DHTQUERYRES" +":" + "NOTEND"+":"+key+":"+value+":"+succKeyValue+":"+type;
    			new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "DHTQUERYRES",sender,msg);
    		}
		}
    	Log.d(TAG,"sendResults : result sent");
    	String msg = "DHTQUERYRES" +":" + "END"+":"+"N"+":"+"N"+":"+succKeyValue+":"+type;
    	new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "DHTQUERYRES",sender,msg);
    }
    public void dhtDisplay(String param)
    {
    	Log.d(TAG,"dhtDisplay : RemoteParamters"+param);
    }
    public void remoteQuery(String param,String reqPort)
    {
    	if(!(reqPort.equals(succKeyValue)))
    	{
    		String msg = "DHT" + ":" + reqPort + ":" + param;
    		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "DHT",succKeyValue,msg);
    	}
    }
    public void updateJoinerSuccessorNode(String splitArray[])
    {
    	if(succKey.compareTo(nodeKey) ==0)
    	{
			String msg = "R" + ":" + nodeKey +":" + myPort +":"+ succKey +":"+ succKeyValue;
			predKey = succKey = splitArray[1];
			predKeyValue = succKeyValue = splitArray[2];
			new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "R",splitArray[2],msg); //ask node to update pred/succ node
    	}
    	else if(succKey.compareTo(nodeKey) < 0 &&
    			(splitArray[1].compareTo(succKey) < 0 || splitArray[1].compareTo(nodeKey) > 0) ) //ask successor to update its predecessor values
    	{
    		String msg = "R" +":"+ splitArray[1] +":"+ splitArray[2] + ":" + "N" +":" + "N" ;
			new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "R",succKeyValue,msg); //ask successor node to update succ node
			msg = "R" + ":" + nodeKey +":" + myPort +":"+ succKey +":"+ succKeyValue;
			new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "R",splitArray[2],msg); //ask node to update pred/succ node
			//update your own succ key
    		succKey = splitArray[1];
    		succKeyValue = splitArray[2];
    	}
    	else if(splitArray[1].compareTo(nodeKey) >0 &&
    			splitArray[1].compareTo(succKey) < 0)
    	{
    		String msg = "R" + ":" + splitArray[1] + ":" + splitArray[2] + ":" + "N" + ":" + "N";
    		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "R",succKeyValue,msg);//ask node to update its predecessor
    		msg = "R" + ":" + nodeKey +":" + myPort +":"+ succKey +":"+ succKeyValue;
			new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "R",splitArray[2],msg); //ask node to update succ node
			succKey = splitArray[1];
    		succKeyValue = splitArray[2];
    	}
    	else
    	{
    		String msg = "B"+":"+splitArray[1]+ ":" +splitArray[2]; 
    		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "B",succKeyValue,msg);
    	}
    	Log.d(TAG,"Pred-->Me-->Succ"+predKeyValue+"-->"+myPort+"-->"+succKeyValue);
    }
    public void updateMySuccPredValues(String splitArray[])
    {
    	if(!splitArray[1].equals("N")){
			predKey = splitArray[1];
			predKeyValue = splitArray[2];
		}
    	if(!splitArray[3].equals("N")){
			succKey = splitArray[3];
			succKeyValue = splitArray[4];
		}
		Log.d(TAG,"keys update at new joinee"+splitArray[1]+splitArray[2]);
		Log.d(TAG,"Pred-->Me-->Succ"+predKeyValue+"-->"+myPort+"-->"+succKeyValue);
    }
    public void onClickGDump(View view)
	{
    	Log.d(TAG,"onClickGDump : Dump Global values");
		Cursor resultCursor = getContentResolver().query(uri, null,
				"*", null, null);
		resultCursor.moveToPosition(-1);
		while (resultCursor.moveToNext()){
			String result = resultCursor.getString(0);
			result +=":";
			result += resultCursor.getString(1);
			TextView tv = (TextView) findViewById(R.id.textView2);
			tv.append(result);
			tv.append("\n");
		}
	}
	public void onClickLDump(View view)
	{
		Cursor resultCursor = getContentResolver().query(uri, null,
				"@", null, null);
		Log.d(TAG,"onClickLDump : Dump local values");
		resultCursor.moveToPosition(-1);
		while (resultCursor.moveToNext()){
			String result = resultCursor.getString(0);
			result +=":";
			result += resultCursor.getString(1);
			TextView tv = (TextView) findViewById(R.id.textView2);
			tv.append(result);
			tv.append("\n");
		}
	}
}

