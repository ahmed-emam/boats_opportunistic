package com.example.aemam.boatsframework_opp_tcp_singlethreaded;

import android.util.Log;

import com.example.aemam.boatsframework_opp_tcp_singlethreaded.model.Packet;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Created by ahmedemam on 12/2/15.
 *
 *
 * NOT USING THIS THREAD RIGHT NOW
 */
public class ConnectionThread extends Thread{

    /*****************************
     *    DIFFERENT log types
     * ***************************
     */
    private static final int DEBUG = 0;
    private static final int WARN = 1;
    private static final int INFO = 2;
    private static final int ERROR = 3;
    /*****************************
     *    DIFFERENT log types
     * ***************************
     */

    private static final String TAG = "Connection_Manager";
    private volatile InputStream inputStream;
    private volatile OutputStream outputStream;
    public volatile boolean threadIsAlive;
    MainActivity mainActivity;
    public int deviceID;
    public volatile boolean peerIsAlive;                        //If the peer associated with this thread
    private boolean sendingToPeer;
    private int counter = 0;
    private FileOutputStream packetLogFile;
    private FileOutputStream mainLogFile;
    private ConnectionThread mainConnectionThread;
    private volatile boolean StreamData;
    private volatile boolean heartBeating;                       //Keep heart beating
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    ScheduledFuture<?> heartBeatHandler;
    ObjectInput objectInputStream;
    ObjectOutput objectOutputStream;
    Socket socket;
    Object outputStreamLock = new Object();
    public final Object synchronizeBytes = new Object();

    public volatile long lastSeenTimeStamp = -1;
    private long lastPeerStatusCheck = -1;

    ReentrantLock lastSeenStampLock;
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
    public String getTimeStamp() {
        return (new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS").format(new Date()));
    }
    /**
     * Function that write 'message' to the log file
     * @param message
     */
    public void log(FileOutputStream fileOutputStream, String message) {
        StringBuilder log_message = new StringBuilder(26 + message.length());
        log_message.append(getTimeStamp());
        log_message.append("\t");
        log_message.append(System.currentTimeMillis());
        log_message.append("\t");
        log_message.append(message);
        log_message.append("\n");

        try {
            fileOutputStream.write(log_message.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void debug(String msg, int type) {
        msg = "["+currentThread().getId()+"]\t"+"[Device "+deviceID+"] " + msg;
        switch (type) {
            case DEBUG:
                Log.d(TAG, msg);
                break;
            case WARN:
                Log.w(TAG, msg);
                break;
            case INFO:
                Log.i(TAG, msg);
                break;
            case ERROR:
                Log.e(TAG, msg);
                break;
        }
    }

    public ConnectionThread(InputStream inputStream, OutputStream outputStream,
                            int deviceID, MainActivity mainActivity, Socket socket,
                            FileOutputStream fileOutputStream, FileOutputStream mainLogfile){
        mainConnectionThread = this;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.deviceID = deviceID;
        this.socket = socket;
        this.mainActivity = mainActivity;
        this.packetLogFile = fileOutputStream;
        this.mainLogFile = mainLogfile;
        threadIsAlive = true;

    }


    @Override
    public void run() {
        //   lock = new ReentrantLock();

//        lock.lock();
//        try {
        StreamData = false;
        sendingToPeer = false;
//        }
//        finally {
//            lock.unlock();
//        }


        peerIsAlive = true;
        int sinkID = 7;
        int myID = mainActivity.DEVICE_ID;
        long interruptTimeStamp = -1;

        try {
            debug("Started the thread", DEBUG);
            objectOutputStream = new ObjectOutputStream(outputStream);
//            objectOutputStream.flush();
            objectInputStream = new ObjectInputStream(inputStream);
        }
        catch(IOException e) {
            threadIsAlive = false;
            cancel();
        }

        lastSeenTimeStamp = System.currentTimeMillis();


//            debug("Started the thread",DEBUG);
//            objectOutputStream = new ObjectOutputStream(outputStream);
//            debug("Made the object outputstream",DEBUG);
//            objectOutputStream.flush();
////            startHeartBeat();
//            objectInputStream = new ObjectInputStream(inputStream);
//            debug("Made the object inputstream",DEBUG);
//

        while(threadIsAlive) {

            try {
                Packet incomingPacket = (Packet) objectInputStream.readObject();
//                peerIsAlive = true;
                lastSeenTimeStamp = System.currentTimeMillis();
//                Packet incomingPacket = serialize(inputStream);
//                debug(incomingPacket.toString(), DEBUG);
                if (((mainActivity.received_bytes > mainActivity.sent_bytes) ||
                        (mainActivity.experimentRunning && myID == 9))
                        && myID > sinkID && !sendingToPeer
                        && (deviceID + 1) == myID) {
                    sendStream(myID - 1);
                }
//                else {
//                    stopStream();
//                }


//                if (deviceID > myID)
//                    log(mainLogFile, "Bytes in socket " + inputStream.available());


                if (incomingPacket.getType() != Packet.HEARTBEAT) {
                    log(packetLogFile, device_ip_adresses[deviceID - 1] + "\t" +
                            device_ip_adresses[myID - 1] +
                            "\t" + incomingPacket.getSourceId() + "\t" + incomingPacket.getDestId() +
                            "\t" + sizeOf(incomingPacket) + "\t" + incomingPacket.getType() +
                            "\t" + incomingPacket.getExperimentID());

                    if (incomingPacket.getExperimentID() != mainActivity.experiment_id) {
                        mainActivity.experiment_id = incomingPacket.getExperimentID();
                        log(mainLogFile, "Starting experiment " + mainActivity.experiment_id + " sent " + mainActivity.sent_bytes + " received " + mainActivity.received_bytes + " bytes");
                        mainActivity.sent_bytes = 0;
                        mainActivity.received_bytes = 0;
//                        }
                    }
//                    synchronized (synchronizeBytes) {
                    mainActivity.received_bytes += sizeOf(incomingPacket);

                }
//                if (myID == incomingPacket.getDestId()) {
//                    byte tagByte = incomingPacket.getType();
//
//
//                    switch (tagByte) {
//
//                        case Packet.STREAM:
//                            break;
//
//                        case Packet.HEARTBEAT:
//
//                            break;
//                    }
//
//                } else {

                //Route
//                    int nextHop = mainActivity.findRoute(incomingPacket.getDestId());
//                    if (nextHop == -1) {
//                        //There is no route for this packet, discard the packet for now
//                        debug("No route for " + incomingPacket.getDestId(), WARN);
//                    } else {
//                        ConnectionThread nextHopThread = mainActivity.getWorkingThread(nextHop);
//
//                        if (nextHopThread != null) {
//                            nextHopThread.sendPacket(incomingPacket);
//                        } else {
//                            debug("No worker thread for " + nextHop, ERROR);
//                        }
//                    }
//                }
            } catch(InterruptedIOException e){
                debug(e.toString(), WARN);

                long currentTime = System.currentTimeMillis();
                debug("Interrupted after " + (currentTime - interruptTimeStamp) + " milliseconds", DEBUG);
                interruptTimeStamp = currentTime;

                if (((mainActivity.received_bytes > mainActivity.sent_bytes) ||
                        (mainActivity.experimentRunning && myID == 9))
                        && myID > sinkID && !sendingToPeer
                        && (deviceID + 1) == myID) {
                    sendStream(myID - 1);
                }

                checkPeer();
            }
            catch (IOException e) {
                debug(e.toString(), WARN);
                e.printStackTrace();
                cancel();
            } catch (ClassNotFoundException e) {
                debug(e.toString(), WARN);
                e.printStackTrace();
            }
        }


//        if(deviceID == (myID+1))
//            log(packetLogFile, device_ip_adresses[deviceID - 1] + "\t" +
//                    device_ip_adresses[myID - 1] +
//                    "\t" + deviceID + "\t" + (myID) +
//                    "\t" + 0 + "\t" + Packet.STREAM + "\t" + mainActivity.experiment_id);

    }

//    public void startHeartBeat(){
//        debug("Starting heart-beat to "+deviceID, INFO);
//        heartBeating = true;
//
//        heartBeatHandler = scheduler.scheduleAtFixedRate(sendHeartBeatPacket, 10, 100, TimeUnit.MILLISECONDS);
//
//    }


    public void checkPeer(){
        int delay_between_checks = 2000; //2 secs
        long currentTime = System.currentTimeMillis();
        if(lastPeerStatusCheck == -1)
            lastPeerStatusCheck = currentTime;
        if((currentTime - lastPeerStatusCheck) >= delay_between_checks){
            //Haven't seen the peer for 2 Seconds
            if((currentTime - lastSeenTimeStamp) >= 2000){
                debug("Peer "+deviceID+" is dead", DEBUG);
                log(mainLogFile, "Peer "+deviceID+" is dead");
                cancel();
            }else{
                debug("Peer "+deviceID+" is alive", DEBUG);
            }
        }
    }

    private class SendStreamRunnable implements Runnable{
        int nodeID;
        public SendStreamRunnable(int nodeID){
            this.nodeID = nodeID;
        }
        @Override
        public void run() {
//            stopHeartBeat();
            mainActivity.debug("Sending Stream to " + nodeID);
            debug("Sending Stream to " + nodeID, DEBUG);
            log(mainLogFile, "Sending stream to "+nodeID);
            StreamData = true;
//            int id = mainActivity.experiment_id;
            int device_ID = mainActivity.DEVICE_ID;
            int chunkSize = 10240; //10 KB
            byte[] buf = new byte[chunkSize];

            debug("Thread is alive:"+threadIsAlive +"\tStream Data:"+StreamData
                    +"\tRX:"+mainActivity.received_bytes+"\t"+deviceID+" is Alive:"+peerIsAlive, DEBUG);
            log(mainLogFile, "Thread is alive:"+threadIsAlive +"\tStream Data:"+StreamData
                    +"\tRX:"+mainActivity.received_bytes+"\t"+deviceID+" is Alive:"+peerIsAlive);

            while (threadIsAlive && StreamData &&
                    ( (mainActivity.received_bytes > mainActivity.sent_bytes) ||
                            (device_ID == 9 && mainActivity.experimentRunning) ) ) {
                new Random().nextBytes(buf);
                Packet packet = new Packet(device_ID, nodeID, Packet.STREAM, mainActivity.experiment_id, buf);
                sendPacket(packet);
            }

            debug("Thread is alive:"+threadIsAlive +"\tStream Data:"+StreamData
                    +"\tRX:"+mainActivity.received_bytes+"\t"+deviceID+" is Alive:"+peerIsAlive, DEBUG);
            log(mainLogFile, "Thread is alive:"+threadIsAlive +"\tStream Data:"+StreamData
                    +"\tRX:"+mainActivity.received_bytes+"\t"+deviceID+" is Alive:"+peerIsAlive);
            sendingToPeer = false;
            StreamData = false;

//            if(threadIsAlive)
//                startHeartBeat();

//            flushOutputStream();
            debug("Stopped Stream to " + nodeID, DEBUG);
            mainActivity.debug("Stopped Stream to " + nodeID);
            log(mainLogFile, "Stopped Stream to " + nodeID);

        }
    }

    private void flushOutputStream(){
        try {
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
//
//    /**
//     * Send file routine, the file is cut to 10 kb chunks.
//     * Each 10 KB is encapsulated with additional info and sent to the destination node
//     * @param nodeID
//
//     */
//    public void sendStream(int nodeID) {
//        mainActivity.debug("Sending Stream to " + nodeID);
//        StreamData = true;
////        try {
//        int chunkSize = 10240, len; //10 KB
//        byte[] buf = new byte[chunkSize];
//        new Random().nextBytes(buf);
//
//        while (engineRunning &&  StreamData) {
//            Packet packet = new Packet(MainActivity.DEVICE_ID, nodeID, STREAM, buf.length, buf);
//            sendPacket(packet);
//            debug("Sending stream");
//        }
//    }

    /**
     * Send file routine, the file is cut to 10 kb chunks.
     * Each 10 KB is encapsulated with additional info and sent to the destination node
     * @param nodeID

     */
    public void sendStream(int nodeID) {
        sendingToPeer = true;
        SendStreamRunnable sendStreamRunnable = new SendStreamRunnable(nodeID);
        Thread sendStreamOnSeperateThread = new Thread(sendStreamRunnable);
        sendStreamOnSeperateThread.setPriority(Thread.MIN_PRIORITY);
        sendStreamOnSeperateThread.start();
    }

//    public void stopStream(){
//        StreamData = false;
//
//        log(mainLogFile, "Stopped stream");
//        debug("Stopped stream from stopStream", INFO);
//        stopHeartBeat();
//        try {
//            synchronized (outputStreamLock) {
//                socket.shutdownOutput();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        log(mainLogFile, "shutting down the outputstream");
//
//    }

    public synchronized void sendPacket(Packet packet){
        try {


            if(packet.getType() == Packet.STREAM) {
//                    synchronized (synchronizeBytes) {
                if ((mainActivity.received_bytes > mainActivity.sent_bytes) || mainActivity.DEVICE_ID == 9) {
                    objectOutputStream.writeObject(packet);
                    int packetSize = sizeOf(packet);
//                    if (mainActivity.DEVICE_ID != 9) {
//                        mainActivity.received_bytes -= packetSize;
                    mainActivity.sent_bytes += packetSize;
//                        }
//                    }
                }
            }
            else
                objectOutputStream.writeObject(packet);

//            outputStream.flush();

        }catch(IOException e){
            debug(e.toString(), ERROR);
            cancel();
            e.printStackTrace();
        }
    }

    /**
     * Sending heart beat packets to the peer
     * If sending a heart beat fails then we lost the connection
     */
    public void sendHeartBeatPacket() {
        Packet packet = new Packet(MainActivity.DEVICE_ID, deviceID, Packet.HEARTBEAT, 0, new byte[1]);
        sendPacket(packet);
        debug("Sending Heart-Beat", DEBUG);
    }

    public void startHeartBeat(){
        heartBeating = true;
        Thread heartBeatThread = new Thread(heartBeat);
        heartBeatThread.setPriority(Thread.MAX_PRIORITY);
        heartBeatThread.start();
    }

    Runnable heartBeat =new Runnable() {
        public long last_peer_check = -1;

        @Override
        public void run() {


            while(heartBeating){
                long currentTime = System.currentTimeMillis();
                long lastSeenStamp = lastSeenTimeStamp;
                if(last_peer_check == -1){
                    last_peer_check = currentTime;
                }

                debug("Last Seen at "+(currentTime - lastSeenStamp ), DEBUG);
                if((currentTime - last_peer_check) >= 1000) {
                    if((currentTime - lastSeenStamp ) <= 2000){
                        debug(deviceID+" is alive", INFO);
                    }
                    else {
                        debug(deviceID+" is dead", WARN);
                        StreamData = false;
                        cancel();
                        heartBeating = false;
                        break;
                    }
                    counter = 0;
                    peerIsAlive = false;
                    last_peer_check = currentTime;
                }
                sendHeartBeatPacket();
                try {
                    Thread.sleep(500);
                }
                catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    };

    //    Runnable sendHeartBeatPacket = new Runnable() {
//        @Override
//        public void run() {
//            //Check if peer is alive after 2 seconds and i did not hear from the peer
//            long currentTime = System.currentTimeMillis();
//            if(lastSeenTimeStamp == -1)
//                lastSeenTimeStamp = currentTime;
//
//            if((currentTime - lastSeenTimeStamp) > 2000) {
//                if(peerIsAlive){
//                    debug(deviceID+" is alive", INFO);
//                }
//                else {
//                    debug(deviceID+" is dead", WARN);
//                    StreamData = false;
//                    cancel();
//                    return;
//                }
//                counter = 0;
//                peerIsAlive = false;
//                lastSeenTimeStamp = currentTime;
//            }
//            sendHeartBeatPacket(); //Send a heart beat
//
//            counter++;
//        }
//    };
    public void stopHeartBeat(){
        debug("Stopping heart-beat to "+deviceID, INFO);

//        if(heartBeatHandler != null)
//            heartBeatHandler.cancel(false);
        heartBeating = false;
//        scheduler.shutdownNow();
    }




    public void cancel(){
//        stopHeartBeat();

        StreamData = false;
        threadIsAlive = false;

//                log(mainLogFile, "Bytes in socket " + outputStream.available());
        log(mainLogFile, "Sent " + mainActivity.sent_bytes + " bytes to " + deviceID + " received "+ mainActivity.received_bytes+ " bytes from " + deviceID + " in experiment " + mainActivity.experiment_id);
        debug("Sent " + mainActivity.sent_bytes + " bytes to " + deviceID + " in experiment " + mainActivity.experiment_id, DEBUG);


        try {
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        mainActivity.removeNode(deviceID);
        //   mainActivity = null;



    }

    public static final int sizeOf(Object object) throws IOException {

        if (object == null)
            return -1;

        // Special output stream use to write the content
        // of an output stream to an internal byte array.
        ByteArrayOutputStream byteArrayOutputStream =
                new ByteArrayOutputStream();

        // Output stream that can write object
        ObjectOutputStream objectOutputStream =
                new ObjectOutputStream(byteArrayOutputStream);

        // Write object and close the output stream
        objectOutputStream.writeObject(object);
        objectOutputStream.flush();
        objectOutputStream.close();

        // Get the byte array
        byte[] byteArray = byteArrayOutputStream.toByteArray();

        // TODO can the toByteArray() method return a
        // null array ?
        return byteArray == null ? 0 : byteArray.length;


    }

//    public Packet serialize(InputStream inputStream){
//        DataInputStream dataInputStream = new DataInputStream(inputStream);
//        try {
//            int sourceID = dataInputStream.readInt();
//            int destID = dataInputStream.readInt();
//            byte type = dataInputStream.readByte();
//            int experimentID = dataInputStream.readInt();
//            if(type == Packet.DATA){
//                int chunkSize = 10240; //10 KB
//                byte[] data = new byte[chunkSize];
//                dataInputStream.readFully(data);
//                return new Packet(sourceID, destID, type, experimentID, data);
//            }
//            else{
//                return new Packet(sourceID, destID, type, experimentID, null);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        return null;
//    }
//    public byte[] deserialize(Packet packet){
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
//        try {
//            dataOutputStream.writeInt(packet.getSourceId());
//            dataOutputStream.writeInt(packet.getDestId());
//            dataOutputStream.writeByte(packet.getType());
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }
}