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

import com.bumptech.glide.Glide;

import com.tencent.liteav.TXLiteAVCode;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.tencent.trtc.TRTCCloud;
import com.tencent.trtc.TRTCCloudDef;
import com.tencent.trtc.TRTCCloudListener;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static com.tencent.trtc.TRTCCloudDef.TRTC_APP_SCENE_AUDIOCALL;

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
public class AudioActivity extends Activity  {

    private static final String TAG = "AudioActivity";
    private static final int REQ_PERMISSION_CODE  = 0x1000;
    private TextView                        mTitleText;                 //【控件】页面Title
    private ImageView                       mBackButton;                //【控件】返回上一级页面
    private ImageView                       mHeadPicture;               //【控件】头像
    private TextView                        mNickName;                  //【控件】昵称
    private TextView                        mTips;                      //【控件】提示信息
    private TextView                        mExpireTime;                //【控件】剩余时间
    private Button                          mHandfree;                  //【控件】免提
    private Button                          mMuteAudio;                 //【控件】开启、关闭本地声音采集并上行
    private Button                          mLogInfo;                   //【控件】开启、关闭日志显示

    private TRTCCloud                       mTRTCCloud;                 // SDK 核心类
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
    private String                          mGuestImg;
    private String                          mGuestName;
    private String                          errorMsg;
    private boolean                         firstEnter;
    private boolean                         connected;
    private boolean                         closeMic;
    private Timer                           mtimer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(wwaretrtc.getResourceId("activity_audio","layout"));
        //getSupportActionBar().hide(); // work on AppCompatActivity
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); 
        handleIntent();
        firstEnter = false;
        connected = true;
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
            if(intent.getStringExtra("guestImg") != null){
                mGuestImg = intent.getStringExtra("guestImg");
            }
            if(intent.getStringExtra("guestName") != null){
                mGuestName = intent.getStringExtra("guestName");
            }
        }
    }
    private void initView(){
        mTitleText          = findViewById(wwaretrtc.getResourceId("trtc_tv_room_number","id"));
        mBackButton         = findViewById(wwaretrtc.getResourceId("trtc_ic_back","id"));
        //mLocalPreviewView   = findViewById(wwaretrtc.getResourceId("trtc_tc_cloud_view_main","id"));
        //mMuteVideo          = findViewById(wwaretrtc.getResourceId("trtc_btn_mute_video","id"));
        mMuteAudio          = findViewById(wwaretrtc.getResourceId("trtc_btn_mute_audio","id"));
        //mSwitchCamera       = findViewById(wwaretrtc.getResourceId("trtc_btn_switch_camera","id"));
        //mLogInfo            = findViewById(wwaretrtc.getResourceId("trtc_btn_log_info","id"));
        //mVideoMutedTipsView = findViewById(wwaretrtc.getResourceId("ll_trtc_mute_video_default","id"));
        mHandfree           = findViewById(wwaretrtc.getResourceId("trtc_btn_handfree","id"));
        mHeadPicture        = findViewById(wwaretrtc.getResourceId("host_headimg_1","id"));
        mNickName           = findViewById(wwaretrtc.getResourceId("host_nickname_1","id"));
        mTips               = findViewById(wwaretrtc.getResourceId("rtc_tip_text","id"));
        mExpireTime         = findViewById(wwaretrtc.getResourceId("expire_time_show","id"));

        mTips.setVisibility(View.GONE);
        int cmm = mParseSec / 60 ;
        int css = mParseSec % 60;
        mExpireTime.setText("剩余时间："+String.format(Locale.ENGLISH,"%02d",cmm)+":"+String.format(Locale.ENGLISH,"%02d",css));
        /*if (!TextUtils.isEmpty(mUsername)) {
            String tmpstr = mUsername+"的咨询室";
            mTitleText.setText(tmpstr);
        }
        */
        mNickName.setText(mGuestName);

        Glide.with(this).load(mGuestImg).into(mHeadPicture);
        Activity that = this;
        mBackButton.setOnClickListener(view -> {
            exitRoom();
            //finish();
            //Toast.makeText((RTCActivity)this,"test",Toast.LENGTH_SHORT).show();
        });
        mMuteAudio.setOnClickListener(view -> {
            if(closeMic){
                Toast.makeText(this,"无法双方均关闭麦克风",Toast.LENGTH_SHORT).show();
            }else{
              muteAudio();  
            }
            
            //Toast.makeText((RTCActivity)this,"test",Toast.LENGTH_SHORT).show();
        });
        mHandfree.setOnClickListener(view->{
            handfree();
        });
        /*mLogInfo.setOnClickListener(view->{
            showDebugView();
        });*/
    }
    private void enterRoom() {
        mTRTCCloud = TRTCCloud.sharedInstance(getApplicationContext());
        mTRTCCloud.setListener(new TRTCCloudImplListener(AudioActivity.this));

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

        //通话默认设置为主播角色
        //trtcParams.role = TRTCRoleAnchor;

        // 进入通话
        mTRTCCloud.enterRoom(trtcParams, TRTC_APP_SCENE_AUDIOCALL);
        // 开启本地声音采集并上行
        mTRTCCloud.startLocalAudio();

        /*TRTCCloudDef.TRTCAudioRecordingParams encParam = new TRTCCloudDef.TRTCAudioRecordingParams();
        encParam.filePath = this.getCacheDir().getPath()+"audio.aac";
        mTRTCCloud.startAudioRecording(encParam);*/
    }
    @Override
    protected void onDestroy() {
        if(mtimer != null){
            mtimer.cancel();
            mtimer = null;    
        }
        
        super.onDestroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //exitRoom();
    }
    /*
    * 中断本地推送及远程接收
    */
    private void stopTRTC(){
        mTRTCCloud.stopLocalAudio();
        //finish();
    }
    /**
     * 离开通话
     */
    private void exitRoom() {
        Intent intent = new Intent();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        endTime = sdf.format(new Date());
        intent.putExtra("endtime",endTime);
        intent.putExtra("starttime",startTime);
        intent.putExtra("errormsg",errorMsg);
        setResult(0,intent);

        mTRTCCloud.stopLocalAudio();
        //mTRTCCloud.stopLocalPreview(); 
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
        }  else if (id == wwaretrtc.getResourceId("trtc_btn_mute_audio","id")) {
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

    
    private void showDebugView() {
        mLogLevel = (mLogLevel + 1) % 3;
        mTRTCCloud.showDebugView(mLogLevel);
    }

    private class TRTCCloudImplListener extends TRTCCloudListener {

        private WeakReference<AudioActivity>      mContext;

        public TRTCCloudImplListener(AudioActivity activity) {
            super();
            mContext = new WeakReference<>(activity);
        }
        @Override
        public void onEnterRoom(long result){
            //String tips = "等待对方进入咨询室";
            //mTips.setText(tips);
            AudioActivity activity = mContext.get();
          if(result > 0){
            Toast.makeText(activity, "欢迎来到咨询室！" , Toast.LENGTH_SHORT).show();
          }
        }

        @Override
        public void onRemoteUserEnterRoom(String userId){
            //String tips = "对方进入咨询室，即将开始通话！";
            //mTips.setText(tips);
          AudioActivity activity = mContext.get();
          
          Toast.makeText(activity, "对方进入咨询室，即将开始通话！" , Toast.LENGTH_SHORT).show();
         
        }

        @Override
        public void onRemoteUserLeaveRoom(String userId,int reason){
            String tips = "";

            if(reason == 1){
                tips = "对方网络异常中断通话！";
            }else {
                tips = "对方已退出咨询室！";
            }
            AudioActivity activity = mContext.get();
            
            Toast.makeText(activity, tips , Toast.LENGTH_SHORT).show();
                        
              //Log.i(TAG,"AUDIOLOGTEST:onRemoteUserLeaveRoom");
        }
        @Override
        public void onUserAudioAvailable(String userId, boolean avaliable) {
            //super.onUserAudioAvailable(userId, avaliable);
            //Log.i(TAG,"AUDIOLOGTEST:onUserAudioAvailable");
            AudioActivity activity = mContext.get();
               
            if(avaliable){
                closeMic = false;
                mTips.setVisibility(View.GONE);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
                startTime = sdf.format(new Date());
                if(!firstEnter){
                    controltips();
                }
            }else{
                closeMic = true;
                if(connected){
                    String tips = "对方已静音";
                    Toast.makeText(activity, tips , Toast.LENGTH_SHORT).show();    
                }
                
            }
            
        }
        /* @Override
        public void onUserAudioAvailable(String userId, boolean available) {
            Log.d(TAG, "onUserAudioAvailable userId " + userId + ", mUserCount " + mUserCount + ",available " + available);
            
        }*/
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
                      AudioActivity activity = mContext.get();
                      
                    runOnUiThread(new Runnable() {
                        //@Override
                        public void run() {
                            if(mParseSec == 300){
                                Toast.makeText(activity, "通话时间剩余5分钟" , Toast.LENGTH_SHORT).show();
                            }else if(mParseSec == 60){
                                connected = false;
                                Toast.makeText(activity, "通话时间剩余1分钟" , Toast.LENGTH_SHORT).show();
                            }
                            String showtext = "剩余时间："+String.format(Locale.ENGLISH,"%02d",mm)+":"+String.format(Locale.ENGLISH,"%02d",ss);
                              
                            mExpireTime.setText(showtext);
                            if(mParseSec <= 0){
                                mExpireTime.setText("剩余时间为0，结束通话！");
                            }
                        }
                    });
                    if (mParseSec <= 0) {
                      //mExpireTime.setText("剩余时间为0，结束通话！");
                      //mTRTCCloud.stopLocalAudio();
                      exitRoom();
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
            AudioActivity activity = mContext.get();
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
