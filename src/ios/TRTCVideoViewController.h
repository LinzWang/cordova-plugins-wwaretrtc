#import <UIKit/UIKit.h>
#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>

#import <TXLiteAVSDK_TRTC/TRTCCloud.h>
#import <TXLiteAVSDK_TRTC/TRTCCloudDef.h>
#import <TXLiteAVSDK_TRTC/TRTCCloudDelegate.h>

typedef enum : NSUInteger {
    TRTC_IDLE,       // SDK 没有进入视频通话状态
    TRTC_ENTERED,    // SDK 视频通话进行中
} TRTCStatus;

@interface TRTCVideoViewController : UIViewController  <
UITextFieldDelegate,
TRTCCloudDelegate>
{
    TRTCStatus                _roomStatus;
    
    NSString                 *_mainViewUserId;     //视频画面支持点击切换，需要用一个变量记录当前哪一路画面是全屏状态的
    UIView                   *_remoteView;
    UIView                   *_localView;
    UIView                   *_holderView;
    
    UIButton                 *_btnback;            //退出房间
    UIButton                 *_btnVideoMute;       //上行静画
    UIButton                 *_btnAudioMute;       //上行静音
    UIButton                 *_btnHandfree;        //切换听筒和扬声器
    UIButton                 *_btnLog;             //切换听筒和扬声器
    UILabel                  *_titleLabel;
    UILabel                  *_expireTimeLabel;
    
   
    NSInteger                _showLogType;         //LOG浮层显示详细信息还是精简信息
    NSInteger                _layoutBtnStaΩte;      //布局切换按钮状态
    BOOL                     _videoMuted;
    BOOL                     _muteSwitch;
    BOOL                     _handfree;
    CGFloat                  _dashboardTopMargin;
}


@property (nonatomic) TRTCParams *param;
@property uint32_t sdkAppid;
@property (nonatomic, copy) NSString* roomID;
@property (nonatomic, copy) NSString* selfUserID;
@property NSString  *selfUserSig;
@property (nonatomic, retain) TRTCCloud *trtc;               //TRTC SDK 实例对象           //本地画面的view
@property (nonatomic, retain) UIView *myview;
@property (nonatomic) NSInteger toastMsgCount;
@property (nonatomic) CGFloat toastMsgHeight;
@property (nonatomic) int totaltime;

@end
