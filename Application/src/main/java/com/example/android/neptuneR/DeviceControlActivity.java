/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.neptuneR;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v13.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.GyroBmi160;

import com.yonsei.dclab.OSEAFactory;
import com.yonsei.dclab.chart.BIA_Chart;
import com.yonsei.dclab.chart.ECG_Chart;
import com.yonsei.dclab.chart.Moi_Chart;
import com.yonsei.dclab.file.FileManager;
import com.yonsei.dclab.packet.Packet;
import com.yonsei.dclab.packet.PacketParser;
import com.yonsei.dclab.processing.QRSDetector2;

import bolts.Task;

/**
 * 디바이스와 연결 된 뒤에 실행되는 메인 클래스
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();
    int sampleRate = 256;

    public static final String EXTRAS_DEVICE_NAME = "NE_BELT";
    public static final String EXTRAS_DEVICE_ADDRESS = "98:2D:68:2D:60:00";

    private static final String[] deviceUUIDs = {"ED:C9:56:60:56:4C", "FD:0F:59:E2:F4:C5"};//"FD:0F:59:E2:F4:C5" "D4:25:5C:D6:2E:F5"
    private BtleService.LocalBinder serviceBinder;

    //data catch map for accel & gyro scope
    private Map<String, Accelerometer> accelerometerSensors = new HashMap<>();
    private Map<String, GyroBmi160> gyroSensors = new HashMap<>();

    private Map<String, TextView> sensorOutputs = new HashMap<>();
    private Map<String, TextView> gyrosensorOutputs = new HashMap<>();


    private Route streamRoute;

    private int mfile_Num;
    // for layout
    private TextView mSaveView;
    private Button mSaveButton;
    private Button mStartButton;
    private Button mBiaSetButton;
    private TextView mEcgctrl;
    private EditText mEcgst;
    private EditText mEcged;

    private TextView mBiactrl;
    private Button mOffNeAlarmButton;

    private EditText mRotst;
    private EditText mRoted;
    private Button mFlag;
    private int flag_marker=0;
    private RadioButton mStand;
    private RadioButton mSit;
    private RadioButton mLie;


    private String mDeviceName;
    private String mDeviceAddress;
    private BIA_Chart mBIA_Chart;
    private ECG_Chart mECG_Chart;
    private Moi_Chart mMoi_Chart;

    private TextView mTextView_Heartrate;
    private TextView mTextView_BodyImpedance;
    private TextView mTextView_MoistureSensor;

    private Handler mUpdateDataHandler;
    private  Handler mRotationHandler;
//    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

    private boolean mConnected = false;
    private FileManager mFileManager;
    private PacketParser mPacketParser;
    private Vibrator vibe;
    private SoundPool sound;
    private int music;
    private int NE_event;
    private int ne_event_lock = 0;
    private int minute_now = 60;

    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mNotifyCharacteristic_W;
    private BluetoothGattCharacteristic mNotifyCharacteristic_C;

    private ArrayList<Integer> mBiaDataList = new ArrayList<Integer>();
    private ArrayList<Integer> mEcgDataList = new ArrayList<Integer>();
    private ArrayList<Integer> mMoiDataList = new ArrayList<Integer>();
    private ArrayList<Integer> RR_buf = new ArrayList<Integer>();
    private int MULDataListSize = 64;

    // 256Hz(64*4), 20sec (10*2)
    private int BiaDataListSize = 64 * 4 * 10 * 2;
    private int EcgDataListSize = 64 * 4 * 10 * 2;
    private int MoiDataListSize = 64 * 4 * 10 * 2;

    private String Posture = "NA";
    private int NEventMarker = 0;
    private int BiaMarker = 0;
    private int Heartrate = 0;

    private int bell_max = 8250;
    private int bell_min = 8150;

    private int rot_state = 0;
    private int rot_st;
    private int rot_ed;
    private int gain_st = (byte) 0x01;
    private int gain_ed = (byte) 0x07;

    private int[] biaDataArray = new int[BiaDataListSize];
    private int[] ecgDataArray = new int[EcgDataListSize];
    private int[] moiDataArray = new int[MoiDataListSize];

    private static String[] STORAGE_PERMISSION = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    /**
     * Code to manage Service lifecycle.
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.i(TAG,"Main service connect");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final ServiceConnection meta_ServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.i(TAG,"meta service connect");
            serviceBinder = (BtleService.LocalBinder) iBinder;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    /**
     * BLE의 상태에 따라 지시를 내려주는 함수
     *
     *  BLE 연결됨
     *  BLE 끊김
     *  BLE 찾음--> GATT서비스를 설정
     *  BLE 데이터를 받음 --> 패킷을 뜯고 화면에 출력함
     *
     * */
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) { //연결됨
                mConnected = true;
                invalidateOptionsMenu();
                Toast.makeText(getApplicationContext(),"BLE service connected", Toast.LENGTH_SHORT).show();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) { // 연결 끊김
                mConnected = false;
                invalidateOptionsMenu();
                Toast.makeText(getApplicationContext(),"BLE service disconnected", Toast.LENGTH_SHORT).show();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) { //BLE를 찾은 뒤
                // Show all the supported services and characteristics on the user interface.
                getGattServices(mBluetoothLeService.getSupportedGattServices());//GATT설정
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) { // 데이터가 교환중임
//                Log.d(TAG, String.format("MyDEBUG: mGattUpdateReceiver[2] = %s", action));
                byte[] data = intent.getByteArrayExtra(BluetoothLeService.ACTION_DATA_AVAILABLE); //교환된 데이터를 받음

                if (data != null) { //교환된 데이터가 존재할때
//                    Log.d(TAG, String.format("MyDEBUG: mGattUpdateReceiver[3] = %s", action));
                    try { //연결이 끊길때 수행하면 에러가 날 수 있으므로 try로 전환함
                        if (false) {
                            Log.d(TAG, String.format("DATA_AVAILABLE = %d", data.length));
                            final StringBuilder stringBuilder = new StringBuilder(data.length);
                            for (byte byteChar : data)
                                stringBuilder.append(String.format("%02X ", byteChar));
                            Log.d(TAG, "DATA_AVAILABLE = " + stringBuilder.toString());
                        }

                        //패킷을 뜯음
                        mPacketParser.add(data, 0, data.length);
                        //데이터에 뜯어진 패킷을 덮어씌움
                        data = mPacketParser.get();
                        //앱에서 만든 패킷 클래스에 다시 저장함
                        Packet packet_v0 = new Packet(mDeviceName, mDeviceAddress, data);

                        if (packet_v0.isMULdata()){//받은 패킷이 3채널 데이터일때 아래 함수 실행
//                            Log.e(TAG,"Get the data");
                            receivedData(packet_v0);
                        }

                    } catch (Exception e) {

                    }
                }
            }
        }
    };

    /**
     * 버튼이 눌리면 반응하는 함수
     * start_button : 시작
     * bia_rot : bia interval을 시작
     * */
    Button.OnClickListener mClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            switch (v.getId()) { // 아이템의 ID로 식별함
//                case R.id.save:
//                    mfile_Num = 0;
//                    mFileManager.createFile("NE_" + (mfile_Num));
//                    break;
                case R.id.start_button: // Start버튼이 눌리면, 기기로부터 얻
                    Log.d(TAG,"start");
//                    SystemClock.sleep(100);
//                    request_basic();
                    SystemClock.sleep(100);
                    request_3ch();
                    SystemClock.sleep(100);
                    request_Initial();
                    SystemClock.sleep(100);
                    request_pm();
                    SystemClock.sleep(100);
                    request_ecg_gain();
                    SystemClock.sleep(100);
                    request_start();
//                    SystemClock.sleep(100);
                    Toast.makeText(getApplicationContext(),"Send start signal to device", Toast.LENGTH_SHORT).show();
                    break;
                case R.id.bia_rot:
                    gain_st = Integer.parseInt( ((EditText) findViewById(R.id.ecg_st)).getText().toString() );
                    gain_ed = Integer.parseInt( ((EditText) findViewById(R.id.ecg_ed)).getText().toString() );
                    if (rot_state == 0){
                        rot_st = Integer.parseInt( ((EditText) findViewById(R.id.rot_st)).getText().toString() );
                        rot_ed = Integer.parseInt( ((EditText) findViewById(R.id.rot_ed)).getText().toString() );
//                        bia_temp = rot_st+rot_ed;
                        request_bia_off(); //bia off at first button touched
                        SystemClock.sleep(100);
                        request_ecg_ctrl((byte) gain_st);
                        mRotationHandler.postDelayed(rotationMethod, 0);
                        mBiaSetButton.setText("Stop interval");
                        rot_state = 1; //rotation STATE on
                        Toast.makeText(getApplicationContext(),"Send BIA interval start packet to device", Toast.LENGTH_SHORT).show();
                    }else if (rot_state ==1){
                        mRotationHandler.removeCallbacks(rotationMethod);
                        request_bia_on(); //bia on
                        SystemClock.sleep(100);
                        request_ecg_ctrl((byte) gain_ed);
                        mBiaSetButton.setText("Start interval");
                        rot_state = 0; //rotation STATE off
                        bia_temp = 0; //initialize the iterator
                        Toast.makeText(getApplicationContext(),"Send BIA interval stop packet to device", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case R.id.ne_alram:
                    if(NE_event == 1) {
                        // sound.stop(music);
                        // sound.release();
                        sound.autoPause();
                        vibe.cancel();
                        NE_event = 0;// 0 = normal state
                        ne_event_lock = 1;
                        minute_now = mFileManager.getMinute();
                        Toast.makeText(getApplicationContext(), "알람이 꺼졌어요", Toast.LENGTH_LONG).show();
                        mOffNeAlarmButton.setBackgroundColor(Color.LTGRAY);
                        vibe.vibrate(40);
                    }
                    else {
                        Toast.makeText(getApplicationContext(), "알람이 울릴때 눌려주세요", Toast.LENGTH_LONG).show();
                        vibe.vibrate(40);
                    }
                    break;
                case R.id.scale_set_button:
                    int max = Integer.parseInt( ((EditText) findViewById(R.id.edit_middle_set)).getText().toString() );
                    int min = Integer.parseInt( ((EditText) findViewById(R.id.edit_term_set)).getText().toString() );
                    mBIA_Chart.setColor(0xff008b8b,max,min);
                case R.id.flag_button:
                    flag_marker += 1;
                    String tmp_msg = "#"+String.valueOf(flag_marker)+" Flag";
                    mFlag.setText(tmp_msg);
                default:
                    break;
            }
        }
    };

    /**
     * 받은 데이터를 처리하는 함수
     * 스트리밍이 될때마다 호출이 되며
     * packet의 속도가 1/4초에 1번씩 보내므로 1/4초마다 한번씩 실행됨
     * */
    public void receivedData(Packet packet) {
        //패킷에서 얻은 데이터를 List로 생성함
        for (int i = 0; i < MULDataListSize; i++) {
            mEcgDataList.add(packet.rawData.get(0).get(i));
            mBiaDataList.add(packet.rawData.get(1).get(i));
            mMoiDataList.add(packet.rawData.get(2).get(i));
        }
        //데이터의 frequency에 맞지 않게 들어온 경우 삭제함(20초 기준)
        while(mEcgDataList.size() > EcgDataListSize){
            mBiaDataList.remove(0);
            mEcgDataList.remove(0);
            mMoiDataList.remove(0);
        }
        //얻은 List를 20초 간 활성화 시킴
        if(mEcgDataList.size()>=EcgDataListSize){ //초기 20초 이전에 돌아가는 함수
            for (int i = 0; i < EcgDataListSize; i++) {// 10sec
                biaDataArray[i] = mBiaDataList.get(i);
                ecgDataArray[i] = mEcgDataList.get(i);
                moiDataArray[i] = mMoiDataList.get(i);
            }
        }else{ //20초 이후에 돌아가는 함수
            for (int i = 0; i < mEcgDataList.size(); i++){
                biaDataArray[i] = mBiaDataList.get(i);
                ecgDataArray[i] = mEcgDataList.get(i);
                moiDataArray[i] = mMoiDataList.get(i);
            }
        }
        //알람이 울림 128*2*20
        for(int i = 37; i < 40; i++) {
            //Set Urine bell
            if (moiDataArray[128*i] <= bell_max && moiDataArray[128*i] >= bell_min) {
                if (moiDataArray[128*i + 64] <= bell_max && moiDataArray[128*i + 64] >= bell_min) { //Ring alert when moiData range is shorted
                    if (NE_event == 0 && ne_event_lock == 0) {
                        vibe.vibrate(100000); //If NE event detected vibrate
                        music = sound.load(this, R.raw.sample, 1);
                        sound.play(music, 1, 1, 0, -1, 1);
                        sound.autoResume();
                        NE_event = 1; //Mark the event
//                        mFileManager.uploadFile();
//                        mFileManager.uploadMoFile();
//                        mNow_state.setText("> 야뇨가 감지되었습니다");
//                        mNow_guide.setText("\n\n1. 알람을 끄기위해 화면 중앙에 위치한 '알람 끄기' 버튼을 눌러주세요\n\n2.아이를 화장실로 데려가 잔뇨를 볼 수 있도록 도와주세요");
                        mOffNeAlarmButton.setBackgroundColor(Color.RED);

                    }
                }
            }
        }

        //Detect qrs pulse
        QRSDetector2 qrsDetector = OSEAFactory.createQRSDetector2(sampleRate);
        int beat_temp_buf = 0 ;
        int hr  = 0;
        //make HR_buf
        for (int i = 0; i < ecgDataArray.length; i++) {
            int result = qrsDetector.QRSDet(ecgDataArray[i]);
            if (result != 0) {
                beat_temp_buf = (i-result) - beat_temp_buf;
                hr = (60*sampleRate)/beat_temp_buf;
                if (hr > 40 && hr < 200){
                    RR_buf.add(hr);
                }
                ecgDataArray[i-result] = 0;
            }
        }

        //get the HR
        int buf_size = RR_buf.size ();
        if (RR_buf.size()>3){
            int sum = 0;
            int count = 0;
            for (int i = 1; i<buf_size; i++){
                sum += RR_buf.get(i);
                count +=1;
            }
            Heartrate = (int) sum/count;
            mTextView_Heartrate.setText(String.format("%,d", Heartrate));
            RR_buf.clear();
        }

        //화면에 BIA, MOI 패킷의 맨 마지막을 출력함 1/4초마다 출력됨
        if (mBIA_Chart != null) {
            mBIA_Chart.updateChart(biaDataArray);
            mECG_Chart.updateChart(ecgDataArray);
            mMoi_Chart.updateChart(moiDataArray);
            if (mTextView_BodyImpedance != null) mTextView_BodyImpedance.setText(String.format("%,d", packet.rawData.get(1).get(MULDataListSize-1)));
            if (mTextView_MoistureSensor != null) mTextView_MoistureSensor.setText(String.format("%,d", packet.rawData.get(2).get(MULDataListSize-1)));
        }

        mFileManager.saveData(packet, NEventMarker, BiaMarker, Heartrate, Posture, flag_marker);
        NEventMarker = 0;

        int fileKb = (int) (mFileManager.getFileSize()/1000);
        mSaveView.setText("Storage Time : " + (mfile_Num) + "h" + mFileManager.getStorageTime()+"/File Size : "+fileKb+" KB");
        if (mFileManager.getHours() == 1){
            mfile_Num ++;
            mFileManager.createFile("NE_" + (mfile_Num));// Later, it will be user name.
            mFileManager.createMoFile("MO_" + (mfile_Num));// Later, it will be user name.
        }
        if ((mFileManager.getMinute() != minute_now) && ((mFileManager.getMinute() % 1) == 0) ){
            ne_event_lock = 0;
            //music = sound.load(this, R.raw.sample, 1);
        }
    }

    /**
     * BLE GATT 설정하는 함수
     * */
    private void getGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        for (BluetoothGattService gattService : gattServices) {
            Log.d(TAG, String.format("BluetoothGattService = %s", gattService.getUuid().toString()));
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                if(gattCharacteristic.getUuid().toString().equals(BleUuid.CHAR_MARGAUXL_READ)) {
                    Log.d(TAG,"read_checked");
                    mNotifyCharacteristic = gattCharacteristic;
                    mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, true);
                }
                else if(gattCharacteristic.getUuid().toString().equals(BleUuid.CHAR_MARGAUXL_FLOW_CTRL)) {
                    Log.d(TAG,"ctrl_checked");
                    mNotifyCharacteristic_C = gattCharacteristic;
                }
                else if(gattCharacteristic.getUuid().toString().equals(BleUuid.CHAR_MARGAUXL_WRITE)) {
                    Log.d(TAG,"write_checked");
                    mNotifyCharacteristic_W = gattCharacteristic;
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        ActivityCompat.requestPermissions(DeviceControlActivity.this, STORAGE_PERMISSION, 1);

        mFileManager = new FileManager();
        sound = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
        music = sound.load(this, R.raw.sample, 1);
        NE_event = 0;

        mBIA_Chart = (BIA_Chart) findViewById(R.id.bia_chart);
        mBIA_Chart.setColor(0xff008b8b,1700,1550);
        mECG_Chart = (ECG_Chart) findViewById(R.id.ecg_chart);
        mECG_Chart.setColor(0xff800000);
        mMoi_Chart = (Moi_Chart) findViewById(R.id.moi_chart);
        mMoi_Chart.setColor(0xffff8800);

        vibe = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        mTextView_Heartrate = (TextView) findViewById(R.id.textView_heartrate);
        mTextView_BodyImpedance = (TextView) findViewById(R.id.textView_bodyImpedance);
        mTextView_MoistureSensor = (TextView) findViewById(R.id.textView_moisturesensor);

        mStartButton = (Button) findViewById(R.id.start_button);
        mStartButton.setOnClickListener(mClickListener);
        mBiaSetButton =  (Button) findViewById(R.id.bia_rot);
        mBiaSetButton.setOnClickListener(mClickListener);
        mOffNeAlarmButton = (Button) findViewById(R.id.ne_alram);
        mOffNeAlarmButton.setOnClickListener(mClickListener);

        mEcgctrl = (TextView) findViewById(R.id.text_ecg);
        mEcgst = (EditText) findViewById(R.id.ecg_st);
        mEcged = (EditText) findViewById(R.id.ecg_ed);
        mBiactrl = (TextView) findViewById(R.id.text_rot);
        mRotst = (EditText) findViewById(R.id.rot_st);
        mRoted = (EditText) findViewById(R.id.rot_ed);
        mSaveView = (TextView) findViewById(R.id.save_view);


        mFlag =(Button) findViewById(R.id.flag_button);
        mFlag.setOnClickListener(mClickListener);
        mStand = (RadioButton) findViewById(R.id.Stand);
        mStand.setOnClickListener(flagOnclicklistener);
        mSit = (RadioButton) findViewById(R.id.Sit);
        mSit.setOnClickListener(flagOnclicklistener);
        mLie = (RadioButton) findViewById(R.id.Lie);
        mLie.setOnClickListener(flagOnclicklistener);

        getActionBar().setTitle("NEptuNE_RU v1");
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        getApplicationContext().bindService(new Intent(this, BtleService.class), meta_ServiceConnection, BIND_AUTO_CREATE);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        mPacketParser = new PacketParser();

        mFileManager.createFile("NE_" + (mfile_Num));// Later, it will be user name.
        mFileManager.createMoFile("MO_" + (mfile_Num));// Later, it will be user name.
        Arrays.fill(biaDataArray, 0);
        Arrays.fill(ecgDataArray, 0);
        Arrays.fill(moiDataArray, 0);

        mRotationHandler = new Handler();

//        for (final String deviceUUID : deviceUUIDs) {
////            ConstraintLayout const_layout = (ConstraintLayout) findViewById(R.id.metawear_constraint_layout);
//            View metawearView = inflater.inflate(R.layout.view_metawear, null);
//            metawearView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
//
//            Switch metawearSwitch = (Switch) metawearView.findViewById(R.id.switchMetawear);
//            TextView sensorOutput = (TextView) metawearView.findViewById(R.id.txtSensorOutput);
//            TextView gyrosensorOutput =(TextView) metawearView.findViewById(R.id.txtGyroSensorOutput);
//
//            sensorOutputs.put(deviceUUID, sensorOutput);
//            gyrosensorOutputs.put(deviceUUID, gyrosensorOutput);
//
//            metawearSwitch.setOnCheckedChangeListener((compoundButton, enable) -> {
//                if (enable) {
//                    connectToMetawear(deviceUUID);
//                } else {
//                    stopAccelerometer(deviceUUID);
//                    stopGyro(deviceUUID);
//                }
//            });

        /**
         * left metawear setting
         */
        String left_device = deviceUUIDs[0];
        Switch metawearSwitch_l = (Switch) findViewById(R.id.switchMetawear_l);
        TextView sensorOutput_l = (TextView) findViewById(R.id.txtSensorOutput_l);
        TextView gyrosensorOutput_l =(TextView) findViewById(R.id.txtGyroSensorOutput_l);

        sensorOutputs.put(left_device, sensorOutput_l);
        gyrosensorOutputs.put(left_device, gyrosensorOutput_l);


        metawearSwitch_l.setOnCheckedChangeListener((compoundButton, enable) -> {
            if (enable) {
                connectToMetawear(left_device);
            } else {
                stopAccelerometer(left_device);
                stopGyro(left_device);
            }
        });

        metawearSwitch_l.setText("Device " + left_device);
        sensorOutput_l.setText("- -");
        gyrosensorOutput_l.setText("- -");

        /**
         * right metawear setting
         */

        String right_device = deviceUUIDs[1];
        Switch metawearSwitch_r = (Switch) findViewById(R.id.switchMetawear_r);
        TextView sensorOutput_r = (TextView) findViewById(R.id.txtSensorOutput_r);
        TextView gyrosensorOutput_r =(TextView) findViewById(R.id.txtGyroSensorOutput_r);

        sensorOutputs.put(right_device, sensorOutput_r);
        gyrosensorOutputs.put(right_device, gyrosensorOutput_r);


        metawearSwitch_r.setOnCheckedChangeListener((compoundButton, enable) -> {
            if (enable) {
                connectToMetawear(right_device);
            } else {
                stopAccelerometer(right_device);
                stopGyro(right_device);
            }
        });
        metawearSwitch_r.setText("Device " + right_device);
        sensorOutput_r.setText("- -");
        gyrosensorOutput_r.setText("- -");
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mGattUpdateReceiver);
        unbindService(mServiceConnection);


        try{
            mRotationHandler.removeCallbacks(rotationMethod);
        }catch (Exception e){

        }
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * BIA interval을 하기 위한 메서드
     * */
    int bia_temp=0;
    private Runnable rotationMethod = new Runnable() {
        public void run() {
            if (bia_temp == rot_st ) {
                request_bia_on();
                SystemClock.sleep(100);
//                request_ecg_gain();
//                gain_ed = Integer.parseInt( ((EditText) findViewById(R.id.ecg_ed)).getText().toString());
                request_ecg_ctrl((byte) gain_ed);
                SystemClock.sleep(100);
                Toast.makeText(getApplicationContext(),"Turn on the BIA signal", Toast.LENGTH_SHORT).show();
            }else if(bia_temp == rot_st+rot_ed) {
                request_bia_off();
                SystemClock.sleep(100);
//                request_some();
//                gain_st = Integer.parseInt( ((EditText) findViewById(R.id.ecg_st)).getText().toString());
                request_ecg_ctrl((byte) gain_st);
                SystemClock.sleep(100);
                bia_temp  = -1;
                Toast.makeText(getApplicationContext(),"Turn off the BIA signal", Toast.LENGTH_SHORT).show();
            }
            bia_temp = bia_temp + 1;
            mRotationHandler.postDelayed(rotationMethod, 1000);
        }
    };

    /**
     * 패킷 송신관련
     * */
    //BIA, MOI, ECG 3채널을 요청
    public void request_3ch() {
        Log.d(TAG, String.format("0b 74 0500"));
        BiaMarker = 1;
        setMargauxLWrite(new byte[]{(byte) 0x55, (byte) 0xaa, (byte) 0xff, (byte) 0xff,
                (byte) 0x04, (byte) 0x00, (byte) 0x0B, (byte) 0x74, (byte) 0x00, (byte) 0x05,
                (byte) 0x44, (byte) 0x99, (byte) 0xee, (byte) 0xee});
    }

    //디바이스가 기존에 갖고있던 설정을 초기화함
    public void request_Initial(){
        Log.d(TAG, String.format("0b 70 0001"));
        setMargauxLWrite(new byte[]{(byte) 0x55, (byte) 0xaa, (byte) 0xff, (byte) 0xff,
                (byte) 0x04, (byte) 0x00, (byte) 0x0B, (byte) 0x70, (byte) 0x01, (byte) 0x00,
                (byte) 0x44, (byte) 0x99, (byte) 0xee, (byte) 0xee});
    }

    //streaming을 시작함
    public void request_start() {
        Log.d(TAG, String.format("0b 70 0002"));
        setMargauxLWrite(new byte[]{(byte) 0x55, (byte) 0xaa, (byte) 0xff, (byte) 0xff,
                (byte) 0x04, (byte) 0x00, (byte) 0x0B, (byte) 0x70, (byte) 0x02, (byte) 0x00,
                (byte) 0x44, (byte) 0x99, (byte) 0xee, (byte) 0xee});
    }

    //Powermanager 를 실행함
    public void request_pm() {
        Log.d(TAG, String.format("0b 13 0004"));
        setMargauxLWrite(new byte[]{(byte) 0x55, (byte) 0xaa, (byte) 0xff, (byte) 0xff,
                (byte) 0x04, (byte) 0x00, (byte) 0x0B, (byte) 0x13, (byte) 0x04, (byte) 0x00,
                (byte) 0x44, (byte) 0x99, (byte) 0xee, (byte) 0xee});
    }
    public void request_some() {
        Log.d(TAG, String.format("1B 4001A104 00000001"));
//        mEcgctrl.setText(String.format("1B 4001A104 000000 01"));
        setMargauxLWrite(new byte[]{(byte) 0x55, (byte) 0xaa, (byte) 0xff, (byte) 0xff,
                (byte) 0x09, (byte) 0x00, (byte) 0x1B, (byte) 0x04, (byte) 0xA1, (byte) 0x01, (byte) 0x40,
                (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x44, (byte) 0x99, (byte) 0xee, (byte) 0xee});
    }

    //ECG gain을 07(가장큼)로 설정함
    public void request_ecg_gain() {
        Log.d(TAG, String.format("1B 4001A104 00000007_ecg_gain"));
//        mEcgctrl.setText(String.format("1B 4001A104 000000 07"));
        setMargauxLWrite(new byte[]{(byte) 0x55, (byte) 0xaa, (byte) 0xff, (byte) 0xff,
                (byte) 0x09, (byte) 0x00, (byte) 0x1B, (byte) 0x04, (byte) 0xA1, (byte) 0x01, (byte) 0x40,
                (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x44, (byte) 0x99, (byte) 0xee, (byte) 0xee});
    }
    //input을 받아 해당 수를 ECG gain으로 설정함 (01~07까지 가능)
    public void request_ecg_ctrl(byte input) {
        Log.d(TAG, String.format("1B 4001A104 000000 %02X", input));
//        mEcgctrl.setText(String.format("1B 4001A104 000000 %02X", input));
        setMargauxLWrite(new byte[]{(byte) 0x55, (byte) 0xaa, (byte) 0xff, (byte) 0xff,
                (byte) 0x09, (byte) 0x00, (byte) 0x1B, (byte) 0x04, (byte) 0xA1, (byte) 0x01, (byte) 0x40,
                input, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x44, (byte) 0x99, (byte) 0xee, (byte) 0xee});
    }
    //초기에 연결확인을 요청함
//    public void request_basic() {
//        Log.d(TAG, String.format("0a 00 0000"));
//        setMargauxLWrite(new byte[]{(byte) 0x55, (byte) 0xaa, (byte) 0xff, (byte) 0xff,
//                (byte) 0x04, (byte) 0x00, (byte) 0x0A, (byte) 0x00, (byte) 0x00, (byte) 0x00,
//                (byte) 0x44, (byte) 0x99, (byte) 0xee, (byte) 0xee});
//    }

    //BIA를 끄도록 요청함
    public void request_bia_off() {
        Log.d(TAG, String.format("1B 4001A348 00000000_bia_off"));
//        mBiactrl.setText(String.format("1B 4001A348 000000_00"));
        BiaMarker = 0;
        setMargauxLWrite(new byte[]{(byte) 0x55, (byte) 0xaa, (byte) 0xff, (byte) 0xff,
                (byte) 0x09, (byte) 0x00, (byte) 0x1B, (byte) 0x48, (byte) 0xA3, (byte) 0x01, (byte) 0x40,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x44, (byte) 0x99, (byte) 0xee, (byte) 0xee});
    }
    //BIA를 켜도록 요청함
    public void request_bia_on() {
        Log.d(TAG, String.format("1B 4001A348 00000003_bia_on"));
//        mBiactrl.setText(String.format("1B 4001A348 000000_03"));
        BiaMarker = 1;
        setMargauxLWrite(new byte[]{(byte) 0x55, (byte) 0xaa, (byte) 0xff, (byte) 0xff,
                (byte) 0x09, (byte) 0x00, (byte) 0x1B, (byte) 0x48, (byte) 0xA3, (byte) 0x01, (byte) 0x40,
                (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x44, (byte) 0x99, (byte) 0xee, (byte) 0xee});
    }

    /**
     * 지정 패킷을 BLE로 보내서 write하는 함수
     * input은 패킷이어야함
     *
     * */
    private void setMargauxLWrite(byte[] val)
    {
        try{
            if (mNotifyCharacteristic_W != null) {
                mNotifyCharacteristic_W.setValue(val);
                mBluetoothLeService.writeCharacteristic(mNotifyCharacteristic_W);
            }
        }catch(Exception e){

        }
    }

    /**
     * GATT intent를 관리하는 함수
     * */
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_WRITE);
        return intentFilter;
    }

    /**
     * Meta wear multi connection을 위한 함수
     * */
    public static Task<Void> reconnect(final MetaWearBoard board) {
        return board.connectAsync()
                .continueWithTask(task -> {
                    if (task.isFaulted()) {
                        return reconnect(board);
                    } else if (task.isCancelled()) {
                        return task;
                    }
                    return Task.forResult(null);
                });
    }

    /**
     * mbient Connect
     * @param deviceUUID
     */
    public void connectToMetawear(String deviceUUID){
        BluetoothManager btManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothDevice btDevice = btManager.getAdapter().getRemoteDevice(deviceUUID);
        MetaWearBoard mwBoard = serviceBinder.getMetaWearBoard(btDevice);

//        // Accel, Gyro configure part
//        Accelerometer accelerometer = accelerometerSensors.get(mwBoard.getMacAddress());
//        accelerometer.configure()
//                .odr(100f)       // Set sampling frequency to 100Hz, or closest valid ODR
//                .range(4f)      // Set data range to +/-4g, or closet valid range
//                .commit();
//        GyroBmi160 gyroBmi160 = gyroSensors.get(mwBoard.getMacAddress());
//        gyroBmi160.configure()
//                .odr(OutputDataRate.ODR_100_HZ)
//                .range(Range.FSR_2000)
//                .commit();

        mwBoard.connectAsync()
                .continueWithTask(task -> {
                    if (task.isCancelled()) {
                        return task;
                    }
                    return task.isFaulted() ? reconnect(mwBoard) : Task.forResult(null);
                })
                .continueWith(task -> {
                    if (!task.isCancelled()) {
                        startAccelerometer(mwBoard);
                        startGyro(mwBoard);
                    }
                    return null;
                });
    }

    /**
     * Acc Sensor
     * @param mwBoard
     */
    private void startAccelerometer(MetaWearBoard mwBoard){
        Accelerometer accelerometer = accelerometerSensors.get(mwBoard.getMacAddress());
        if (accelerometer == null) {

            accelerometer = mwBoard.getModule(Accelerometer.class);
            accelerometerSensors.put(mwBoard.getMacAddress(), accelerometer);
        }

        TextView sensorOutput = sensorOutputs.get(mwBoard.getMacAddress());

        accelerometer.acceleration().addRouteAsync(source -> source.stream((data, env) -> {
            final Acceleration value = data.value(Acceleration.class);
            runOnUiThread(() -> sensorOutput.setText(value.x() + ", " + value.y() + ", " + value.z()));
            mFileManager.saveData(mwBoard.getMacAddress(), value.x(), value.y(), value.z(),"accel");
        })).continueWith(task -> {
            streamRoute = task.getResult();
            accelerometerSensors.get(mwBoard.getMacAddress()).acceleration().start();
            accelerometerSensors.get(mwBoard.getMacAddress()).start();
            return null;
        });
    }

    protected void stopAccelerometer(String deviceUUID) {
        Accelerometer accelerometer = accelerometerSensors.get(deviceUUID);
        accelerometer.stop();
        accelerometer.acceleration().stop();
        if (streamRoute != null){
            streamRoute.remove();
        }
    }

    /**
     * Gyro Sensor
     * @param mwBoard
     */
    private void startGyro(MetaWearBoard mwBoard){
        GyroBmi160 gyroBmi160 = gyroSensors.get(mwBoard.getMacAddress());
        if (gyroBmi160 == null) {

            gyroBmi160 = mwBoard.getModule(GyroBmi160.class);
            gyroSensors.put(mwBoard.getMacAddress(), gyroBmi160);
        }

        TextView gyrosensorOutput = gyrosensorOutputs.get(mwBoard.getMacAddress());

        gyroBmi160.angularVelocity().addRouteAsync(source -> source.stream((data, env) -> {
            final AngularVelocity value = data.value(AngularVelocity.class);
            runOnUiThread(() -> gyrosensorOutput.setText(value.x() + ", " + value.y() + ", " + value.z()));
            mFileManager.saveData(mwBoard.getMacAddress(), value.x(), value.y(), value.z(), "gyro");
        })).continueWith(task -> {
            streamRoute = task.getResult();
            gyroSensors.get(mwBoard.getMacAddress()).angularVelocity().start();
            gyroSensors.get(mwBoard.getMacAddress()).start();
            return null;
        });
    }

    protected void stopGyro(String deviceUUID) {
        GyroBmi160 gyroBmi160 =gyroSensors.get(deviceUUID);
        gyroBmi160.stop();
        gyroBmi160.angularVelocity().stop();
        if (streamRoute != null){
            streamRoute.remove();
        }
    }

    /**
     * onclick listener for flag
     */
    RadioButton.OnClickListener flagOnclicklistener
            = new RadioButton.OnClickListener() {

        public void onClick(View v) {
            if (mStand.isChecked()){
                Posture = "Start";
            }else if (mSit.isChecked()){
                Posture = "Pause";
            }else if (mLie.isChecked()){
                Posture = "End";
            }else {
                Posture = "NA";
            }
        }
    };
}