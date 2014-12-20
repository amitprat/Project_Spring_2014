package edu.buffalo.cse.cse486586.groupmessenger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.os.AsyncTask;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

@SuppressLint({ "NewApi", "UseSparseArrays" })
public class GroupMessengerActivity extends Activity {
	private String TAG = GroupMessengerActivity.class.getSimpleName();
	OnPTestClickListener obj;
	
	private static String[] PORTS = {"11108","11112","11116","11120","11124"};
	private byte[] IP_ADDR = {10,0,2,2};
	final static int SERVER_PORT = 10000;
	static final int TOTAL_COUNT = 5;
	static final int MAGIC_NO = 4;
	static final String SEQUENCER = PORTS[MAGIC_NO];
	private final String groupId1 = "GROUP_CHAT";
	private final String groupId2 = "GROUP_ORDER";
	private String myPort;
	private HashMap<Integer,String> listTotalOrder;
	private HashMap<Integer,String> holdBackQueue;
	private Map<Integer,String> senderHoldBackQueue;
	private int uniqueMessagesId = 0;
	private int[] vectorClock =new int[TOTAL_COUNT];
	private int[] seqClock =new int[TOTAL_COUNT];
	private int R_g = 0;
	private int S_g = 0;
	private String msg;
	private TextView tv;
	private EditText et;
	private ExecutorService executorService = Executors.newSingleThreadExecutor(); 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		listTotalOrder = new HashMap<Integer,String>();
		holdBackQueue = new HashMap<Integer, String>();
		senderHoldBackQueue = new HashMap<Integer,String>();
		setContentView(R.layout.activity_group_messenger);
		tv = (TextView) findViewById(R.id.textView1);
		et = (EditText) findViewById(R.id.editText1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        findViewById(R.id.button1).setOnClickListener(
               new OnPTestClickListener(tv, getContentResolver()));
		//to find the current running avd's port number
		TelephonyManager tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
	    myPort = String.valueOf((Integer.parseInt(portStr) * 2));
		try {
			ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
			new serverTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
		} catch (Exception e) {
			Log.e(TAG, "Can't create a ServerSocket");
			return;
		}
		findViewById(R.id.button4).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				msg = et.getText().toString();
				et.setText("");
				TextView local = (TextView) findViewById(R.id.local);
				local.append(msg+"\t");
				msg = msg + ":" + myPort;
				Log.v(TAG,"sending message from client");
				new clientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg,myPort);
				uniqueMessagesId = uniqueMessagesId + 1;
			}
		});
	}
class serverTask extends AsyncTask<ServerSocket,String, Void>
{
	List <Integer> list = new ArrayList <Integer> ();
	List <Integer> list1 = new ArrayList <Integer> ();
	@SuppressLint("UseSparseArrays")
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
				String[] splitArray = line.split(":");
				if(splitArray[4].equals(groupId1)) //splitArray[4] = groupId (CHAT/ORDER)
				{
					holdBackQueue.put(Integer.parseInt(splitArray[3]),line); //splitArray[3] = uniqueMessageId
					Log.d("DEBUG","holdBackQueue.put : " + Integer.parseInt(splitArray[3])+ line);
					if(splitArray[2].equals(SEQUENCER))//splitArray[1] = recieverPortNo as Group Identifier
					{
						Log.d("DEBUG", "Sequencer role");
						int l=0;
						while(!(PORTS[l].equals(splitArray[1])))
							l++; //sender position
						senderHoldBackQueue.put(l, line);
						for (Integer key : senderHoldBackQueue.keySet()) 
						{
							int j=0,k=0,flag=1;
							l = 0;
							String msg1 = senderHoldBackQueue.get(key);
							String[] temp = msg1.split("#");
							String[] splitArray0 = temp[0].split(":");
							String[] splitArray1 = temp[1].split(":");
							while(!(PORTS[l].equals(splitArray0[1])))
								l++;
							while(!(PORTS[j].equals(splitArray0[2])))
								j++;
							for(k=0;k<TOTAL_COUNT;k++)
							{
								if( (key != k) && (seqClock[k] < (Integer.parseInt(splitArray1[k]))))
								{
									flag = 0;
									break;
								}
							}
							if( (flag == 1) && (Integer.parseInt(splitArray1[key]) == (seqClock[key]+1)))
							{
								seqClock[key] += 1;
								list.add(key);
								executorService.submit(new ServiceRequest(cl_socket,  splitArray0[3],splitArray0[2], groupId2));
							}
						}
						ListIterator<Integer> it = list.listIterator();
						 if(it.hasNext()) {  
					            Integer item = it.next();
					            it.remove();
					            senderHoldBackQueue.remove(item);
					        }  
					}
					cl_socket.close();
				}
				else if(splitArray[4].equals(groupId2))
				{
					if(!holdBackQueue.isEmpty() && (msg = holdBackQueue.get(Integer.parseInt(splitArray[0])))!= null)
					{
						String[] arr = msg.split(":");
						msg = arr[0] + ":" + splitArray[0] +":" +splitArray[1];
						listTotalOrder.put(Integer.parseInt(splitArray[0]),msg);
						holdBackQueue.remove(Integer.parseInt(splitArray[0]));
						int flag = 1;
						while(flag ==1)
						{
							flag = 0;
							for (Integer key : listTotalOrder.keySet()) 
							{
								msg = listTotalOrder.get(key);
								arr = msg.split(":");
								if(Integer.parseInt(arr[2]) == R_g)
								{
									list1.add(Integer.parseInt(arr[1]));
									R_g = R_g + 1;
									publishProgress(arr[2],arr[0]);
									flag = 1;
								}
							}
						 ListIterator<Integer> it = list1.listIterator();
						 while(it.hasNext()) 
						 {  
					         Integer item = it.next();
					         it.remove();
					         listTotalOrder.remove(item);
					     } 
						}
					}
					
					cl_socket.close();
				}
			}
		}catch(Exception e)
		{
			Log.e(TAG, "Accept Failed!!"+e);
		}
		return null;
	}
	@Override
	protected void onProgressUpdate(String... values) {
		super.onProgressUpdate(values);
		obj = new OnPTestClickListener(tv, getContentResolver());
        obj.insertPair(values[0],values[1]);
		Log.d("Alert","message_publish : key = "+values[0]+ "value = " + values[1]);
		TextView remote = (TextView) findViewById(R.id.remote);
		remote.append(values[1]+"\n");
		TextView localTextView = (TextView) findViewById(R.id.local);
        localTextView.append("\n");
    }
}

class clientTask extends AsyncTask<String,Void,Void>
{
	@Override
	protected Void doInBackground(String... arg0) {
		int i = 0,w=0;
		String remotePort = "";
		String myPort = arg0[1];
		String msg_id = myPort + uniqueMessagesId;
        while(!(PORTS[w].equals(myPort)))
        	w++;
        vectorClock[w] += 1;
		while(i < TOTAL_COUNT)
		{
			remotePort = PORTS[i];
			String msg = "";
			msg = arg0[0];
			msg = msg + ":" + remotePort;
			msg = msg + ":" + msg_id + ":" + groupId1;
			msg += ":#";
			int u = 0;
			for(u=0;u<TOTAL_COUNT;u++)
				msg += vectorClock[u] + ":";
 			try {
				Socket newSocket = new Socket(InetAddress.getByAddress(IP_ADDR), Integer.parseInt(remotePort));
				PrintWriter out = new PrintWriter(new OutputStreamWriter(newSocket.getOutputStream()));
				out.write(msg);
				out.flush();
				newSocket.close();
			} catch (UnknownHostException e) {
				Log.e(TAG, "Unknown socket IOException"+e);
				e.printStackTrace();
			}catch (IOException e) {
				Log.e(TAG, "ClientTask socket IOException"+e);
			}
			i++;
		}
		return null;
	}
}
class ServiceRequest implements Runnable {
	String msg;
	String type;
	String recvPort;
	String sendMsg;
	int seq_no;
    public ServiceRequest(Socket connection,String msg,String port,String msg1) {
        this.msg = msg;
        this.type = msg1;
        this.recvPort = port;
        this.seq_no = S_g; 
        S_g = S_g + 1;
    }
    public void run() {
    	int i = 0;
		String remotePort;
		while(i < TOTAL_COUNT)
		{
			sendMsg = "";
			sendMsg = msg + ":" + seq_no + ":" +recvPort + ":" +0 +":" +type; //msg:SeqNo:recvPort:0:groupId:0 
			remotePort = PORTS[i];
 			try {
				Socket newSocket = new Socket(InetAddress.getByAddress(IP_ADDR), Integer.parseInt(remotePort));
				PrintWriter out = new PrintWriter(new OutputStreamWriter(newSocket.getOutputStream()));
				Log.d("DEBUG","Message Send By Sequencer : " + sendMsg);
				out.write(sendMsg);
				out.flush();
				newSocket.close();
			} catch (UnknownHostException e) {
				Log.e(TAG, "Unknown socket IOException");
				e.printStackTrace();
			}catch (IOException e) {
				Log.e(TAG, "ClientTask socket IOException");
			}
			i++;
		}
    }        
  }
}