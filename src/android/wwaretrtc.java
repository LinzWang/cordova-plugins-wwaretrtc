package org.wware.wwaretrtc;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.Manifest;
import android.util.Log;
import android.widget.Toast;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.wware.wwaretrtc.activity.RTCActivity;
import org.wware.wwaretrtc.activity.AudioActivity;

public class wwaretrtc extends CordovaPlugin {
  private CallbackContext callbackContext;
  private static Application app;
  private static String packagename;
  private static Resources resource;
  private static final String TAG = "wwaretrtcclass";
  protected static String SDKAPPID;
  @Override
  public void pluginInitialize() {
    super.pluginInitialize();
    SDKAPPID = webView.getPreferences().getString("SDKAPPID", "t").substring(1);
    Log.i(TAG,"SDKAPPID in pluginInitialize: "+SDKAPPID);
    app = this.cordova.getActivity().getApplication();
    packagename = app.getPackageName();
    resource = app.getResources();
  }
  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
      this.callbackContext = callbackContext;

      if(action.equals("testing")) {
              String message = args.getString(0);
              this.testing(message, callbackContext);
              return true;
      }
      if(action.equals("enterroom")) {
              JSONObject message = args.getJSONObject(0);
              this.enterroom(message, callbackContext);
              return true;
      }
      if(action.equals("exitroom")) {
              JSONObject message = args.getJSONObject(0);
              this.exitroom(message, callbackContext);
              return true;
      }
      if(action.equals("checkPermission")){
        JSONObject message = args.getJSONObject(0);
        this.checkPermission(message, callbackContext);
        return true;
      }
      return false;
    }


    private void testing(String message, CallbackContext callbackContext){
            String version= "0.0.1";
            if(message != null && message.length()>0){
                  callbackContext.success("调用成功："+message+version);
              }else{
                  callbackContext.error("调用失败：无有效参数");
              }
    }
    private void enterroom_duplicate(JSONObject message, CallbackContext callbackContext){
      final CordovaPlugin plugin = this;
      
      //Toast.makeText(plugin.cordova.getActivity(), "enter in enterroom Function", Toast.LENGTH_SHORT).show();
      Log.i(TAG,"enter enterroom function");
      String userId;
      String roomName;
      String roomToken;
      String enableMergeStream;

      try{
        userId = message.getString("id");
        roomName = message.getString("roomid");
        roomToken = message.getString("roomtoken");
        enableMergeStream = message.getString("enable_merge_stream");
        Log.i(TAG,"enterroom parse params");
        Activity myActivity = plugin.cordova.getActivity();
        new Thread(new Runnable() {
          @Override
          public void run() {
             Log.i(TAG,"enterroom new thread and run");
            myActivity.runOnUiThread(new Runnable() {
              @Override
              public void run() {
                 Log.i(TAG,"enterroom run on uiThread");
                if (roomToken == null) {
                  //Toast.makeText(plugin.cordova.getActivity(), "roomToken ", Toast.LENGTH_SHORT).show();
                   Log.i(TAG,"enterroom token is null");
                  return;
                }
                Log.i(TAG,"enterroom create new intent");
                Intent intent = new Intent(myActivity, RTCActivity.class);
                intent.putExtra("roomid", roomName.trim());
                intent.putExtra("roomToken", roomToken);
                intent.putExtra("userid", userId);
                intent.putExtra("enable_merge_stream", enableMergeStream);
                Log.i(TAG,"enterroom start activity");
                myActivity.startActivity(intent);
              }
            });
          }
        }).start();  
      }catch(JSONException err){
        Log.i(TAG,"enterroom error:"+err.getMessage());
        callbackContext.error("params error");
      }
    }

    private void enterroom(JSONObject message, CallbackContext callbackContext) {
            final CordovaPlugin plugin = this;
            
            cordova.getThreadPool().execute(new Runnable(){
              public void run() {
                try{
                  String userId = message.getString("userid");
                  String roomId = message.getString("roomid");
                  String username = message.getString("username");
                  String userSig = message.getString("usersig");
                  String guestImg = message.getString("guestimg");
                  String guestName = message.getString("guestname");
                  String roomType = message.getString("roomtype");
                  String estimateTime = message.getString("estimatetime");
                                    
                  //为通话页面构建intent 
                  Intent trtcIntent;
                  if(roomType.equals("audio")){
                    trtcIntent = new Intent(plugin.cordova.getActivity().getApplicationContext(),AudioActivity.class);
                    trtcIntent.putExtra("guestImg", guestImg);
                    trtcIntent.putExtra("guestName", guestName); 
                  }else{
                    trtcIntent = new Intent(plugin.cordova.getActivity().getApplicationContext(),RTCActivity.class);
                  }
                  
                  //加入将要传输到activity中的参数
                  //cordova.setActivityResultCallback (plugin);
                  
                  trtcIntent.putExtra("appid",SDKAPPID);
                  trtcIntent.putExtra("roomId", roomId);
                  trtcIntent.putExtra("userSig", userSig);
                  trtcIntent.putExtra("username", username);
                  trtcIntent.putExtra("userId", userId);
                  trtcIntent.putExtra("estimateTime",estimateTime);
                  //启动activity
                  
                  cordova.getActivity().runOnUiThread(new Runnable(){
                    public void run() {
                        cordova.startActivityForResult(plugin, trtcIntent, 0);
                    }
                  });
                }catch(JSONException err){
                  Log.i(TAG,"deal error"+err.getMessage());
                  callbackContext.error(err.getMessage());
                }
                
              }
             });

            
    }
    private void exitroom(JSONObject message, CallbackContext callbackContext){
            callbackContext.success("调用exitroom成功");
    }
    private void checkPermission(JSONObject message, CallbackContext callbackContext){
      //Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_EXTERNAL_STORAGE
        //String[] b = list.toArray(new String[list.size()]);
            List<String> permissions = new ArrayList<>();
            if(!cordova.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
              permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if(!cordova.hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)){
             permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE); 
            }
            if(!cordova.hasPermission(Manifest.permission.CAMERA)){
              permissions.add(Manifest.permission.CAMERA);
            }
            if(!cordova.hasPermission(Manifest.permission.RECORD_AUDIO)){
              permissions.add(Manifest.permission.RECORD_AUDIO);
            }

            String [] mpermissions = permissions.toArray(new String[0]);
            if(mpermissions.length>0){
              cordova.requestPermissions(this, 0, mpermissions);  
            }else{
              callbackContext.success("权限已全部申请");
            }
            
            //callbackContext.success("调用checkPermission成功");
    }
    /*@Override
    public ExecutorService getThreadPool() {
        return threadPool;
    }*/
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
      for(int r:grantResults)
      {
          if(r == PackageManager.PERMISSION_DENIED)
          {
            callbackContext.error("用户拒绝授予权限");
            //this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, PERMISSION_DENIED_ERROR));
            return;
          }
      }
      callbackContext.success("申请成功");
    }
   
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
       // 根据resultCode判断处理结果
       //super.onActivityResult(requestCode, resultCode, intent);
       if(resultCode==0){
           String starttime="";
           String endtime="";
          if(intent != null){
            starttime = intent.getStringExtra("starttime");
            endtime = intent.getStringExtra("endtime");
          }
           try{
            JSONObject value = new JSONObject();
            value.put("starttime",starttime);
            value.put("endtime",endtime);
            value.put("success",true);
            callbackContext.success(value);
           }catch (JSONException err){
               callbackContext.error(err.getMessage());
           }
           //暂定返回通话完成

       }
    }
    public static int getResourceId(String name, String type) {
        int ic = resource.getIdentifier(name, type, packagename);
        return ic;
    }
    public static void setResource(Application _app) {
        app = _app;
        resource = app.getResources();
        packagename = app.getPackageName();
    }
    public Bundle onSaveInstanceState() {
      Bundle state = new Bundle();
      return state;
    }
    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
      this.callbackContext = callbackContext;
    }
} 
