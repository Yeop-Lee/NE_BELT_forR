package com.yonsei.dclab.packet;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Yeop_DCLab on 2017-08-23.
 * 패킷 파서는 받은 패킷을 start sync -->end sync로 끊어서
 * 한 패킷을 구분해 주는 함수임
 * Start sync: 55aaffff
 * End sync: 4499eeee
 *
 * 주요 기능:
 * mBuffer 라는 변수에 패킷들이 무한히 쌓여 저장됨
 * mBuffer 에서 start sync와 end sync을 한 단위로 잘라서 mPacket으로 저장한뒤 mBuffer는 지워짐
 */
public class PacketParser {
    private final static String TAG = PacketParser.class.getSimpleName();

    public ArrayList<Byte> mBuffer = new ArrayList<Byte>();
    public ArrayList<Byte> mPacket = new ArrayList<Byte>();
    public ArrayList<ArrayList<Byte>> mPacketList = new ArrayList< ArrayList<Byte>>();

//    private enum STATUS { SYNC_STOP, SYNC_START}
//    private STATUS mSTATUS = STATUS.SYNC_STOP;

    public int mstart = 0;

    /**
     * Start sync만을 보고 패킷을 파싱함
     * End sync를 보지 않기 때문에 패킷 로스 등의 일이 발생하였을시, 데이터가 망가질 위험할 수 있음
     * 따라서 아래 runParser0ld를 이용하는 것을 권장함.
     * */
    public void fast_add(byte[] data, int start, int length){
        if (mBuffer.get(0) == (byte)0x55 & mBuffer.get(1) == (byte)0xaa & mBuffer.get(2) == (byte)0xff & mBuffer.get(3) == (byte)0xff) {
            mPacketList.add(mPacket);
            mPacket = new ArrayList<Byte>();
            mstart = 0;
        }
        for (int i = mstart; i<mstart+length; i++) mPacket.add(data[i]);
        mstart = mstart+length;
    }

    /**
     * Start sync와 Enc sync를 보고 패킷을 파싱함
     *
     * */
    private void runParserOld() {
        while (mBuffer.size() > 4) {
            if (mBuffer.get(0) == (byte)0x55 & mBuffer.get(1) == (byte)0xaa & mBuffer.get(2) == (byte)0xff & mBuffer.get(3) == (byte)0xff) {
                //패킷이 시작되면 새로운 mPacket을 생성
//                Log.w(TAG,"start sync");
                mPacket = new ArrayList<Byte>();
            }else if (mBuffer.get(0) == (byte) 0x44 & mBuffer.get(1) == (byte) 0x99 & mBuffer.get(2) == (byte) 0xee & mBuffer.get(3) == (byte) 0xee) {
                //패킷이 끝나면 완성된 한 패킷(mPacket)을 추가함
//                Log.w(TAG,"end sync");
                mPacketList.add(mPacket);
            }
            //mBuffer pop
            mPacket.add(mBuffer.get(0));
            mBuffer.remove(0);
        }
    }

    /**
     * 메인 함수에서 불리는 함수
     * 받은 패킷(data)을 mBuffer에 저장하고 파싱을 시작함
     * */
    public void add(byte[] data, int start, int length) {
        for(int i = start; i < start+length; i++) mBuffer.add(data[i]);
//        runParser();
        runParserOld();
    }

    /**
     * 파싱된 패킷 데이터를 보내줌
     * */
    public byte[] get() {
        if (mPacketList.size() == 0) return null;
        else {
            ArrayList<Byte> byteList = mPacketList.get(0);
            mPacketList.remove(0);
//            Log.e(TAG, String.valueOf(mPacketList.size()));
            byte[] data = new byte[byteList.size()];
            for(int i = 0; i < data.length; i++) data[i] = byteList.get(i);
            return data;
        }
    }

//    private void runParser() {
//
//        switch (mSTATUS) {
//            case SYNC_STOP:
//                while (mBuffer.size() > 4) {
//                    if (mBuffer.get(0) == (byte) 0xff & mBuffer.get(1) == (byte) 0xff & mBuffer.get(2) == (byte) 0xff & mBuffer.get(3) == (byte) 0xff) {
//                        Log.w(TAG, "recived : 55aaffff");
//                        mPacket = new ArrayList<Byte>();
//                        for(int i = 0; i < 4; i++) mPacket.add(mBuffer.remove(0));
//                        mSTATUS = STATUS.SYNC_START;
//                        break;
//                    }
//                    mBuffer.remove(0);
//                }
//                if(mSTATUS == STATUS.SYNC_STOP) break;
//            case SYNC_START:
//                int syncEndIndex = getSyncEndIndex();
//                if(syncEndIndex > 0) {
//                    for(int i = 0; i < syncEndIndex; i++) {
//                        mPacket.add(mBuffer.remove(0));
//                    }
//                    mPacketList.add(mPacket);
//                    mSTATUS = STATUS.SYNC_STOP;
//                }
//                break;
//            default:
//                break;
//        }
//    }



    public int getSyncEndIndex() {
        int i = mBuffer.size() - 4;
        if (mBuffer.get(i) == (byte) 0x44 & mBuffer.get(i + 1) == (byte) 0x99 & mBuffer.get(i + 2) == (byte) 0xee & mBuffer.get(i + 3) == (byte) 0xee) return 1;
//            for(int i = EndSize-4;i<EndSize;i++) {
//                if (mBuffer.get(i) == (byte) 0x44 & mBuffer.get(i + 1) == (byte) 0x99 & mBuffer.get(i + 2) == (byte) 0xee & mBuffer.get(i + 3) == (byte) 0xee) {
//                    return i;
//            }
//        }
//        for(int i = 0; i < mBuffer.size() - 3; i++) {
//            if (mBuffer.get(i) == (byte) 0x44 & mBuffer.get(i + 1) == (byte) 0x99 & mBuffer.get(i + 2) == (byte) 0xee & mBuffer.get(i + 3) == (byte) 0xee) {
//                return i;
//            }
//        }
        return -1;
    }

    /*
    public byte[] get() {
        if (mPacketList.size() == 0) return null;
        else {
            ArrayList<Byte> byteList = mPacketList.get(0);

            byte[] data = new byte[byteList.size()];
            for(int i = 0; i < data.length; i++) data[i] = byteList.get(i);

            Packet_v0 packet_v0 = new Packet_v0("", "", data);
            if (packet_v0.isLengthAvailable()) {
                mPacketList.remove(0);
                return data;
            } else {

            } return null;
        }
    }
    */
}
