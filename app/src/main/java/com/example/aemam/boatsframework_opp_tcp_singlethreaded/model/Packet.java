package com.example.aemam.boatsframework_opp_tcp_singlethreaded.model;

import java.io.Serializable;

/**
 * Created by ahmedemam on 12/2/15.
 */
public class Packet implements Serializable {

    /**************************************
     *             Packet Types
     * *************************************
     */
    public static final byte HEARTBEAT = 1;
    public static final byte STREAM = 2;
    public static final byte DATA = 3;
    public static final byte TERMINATE = 4;    //Terminate connection
    public static final byte DELETE = 5;

    /**************************************
     *             Packet Types
     * *************************************
     */

    int sourceId;
    int destId;
    byte type;
    int experimentID;
    public byte[] payload;
    public Packet(){
        sourceId = 0;
        destId = 0;
        type = 0;
    }
    public Packet(int sourceId, int destId, byte type, int experimentID, byte[] payload){
        this.sourceId = sourceId;
        this.destId = destId;
        this.type = type;
        this.experimentID = experimentID;
        this.payload = payload;
    }

    public int getSourceId() {
        return sourceId;
    }

    public int getExperimentID() {
        return experimentID;
    }

    public int getDestId() {
        return destId;
    }

    public byte getType() {
        return type;
    }

    public void setSourceId(int sourceId) {
        this.sourceId = sourceId;
    }

    public void setDestId(int destId) {
        this.destId = destId;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "| "+this.sourceId+" | "+this.destId+" | "+this.type+
                " | Experiment ID: "+this.experimentID +" | ";
    }
}
