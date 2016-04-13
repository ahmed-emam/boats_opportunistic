package com.example.aemam.boatsframework_opp_tcp_singlethreaded;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.location.Location;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener{
    private static BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private static final String uuid = "e110cf10-7866-11e4-82f8-0800200c9a66";

    public static String TAG = "boats_framework";
    public static final int  MAX_CONNECTIONS = 1024;
    MainActivity mainActivity;
    public OutputStream commandCenterOutputStream;
    private static FileOutputStream locationFile;

    private long timeStampOfLocationUpdate = 0;


    public int experiment_id = 0;
    public int prev_experiment_id = -1;
    public boolean experimentRunning = false;

    public static final int TOAST_MSG = 0;
    public static final int TOAST_MSG_SHORT = 1;
    public static final int TEXT_MSG = 2;

    //The radius of the threshold area surrounding the point of interest
    public static final float thresholdDistance = 5.0f;
    public static  double firstLocationLat = 0.0;
    public static  double firstLocationLon = 0.0;
    public static  double SecondLocationLat = 0.0;
    public static  double SecondLocationLon = 0.0;
    private static float slowingDownSpeed = 0.75f;

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 800;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    protected LocationRequest mLocationRequest;

    /**
     * Represents a geographical location.
     */

    protected Location mCurrentLocation = null;


    public String[] device_Wifi_adresses = {
            "D8:50:E6:83:D0:2A",
            "D8:50:E6:83:68:D0",
            "D8:50:E6:80:51:09",
            "24:DB:ED:03:47:C2",
            "24:DB:ED:03:49:5C",
            "8c:3a:e3:6c:a2:9f",
            "8c:3a:e3:5d:1c:ec",
            "c4:43:8f:f6:3f:cd",
            "f8:a9:d0:02:0d:2a",
            "10:bf:48:ef:e9:c1",
            "30:85:a9:60:07:3b"
    };


    public String[] device_ip_adresses = {
            "",
            "",
            "",
            "",
            "",
            "10.0.0.1",
            "10.0.0.2",
            "10.0.0.3",
            "10.0.0.4",
            "",
            ""
    };

    public String[] command_center_bt_adresses = {
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "24:DB:ED:03:47:F3",        //192.168.1.124
            "24:DB:ED:03:4F:BB",        //192.168.1.125
            "",
            ""
    };

    //    Handler activitiesCommHandler;
    Handler timedTasks;

    //    static ConnectionThread[] connectionThreads = new ConnectionThread[MAX_CONNECTIONS];

    volatile ConnectionThread[] connectionThreads = new ConnectionThread[MAX_CONNECTIONS];
    public boolean[] alivePeers = new boolean[MAX_CONNECTIONS];
    public int[] num_heartBeats = new int[MAX_CONNECTIONS];

    public static int DEVICE_ID = 0;                                   //This device's ID
    public int proxy = 8;
    public int sender = 9;
    public int sink = 7;
    private static FileOutputStream logfile;
    private static FileOutputStream packetlogfile;
    public static String rootDir = Environment.getExternalStorageDirectory().toString() +
            "/MobiBots/boats_opportunistic/tcp/";

    private WifiAdhocServerThread serverThread;
    private WifiBroadcast_client broadcast_client;
    private WifiBroadcast_server broadcast_server;
    private CommandsThread commandsThread;
    private BluetoothThread bluetoothThread;

    public int port = 7777;
    //    private ReadingThread readingThread;
//    private WorkerThread workerThread;
//    private WritingThread writingThread;
    public volatile long received_bytes = 0;
    public volatile long sent_bytes = 0;
    public DatagramSocket broadcastSocket = null;
    public Object synchronizeBytes = new Object();



    Ringtone alarmRingtone;
    Ringtone startExperimentRingtone;
    Ringtone endExperimentRingtone;

    public int findDevice_IpAddress(String ipAddress){
        for (int i = 0; i < device_ip_adresses.length; i++) {
            if (device_ip_adresses[i].equalsIgnoreCase(ipAddress)) {
                return (i + 1);
            }
        }
        return -1;
    }
    /**
     * Return device's ID mapping according to the device's wifi mac address
     * @param deviceAddress     Bluetooth Address
     * @return                  device ID
     */
    public int macAddress_to_deviceID(String deviceAddress) {
        for (int i = 0; i < device_Wifi_adresses.length; i++) {
            if (device_Wifi_adresses[i].equalsIgnoreCase(deviceAddress)) {
                return (i + 1);
            }
        }
        return -1;
    }

    public synchronized String getTimeStamp() {
        return (new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS").format(new Date()));
    }

    /**
     * Function that write 'message' to the log file
     * @param message
     */
    public synchronized void logLocation(String message) {
        StringBuilder log_message = new StringBuilder(26 + message.length());
        log_message.append(getTimeStamp());
        log_message.append("\t");
        log_message.append(System.currentTimeMillis());
        log_message.append("\t");
        log_message.append(message);
        log_message.append("\n");


        try {
            locationFile.write(log_message.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    /**
     * Function that write 'message' to the log file
     * @param message
     */
    public synchronized void log(String message) {
        StringBuilder log_message = new StringBuilder(26 + message.length());
        log_message.append(getTimeStamp());
        log_message.append("\t");
        log_message.append(System.currentTimeMillis());
        log_message.append("\t");
        log_message.append(message);
        log_message.append("\n");


        try {
            logfile.write(log_message.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    /**
     * Print out debug messages if "D" (debug mode) is enabled
     * @param message
     */
    public void debug(String message) {

        Log.d(TAG, message);

        Message toastMSG = mainActivity.UIHandler.obtainMessage(TEXT_MSG);
        byte[] toastMSG_bytes =  (message).getBytes();
        toastMSG.obj = toastMSG_bytes;
        toastMSG.arg1 = toastMSG_bytes.length;
        mainActivity.UIHandler.sendMessage(toastMSG);
    }


    /**
     * TODO: Don't forget to change this to a static Handler to prevent memory leak
     */
    public final Handler UIHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TOAST_MSG:
                    byte[] message = (byte[]) msg.obj;
                    String theMessage = new String(message, 0, msg.arg1);
                    Toast.makeText(getApplicationContext(), theMessage, Toast.LENGTH_LONG).show();
                    break;
                case TOAST_MSG_SHORT:
                    message = (byte[]) msg.obj;
                    theMessage = new String(message, 0, msg.arg1);
                    Toast.makeText(getApplicationContext() , theMessage,Toast.LENGTH_SHORT).show();
                    break;
                case TEXT_MSG:
                    TextView view = (TextView) findViewById(R.id.textView2);

                    message = (byte[]) msg.obj;
                    theMessage = new String(message, 0, msg.arg1);
                    theMessage += "\n";

                    view.append(theMessage);
                    break;
            }
        }
    };

    public void dump_tcp(){
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HH:mm:ss").format(new Date());
            String dir_timeStamp = new SimpleDateFormat("yyyy_MM_dd").format(new Date());
            String command = "tcpdump -s 0 -v -w "+ rootDir+dir_timeStamp + "/"+timeStamp+".pcap\n";
            debug(command);

            // Log.e("command",command);
            os.writeBytes(command);
            os.flush();
            //      os.writeBytes("exit\n");

            //      os.flush();
            //     process.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        catch (InterruptedException e) {
//            e.printStackTrace();
//        }

    }

    /**
     * Create the log file
     */
    private void initLogging() {
        File mediaStorageDir = new File(rootDir);
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "failed to create directory");
            }
        }
        String dir_timeStamp = new SimpleDateFormat("yyyy_MM_dd").format(new Date());

        mediaStorageDir = new File(rootDir  + dir_timeStamp);
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "failed to create directory");
            }
        }




        String timeStamp = new SimpleDateFormat("yyyyMMdd_HH:mm:ss").format(new Date());
        File file = new File(rootDir  + dir_timeStamp + "/" + "device_" +DEVICE_ID + "_" +timeStamp + ".txt");

        Log.d(TAG, file.getPath());
        try {
            file.createNewFile();
            logfile = new FileOutputStream(file);
            int stringId = this.getApplicationInfo().labelRes;
            log("App name: "+this.getString(stringId));

            String[] cmd = new String[] { "logcat","-c" };
            Runtime.getRuntime().exec(cmd);

            // dump_tcp();

            cmd = new String[] { "logcat", "-f",
                    rootDir  + dir_timeStamp + "/"+this.getString(stringId)+"_"+timeStamp, "-v", "time",  "boatsframework_opp_tcp:V" };
            Runtime.getRuntime().exec(cmd);




            file = new File(rootDir  + dir_timeStamp+ "/" + "location.txt");
            if (!file.exists())
                file.createNewFile();
            locationFile = new FileOutputStream(file, true);
            String header = "Lat\tLong\tAltitude\tSpeed\tBearing\tDistance\n";
            locationFile.write(header.getBytes());


            file = new File(rootDir  + dir_timeStamp + "/" + "packets_device_" +DEVICE_ID + "_" +timeStamp);
            if (!file.exists()) {
                file.createNewFile();
                packetlogfile = new FileOutputStream(file, true);
                header = "Time\t\tMilliseconds\tSource_IP\tDestination_IP\tSource\tDestination\tpacket_size\tpacket_type\texperiment_id\n";
                packetlogfile.write(header.getBytes());

            }
            else
                packetlogfile = new FileOutputStream(file, true);




        } catch (IOException e) {
            e.printStackTrace();
        }
    }


//    /**
//     * Synchronized function to remove the thread responsible for communication with nodeID "node"
//     * @param node  nodeID
//     */
//    public synchronized void removeNode(int node){
//        debug("Disconnecting from "+node);
//        log("Disconnecting from "+node);
//        try{
//            if(alarmRingtone.isPlaying())
//                alarmRingtone.stop();
//            alarmRingtone.play();
//            timedTasks.postDelayed(stopAlarm, 4000);
//        }
//        catch (Exception e){
//            e.printStackTrace();
//        }
////        removeRoutesAssociatedWithNode(node);
//
////        connectionThreads[node-1] = null;
//    }

    /**
     * Synchronized function to remove the thread responsible for communication with nodeID "node"
     * @param node  nodeID
     */
    public synchronized void removeNode(int node){
        debug("Disconnected from "+node);
        log("Disconnected from "+node);
        try{
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

            if(alarmRingtone.isPlaying())
                alarmRingtone.stop();
            alarmRingtone.play();
            timedTasks.postDelayed(stopAlarm, 4000);
        }
        catch (Exception e){
            e.printStackTrace();
        }

        connectionThreads[node-1] = null;
    }

    /**
     * Synchronized function to add the thread responsible for communication with nodeID "node"
     * @param thread    Communication thread
     * @param node      nodeID
     */
    public synchronized void addNode(ConnectionThread thread,int node){
        debug("Connected to "+node);
        log("Connected to "+node);
        try{
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();

        }
        catch (Exception e){
            e.printStackTrace();
        }
//        addRoute(node, new Route(node, 1));
        connectionThreads[node-1] = thread;
//        alivePeers[node-1] = true;
    }
//

//    /**
//     * Synchronized function to add the thread responsible for communication with nodeID "node"
//     * @param thread    Communication thread
//     * @param node      nodeID
//     */
//    public synchronized void addNode(ConnectionThread thread,int node){
//
//        try{
//            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
//            r.play();
//
//        }
//        catch (Exception e){
//            e.printStackTrace();
//        }
//        connectionThreads[node-1] = thread;
//    }

    Runnable startAdvertising = new Runnable() {
        @Override
        public void run() {

        }
    };
    Runnable stopAlarm = new Runnable() {
        @Override
        public void run() {
            if(alarmRingtone.isPlaying())
                alarmRingtone.stop();
        }
    };
    Runnable stopStartExperimentRingtone = new Runnable() {
        @Override
        public void run() {
            if(startExperimentRingtone.isPlaying())
                startExperimentRingtone.stop();
        }
    };
    Runnable stopEndExperimentRingtone = new Runnable() {
        @Override
        public void run() {
            if(endExperimentRingtone.isPlaying())
                endExperimentRingtone.stop();
        }
    };
//    Runnable changeRoutingTable = new Runnable() {
//        @Override
//        public void run() {
//            PriorityQueue<Route> queue = routeTable.get(9);
//            for(Route route : queue){
//                if(route.getNextHop() == 9)
//                    queue.remove(route);
//            }
//        }
//    };
//    Runnable startStreaming = new Runnable() {
//        @Override
//        public void run() {
//            connectionThreads[8].sendStream(9);
//        }
//    };
//    Runnable stopStreaming = new Runnable() {
//        @Override
//        public void run() {
//            connectionThreads[8].stopStream();
//        }
//    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainActivity = this;
        alarmRingtone = RingtoneManager.getRingtone(this,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
        startExperimentRingtone = RingtoneManager.getRingtone(this,
                Uri.fromFile(new File(Environment.getExternalStorageDirectory().toString() +
                        "/Ringtones/very_loud.mp3")));

        endExperimentRingtone = RingtoneManager.getRingtone(this,
                Uri.fromFile(new File(Environment.getExternalStorageDirectory().toString() +
                        "/Ringtones/loud.mp3")));


        WifiManager wifiMgr = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();

        DEVICE_ID = macAddress_to_deviceID(wifiInfo.getMacAddress());

        initLogging();
        debug("App Started");


        if(checkIBSSMode()){
            debug("You are in IBSS mode");
        }
        else{
            try{
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
            }
            catch (Exception e){
                e.printStackTrace();
            }

            debug("NOT in IBSS mode");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            finish();
        }



        timedTasks = new Handler();

        if(serverThread == null) {
            debug("Initializing adhoc server thread");
            serverThread = new WifiAdhocServerThread(this);
            serverThread.start();
        }
        if(broadcast_server == null) {
            debug("Initializing broadcast server thread");
            broadcast_server = new WifiBroadcast_server();
            broadcast_server.start();
        }

        if(broadcast_client == null) {
            debug("Initializing connecting to peer thread");
            broadcast_client = new WifiBroadcast_client();
            broadcast_client.start();
        }

        if(bluetoothThread == null && (DEVICE_ID == sender || DEVICE_ID == proxy)) {
//            debug("Initializing adhoc server thread");
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(command_center_bt_adresses[DEVICE_ID-1]);
            bluetoothThread = new BluetoothThread(device);
            bluetoothThread.start();
        }

//        commandsThread = new CommandsThread();
//        commandsThread.start();


//        if(DEVICE_ID == 7) {
//            timedTasks.postDelayed(startStreaming, (40000));
//            timedTasks.postDelayed(changeRoutingTable, (30000 * 2));
//            timedTasks.postDelayed(stopStreaming, (20000*4));
//        }
        TextView view = (TextView) findViewById(R.id.textView2);
        view.setMovementMethod(new ScrollingMovementMethod());
        buildGoogleApiClient();
    }

    public void startCommunicationThreads(int nodeID, Socket socket,
                                          InputStream inputStream, OutputStream outputStream,
                                          MainActivity mainActivity){
//        int socket_read_timeout = 500; //500 ms
//        try {
//            socket.setSoTimeout(socket_read_timeout);
//        }catch (SocketException e) {
//            e.printStackTrace();
//        }

        ConnectionThread connectionThread = new ConnectionThread(inputStream, outputStream, nodeID,
                mainActivity, socket, packetlogfile, logfile);
//        threadpool.execute(connectionThread);
        connectionThread.start();
        addNode(connectionThread, nodeID);


    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);



    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Hook up to the GPS system
        mGoogleApiClient.connect();

    }
    /**
     * Removes location updates from the FusedLocationApi.
     */
    protected void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mGoogleApiClient.isConnected())
            startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected())
            stopLocationUpdates();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }
/*
    public int approachEndPoints(Location currentLocation){
        float results[] = new float[5];
        log("\tCurrent\t"+currentLocation.getLatitude()+"\t"+currentLocation.getLongitude());
//        debug("\tCurrent \t"+currentLocation.getLatitude()+"\t"+currentLocation.getLongitude());

        Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(),
                firstLocationLat, firstLocationLon, results);
        debug("Distance to "+firstLocationLat+","+firstLocationLon+":"+results[0]);
        log("\tFirst\t"+firstLocationLat+"\t"+firstLocationLon+"\t"+results[0]);
        sendToCommandCenter("First\t"+results[0]);
        if(results[0] <= thresholdDistance)
            return 1;

        Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(),
                SecondLocationLat, SecondLocationLon, results);
        log("\tSecond\t"+SecondLocationLat+"\t"+SecondLocationLon+"\t"+results[0]);
        debug("Distance to "+SecondLocationLat+","+SecondLocationLon+":"+results[0]);
        sendToCommandCenter("Second\t"+results[0]);
        if(results[0] <= thresholdDistance)
            return 2;

        return 0;
    }
    */


    @Override
    public void onConnected(Bundle bundle) {
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        debug("Current Location " + location);

        logLocation(location.toString());

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        debug("connection with google api client failed");
    }


    public void change_first_point(View v){
        if(mCurrentLocation != null) {
            firstLocationLat = mCurrentLocation.getLatitude();
            firstLocationLon = mCurrentLocation.getLongitude();
            log("Changed first point to "+firstLocationLat+","+firstLocationLon);
            debug("Changed first point to "+firstLocationLat+","+firstLocationLon);
        }
    }
    public void change_second_point(View v){
        if(mCurrentLocation != null) {
            SecondLocationLat = mCurrentLocation.getLatitude();
            SecondLocationLon = mCurrentLocation.getLongitude();
            log("Changed first point to "+SecondLocationLat+","+SecondLocationLon);
            debug("Changed first point to "+SecondLocationLat+","+SecondLocationLon);
        }
    }
    public void startExperiment(){
        if (startExperimentRingtone.isPlaying())
            startExperimentRingtone.stop();
        startExperimentRingtone.play();
        timedTasks.postDelayed(stopStartExperimentRingtone, 4000);


//        workerThread.sendStream(7);
        if(DEVICE_ID == 9) {
            experimentRunning = true;
            experiment_id++;

            received_bytes = 1;
            sent_bytes = 0;
//                workerThread.sendStream(nodeID);
        }
        log("Starting Experiment "+experiment_id);
        debug("Starting Experiment "+experiment_id);
        sendToCommandCenter("[Device "+DEVICE_ID+"] Starting Experiment "+experiment_id);

    }
    public void endExperiment(){
        experimentRunning = false;

        if (endExperimentRingtone.isPlaying())
            endExperimentRingtone.stop();
        endExperimentRingtone.play();
        timedTasks.postDelayed(stopEndExperimentRingtone, 4000);


        for(int i = 0; i < connectionThreads.length; i++){
            if(connectionThreads[i] != null){
                debug("closing connection with "+(i+1));
                connectionThreads[i].cancel();

            }
        }


        log("Ending Experiment "+experiment_id+" last RX: "+received_bytes+" sent "+sent_bytes+" bytes");
        log("Ending Experiment "+experiment_id);
        debug("Ending Experiment "+experiment_id+" last RX: "+received_bytes+" sent "+sent_bytes+" bytes");
        sendToCommandCenter("[Device "+DEVICE_ID+"] Ending Experiment "+experiment_id);

        received_bytes = 0;
        sent_bytes = 0;


//        int nextHop = findRoute(7);
//        debug("Stooping stream to "+7+" via "+nextHop);
//        if(nextHop != -1) {
//            WorkerThread thread = mainActivity.getWorkingThread(nextHop);
//            if(thread != null){
//                debug(thread.deviceID+" is stopping stream");
//                thread.stopStream();
//            }
//        }
//
//        for(int i = 0; i < connectionThreads.length; i++){
//            if(connectionThreads[i] != null){
//                connectionThreads[i].cancel();
//            }
//        }
    }


    @Override
    protected void onDestroy() {

        debug("App Destroyed");
        // Disconnect from GPS updates
//        LocationManager gps;
//        gps = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
//        gps.removeUpdates(locationListener);
//        serverThread.cancel();
//        broadcast_client.cancel();
//        broadcast_server.cancel();
//        commandsThread.cancel();

        super.onDestroy();
    }


    /**
     * This is a hack to check if IBSS mode is enabled or not
     * @return True     if IBSS is on
     *         False    Otherwise
     */
    public boolean checkIBSSMode(){
        Process su = null;
        DataOutputStream stdin = null ;
        try {
            su = Runtime.getRuntime().exec(new String[]{"su", "-c", "system/bin/sh"});
            stdin = new DataOutputStream(su.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            stdin.writeBytes("iw dev wlan0 info | grep type | awk {'print $2'}\n");
            InputStream stdout = su.getInputStream();
            byte[] buffer = new byte[1024];

            //read method will wait forever if there is nothing in the stream
            //so we need to read it in another way than while((read=stdout.read(buffer))>0)
            int read = stdout.read(buffer);
            String out = new String(buffer, 0, read);

            if(out.contains("IBSS")){

                return true;
            }else{
                return false;
            }

        }catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    public void sendToCommandCenter(String msg){
        if(commandCenterOutputStream != null)
            try {
                commandCenterOutputStream.write((msg + "\n\r").getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    Runnable stopStream = new Runnable() {
        @Override
        public void run() {
//            received_bytes = 0;
            debug("Stopped stream from mainactivity");
            endExperiment();
        }
    };

    private class SendFileRunnable implements Runnable{
        int nodeID;
        public SendFileRunnable(int nodeID){
            this.nodeID = nodeID;
        }
        @Override
        public void run() {


//            if(DEVICE_ID == 9) {
//                received_bytes = 1;
            startExperiment();
//            timedTasks.postDelayed(stopStream, 50000);
//                workerThread.sendStream(nodeID);
//            }
//            int nextHop = findRoute(nodeID);
//            debug("Sending file to "+nodeID+" via "+nextHop);
//            if(nextHop != -1) {
//                WorkerThread thread = mainActivity.getWorkingThread(nextHop);
//                if(thread != null){
//                    thread.sendFile(nodeID, "5MB");
//                }
//            }
        }
    }

    public class CommandsThread extends Thread{

        ServerSocket serverSocket = null;
        boolean serverOn = true;
        Socket client = null;

        public CommandsThread() {

            try {
                serverSocket = new ServerSocket(8888);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void writeInstructions(OutputStream outputStream){
            String out = "\t\t*****WELCOME to the command center for Scenario 2 Experiment*****\n" +
                    "Usage: <command>\n" +
                    "command\tDescription\n" +
                    "send <NodeID> \tSend Stream to <NodeID>\n" +
                    "stop\tStop Stream of data\n" +
                    "remove <DestID> <NextHop>\tRemove route (DestID, NextHop) from routing table\n" +
                    "\n\r";
            try {
                outputStream.write(out.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
//            timedTasks.postDelayed(printRoutingTable, 2000);
            while(serverOn){
                try {
                    client = serverSocket.accept();
                    Log.d(TAG, "Connected to: " + client.getInetAddress().getHostAddress());
                    Log.d(TAG, "Connected to Local Address: " + client.getLocalAddress().getHostAddress());


                    commandCenterOutputStream= client.getOutputStream();
                    //            writeInstructions(commandCenterOutputStream);

                    InputStream inputStream = client.getInputStream();
                    int len;
                    byte[] buf = new byte[1024];
                    commandCenterOutputStream.write("Type in your command: ".getBytes());
                    while((len = inputStream.read(buf)) > 0){
                        String newCommand = new String(buf, 0, len);
                        Log.d(TAG, "COMMAND: "+newCommand);
//                        sendToCommandCenter("Your command was: "+newCommand);
                        Scanner lineScanner = new Scanner(newCommand);

                        if(newCommand.contains("send")){
                            lineScanner.next();
//                            if(lineScanner.hasNextInt()) {
//                                int node = lineScanner.nextInt();
//                            lineScanner.next();
//                            SendFileRunnable sendFile = new SendFileRunnable(7);
//                            Thread thread = new Thread(sendFile);
//                            thread.start();
                            startExperiment();
//                            }
//                            else{
//                                sendToCommandCenter("Invalid command");
//                                lineScanner.nextLine();
//                            }

                        }
                        else if(newCommand.contains("first")){
                            lineScanner.next();
                            if(lineScanner.hasNextDouble())
                                firstLocationLat = lineScanner.nextDouble();
                            else{
                                //                            sendToCommandCenter("Insert <lat>");
                                lineScanner.nextLine();
                                continue;
                            }
                            if(lineScanner.hasNextDouble())
                                firstLocationLon = lineScanner.nextDouble();
                            else{
                                //                              sendToCommandCenter("Insert <lon>");
                                lineScanner.nextLine();
                                continue;
                            }
                            debug("New Location for first: "+firstLocationLat+","+firstLocationLon);
                        }
                        else if(newCommand.contains("second")){
                            lineScanner.next();
                            if(lineScanner.hasNextDouble())
                                SecondLocationLat = lineScanner.nextDouble();
                            else{
                                //                          sendToCommandCenter("Insert <lat>");
                                lineScanner.nextLine();
                                continue;
                            }
                            if(lineScanner.hasNextDouble())
                                SecondLocationLon = lineScanner.nextDouble();
                            else{
                                //                        sendToCommandCenter("Insert <lon>");
                                lineScanner.nextLine();
                                continue;
                            }
                            debug("New Location for second: "+SecondLocationLat+","+SecondLocationLon);
                            float[] result = new float[5];
                            Location.distanceBetween(firstLocationLat, firstLocationLon,
                                    SecondLocationLat, SecondLocationLon, result);
                            debug("Distance between two points: "+result[0]);
                        }
                        else if(newCommand.contains("stop")){
                            lineScanner.next();
//                            debug("!!!!!!!!!!!!STOP STREAM!!!!!!!!!!!!");
//                            received_bytes = 0;
                            endExperiment();
//                                    workerThread.stopStream();
                        }
                        else if(newCommand.contains("sync")){
                            lineScanner.next();
                            String messageStr = new SimpleDateFormat("yyyyMMdd.HHmmss").format(new Date());
                            byte[] messageByte = messageStr.getBytes();

                            InetAddress group = InetAddress.getByName("10.0.0.255");
                            DatagramPacket packet = new DatagramPacket(messageByte, messageByte.length, group, 5555);
                            debug("Broadcasting "+messageStr);
                            broadcastSocket.send(packet);
                        }

                        else{
                            debug("Invalid command");
                            commandCenterOutputStream.write((lineScanner.nextLine() +
                                    "\tNOT SUPPORTED\n\r").getBytes());
                        }

                        commandCenterOutputStream.write("Type in your command: ".getBytes());
                    }
                }catch(IOException e){
                    cancel();
                    e.printStackTrace();
                }
            }
            debug("Stopped command center");
        }


        public void cancel(){
            try {
                if(client!=null) {
                    client.close();
                    client = null;
                }
                else {
                    serverOn = false;
                    serverSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class BluetoothThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        InputStream bluetoothInputStream;
        public BluetoothThread(BluetoothDevice device){
            BluetoothSocket tmp = null;
            mmDevice = device;

            try{
                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString(uuid));
            }catch(IOException e){
                Log.e(TAG, e.getMessage());
            }
            mmSocket = tmp;
        }

        public void run(){
            Log.i(TAG, "BEGIN mConnectThread "+this);
            mBluetoothAdapter.cancelDiscovery();

            try{
                mmSocket.connect();
                debug("Client: Connected");
                InputStream inputStream = mmSocket.getInputStream();
                commandCenterOutputStream = mmSocket.getOutputStream();
                int len = 0;
                int size = 1024; //5 MB
                byte[]buf = new byte[size];

                while((len = inputStream.read(buf)) > 0){
                    String newCommand = new String(buf, 0, len);
                    Log.d(TAG, "COMMAND: "+newCommand);
                    //         sendToCommandCenter("Your command was: "+newCommand);
                    Scanner lineScanner = new Scanner(newCommand);

                    if(newCommand.contains("send")){
                        lineScanner.next();
                        startExperiment();
                    }
                    else if(newCommand.contains("stop")){
                        lineScanner.next();
                        endExperiment();
                    }
                    else if(newCommand.contains("timesync")){
                        lineScanner.next();
                        changeSystemTime(lineScanner.next());
                    }
                    else{
                        debug("Invalid command");
//                                commandCenterOutputStream.write((lineScanner.nextLine() +
//                                        "\tNOT SUPPORTED\n\r").getBytes());
                    }
                }


                Log.d(TAG, "Client: Connected");
            }catch(IOException connectException){
                connectException.printStackTrace();
                try{
                    mmSocket.close();
                }catch(IOException closeException){
                    closeException.printStackTrace();
                }

                Log.d(TAG, "Failed connecting");
//                connect_to_device();

                return;
            }


//            manageConnectedSocket(mmSocket);

            Log.d(TAG,"End of ConnectThread "+this);
        }
        public void cancel(){
            try{
                mmSocket.close();
            }catch(IOException e){}
        }
    }



//    public class BluetoothThread extends Thread{
//        private final BluetoothServerSocket mmServerSocket;
//        boolean serverOn = true;
//        long timeStart = 0;
//        long timeEnd = 0;
//        BluetoothSocket client = null;
//
//        public BluetoothThread() {
//            BluetoothServerSocket tmp = null;
//
//            try {
//                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("Test", UUID.fromString(uuid));
//            } catch (IOException e) {
//                Log.e(TAG, e.getMessage());
//            }
//            mmServerSocket = tmp;
//        }
//
//        public void run() {
//            while (serverOn) {
//
//                try {
//                    Log.d(TAG, "SERVER: waiting for connection");
//                    client = mmServerSocket.accept();
//
//
//
//                } catch (IOException e) {
//                    Log.e(TAG, e.getMessage());
//                    continue;
//                }
//
//                if (client != null) {
//
//
//                    try {
//
//                        timeStart = 0;
//                        timeEnd = 0;
//
//
//                        InputStream inputStream = client.getInputStream();
//                        commandCenterOutputStream = client.getOutputStream();
//                        int len = 0;
//                        int size = 1024; //5 MB
//                        byte[]buf = new byte[size];
//
//                        while((len = inputStream.read(buf)) > 0){
//                            String newCommand = new String(buf, 0, len);
//                            Log.d(TAG, "COMMAND: "+newCommand);
//                            //         sendToCommandCenter("Your command was: "+newCommand);
//                            Scanner lineScanner = new Scanner(newCommand);
//
//                            if(newCommand.contains("send")){
//                                lineScanner.next();
//
////                                startExperiment();
//
//                            }
//
//                            else if(newCommand.contains("stop")){
//                                lineScanner.next();
//
////                                endExperiment();
//                            }
//
//                            else{
//                                debug("Invalid command");
////                                commandCenterOutputStream.write((lineScanner.nextLine() +
////                                        "\tNOT SUPPORTED\n\r").getBytes());
//                            }
//                        }
//
//
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                        cancel();
//                    }
//                }
//            }
//            Log.d(TAG, "End of BT_ServerThread " + this);
//        }
//
//        public void closeSocket(){
//            try {
//                client.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        public void cancel() {
//            Log.d(TAG, "Closing the socket");
//            try {
//                serverOn = false;
//                mmServerSocket.close();
//            } catch (IOException e) {
//            }
//        }
//    }


    public class WifiAdhocServerThread extends Thread {
        ServerSocket serverSocket = null;
        boolean serverOn = true;

        public WifiAdhocServerThread(MainActivity activity) {

            try {
                serverSocket = new ServerSocket(8000);

                Log.i("TelnetServer", "ServerSocket Address: " + serverSocket.getLocalSocketAddress());

            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            debug("Started connecting to peers");
            while (serverOn) {
                try {
                    Log.d(TAG, "Server Thread");
                    Log.i(TAG, "Started: " + serverSocket.getLocalSocketAddress());
                    Socket client = serverSocket.accept();
                    client.setKeepAlive(false);
                    Log.d(TAG, "Connected to: " + client.getInetAddress().getHostAddress());
                    Log.d(TAG, "Connected to Local Address: " + client.getLocalAddress().getHostAddress());


                    int deviceId = findDevice_IpAddress(client.getInetAddress().getHostAddress());

                    debug("SERVER Connected to "+deviceId+" "+client.getInetAddress().getHostAddress());
                    if(connectionThreads[deviceId-1] != null){
                        connectionThreads[deviceId-1].cancel();
                        //removeNode(deviceId);
                    }
                    //       int size = 9999999;
                    //       client.setReceiveBufferSize(size);
                    startCommunicationThreads(deviceId, client,
                            client.getInputStream(), client.getOutputStream(), mainActivity);

                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
            debug("Stopped connecting to peers");
        }
        public void cancel(){
            try {
                serverOn = false;
                serverSocket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void printConnectedNodes(){
//        for(int i = 0; i < connectionThreads.length; i++){
//            if(connectionThreads[i] != null){
//                debug("Connected to "+(i+1));
//                //sendToCommandCenter("Connected to "+(i+1));
//            }
//        }
        //   debug("RX:"+received_bytes);
    }


    public class WifiAdhocClientThread extends Thread {
        String hostAddress;
        DataInputStream inputStream;
        DataOutputStream outputStream;
        int device_Id;


        public WifiAdhocClientThread(
                String host) {
//            mainActivity = activity;
            hostAddress = host;
        }

        @Override
        public void run() {
            /**
             * Listing 16-26: Creating a client Socket
             */
            int timeout = 500;
            int port = 8000;

            Socket socket = new Socket();
            try {
                device_Id = findDevice_IpAddress(hostAddress);
                debug("Connecting to " + device_Id + " @ " + hostAddress);
                socket.bind(null);
                socket.connect((new InetSocketAddress(hostAddress, port)), 500);

                debug("[CLIENT] Connected to "+device_Id+" "+hostAddress);

                //      int size = 9999999;
                //     socket.setReceiveBufferSize(size);
                startCommunicationThreads(device_Id, socket,
                        socket.getInputStream(), socket.getOutputStream(), mainActivity);

//                otherConnection = new ConnectionThread(mainActivity, socket.getInputStream(),
//                        socket.getOutputStream(), true, device_Id);
//                otherConnection.start();
//                addNode(otherConnection, device_Id);

            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            }

        }
    }


    private class WifiBroadcast_server extends Thread{
        int port = 5555;
        boolean engineOn = true;
        public WifiBroadcast_server(){
            engineOn = true;
            if(broadcastSocket == null)
                try {
                    broadcastSocket = new DatagramSocket(port);
                    broadcastSocket.setBroadcast(true);
                } catch (SocketException e) {
                    e.printStackTrace();
                }
        }

        @Override
        public void run() {
            debug("Started advertising presence");
            while(engineOn){
                try {
                    String messageStr =  Integer.toString(DEVICE_ID);
                    byte[] messageByte = messageStr.getBytes();

                    InetAddress group = InetAddress.getByName("10.0.0.255");
                    DatagramPacket packet = new DatagramPacket(messageByte, messageByte.length, group, port);
//                    debug("Broadcasting "+messageStr);
                    broadcastSocket.send(packet);
                    //printConnectedNodes();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
                catch (IOException e){
                    e.printStackTrace();
                    break;

                }
            }
            debug("Stopped advertising presence");
        }

        public void cancel(){
//            debug("Stopped advertising presence");
            engineOn = false;
            if(broadcastSocket!= null) {
                broadcastSocket.close();
                broadcastSocket = null;
            }
        }
    }


    private class WifiBroadcast_client extends Thread{
        int port = 5555;
        boolean engineOn = true;
        public WifiBroadcast_client(){
            engineOn = true;
            if(broadcastSocket == null)
                try {
                    broadcastSocket = new DatagramSocket(port);
                    broadcastSocket.setBroadcast(true);
                } catch (SocketException e) {
                    e.printStackTrace();
                }
        }

        @Override
        public void run() {
            debug("Started searching for peers");
            while(engineOn){
                try {
                    byte[] buf = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    broadcastSocket.receive(packet);
                    byte[]data = packet.getData();

                    String messageStr = new String(data, 0,packet.getLength());

                    Scanner stringScanner = new Scanner(messageStr);
                    //

                    if(stringScanner.hasNextInt()) {
                        int deviceID = stringScanner.nextInt();
                        if(deviceID!=DEVICE_ID) {
                            Log.d(TAG, "Found device " + deviceID);
//                            debug("Found device " + deviceID);
                            if((deviceID - 1) == DEVICE_ID) {
                                if (connectionThreads[deviceID - 1] == null) {
                                    //      new WifiAdhocClientThread(device_ip_adresses[deviceID - 1]).start();

                                    Socket socket = new Socket();
                                    try {
                                        int port = 8000;
                                        debug("Connecting to " + deviceID + " @ " + device_ip_adresses[deviceID - 1]);
                                        socket.bind(null);
                                        socket.connect((new InetSocketAddress(device_ip_adresses[deviceID - 1], port)), 600);
                                        socket.setKeepAlive(false);
                                        debug("[CLIENT] Connected to " + deviceID + " " + device_ip_adresses[deviceID - 1]);

                                        //     int size = 9999999;
                                        //      socket.setReceiveBufferSize(size);
                                        startCommunicationThreads(deviceID, socket,
                                                socket.getInputStream(), socket.getOutputStream(), mainActivity);

//                                        log("Receive Buffer size: " + socket.getReceiveBufferSize());
//                                        ConnectionThread connectionThread = new ConnectionThread
//                                                (socket.getInputStream(), socket.getOutputStream(), deviceID, mainActivity,
//                                                        socket, synchronizeLock,packetlogfile, logfile);
//                                        addNode(connectionThread, deviceID);
//                                        connectionThread.start();
                                    }
                                    catch(IOException e){
                                        e.printStackTrace();
                                    }
                                }
                            }
//                            if(connectionThreads[deviceID - 1] != null)
//                                connectionThreads[deviceID-1].lastSeenTimeStamp = System.currentTimeMillis();
                        }
                    }else{
                        String command = stringScanner.next();
                        changeSystemTime(command);
                    }


                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }
            debug("Stopped searching for peers");
        }
        public void cancel(){
            engineOn = false;
        }
    }

    private void changeSystemTime(String timestmp){
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            String command = "date -s "+timestmp+"\n";
            debug(command);

            // Log.e("command",command);
            os.writeBytes(command);
            os.flush();
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
            sendToCommandCenter("[Device "+DEVICE_ID+" ] Synced Time");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
