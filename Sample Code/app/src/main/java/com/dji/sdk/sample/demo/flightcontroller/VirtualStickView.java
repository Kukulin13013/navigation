package com.dji.sdk.sample.demo.flightcontroller;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.*;

import com.chaquo.python.Kwarg;
import com.chaquo.python.PyObject;
import com.chaquo.python.android.AndroidPlatform;
import com.chaquo.python.Python;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.internal.OnScreenJoystickListener;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.utils.CallbackHandlers;
import com.dji.sdk.sample.internal.utils.DialogUtils;
import com.dji.sdk.sample.internal.utils.DownloadHandler;
import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;
import com.dji.sdk.sample.internal.utils.OnScreenJoystick;
import com.dji.sdk.sample.internal.utils.ToastUtils;
import com.dji.sdk.sample.internal.view.PresentableView;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.gimbal.CapabilityKey;
import dji.common.gimbal.RotationMode;
import dji.common.util.DJIParamMinMaxCapability;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.flightcontroller.simulator.InitializationData;
import dji.common.flightcontroller.simulator.SimulatorState;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.gimbal.Rotation;
import dji.common.model.LocationCoordinate2D;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.PlaybackManager;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.media.DownloadListener;
import dji.sdk.products.Aircraft;
import dji.sdk.media.MediaFile;
import dji.sdk.media.MediaManager;
import com.dji.sdk.sample.demo.flightcontroller.GPSRotateAngle;

import org.jetbrains.annotations.NotNull;

import dji.keysdk.FlightControllerKey;
import dji.keysdk.KeyManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.flightcontroller.Simulator;
import dji.sdk.sdkmanager.DJISDKManager;

//TODO: Refactor needed

/**
 * Class for virtual stick.
 */
public class VirtualStickView extends RelativeLayout
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, PresentableView {

    private boolean yawControlModeFlag = true;
    private boolean rollPitchControlModeFlag = true;
    private boolean verticalControlModeFlag = true;
    private boolean horizontalCoordinateFlag = true;

    private Gimbal gimbal = null;
    private int currentGimbalId = 0;

    private MediaFile media;
    private MediaManager mediaManager ;
    private int DownloadFlag = 0;

    private Button btnEnableVirtualStick;
    private Button btnDisableVirtualStick;
    private Button btnHorizontalCoordinate;
    private Button btnSetYawControlMode;
    private Button btnSetVerticalControlMode;
    private Button btnSetRollPitchControlMode;
    private ToggleButton btnSimulator;
    private Button btnTakeOff;
    private Button btnSetZero;
    private Button btnLeftUp;
    private Button btnRoll;
    private Button btnPitch;
    private Button btnYaw;
    private Button btnGetGps;
    private Button btnCalAngle;
    private Button btnCamera;
    private Button btnGimbalReset;
    private Button btnSDcardFile;
    private Button btnPython;

    private TextView textView;
    private TextView timerView;

    private OnScreenJoystick screenJoystickRight;
    private OnScreenJoystick screenJoystickLeft;

    private Timer sendVirtualStickDataTimer;
    private SendVirtualStickDataTask sendVirtualStickDataTask;
    //private Timer checkLocationArrivedTimer;
    private CheckLocationArrivedTask checkLocationArrivedTask;

    private float ArrivedFlag = 0;
    private float pitch;
    private float roll;
    private float yaw;
    private float throttle;
    private double lat1;
    private double lat2 = 22.0016;
    private double lon1;
    private double lon2 = 113.0015;
    private FlightControllerKey isSimulatorActived;

    public VirtualStickView(Context context) {
        super(context);
        init(context);
    }

    @NonNull
    @Override
    public String getHint() {
        return this.getClass().getSimpleName() + ".java";
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setUpListeners();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (null != sendVirtualStickDataTimer) {
            if (sendVirtualStickDataTask != null) {
                sendVirtualStickDataTask.cancel();

            }
            sendVirtualStickDataTimer.cancel();
            sendVirtualStickDataTimer.purge();
            sendVirtualStickDataTimer = null;
            sendVirtualStickDataTask = null;
        }
        tearDownListeners();
        super.onDetachedFromWindow();
    }

    private void init(Context context) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.view_virtual_stick, this, true);

        initAllKeys();
        initUI();
    }

    private void PythonCode(){
        Python py = Python.getInstance();
        PyObject obj1 = py.getModule("hello").callAttr("add", 2,3);
        // 将Python返回值换为Java中的Integer类型
        Integer sum = obj1.toJava(Integer.class);
        ToastUtils.setResultToToast("sum = "+sum);

    }

    private void initAllKeys() {
        isSimulatorActived = FlightControllerKey.create(FlightControllerKey.IS_SIMULATOR_ACTIVE);
    }

    private void initUI() {
        btnEnableVirtualStick = (Button) findViewById(R.id.btn_enable_virtual_stick);
        btnDisableVirtualStick = (Button) findViewById(R.id.btn_disable_virtual_stick);
        btnHorizontalCoordinate = (Button) findViewById(R.id.btn_horizontal_coordinate);
        btnSetYawControlMode = (Button) findViewById(R.id.btn_yaw_control_mode);
        btnSetVerticalControlMode = (Button) findViewById(R.id.btn_vertical_control_mode);
        btnSetRollPitchControlMode = (Button) findViewById(R.id.btn_roll_pitch_control_mode);
        btnTakeOff = (Button) findViewById(R.id.btn_take_off);
        btnSetZero = (Button) findViewById(R.id.btn_set_zero);
        btnLeftUp = (Button) findViewById(R.id.btn_left_up);
        btnRoll = (Button) findViewById(R.id.btn_roll);
        btnPitch = (Button) findViewById(R.id.btn_pitch);
        btnYaw = (Button) findViewById(R.id.btn_yaw);
        btnGetGps = (Button) findViewById(R.id.btn_get_gps);
        btnCalAngle = (Button) findViewById(R.id.btn_cal_angle);
        btnCamera = (Button) findViewById(R.id.btn_camera_shoot);
        btnGimbalReset = (Button) findViewById(R.id.btn_gimbal_reset);
        btnSDcardFile = (Button) findViewById(R.id.btn_get_sdfile);
        btnSimulator = (ToggleButton) findViewById(R.id.btn_start_simulator);
        btnPython = (Button) findViewById(R.id.btn_python);

        textView = (TextView) findViewById(R.id.textview_simulator);
        timerView = (TextView) findViewById(R.id.textview_timertask);

        screenJoystickRight = (OnScreenJoystick) findViewById(R.id.directionJoystickRight);
        screenJoystickLeft = (OnScreenJoystick) findViewById(R.id.directionJoystickLeft);

        btnEnableVirtualStick.setOnClickListener(this);
        btnDisableVirtualStick.setOnClickListener(this);
        btnHorizontalCoordinate.setOnClickListener(this);
        btnSetYawControlMode.setOnClickListener(this);
        btnSetVerticalControlMode.setOnClickListener(this);
        btnSetRollPitchControlMode.setOnClickListener(this);
        btnTakeOff.setOnClickListener(this);
        btnSetZero.setOnClickListener(this);
        btnLeftUp.setOnClickListener(this);
        btnRoll.setOnClickListener(this);
        btnPitch.setOnClickListener(this);
        btnYaw.setOnClickListener(this);
        btnGetGps.setOnClickListener(this);
        btnCalAngle.setOnClickListener(this);
        btnCamera.setOnClickListener(this);
        btnGimbalReset.setOnClickListener(this);
        btnSDcardFile.setOnClickListener(this);
        btnPython.setOnClickListener(this);
        btnSimulator.setOnCheckedChangeListener(VirtualStickView.this);

        Boolean isSimulatorOn = (Boolean) KeyManager.getInstance().getValue(isSimulatorActived);
        if (isSimulatorOn != null && isSimulatorOn) {
            btnSimulator.setChecked(true);
            textView.setText("Simulator is On.");
        }
    }

    private void setUpListeners() {
        Simulator simulator = ModuleVerificationUtil.getSimulator();
        if (simulator != null) {
            simulator.setStateCallback(new SimulatorState.Callback() {
                @Override
                public void onUpdate(@NonNull final SimulatorState simulatorState) {
                    ToastUtils.setResultToText(textView,
                            "Yaw : "
                                    + simulatorState.getYaw()
                                    + ","
                                    + "X : "
                                    + simulatorState.getPositionX()
                                    + "\n"
                                    + "Y : "
                                    + simulatorState.getPositionY()
                                    + ","
                                    + "Z : "
                                    + simulatorState.getPositionZ());
                }
            });
        } else {
            ToastUtils.setResultToToast("Disconnected!");
        }

    }

    private void tearDownListeners() {
        Simulator simulator = ModuleVerificationUtil.getSimulator();
        if (simulator != null) {
            simulator.setStateCallback(null);
        }
       screenJoystickLeft.setJoystickListener(null);
       screenJoystickRight.setJoystickListener(null);
    }

    private Gimbal getGimbalInstance() {
        if (gimbal == null) {
            initGimbal();
        }
        return gimbal;
    }

    private void initGimbal() {
        if (DJISDKManager.getInstance() != null) {
            BaseProduct product = DJISDKManager.getInstance().getProduct();
            if (product != null) {
                if (product instanceof Aircraft) {
                    gimbal = ((Aircraft) product).getGimbals().get(currentGimbalId);
                } else {
                    gimbal = product.getGimbal();
                }
            }
        }
    }

    private void sendRotateGimbalCommand(Rotation rotation) {

        Gimbal gimbal = getGimbalInstance();
        if (gimbal == null) {
            return;
        }

        gimbal.rotate(rotation, new CallbackHandlers.CallbackToastHandler());
    }

    private boolean isModuleAvailable() {
        return (null != DJISampleApplication.getProductInstance()) && (null != DJISampleApplication.getProductInstance()
                .getCamera());
    }



    @Override
    public void onClick(View v) {
        FlightController flightController = ModuleVerificationUtil.getFlightController();
        if (flightController == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.btn_enable_virtual_stick:
                flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        DialogUtils.showDialogBasedOnError(getContext(), djiError);
                    }
                });

                DJISampleApplication.getAircraftInstance().getFlightController().
                        setRollPitchCoordinateSystem(FlightCoordinateSystem.GROUND);//roll body
                DJISampleApplication.getAircraftInstance().getFlightController().
                        setVerticalControlMode(VerticalControlMode.POSITION);// vertical
                DJISampleApplication.getAircraftInstance().getFlightController().
                        setYawControlMode(YawControlMode.ANGULAR_VELOCITY);//angular_velocity
                DJISampleApplication.getAircraftInstance().getFlightController().
                        setRollPitchControlMode(RollPitchControlMode.VELOCITY);//angle
                ToastUtils.setResultToToast(flightController.getRollPitchControlMode().name());

                //定时器用来定时，200ms发送一次数据

                if (null == sendVirtualStickDataTimer) {
                    sendVirtualStickDataTask = new SendVirtualStickDataTask();
                    sendVirtualStickDataTimer = new Timer();
                    sendVirtualStickDataTimer.schedule(sendVirtualStickDataTask, 100, 200);
                }

                break;

            case R.id.btn_disable_virtual_stick:
                flightController.setVirtualStickModeEnabled(false, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        DialogUtils.showDialogBasedOnError(getContext(), djiError);
                    }
                });
                break;

            case R.id.btn_roll_pitch_control_mode:
                if (rollPitchControlModeFlag) {
                    flightController.setRollPitchControlMode(RollPitchControlMode.ANGLE);
                    rollPitchControlModeFlag = false;
                } else {
                    flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
                    rollPitchControlModeFlag = true;
                }
                try {
                    ToastUtils.setResultToToast(flightController.getRollPitchControlMode().name());
                } catch (Exception ex) {
                }
                break;

            case R.id.btn_yaw_control_mode:
                if (yawControlModeFlag) {
                    flightController.setYawControlMode(YawControlMode.ANGLE);
                    yawControlModeFlag = false;
                } else {
                    flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
                    yawControlModeFlag = true;
                }
                try {
                    ToastUtils.setResultToToast(flightController.getYawControlMode().name());
                } catch (Exception ex) {
                }
                break;

            case R.id.btn_vertical_control_mode:
                if (verticalControlModeFlag) {
                    flightController.setVerticalControlMode(VerticalControlMode.POSITION);
                    verticalControlModeFlag = false;
                } else {
                    flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
                    verticalControlModeFlag = true;
                }
                try {
                    ToastUtils.setResultToToast(flightController.getVerticalControlMode().name());
                } catch (Exception ex) {
                }
                break;

            case R.id.btn_horizontal_coordinate:
                if (horizontalCoordinateFlag) {
                    flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.GROUND);
                    horizontalCoordinateFlag = false;
                } else {
                    flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
                    horizontalCoordinateFlag = true;
                }
                try {
                    ToastUtils.setResultToToast(flightController.getRollPitchCoordinateSystem().name());
                } catch (Exception ex) {
                }
                break;

            case R.id.btn_take_off:

                flightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        DialogUtils.showDialogBasedOnError(getContext(), djiError);

                    }
                });

                break;

            case R.id.btn_set_zero:
                yaw = 0;
                pitch = 0;
                roll = 0;
                throttle = 1f;
                break;

            case R.id.btn_left_up:
                yaw = 0;
                pitch = 0.02f;
                roll = 0.02f;
                throttle = 3f;
                break;

            case R.id.btn_roll:
                yaw = 0;
                pitch = 0;
                roll = 0.5f;
                throttle = 1.5f;
                break;

            case R.id.btn_pitch:
                yaw = 0;
                pitch = 0.5f;
                roll = 0;
                throttle = 1.5f;
                break;

            case R.id.btn_yaw:
                yaw = 3f;
                pitch = 0;
                roll = 0;
                throttle = 1.5f;
                break;

            case R.id.btn_get_gps:
                double latitude = 22;
                double longtitude = 113;
                float altitude =1.5f;

                FlightControllerState GPSLocation = new FlightControllerState();
                GPSLocation = flightController.getState();
                LocationCoordinate3D locationCoordinate3D = new LocationCoordinate3D(latitude,longtitude,altitude);
                locationCoordinate3D = GPSLocation.getAircraftLocation();
                lat1 = locationCoordinate3D.getLatitude();
                lon1 = locationCoordinate3D.getLongitude();
                //ToastUtils.setResultToToast( locationCoordinate3D.toString() );

                double setGPSData[] = {lat2,lon2};//zhezhi
                double reqGPSData_vice[] = {lat1,lon1};//yuan
                double[] CalResult = GPSRotateAngle.CalAngle(reqGPSData_vice, setGPSData);
                double Distance2Point = CalResult[0];
                double RotateAngle = CalResult[1];

                if(ArrivedFlag == 1){
                    checkLocationArrivedTask.cancel();
                    timerView.setText("arrived");
                }

                ToastUtils.setResultToToast( "location=" + locationCoordinate3D.toString() + "\n"
                        + "ANGEL=" + RotateAngle + "\n"
                        + " DISTANCE=" + Distance2Point + "\n"
                        + " roll=" + roll + "\n"
                        + " pitch=" + pitch
                );

                break;

            case R.id.btn_cal_angle:


                if (null == checkLocationArrivedTask) {
                    checkLocationArrivedTask = new CheckLocationArrivedTask();
                    sendVirtualStickDataTimer.schedule(checkLocationArrivedTask,100,500);
                }
                break;

            case R.id.btn_camera_shoot:
                Gimbal gimbal = getGimbalInstance();
                if (gimbal == null) {
                    return;
                }

                Object key = CapabilityKey.ADJUST_PITCH;
                Number minValue = ((DJIParamMinMaxCapability) (gimbal.getCapabilities().get(key))).getMin();
                Rotation.Builder builder = new Rotation.Builder().mode(RotationMode.ABSOLUTE_ANGLE).time(2);
                builder.pitch(minValue.floatValue());
                sendRotateGimbalCommand(builder.build());

                if (isModuleAvailable()) {
                    if (ModuleVerificationUtil.isMavicAir2()){
                        DJISampleApplication.getProductInstance()
                                .getCamera()
                                .setFlatMode(SettingsDefinitions.FlatCameraMode.PHOTO_SINGLE, new CommonCallbacks.CompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {
                                        if (djiError == null){
                                            ToastUtils.setResultToToast("success set photo single");
                                        }else {
                                            ToastUtils.setResultToToast("faliure set photo single");
                                        }
                                    }
                                });
                        /*DJISampleApplication.getProductInstance()
                                .getCamera()
                                .setFlatMode(SettingsDefinitions.FlatCameraMode.PHOTO_SINGLE, djiError -> ToastUtils.setResultToToast("SetCameraMode to shootPhoto"));*/
                    }

                    DJISampleApplication.getProductInstance()
                            .getCamera()
                            .startShootPhoto(new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (null == djiError) {
                                        ToastUtils.setResultToToast("success shooting");
                                        //ToastUtils.setResultToToast(getContext().getString(R.string.success));
                                    } else {
                                        ToastUtils.setResultToToast(djiError.getDescription());
                                    }
                                }
                            });

                }
                break;

            case R.id.btn_gimbal_reset:
                Gimbal gimbal1 = getGimbalInstance();
                if (gimbal1 != null) {
                    gimbal1.reset(null);
                } else {
                    ToastUtils.setResultToToast("The gimbal is disconnected.");
                }
                break;

            case R.id.btn_get_sdfile:
                ToastUtils.setResultToToast("btnsdfile");
                if (ModuleVerificationUtil.isCameraModuleAvailable()) {
                    if (ModuleVerificationUtil.isMediaManagerAvailable()) {
                        Camera myCamera = DJISampleApplication.getProductInstance().getCamera();
                        if (mediaManager == null) {
                            mediaManager = myCamera.getMediaManager();//DJISampleApplication.getProductInstance().getCamera().getMediaManager();
                            fetchMediaList();
                            ToastUtils.setResultToToast("media get success");
                        }

                        if (myCamera.isFlatCameraModeSupported()){
                            ToastUtils.setResultToToast("flat supported");
                            myCamera.setFlatMode(SettingsDefinitions.FlatCameraMode.PHOTO_SINGLE, new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    ToastUtils.setResultToToast("setflatmode_success");
                                }
                            });
                            myCamera.enterPlayback(new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError == null){
                                        ToastUtils.setResultToToast("entermode_success_playback");
                                        myCamera.getMode(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.CameraMode>() {
                                            @Override
                                            public void onSuccess(SettingsDefinitions.CameraMode cameraMode) {
                                               if (cameraMode == SettingsDefinitions.CameraMode.PLAYBACK){
                                                   fetchMediaList();
                                                   if (media == null){
                                                       fetchMediaList();
                                                   }else if (media != null){
                                                       ToastUtils.setResultToToast("media is not null"+media.getFileName());

                                                       File destDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/");
                                                       //media.fetchFileData(destDir,"djiphoto",new DownloadHandler<>());
                                                       media.fetchFileData(destDir, "testName_djiphoto", new DownloadListener<String>() {
                                                           @Override
                                                           public void onStart() {

                                                           }

                                                           @Override
                                                           public void onRateUpdate(long l, long l1, long l2) {

                                                           }

                                                           @Override
                                                           public void onRealtimeDataUpdate(byte[] bytes, long l, boolean b) {

                                                           }

                                                           @Override
                                                           public void onProgress(long l, long l1) {

                                                           }

                                                           @Override
                                                           public void onSuccess(String s) {
                                                               ToastUtils.setResultToToast("download success");
                                                           }

                                                           @Override
                                                           public void onFailure(DJIError djiError) {
                                                               ToastUtils.setResultToToast("download fail" + djiError);
                                                           }
                                                       });
                                                   }
                                               }
                                            }

                                            @Override
                                            public void onFailure(DJIError djiError) {

                                            }
                                        });

                                    }
                                    else {
                                        ToastUtils.setResultToToast("setmodeerror"+djiError);
                                    }
                                }
                            });


                            //fetchMediaList();

                        }else {
                            ToastUtils.setResultToToast("playback not supported");
                        }
                    } else {
                        ToastUtils.setResultToToast("not supported");
                    }
                }
                break;

            case R.id.btn_python:
                PythonCode();
                break;

            default:
                break;
        }
    }

    private void fetchMediaList() {
        ToastUtils.setResultToToast("fetchmeadialist");
        if (ModuleVerificationUtil.isMediaManagerAvailable()) {
            if (mediaManager != null) {
                mediaManager.refreshFileListOfStorageLocation(SettingsDefinitions.StorageLocation.SDCARD, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        String str;
                        if (null == djiError) {
                            List<MediaFile> djiMedias = mediaManager.getSDCardFileListSnapshot();

                            if (null != djiMedias) {
                                if (!djiMedias.isEmpty()) {
                                    media = djiMedias.get(1);
                                    str = media.getFileName();

                                    if (media == null){
                                        ToastUtils.setResultToToast("media is null:" + (media == null));
                                    }
                                    else {
                                        ToastUtils.setResultToToast("media is not null, get file:" + str + media.getFileSize());
                                    }

                                    //ToastUtils.setResultToToast("get file: " + str);

                                } else {
                                    str = "No Media in SD Card";
                                    ToastUtils.setResultToToast(str);
                                }

                            }
                        } else {
                            ToastUtils.setResultToToast("refresh fail" + djiError);
                        }
                    }
                });
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (compoundButton == btnSimulator) {
            onClickSimulator(b);
        }
    }

    private void onClickSimulator(boolean isChecked) {
        Simulator simulator = ModuleVerificationUtil.getSimulator();
        if (simulator == null) {
            return;
        }
        if (isChecked) {

            textView.setVisibility(VISIBLE);

            simulator.start(InitializationData.createInstance(new LocationCoordinate2D(23, 113), 10, 10),
                    new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {

                        }
                    });
        } else {

            textView.setVisibility(INVISIBLE);

            simulator.stop(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {

                }
            });
        }
    }

    @Override
    public int getDescription() {
        return R.string.flight_controller_listview_virtual_stick;
    }

    private class SendVirtualStickDataTask extends TimerTask {

        @Override
        public void run() {
            if (ModuleVerificationUtil.isFlightControllerAvailable()) {
                DJISampleApplication.getAircraftInstance()
                        .getFlightController()
                        .sendVirtualStickFlightControlData(new FlightControlData(pitch,
                                        roll,
                                        yaw,
                                        throttle),
                                new CommonCallbacks.CompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {

                                    }
                                });
            }
        }
    }

    private class CheckLocationArrivedTask extends TimerTask {
        double PresentLatitude ;
        double PresentLongtitude;
        float PresentAltitude;
        @Override
        public void run() {

            FlightController flightController2 = ModuleVerificationUtil.getFlightController();
            FlightControllerState GPSLocation = new FlightControllerState();
            GPSLocation = flightController2.getState();
            LocationCoordinate3D locationCoordinate3D2 = new LocationCoordinate3D(PresentLatitude, PresentLongtitude, PresentAltitude);
            locationCoordinate3D2 = GPSLocation.getAircraftLocation();
            PresentLatitude = locationCoordinate3D2.getLatitude();
            PresentLongtitude = locationCoordinate3D2.getLongitude();
            double[] PresentLocation = {PresentLatitude,PresentLongtitude};
            double[] SetGPSData = {lat2,lon2};
            double[] CalRes = GPSRotateAngle.CalAngle(PresentLocation, SetGPSData);
            double[] FlyData = GPSRotateAngle.CalControlData(CalRes);
            if(CalRes[0]<50){
                GPSRotateAngle.BaseVer = 10.0;
                if (CalRes[0]<30){
                    GPSRotateAngle.BaseVer = 8.0;
                    if (CalRes[0]<10){
                        GPSRotateAngle.BaseVer = 5.0;
                        if (CalRes[0]<5){
                            GPSRotateAngle.BaseVer = 2.0;
                            if (CalRes[0]<2){
                                GPSRotateAngle.BaseVer = 0.1;
                            }
                        }
                    }
                }
            }
            if(CalRes[0]<0.2){
                yaw = 0;
                pitch = 0;
                roll = 0;
                throttle = 1.5f;
                //timerView.setText("arrived");
                ArrivedFlag = 1;
                //checkLocationArrivedTask.cancel();
            }
            else {
                yaw = 0;
                pitch = (float)FlyData[1];
                roll = (float)FlyData[0];
                throttle = 1.5f;

            }


        }
    }
}



