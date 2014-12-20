package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class SimpleDynamoActivity extends Activity {
	/*
	 * Parameter Declaration
	 */
	SimpleDynamoProvider provider;
	OnTestClickListener obj;
	static final String TAG = SimpleDynamoActivity.class.getSimpleName();
	static final int TOTAL_COUNT = 5;
	static final int TIMEOUT = 500;
	static final int SERVER_PORT = 10000;
	
	static final String[] serverIds = { "5554" ,"5556" ,"5558" , "5560" ,"5562" };
	static final String[] ports = { "11108" ,"11112" ,"11116" , "11120" ,"11124" };
	static  String portStr;
	static  String myPort;
	static  String nodeKey;
	static  String predKey;
	static  TreeMap<String, String> storeRec;
	static MatrixCursor matrixcursor;
	static Cursor[] mCursors;
	static int resultCount;
	private String[] columnNames;
	static Timer timer,timer1,timer2;
	static boolean querydoneflag = false;
	static HashMap<String,String> queryResponseFlag;
	/*
	 * Code Starts
	 */
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_simple_dynamo);
		/*
		 * parameter initialization
		 */
		mCursors = new Cursor[5];
		columnNames = new String[]  {"key","value"};
		matrixcursor = new MatrixCursor(columnNames);
 		startManagingCursor(matrixcursor);
		provider = new SimpleDynamoProvider();
		storeRec = new TreeMap<String, String>();
		TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        obj = new OnTestClickListener(tv, getContentResolver());
        findViewById(R.id.button3).setOnClickListener(
                new OnTestClickListener(tv, getContentResolver()));
        /*
         * Get port no
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        /*
         * Initialize initial server locations in a ring
         * identified by port string hash function i.e. hash(portStr)
         */
        intializeTreeMap();
        /*
         * create server task
         */
        try {
			ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
			new serverTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
		} catch (IOException e) {
			Log.e(TAG, "Can't create a ServerSocket");
			return;
		}
        /*Run Node recovery to handle failure
         * when failure , recover keys from right nodes
         */
        runNodeRecovery();
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
    				processRequest(line);
    				cl_socket.close();
    			}
    		}catch(Exception e)
    		{
    			Log.e(TAG, "Accept Failed!!"+e);
    		}
    		return null;
    	}
    	@SuppressWarnings("deprecation")
    	/*
    	 * Process request at nodes
    	 */
		public void processRequest(String line)
    	{
    		String[] splitArray = line.split(":");
    		if(splitArray[0].equals("INS"))
    		{
    			 //insert to my local database
    			 obj.insertPair(splitArray[1],splitArray[2],"INS");
    			 /*send insert response*/
    			 String msg = "INSRES"+":"+myPort;
			     new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "INSRES",splitArray[3],msg);
    		}
    		if(splitArray[0].equals("INSDUP"))
    		{
    			 //insert to my local database
    			 obj.insertPair(splitArray[1],splitArray[2],"REP");
    		}
    		else if(splitArray[0].equals("INSRES"))
    		{
    			/*cancel timer for insert operation if ack received*/
    			if(timer != null)
    			{
    				timer.cancel();
    				timer.purge();
    			}
    		}
    		else if(splitArray[0].equals("REP"))
    		{
    			//this is replication store
    			obj.insertPair(splitArray[1],splitArray[2],"REP");
    		}
    		else if(splitArray[0].equals("QUE"))
    		{
    			//this is query for key
    			if(splitArray[1].equals("*"))
    				splitArray[1] = "@";
    			Cursor cursor = obj.queryCursor(splitArray[1]);
    			if(cursor != null && cursor.getCount() > 0)
    				sendResults(cursor,splitArray[2],"QUERES");
    		}
    		else if(splitArray[0].equals("QUEREP"))
    		{
    			//this is query in case if replication
    			if(splitArray[1].equals("*"))
    				splitArray[1] = "@";
    			Cursor cursor = obj.queryRepCursor(splitArray[1]);
    			if(cursor != null && cursor.getCount() > 0)
    				sendResults(cursor,splitArray[2],"QUERES");
    		}
    		else if(splitArray[0].equals("QUERES"))
    		{
    			//handle query response
    			querydoneflag = true;
    			if(queryResponseFlag != null && queryResponseFlag.containsKey(splitArray[3]))
    				queryResponseFlag.put(splitArray[3],"Y");
    			String keys = splitArray[1];
    			String values = splitArray[2];
    			String[] keystore = keys.split("#");
    			String[] valuestore = values.split("#");
    			for(int i = 0;i<keystore.length;i++){
    				String key = keystore[i];
    				String value = valuestore[i];
    				matrixcursor.addRow(new Object[] {key,value});
    			}
    			mCursors[resultCount] = matrixcursor;
				matrixcursor = new MatrixCursor(columnNames);
				startManagingCursor(matrixcursor);
				resultCount++;
    		}
    		else if(splitArray[0].equals("RECOVERY"))
    		{
    			//Handle recovery request
    			if(splitArray[1].equals("*"))
    				splitArray[1] = "@";
    			Cursor cursor = obj.queryCursorRec(splitArray[1]);
    			if(cursor != null && cursor.getCount() > 0)
    			{
    				sendResultsRec(cursor,splitArray[2],"RECRES",splitArray[3],splitArray[4]);
    			}
    				
    		}
    		else if(splitArray[0].equals("RECRES"))
    		{
    			//Handle recovery response
    			String keys = splitArray[1];
    			String values = splitArray[2];
    			String[] keystore = keys.split("#");
    			String[] valuestore = values.split("#");
    			for(int i = 0;i<keystore.length;i++){
    				String key = keystore[i];
    				String value = valuestore[i];
    				obj.insertPair(key,value,"REP");
    			}
    		}
    		else if(splitArray[0].equals("DEL"))
    		{
    			//Delete replicate data
    			Log.d(TAG,"Delete for key:"+splitArray[1]);
    			obj.deleteCursor(splitArray[1]);
    		}
    	}
    }
	/*
	 * Client task to send messages
	 */
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
	/*
	 * TreeMap to store sorted order of node keys-portNo pair according to hash(portStr).
	 */
	void intializeTreeMap()
	{
		nodeKey = provider.genHash(portStr);
		predKey = null;
		for(int i=0;i<TOTAL_COUNT;i++)
		{
			String hash = provider.genHash(serverIds[i]);
			if(predKey == null && hash.compareTo(nodeKey) < 0)
				predKey = hash;
			else if(predKey != null && hash.compareTo(predKey) > 0 && hash.compareTo(nodeKey) < 0)
				predKey = hash;
			storeRec.put(hash, ports[i]);
		}
		if(predKey == null)
			predKey = (String) storeRec.lastKey();
	}
	/*
	 * Query in the given DHT ring for the key
	 */
	public boolean dhtQuery(String key,String key1,String queryString)
	{
		  	if(key.compareTo("@") == 0) return true;
		  	if(key.equals("*"))
		  	{
		  		Iterator<Entry<String, String>> iter = storeRec.entrySet().iterator();
		  		queryResponseFlag = new HashMap<String, String>();
		  		while(iter.hasNext())
		  		{
		  			Entry<String, String> map = (Entry<String, String>) iter.next();
		  			String dest = (String) map.getValue();
		  			String msg = queryString +":" + key +":"+myPort;
		  			queryResponseFlag.put(dest, "N");
			    	new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, queryString,dest,msg);
		  		}
		  		delay(2000);
		  		int i = 0;
		  		iter = queryResponseFlag.entrySet().iterator();
		  		while(iter.hasNext())
		  		{
		  			
		  			String val = iter.next().getValue();
		  			if(val != null && val.equals("N"))
				    	break;
		  			i++;
		  		}
		  		String key2 = (String) storeRec.keySet().toArray()[(i+1)%5];
			  	String dest1 = storeRec.get(key2);
		    	String msg = "QUEREP" +":" + key + ":" + myPort + ":" + myPort;
		    	new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "QUEREP",dest1,msg);
		  		return false;
		  	}
		  	if(key1.compareTo(predKey) > 0 && key1.compareTo(nodeKey) <=0) return true;
	    	Iterator<Entry<String, String>> iter = storeRec.entrySet().iterator();
	    	Entry<String, String> map = storeRec.lastEntry();
	    	String pred = (String)map.getKey();
	    	String curr,dest=(String) map.getValue();
	    	
	    	while(iter.hasNext())
			{
				map = (Entry<String, String>) iter.next();
				curr = (String) map.getKey();
				if(pred.compareTo(curr) > 0 && (key1.compareTo(pred) > 0 || key1.compareTo(curr) <= 0))
				{
					dest = (String) map.getValue();
					break;
				}
				if(key1.compareTo(pred) > 0 && key1.compareTo(curr) <= 0)
				{
					dest = (String) map.getValue();
					break;
				}
				pred = curr;
			}
	    	if(dest.equals(myPort)) return true;
	    	String msg = queryString +":" + key +":"+myPort;
	    	new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, queryString,dest,msg);
	    	String dest1 = null;
	    	if(iter.hasNext())
	    		dest1 = iter.next().getValue();
	    	else
	    		dest1 = storeRec.firstEntry().getValue();
	    	querydoneflag = false;
	    	msg = "QUEREP" +":" + key +":"+myPort;
	    	int time = 0;
	    	while(!querydoneflag)
	    	{
	    		delay(500);
	    		time +=500;
	    		if(time > 1000)
	    		{
		    		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "QUEREP",dest1,msg);
		    		break;
	    		}
	    	}
	    	
	    	return false;
	}
	class dhtQueryAllTimeOutHandler extends TimerTask 
	{
		String key,dest;
		String queryString = "QUEREP";
		int i=0;
		public dhtQueryAllTimeOutHandler(String key) {
	         this.key = key;
	     }
		 public void run() {
			  Iterator<Entry<String, String>> iter = queryResponseFlag.entrySet().iterator();
		  	  while(iter.hasNext())
		  	  {
		  			Entry<String, String>  map = (Entry<String, String>) iter.next();
		  			String val = map.getValue();
		  			if(val != null && val.equals("N"))
				    	break;
		  			i++;
		  	  }
		  	  String key1 = (String) storeRec.keySet().toArray()[(i+1)%5];
		  	  dest = storeRec.get(key1);
		  	  Log.d("dhtQueryAllTimeOutHandler","dest - "+dest);
			  String msg = queryString +":" + key +":"+myPort;
			  new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, queryString,dest,msg);
			  timer1.cancel();
			  timer1.purge();
		  }
	}
	class dhtQueryTimeOutHandler extends TimerTask 
	{
		String key,dest;
		String queryString = "QUEREP";
		public dhtQueryTimeOutHandler(String key,String dest) {
	         this.key = key;
	         this.dest = dest;
	     }
		  public void run() {
			  querydoneflag = true;
			  String msg = queryString +":" + key +":"+myPort;
			  Log.d("dhtQueryTimeOutHandler","dest - "+dest);
			  new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, queryString,dest,msg);
			  timer2 = null;
		  }
	 }
	/*
	 * Send request to coordinator node to store key and replicate it in its quorum.
	 */
	 public void sendRequestToDestNode(String key,String value,String hash)
	 {
			Iterator<Entry<String, String>> iter = storeRec.entrySet().iterator();
			String p = (String) storeRec.lastKey();
			String dest = null;
			int pos = 0;
			Entry<String, String> map;
			while(iter.hasNext())
			{
				map = (Entry<String, String>) iter.next();
				dest = (String) map.getKey();
				if( (hash.compareTo(p) > 0 && hash.compareTo(dest) <=0) ||
						( p.compareTo(dest) > 0 && ( (hash.compareTo(p) > 0) || (hash.compareTo(dest) <= 0) ) )
						)
				{
					 String msg = "INS" +":" + key +":" + value+":"+myPort;
					 String destPort = (String) map.getValue();
				     new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "INS",destPort,msg);
				     break;
				}
				p = dest;
				pos++;
			}
			/*Start timer for TIMEOUT and send request to successor of destination port after timeout
		    * otherwise cancel timer if successful response received.
		    */
			int[] destinationsindex={(pos+1)%TOTAL_COUNT,(pos+2)%TOTAL_COUNT};
			String[] keys = {(String) storeRec.keySet().toArray()[destinationsindex[0]],
					(String) storeRec.keySet().toArray()[destinationsindex[1]]};
			String[] destinations = {storeRec.get(keys[0]),storeRec.get(keys[1])};
			Log.d(TAG,"sendRequestToDestNode - "+destinations[0]+destinations[1]);
			timer = new Timer();
		    timer.schedule(new sendRequestToDestNodeTimer(key,value,destinations[0],destinations[1]),200);
	 }
	 /*
	  * Replicate the stored key over next two successor nodes
	  */
	 public void replicateStore(String key,String value)
	 {
			int pos = 0;
			String queryString = "REP";
			Iterator<Entry<String, String>> iter = storeRec.entrySet().iterator();
			while(iter.hasNext())
			{
				Entry<String, String> map = (Entry<String, String>) iter.next();
				String dest = (String) map.getValue();
				if(dest.compareTo(myPort) == 0)
					break;	
				pos++;
			}
			int[] destinationsindex={(pos+1)%TOTAL_COUNT,(pos+2)%TOTAL_COUNT};
			String[] keys = {(String) storeRec.keySet().toArray()[destinationsindex[0]],
					(String) storeRec.keySet().toArray()[destinationsindex[1]]};
			String[] destinations = {storeRec.get(keys[0]),storeRec.get(keys[1])};
			String msg = queryString +":" + key +":" + value+":"+myPort;
			new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, queryString,destinations[0],msg);
			new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, queryString,destinations[1],msg);
			//delay(200);
	 }
	 /*
	  * Recover Nodes after failure
	  */
	 public void nodeRecover(String key,String key1,String queryString)
	 {
		 	Iterator<Entry<String, String>> iter = storeRec.entrySet().iterator();
		 	int index = 0,myindex=-1,index2;
		 	String[] values = new String[5];
		 	Entry<String, String> map;
	    	while(iter.hasNext())
			{
					map = (Entry<String, String>) iter.next();
					values[index] = (String) map.getValue();
					if(values[index].compareTo(myPort) == 0)
					{
						myindex = index;
					}	
					index++;
			}
	    	int[] destinations = {myindex-2,myindex-1,myindex+1,myindex+2};
	    	for(int i=0;i<4;i++)
	    	{
	    		int index1 = index2 = destinations[i];
	    		if(index1 < 0) index2 = 5+index1;
	    		else if(index1> 4) index2 = index1 - 4 -1;
	    		String key2 = (String) storeRec.keySet().toArray()[index2];
	    		String dest = storeRec.get(key2);
	    		String extra = null;
	    		if(index1 > myindex) extra = "SUCC";
	    		else extra = "PRED";
	    		String predNode = storeRec.get(predKey);
	    		String msg = queryString +":" + key +":"+myPort+":"+extra+":"+predNode;
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, queryString,dest,msg);
	    	}
	 }
	 /*
	  * Utility Function - to send results to destination nodes.
	  */
	 void sendResults(Cursor cursor,String sender,String responseString)
	 {
		 StringBuilder sbkey = new StringBuilder();
		 StringBuilder sbvalue = new StringBuilder();
	    	if(cursor != null)
			{
	    		cursor.moveToPosition(-1);
	    		while (cursor.moveToNext()){
	    			String key = cursor.getString(0);
	    			String value = cursor.getString(1);
	    			sbkey.append(key);sbkey.append("#");
	    			sbvalue.append(value);sbvalue.append("#");
	    		}
	    		
			}
	    	if(sbkey != null)
	    	{
	    		String msg = responseString+":"+sbkey.toString()+":"+sbvalue.toString()+":"+myPort;
		    	new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, responseString,sender,msg);
	    	}
	 }
	 /*
	  * Utility function for send results in case of recovery.
	  */
	 void sendResultsRec(Cursor cursor,String sender,String responseString,String id,String predKey2)
	 {
		 StringBuilder sbkey = new StringBuilder();
		 StringBuilder sbvalue = new StringBuilder();
		 StringBuilder sbcontext = new StringBuilder();
	    	if(cursor != null)
			{
	    		cursor.moveToPosition(-1);
	    		while (cursor.moveToNext()){
	    			String key = cursor.getString(0);
	    			String value = cursor.getString(1);
	    			String context = cursor.getString(2);
	    			if(id.equals("SUCC"))//this key should belong to me(sender)
	    			{
	    				int st1 = Integer.parseInt(sender);
	    				st1 = st1/2;
	    				String st2 = String.valueOf(st1);
	    				String nodeKey = provider.genHash(st2);
	    				String key1 = provider.genHash(key);
	    				st1 = Integer.parseInt(predKey2);
	    				st1 = st1/2;
	    				String st3 = String.valueOf(st1);
	    				String predKey1 = provider.genHash(st3);
	    				if( (key1.compareTo(predKey1) > 0 && key1.compareTo(nodeKey) <=0) ||
	    						( predKey1.compareTo(nodeKey) > 0 && ( (key1.compareTo(predKey1) > 0) || (key1.compareTo(nodeKey) <= 0) ) )
	    						)
	    				{
	    					sbkey.append(key);sbkey.append("#");
	    					sbvalue.append(value);sbvalue.append("#");
	    					sbcontext.append(context);sbcontext.append("#");
	    				}
	    			}
	    			else if(id.equals("PRED"))//this key belongs to them i.e. belong to receiver
	    			{
	    				String key1 = provider.genHash(key);
	    				if( (key1.compareTo(predKey) > 0 && key1.compareTo(nodeKey) <=0) ||
	    						( predKey.compareTo(nodeKey) > 0 && ( (key1.compareTo(predKey) > 0) || (key1.compareTo(nodeKey) <= 0) ) )
	    						)
	    				{
	    					sbkey.append(key);sbkey.append("#");
	    					sbvalue.append(value);sbvalue.append("#");
	    					sbcontext.append(context);	sbcontext.append("#");
	    				}
	    			}
	    		}
			}
	    	if(sbkey != null)
	    	{
	    		String msg = responseString +":"+sbkey.toString()+":"+sbvalue.toString()+":"+sbcontext.toString();
	    		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, responseString,sender,msg);
	    	}
	 }
	 /*
	  * Delete Keys over replicated over successor nodes
	  */
	 public void deleteRepKeys(String key)
	 {	
		 	int pos = 0;
		 	String queryString = "DEL";
		 	Iterator<Entry<String, String>> iter = storeRec.entrySet().iterator();
		 	while(iter.hasNext())
			{
				Entry<String, String> map = (Entry<String, String>) iter.next();
				String dest = (String) map.getValue();
				if(dest.compareTo(myPort) == 0)
					break;	
				pos++;
			}
		 	int[] destinations = {pos-2,pos-1,pos+1,pos+2};
	    	for(int i=0;i<4;i++)
	    	{
	    		int index2 = 0;
	    		int index1 = index2 = destinations[i];
	    		if(index1 < 0) index2 = 5+index1;
	    		else if(index1> 4) index2 = index1 - 4 -1;
	    		String key2 = (String) storeRec.keySet().toArray()[index2];
	    		String dest = storeRec.get(key2);
	    		String msg = queryString +":" + key +":"+myPort;
	    		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, queryString,dest,msg);
	    	}
	 }
	 /*
	  * Run node recovery to get all the needed keys from 2 successor/predecessor nodes
	  */
	 public void runNodeRecovery()
	 {
		String hash = provider.genHash("*");
		nodeRecover("*",hash,"RECOVERY");
	 }
	 /*
	  * Start timer after sending request to destination node
	  * If timer expires, then forward request to successor nodes.
	  */
	 class sendRequestToDestNodeTimer extends TimerTask 
	 {
			String key,value,dest,dest1;
			public sendRequestToDestNodeTimer(String key,String value,String dest,String dest1) {
		         this.key = key;
		         this.value = value;
		         this.dest = dest;
		         this.dest1 = dest1;
		     }
			  public void run() {
				String msg = "INSDUP" +":" + key +":" + value+":" + myPort;
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "INSDUP",dest,msg);
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "INSDUP",dest1,msg);
				timer.cancel();
				timer.purge();
			  }
	}
	 public void delay(int amount)
	 {
		 try {
			Thread.sleep(amount);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.simple_dynamo, menu);
		return true;
	}
}
