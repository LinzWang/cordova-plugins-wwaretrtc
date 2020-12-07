package org.wware.wwaretrtc.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.liteav.TXLiteAVCode;
import com.tencent.liteav.beauty.TXBeautyManager;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.tencent.trtc.TRTCCloud;
import com.tencent.trtc.TRTCCloudDef;
import com.tencent.trtc.TRTCCloudListener;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static com.tencent.trtc.TRTCCloudDef.TRTCRoleAnchor;
import static com.tencent.trtc.TRTCCloudDef.TRTC_APP_SCENE_VIDEOCALL;

import org.wware.wwaretrtc.wwaretrtc;
import org.wware.wwaretrtc.GenerateTestUserSig;

/*
*根据TRTC的sampleDemo更改而成的类，主要用于构建视频通话页面，仅保留部分功能
*-切换前置/后置摄像头
*-打开/关闭摄像头
*-打开/关闭麦克风
*显示通话双方的视频画面
*/

//implements View.OnClickListener 
public class RTCActivity extends Activity  {

    private static final String TAG = "RTCActivity";

    private TextView                        mTitleText;                 //【控件】页面Title
    private TextView                        mCountText;                 //【控件】显示文字信息，倒计时，欢迎信息等
    private TXCloudVideoView                mLocalPreviewView;          //【控件】本地画面View
    private TXCloudVideoView                mRemoteView;                //【】远程画面控件
    private ImageView                       mBackButton;                //【控件】返回上一级页面
    private Button                          mMuteVideo;                 //【控件】是否停止推送本地的视频数据
    private Button                          mMuteAudio;                 //【控件】开启、关闭本地声音采集并上行
    //private Button                          mSwitchCamera;              //【控件】切换摄像头
    private Button                          mLogInfo;                   //【控件】开启、关闭日志显示
    private Button                          mHandfree;                  //【控件】开启关闭免提  
    private LinearLayout                    mVideoMutedTipsView;        //【控件】关闭视频时，显示默认头像

    private TRTCCloud                       mTRTCCloud;                 // SDK 核心类
    private boolean                         mIsFrontCamera = true;      // 默认摄像头前置
    private List<String>                    mRemoteUidList;             // 远端用户Id列表
    private List<TXCloudVideoView>          mRemoteViewList;            // 远端画面列表
    private int                             mGrantedCount = 0;          // 权限个数计数，获取Android系统权限
    private int                             mUserCount = 0;             // 房间通话人数个数
    private int                             mLogLevel = 0;              // 日志等级
    private String                          mRoomId;                    // 房间Id
    private String                          mUserId;                    // 用户Id
    private String                          mAppid;                     // SDKAPPID
    private String                          mUsername;
    private String                          mUserSig = null;
    private String                          mEstimateTime;              //预约通话时间
    private int                             mParseSec;                   //转化为秒数的长度
    private String                          startTime;
    private String                          endTime;
    private String                          errorMsg;
    private boolean                         exchancepreview;
    private boolean                         firstEnter;
    private boolean                         connected;
    private boolean                         closeCamera;
    private boolean                         closeMic;
    private Timer                           mtimer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Log.i(TAG,"enter RTCActivity");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(wwaretrtc.getResourceId("activity_rtc","layout"));
        //getSupportActionBar().hide(); //work on AppCompatActivity
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); 
        handleIntent();
        exchancepreview = false;
        connected = true;
        closeCamera = false;
        closeMic = false;
        // 进入房间代表之前申请过权限
        initView();
        enterRoom();
    }
    
    private void handleIntent() {
      //接受并处理从前一个activity传来的参数
        Intent intent = getIntent();
        if (null != intent) {
            if (intent.getStringExtra("userId") != null) {
              mUserId = intent.getStringExtra("userId");
            }
            if (intent.getStringExtra("roomId") != null) {
              mRoomId = intent.getStringExtra("roomId");
            }
            if(intent.getStringExtra("appid") != null){
              mAppid = intent.getStringExtra("appid");
            }
            if(intent.getStringExtra("username") != null){
              mUsername = intent.getStringExtra("username");
            }
            if(intent.getStringExtra("userSig") != null){
              mUserSig = intent.getStringExtra("userSig");
            }
            if(intent.getStringExtra("estimateTime") != null){
              mEstimateTime = intent.getStringExtra("estimateTime");
              mParseSec = Integer.parseInt(mEstimateTime);
            }
        }
    }

    private void initView(){
        mTitleText          = findViewById(wwaretrtc.getResourceId("trtc_tv_room_number","id"));
        mCountText          = findViewById(wwaretrtc.getResourceId("countdown_text","id"));
        mBackButton         = findViewById(wwaretrtc.getResourceId("trtc_ic_back","id"));
        mLocalPreviewView   = findViewById(wwaretrtc.getResourceId("trtc_tc_cloud_view_main","id"));
        mHandfree           = findViewById(wwaretrtc.getResourceId("trtc_btn_handfree","id"));
        mMuteVideo          = findViewById(wwaretrtc.getResourceId("trtc_btn_mute_video","id"));
        mMuteAudio          = findViewById(wwaretrtc.getResourceId("trtc_btn_mute_audio","id"));
        //mSwitchCamera       = findViewById(wwaretrtc.getResourceId("trtc_btn_switch_camera","id"));
        //mLogInfo            = findViewById(wwaretrtc.getResourceId("trtc_btn_log_info","id"));
        mVideoMutedTipsView = findViewById(wwaretrtc.getResourceId("ll_trtc_mute_video_default","id"));
        //mRemoteView         = findViewById(wwaretrtc.getResourceId("trtc_tc_cloud_view_1","id"));
        //修改mEstimateTime为秒
        int cmm = mParseSec / 60 ;
        int css = mParseSec % 60;
        mCountText.setText("剩余时间："+String.format(Locale.ENGLISH,"%02d",cmm)+":"+String.format(Locale.ENGLISH,"%02d",css));
        /*if (!TextUtils.isEmpty(mUsername)) {
            String tmpstr = mUsername+"的咨询室";
            mTitleText.setText(tmpstr);
        }*/

        mBackButton.setOnClickListener(view -> {
            exitRoom();
          });

        mMuteVideo.setOnClickListener(view ->{
          if(closeCamera){
                Toast.makeText(this,"无法双方均关闭摄像头",Toast.LENGTH_SHORT).show();
            }else{
                muteVideo();
            }
        });
        mMuteAudio.setOnClickListener(view ->{
          if(closeMic){
                Toast.makeText(this,"无法双方均关闭麦克风",Toast.LENGTH_SHORT).show();
            }else{
               muteAudio(); 
            }
        });
        
        mHandfree.setOnClickListener(view ->{
          handfree();
        });

        /*mSwitchCamera.setOnClickListener(new View.OnClickListener(){
          @Override
          public void onClick(View v){
            switchCamera();
            //Toast.makeText(this,"test",Toast.LENGTH_SHORT).show();
          }
        });*/
        //mLogInfo.setOnClickListener(this);
        /*mLogInfo.setOnClickListener(view->{
            showDebugView();
        });*/

        mRemoteUidList = new ArrayList<>();
        mRemoteViewList = new ArrayList<>();
        mRemoteViewList.add((TXCloudVideoView)findViewById(wwaretrtc.getResourceId("trtc_tc_cloud_view_1","id")));
        
    }
    private void enterRoom() {
        mTRTCCloud = TRTCCloud.sharedInstance(getApplicationContext());
        mTRTCCloud.setListener(new TRTCCloudImplListener(RTCActivity.this));

        // 初始化配置 SDK 参数
        TRTCCloudDef.TRTCParams trtcParams = new TRTCCloudDef.TRTCParams();
        trtcParams.sdkAppId = Integer.parseInt(mAppid);
        trtcParams.userId = mUserId;
        trtcParams.roomId = Integer.parseInt(mRoomId);
        // userSig是进入房间的用户签名，相当于密码（这里生成的是测试签名，正确做法需要业务服务器来生成，然后下发给客户端）
        if(mUserSig!= null && mUserSig.length() != 0){
          trtcParams.userSig = mUserSig;
        }else{
          trtcParams.userSig = GenerateTestUserSig.genTestUserSig(trtcParams.userId);
        }
        Log.i(TAG,"userSig is :"+trtcParams.userSig);
        //视频通话默认设置为主播角色
        trtcParams.role = TRTCRoleAnchor;

        // 进入通话
        mTRTCCloud.enterRoom(trtcParams, TRTC_APP_SCENE_VIDEOCALL);
        // 开启本地声音采集并上行
        mTRTCCloud.startLocalAudio();
        // 开启本地画面采集并上行
        mTRTCCloud.startLocalPreview(mIsFrontCamera, mLocalPreviewView);

        /*
         * 设置默认美颜效果（美颜效果：自然，美颜级别：5, 美白级别：1）
         * setBeautyStyle 美颜风格.三种美颜风格：0 ：光滑  1：自然  2：朦胧
         * setBeautyLevel 美颜级别，取值范围0 - 9； 0表示关闭，1 - 9值越大，效果越明显。
         * setWhitenessLevel 美白级别，取值范围0 - 9； 0表示关闭，1 - 9值越大，效果越明显。
         * 视频通话场景推荐使用“自然”美颜效果
         */
        TXBeautyManager beautyManager = mTRTCCloud.getBeautyManager();
        beautyManager.setBeautyStyle(1);
        beautyManager.setBeautyLevel(5);
        beautyManager.setWhitenessLevel(1);
        // RTC 通话场景：640*360，15fps，550kbps
        TRTCCloudDef.TRTCVideoEncParam encParam = new TRTCCloudDef.TRTCVideoEncParam();
        encParam.videoResolution = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_640_360;
        encParam.videoFps = 15;
        encParam.videoBitrate = 550;
        encParam.videoResolutionMode = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_MODE_PORTRAIT;
        mTRTCCloud.setVideoEncoderParam(encParam);
    }
    @Override
    protected void onDestroy() {
        if(mtimer != null){
            mtimer.cancel();
            mtimer = null;
        }
        super.onDestroy();
        //Log.i(TAG,"clickback call onDestroy================");
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //exitRoom();
    }
    /*
    * 中断本地推送及远程接收
    */
    private void stopTRTC(){
      mTRTCCloud.stopLocalAudio();
      mTRTCCloud.stopLocalPreview(); 
    }
    

    /**
     * 离开通话
     */
    private void exitRoom() {
        //Log.i(TAG,"clickback call exitRoom================");
        Intent intent = getIntent();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        endTime = sdf.format(new Date());
        intent.putExtra("endtime",endTime);
        intent.putExtra("starttime",startTime);
        intent.putExtra("errormsg",errorMsg);
        setResult(0,intent);

        mTRTCCloud.stopLocalAudio();
        mTRTCCloud.stopLocalPreview(); 
        mTRTCCloud.exitRoom();
        //销毁 trtc 实例
        if (mTRTCCloud != null) {
            mTRTCCloud.setListener(null);
        }
        mTRTCCloud = null;
        TRTCCloud.destroySharedInstance();
        finish();
    }
    /*@Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == wwaretrtc.getResourceId("trtc_ic_back","id")) {
            finish();
        } else if (id == wwaretrtc.getResourceId("trtc_btn_mute_video","id")) {
            muteVideo();
        } else if (id == wwaretrtc.getResourceId("trtc_btn_mute_audio","id")) {
            muteAudio();
        } else if (id == wwaretrtc.getResourceId("trtc_btn_switch_camera","id")) {
            switchCamera();
        } else if (id == wwaretrtc.getResourceId("trtc_btn_log_info","id")) {
            showDebugView();
        }
    }*/
    /*
    * 免提
    * TRTC_AUDIO_ROUTE_EARPIECE 听筒 TRTC_AUDIO_ROUTE_SPEAKER 扬声器
    */
    private void handfree(){
      boolean isSelected = mHandfree.isSelected();
        if (!isSelected) {
            mTRTCCloud.setAudioRoute(TRTCCloudDef.TRTC_AUDIO_ROUTE_EARPIECE);
            mHandfree.setBackground(getDrawable(wwaretrtc.getResourceId("trtccalling_ic_handsfree_disable","mipmap")));
           
        } else {
            mTRTCCloud.setAudioRoute(TRTCCloudDef.TRTC_AUDIO_ROUTE_SPEAKER);
            mHandfree.setBackground(getDrawable(wwaretrtc.getResourceId("trtccalling_ic_handsfree_enable","mipmap")));
            
        }
        mHandfree.setSelected(!isSelected);
    }
    private void muteVideo() {
        boolean isSelected = mMuteVideo.isSelected();
        if (!isSelected) {
            mTRTCCloud.stopLocalPreview();
            mMuteVideo.setBackground(getDrawable(wwaretrtc.getResourceId("rtc_camera_off","mipmap")));
            //mVideoMutedTipsView.setVisibility(View.VISIBLE);
        } else {
            if(exchancepreview){
                mTRTCCloud.startLocalPreview(mIsFrontCamera,mRemoteViewList.get(0));
            }else{
                mTRTCCloud.startLocalPreview(mIsFrontCamera, mLocalPreviewView);
            }
            mMuteVideo.setBackground(getDrawable(wwaretrtc.getResourceId("rtc_camera_on","mipmap")));
            //mVideoMutedTipsView.setVisibility(View.GONE);
        }
        mMuteVideo.setSelected(!isSelected);
    }

    private void muteAudio() {
        boolean isSelected = mMuteAudio.isSelected();
        if (!isSelected) {
            mTRTCCloud.stopLocalAudio();

            mMuteAudio.setBackground(getDrawable(wwaretrtc.getResourceId("rtc_mic_off","mipmap")));
        } else {
            mTRTCCloud.startLocalAudio();
            mMuteAudio.setBackground(getDrawable(wwaretrtc.getResourceId("rtc_mic_on","mipmap")));
        }
        mMuteAudio.setSelected(!isSelected);
    }

    /*private void switchCamera() {
        mTRTCCloud.switchCamera();
        boolean isSelected = mSwitchCamera.isSelected();
        mIsFrontCamera = !isSelected;
        mSwitchCamera.setSelected(!isSelected);
    }*/
    
    private void showDebugView() {
        mLogLevel = (mLogLevel + 1) % 3;
        mTRTCCloud.showDebugView(mLogLevel);
    }

    private class TRTCCloudImplListener extends TRTCCloudListener {

        private WeakReference<RTCActivity>      mContext;

        public TRTCCloudImplListener(RTCActivity activity) {
            super();
            mContext = new WeakReference<>(activity);
        }
        @Override
        public void onEnterRoom(long result){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
            startTime = sdf.format(new Date());
            mCountText.setVisibility(View.VISIBLE);
            RTCActivity activity = mContext.get();
            if(result > 0){
                if(!firstEnter){
                    controltips();
                }
            Toast.makeText(activity, "欢迎来到咨询室！" , Toast.LENGTH_SHORT).show();
          }
        }

        @Override
        public void onRemoteUserEnterRoom(String userId){
            RTCActivity activity = mContext.get();
            
            Toast.makeText(activity, "对方进入咨询室，即将开始通话！" , Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onRemoteUserLeaveRoom(String userId,int reason){
          RTCActivity activity = mContext.get();
          if(reason == 1){
            Toast.makeText(activity, "对方网络异常中断通话！" , Toast.LENGTH_SHORT).show();
          }else {
            Toast.makeText(activity, "对方已退出咨询室！" , Toast.LENGTH_SHORT).show();
          }
        } 
        @Override 
        public void onUserAudioAvailable(String userId, boolean available){
            if(available){
                closeMic = false;
            }else{
                closeMic = true;
            }
        }

        @Override
        public void onUserVideoAvailable(String userId, boolean available) {
            Log.d(TAG, "onUserVideoAvailable userId " + userId + ", mUserCount " + mUserCount + ",available " + available);
            int index = mRemoteUidList.indexOf(userId);

            if (available) {
                closeCamera = false;
                if (index != -1) { //如果mRemoteUidList有，就不重复添加
                    return;
                }
                
                mRemoteUidList.add(userId);
                refreshRemoteVideoViews();
                
            } else {
                RTCActivity activity = mContext.get();
                if(connected){
                    String tips = "对方已关闭摄像头";
                    Toast.makeText(activity, tips , Toast.LENGTH_SHORT).show();
                }
                closeCamera = true;
                if (index == -1) { //如果mRemoteUidList没有，说明已关闭画面
                    return;
                }
                // 关闭用户userId的视频画面
                mTRTCCloud.stopRemoteView(userId);
                mRemoteUidList.remove(index);
                refreshRemoteVideoViews();
            }

        }

        private void refreshRemoteVideoViews() {
            for (int i = 0; i < mRemoteViewList.size(); i++) {
                if (i < mRemoteUidList.size()) {
                    exchancepreview =true;
                    mTRTCCloud.stopLocalPreview();
                    
                    String remoteUid = mRemoteUidList.get(i);
                    mRemoteViewList.get(i).setVisibility(View.VISIBLE);
                    // 开始显示用户userId的视频画面
                    mTRTCCloud.startLocalPreview(mIsFrontCamera,mRemoteViewList.get(i));
                    mTRTCCloud.startRemoteView(remoteUid, mLocalPreviewView);
                } else {
                    exchancepreview =false;
                    mTRTCCloud.stopLocalPreview();
                    mTRTCCloud.startLocalPreview(mIsFrontCamera,mLocalPreviewView);  
                    mRemoteViewList.get(i).setVisibility(View.GONE);
                }
            }
        }
        private void controltips(){
            firstEnter = true;
            mtimer = new Timer();
            mtimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                      mParseSec--;
                      int hh = mParseSec / 60 / 60 % 60;
                      int mm = mParseSec / 60 ;
                      int ss = mParseSec % 60;
                      RTCActivity activity = mContext.get();
                      
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(mParseSec == 300){
                                Toast.makeText(activity, "通话时间剩余5分钟" , Toast.LENGTH_SHORT).show();
                            }else if(mParseSec == 60){
                                connected = false;
                                Toast.makeText(activity, "通话时间剩余1分钟" , Toast.LENGTH_SHORT).show();
                            }
                            String showtext = "剩余时间："+String.format(Locale.ENGLISH,"%02d",mm)+":"+String.format(Locale.ENGLISH,"%02d",ss);
                              
                            mCountText.setText(showtext);
                            if(mParseSec <= 0 ){
                                mCountText.setText("剩余时间为0，结束通话！");
                            }
                        }
                    });
                    if (mParseSec < 0) {
                        exitRoom();
                        //mTRTCCloud.stopLocalAudio();
                        //mTRTCCloud.stopLocalPreview(); 
                        mtimer.cancel();
                        mtimer = null;
                    }
                }
            }, 1000, 1000);
          
        }
        // 错误通知监听，错误通知意味着 SDK 不能继续运行
        @Override
        public void onError(int errCode, String errMsg, Bundle extraInfo) {
            Log.d(TAG, "sdk callback onError");
            RTCActivity activity = mContext.get();
            if (activity != null) {
                errorMsg = errMsg;
                Toast.makeText(activity, "onError: " + errMsg + "[" + errCode+ "]" , Toast.LENGTH_SHORT).show();
                if (errCode == TXLiteAVCode.ERR_ROOM_ENTER_FAIL) {
                    activity.exitRoom();
                }
            }
        }
    }
}
