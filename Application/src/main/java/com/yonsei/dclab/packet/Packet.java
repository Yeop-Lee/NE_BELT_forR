package com.yonsei.dclab.packet;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Yeop_DCLab on 2017-08-23.
 * Start와 End로 잘려진 한 패킷을 원하는 데이터로 변환해줌
 *
 */

public class Packet {

    private final static String TAG = Packet.class.getSimpleName();

    public String deviceName;
    public String deviceMac;
    public long deviceTransfertime;

    public short length;
    public byte cmd;
    public byte addr;
    public short data;
    public int id;
    public byte cfgFormat;
    public byte cfgNumCh;
    public short cfgNumSample;
    public int seqNum;
    public int reserved;
    public ArrayList<ArrayList<Integer>> rawData;
    public ArrayList<Integer> rawDataTemp;

    public byte[] dataArray;

    public Packet() {

    }

    public Packet(String pDeviceName, String pDeviceMac, byte[] data) {
        deviceName = pDeviceName;
        deviceMac = pDeviceMac;
        dataArray = data;
        deviceTransfertime = System.currentTimeMillis();
//        Log.d(TAG, "packet start");
//        Log.d(TAG, String.valueOf(data.length));

        if (data.length > 4) {
//            Log.d(TAG,"start parsing");
            if (data[0] == (byte)0x55 & data[1] == (byte)0xaa & data[2] == (byte)0xff &data[3] == (byte)0xff) {
                this.length = (short) ((data[4] & 0x00ff) + ((data[5] << 8) & 0xff00));
//                Log.d(TAG, String.format("length = %02X", this.length));

                if (this.length > 0) {
                    this.cmd = data[6];
//                    Log.d(TAG, String.format("cmd = %02X", this.cmd));
                    this.addr = data[7];
//                    Log.d(TAG, String.format("addr = %02X", this.addr));
                    this.data = (short) ((data[8] & 0x00ff) + ((data[9] << 8) & 0xff00));

                    // Stream
                    if (this.cmd == 0x10) {
                        this.id = (data[10] & 0x000000ff) + ((data[11] << 8) & 0x0000ff00) + ((data[12] << 16) & 0x00ff0000) + ((data[13] << 24) & 0xff000000);
//                        Log.d(TAG, String.format("id = %02X", this.id));
                        this.cfgNumSample = (short) ((data[14] & 0x00ff) + ((data[15] << 8) & 0xff00));
                        this.cfgNumCh = data[16];
                        this.cfgFormat = data[17];
//                        Log.d(TAG, String.format("cfgNumSample = %02X", this.cfgNumSample));
//                        this.cfgNumCh = (short) ((data[16] & 0x00ff) + ((data[17] << 8) & 0xff00));
//                        Log.d(TAG, String.format("cfgNumCh = %02X", this.cfgNumCh));
//                        Log.d(TAG, String.format("cfgformat = %02X", this.cfgFormat));
                        this.seqNum = (data[18] & 0x000000ff) + ((data[19] << 8) & 0x0000ff00) + ((data[20] << 16) & 0x00ff0000) + ((data[21] << 24) & 0xff000000);
//                        Log.d(TAG, String.format("SEQ_NUM = %d", this.seqNum));
                        this.reserved = (data[22] & 0x000000ff) + ((data[23] << 8) & 0x0000ff00) + ((data[24] << 16) & 0x00ff0000) + ((data[25] << 24) & 0xff000000);
                        rawData =  new ArrayList<ArrayList<Integer>>();
                        if (this.addr == 0x01 && this.id == 0x11000200) {//Hard coding for MUL (ECG 1 , BIA 1, Reserved 200 (moisture)) 16bit
                            for (int j = 0; j < this.cfgNumCh; j++) { // this.cfgNumCh
                                rawDataTemp = new ArrayList<Integer>();
                                for (int i = 0; i < this.cfgNumSample*2; i = i+2) {//\
                                    int index_jumper = this.cfgNumSample*2*(j);
                                    int val = (data[26+index_jumper + i] & 0x00ff) + ((data[26+index_jumper + i + 1] << 8) & 0xff00);
                                    rawDataTemp.add(val);
                                }
                                rawData.add(rawDataTemp);
//                                Log.d(TAG, String.valueOf(rawData.get(j)));
                            }
                        }
                    }else if (this.cmd == 0x08){
                        Log.e(TAG, String.valueOf(data));
                    }
                }
            }
        } else {
            length = 0;
        }
    }
    public boolean isEmpty() {
        if (this.length == 0) return true;
        else return false;
    }

    public boolean isDeviceID() {
        if (this.cmd == 0x08 && this.addr == 0x00) return true;
        else return false;
    }

    public boolean isFirmwareVersion() {
        if (this.cmd == 0x08 && this.addr == 0x0f) return true;
        if (this.cmd == 0x08 && this.addr == 0x01) return true;
        else return false;
    }
    public boolean isMULdata() {
        if (this.cmd == 0x10 && this.addr == 0x01) return true;
        else return false;
    }

//    public boolean isPPGdata() {
//        if (this.cmd == 0x10 && this.addr == 0x20) return true;
//        else return false;
//    }
//
//    public boolean isBIAdata() {
//        if (this.cmd == 0x10 && this.addr == 0x30) return true;
//        else return false;
//    }
//
//    public boolean isGSRdata() {
//        if (this.cmd == 0x10 && this.addr == 0x40) return true;
//        else return false;
//    }
//
//    public boolean isSKTdata() {
//        if (this.cmd == 0x10 && this.addr == 0x50) return true;
//        else return false;
//    }
//
//    public boolean isECGStop() {
//        if (this.cmd == 0x08 && this.addr == 0x1f) return true;
//        else return false;
//    }
//
//    public boolean isPPGStop() {
//        if (this.cmd == 0x08 && this.addr == 0x2f) return true;
//        else return false;
//    }
//
//    public boolean isBIAStop() {
//        if (this.cmd == 0x08 && this.addr == 0x3f) return true;
//        else return false;
//    }
//
//    public boolean isGSRStop() {
//        if (this.cmd == 0x08 && this.addr == 0x4f) return true;
//        else return false;
//    }
//
//    public boolean isSKTStop() {
//        if (this.cmd == 0x08 && this.addr == 0x5f) return true;
//        else return false;
//    }
//
//    public boolean isBiaTxFrequency() {
//        if (this.cmd == 0x08 && this.addr == 0x4f) return true;
//        else return false;
//    }
}
