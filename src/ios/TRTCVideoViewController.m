#import "TRTCVideoViewController.h"



@implementation TRTCVideoViewController

-(void)viewWillAppear:(BOOL)animated{
  NSLog(@"viewWillAppear");
}

-(void)viewWillDissappear:(BOOL)animated{

     [[NSNotificationCenter defaultCenter] postNotificationName:@"Vviewsdissappear" object:nil];
}
/**
 * 检查当前APP是否已经获得摄像头和麦克风权限，没有获取边提示用户开启权限
 */
- (void)viewDidAppear:(BOOL)animated {
    NSLog(@"viewDidAppear");
    //_dashboardTopMargin = [UIApplication sharedApplication].statusBarFrame.size.height + self.navigationController.navigationBar.frame.size.height;
#if !TARGET_IPHONE_SIMULATOR
    //是否有摄像头权限
    AVAuthorizationStatus statusVideo = [AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeVideo];
    if (statusVideo == AVAuthorizationStatusDenied) {
        [self toastTip:@"获取摄像头权限失败，请前往隐私-相机设置里面打开应用权限"];
        return;
    }
    
    //是否有麦克风权限
    AVAuthorizationStatus statusAudio = [AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeAudio];
    if (statusAudio == AVAuthorizationStatusDenied) {
        [self toastTip:@"获取麦克风权限失败，请前往隐私-麦克风设置里面打开应用权限"];
        return;
    }
#endif
    
}

/*
设置进入房间所需的参数，由主页面传进来
*/
- (void)setParam:(TRTCParams *)param
{
    _param = param;
    _sdkAppid = param.sdkAppId;
    _selfUserID = param.userId;
    _selfUserSig = param.userSig;
    _roomID = @(param.roomId).stringValue;
}



- (void)viewDidLoad {
    
   [super viewDidLoad];
   
    _dashboardTopMargin = 0.15;
    if (_trtc == nil) {
        _trtc = [TRTCCloud sharedInstance];
        [_trtc setDelegate:self];
    }
    _roomStatus = TRTC_IDLE;
    //_remoteViewDic = [[NSMutableDictionary alloc] init];
    
    _mainViewUserId = @"";
    _toastMsgCount = 0;
    _toastMsgHeight = 0;
    
    // 初始化 UI 控件
    [self initUI];
    
    // 开始登录、进房
    [self enterRoom];
}
- (void)dealloc {
    if (_trtc != nil) {
        [_trtc exitRoom];
    }
    
    [TRTCCloud destroySharedIntance];
}

#pragma mark - initUI
/**
 * 初始化界面控件，包括主要的视频显示View，以及底部的一排功能按钮
 */
- (void)initUI {
   // self.title = @"咨询室";
    //[self.view setBackgroundColor:UIColorFromRGB(0x333333)];
    self.view.backgroundColor = [UIColor blackColor];
       
    _videoMuted = NO;
    _btnVideoMute = [self createBottomBtnIcon:@"rtc_camera_on" action:@selector(clickVideoMute)];
    
    _muteSwitch = NO;
    _btnAudioMute = [self createBottomBtnIcon:@"rtc_mic_on" action:@selector(clickMute)];
    
    _showLogType = 0;
    //_btnLog = [self createBottomBtnIcon:@"rtc_log_button" action:@selector(clickLog)];
    
    _handfree = YES;
    _btnHandfree = [self createBottomBtnIcon:@"trtccalling_ic_handsfree_enable" action:@selector(clickHandfree)];
   
    _btnback = [self createBottomBtnIcon:@"back" action:@selector(clickBack)];
    
    
    //toastView.backgroundColor = [UIColor whiteColor];
    //_titleLabel.alpha = 0.5;
        
    // 布局底部工具栏
    [self relayoutToolsButton];
    
    // 本地预览view
    //_holderView  = [[UIView alloc] initWithFrame:[[UIScreen mainScreen]bounds] ];
    //[self.view insertSubview:_holderView atIndex:0];
    
	//_localView.delegate = self;
    _localView = [[UIView alloc]init];
    [self.view insertSubview:_localView atIndex:0];
    _localView.backgroundColor = [UIColor colorWithRed:38 green:38 blue:38 alpha:1.0 ];
	//[_localView setBackgroundColor:UIColorFromRGB(0x262626)];
    _remoteView = [[UIView alloc]init];
    [self.view insertSubview:_remoteView belowSubview:_localView];
    _remoteView.backgroundColor =[UIColor colorWithRed:38 green:38 blue:38 alpha:1.0];
    //[_remoteView setBackgroundColor:UIColorFromRGB(0x262626)];
    
    [self relayout];
    
    
}
- (BOOL) isiPhoneX{
    BOOL iphone = NO;
    if(UIDevice.currentDevice.userInterfaceIdiom != UIUserInterfaceIdiomPhone){
        return iphone;
    }
    if(@available(iOS 11.0,*)){
        UIWindow *mainWindow = [[[UIApplication sharedApplication]delegate] window];
        if(mainWindow.safeAreaInsets.bottom >0.0){
            iphone = YES;
        }
    }
    return iphone;
}
// 工具栏布局
- (void)relayoutToolsButton {
    CGSize size = [[UIScreen mainScreen] bounds].size;
    int ICON_SIZE = size.width / 8;
    int addY = 0;
    if([self isiPhoneX]){
        addY = 22;
    }
    // 观众和主播的底部工具栏不一样
    // 观众增加 _btnLinkMic，减少了 _btnLayoutSwitch，_btnBeauty，_btnVideoMute，_btnMute
    // 观众连麦后会比主播多出一个连麦按钮 _btnLinkMic，同时也有其他按钮
    int buttonCount = 3;
    
    float startSpace = 80;
    float centerInterVal = (size.width - 2 * startSpace - ICON_SIZE) / (buttonCount - 1)  - ICON_SIZE;
    float iconY = size.height - ICON_SIZE / 2 - 30;
    
    _titleLabel =[ [UILabel alloc]initWithFrame:CGRectMake(0, 10+addY, size.width, ICON_SIZE)];
    _titleLabel.font = [UIFont boldSystemFontOfSize:17];
    _titleLabel.numberOfLines = 1;
    _titleLabel.textColor = UIColor.whiteColor;
    _titleLabel.textAlignment = NSTextAlignmentCenter;
    _titleLabel.text = @"咨询室";
    
    _expireTimeLabel = [[UILabel alloc]initWithFrame:CGRectMake(0, iconY-ICON_SIZE*2, size.width, ICON_SIZE)];
    _expireTimeLabel.font = [UIFont systemFontOfSize:16];
    _expireTimeLabel.numberOfLines = 1;
    _expireTimeLabel.textColor = UIColor.whiteColor;
    _expireTimeLabel.textAlignment = NSTextAlignmentCenter;
    _expireTimeLabel.text = @"";
    
    
    [self.view addSubview:_titleLabel];
    [self.view addSubview:_expireTimeLabel];

    _btnVideoMute.center = CGPointMake(startSpace + ICON_SIZE / 2, iconY);
    _btnVideoMute.bounds = CGRectMake(0, 0, ICON_SIZE, ICON_SIZE);

    _btnAudioMute.center = CGPointMake(_btnVideoMute.center.x + ICON_SIZE + centerInterVal, iconY);
    _btnAudioMute.bounds = CGRectMake(0, 0, ICON_SIZE, ICON_SIZE);
    
    _btnHandfree.center = CGPointMake(_btnAudioMute.center.x + ICON_SIZE + centerInterVal, iconY);
    _btnHandfree.bounds = CGRectMake(0, 0, ICON_SIZE, ICON_SIZE);
    
    //_btnLog.center = CGPointMake(_btnHandfree.center.x + ICON_SIZE + centerInterVal, iconY);
    //_btnLog.bounds = CGRectMake(0, 0, ICON_SIZE, ICON_SIZE);
    
    _btnback.center = CGPointMake(15+ICON_SIZE/2, 10+addY+ICON_SIZE/2);
    _btnback.bounds = CGRectMake(0, 0, ICON_SIZE, ICON_SIZE);
    //TODO set back button 
   
}
- (UIButton*)createBottomBtnIcon:(NSString*)icon action:(SEL)action
{
    UIButton * btn = [[UIButton alloc]init];
    [btn setImage:[UIImage imageNamed:icon] forState:UIControlStateNormal];
    [btn addTarget:self action:action forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:btn];
    //[self.view bringSubviewToFront:btn];
    return btn;
}

- (void)relayout {
   // NSMutableArray *views = @[].mutableCopy;
    CGSize size = [[UIScreen mainScreen] bounds].size;
    int smallVideoW = size.width/3;
    int smallVideoH = size.width/27*16;

    
    //[self.view sendSubviewToBack:_localView];
        // 默认本地预览为主窗口时仅需初始化本地，
    
    [UIView animateWithDuration:0.25 animations:^{
        if ([self->_mainViewUserId isEqual:@""] || [self->_mainViewUserId isEqual:self->_selfUserID]){
	    	
            self->_localView.frame = CGRectMake(0, 0, size.width, size.height);
	    }else{
            self->_remoteView.frame = CGRectMake(0, 0, size.width, size.height);
            self->_localView.frame = CGRectMake(size.width-smallVideoW-10,30,smallVideoW,smallVideoH);
            
	    }
    } completion:^(BOOL finished) {
        NSLog(@"UIrelayout is over %U",finished);
    }];
    
    
}


/**
 * 防止iOS锁屏：如果视频通话进行中，则方式iPhone进入锁屏状态
 */
- (void)setRoomStatus:(TRTCStatus)roomStatus {
    _roomStatus = roomStatus;
    
    switch (_roomStatus) {
        case TRTC_IDLE:
            [[UIApplication sharedApplication] setIdleTimerDisabled:NO];
            break;
        case TRTC_ENTERED:
            [[UIApplication sharedApplication] setIdleTimerDisabled:YES];
            break;
        default:
            break;
    }
}


/**
 * 加入视频房间：需要 TRTCNewViewController 提供的  TRTCVideoEncParam 函数
 */
- (void)enterRoom {
    // 大画面的编码器参数设置
    // 设置视频编码参数，包括分辨率、帧率、码率等等，这些编码参数来自于 TRTCSettingViewController 的设置
    // 注意（1）：不要在码率很低的情况下设置很高的分辨率，会出现较大的马赛克
    // 注意（2）：不要设置超过25FPS以上的帧率，因为电影才使用24FPS，我们一般推荐15FPS，这样能将更多的码率分配给画质
    TRTCVideoEncParam* encParam = [TRTCVideoEncParam new];
    encParam.videoResolution = TRTCVideoResolution_640_360;
    encParam.videoBitrate = 550;
    encParam.videoFps = 15;
    encParam.resMode = TRTCVideoResolutionModePortrait;
    
   
    [_trtc startLocalAudio];
    
    
    [_trtc setVideoEncoderParam:encParam];
    //
    [_trtc startLocalPreview:true view:_localView];
    
    [self toastTip:@"开始进房"];
    NSLog(@"enterroom in VC:%@",self.param);
    // 进房
    [_trtc enterRoom:self.param appScene:TRTCAppSceneVideoCall];
    [self setRoomStatus:TRTC_IDLE];
    
}

/**
 * 退出房间，并且退出该页面
 */
- (void)exitRoom {
    
   
    [_trtc exitRoom];
    
	[self setRoomStatus:TRTC_IDLE];
}

-(void)startPreview
{
    [_trtc startLocalPreview:true view:_localView];
}

- (void)stopPreview
{
    [_trtc stopLocalPreview];
}

#pragma mark - button
/**
 * 点击打开仪表盘浮层，仪表盘浮层是SDK中覆盖在视频画面上的一系列数值状态
 */
- (void)clickLog {
    
    [_trtc showDebugView:_showLogType];
}

/**
 * 打开或关闭本地视频上行
 */
- (void)clickVideoMute {
    _videoMuted = !_videoMuted;
    
    [_btnVideoMute setImage:[UIImage imageNamed:(_videoMuted ? @"rtc_camera_off" : @"rtc_camera_on")] forState:UIControlStateNormal];
    
    if (_videoMuted) {
        //        [_trtc stopLocalPreview];
        [self stopPreview];
    }
    else {
        //        [_trtc startLocalPreview:YES view:_localView];
        [self startPreview];
        
    }
    [_trtc muteLocalVideo:_videoMuted];
}


/**
 * 点击关闭或者打开本地的音频上行
 */
- (void)clickMute {
    _muteSwitch = !_muteSwitch;
    [_trtc muteLocalAudio:_muteSwitch];
    [_btnAudioMute setImage:[UIImage imageNamed:(_muteSwitch ? @"rtc_mic_off" : @"rtc_mic_on")] forState:UIControlStateNormal];
}

/*
*/
-(void)clickHandfree{
	_handfree = !_handfree;
	[_trtc setAudioRoute:(_handfree?TRTCAudioModeSpeakerphone :TRTCAudioModeEarpiece )];
	[_btnHandfree setImage:[UIImage imageNamed:(_handfree ? @"trtccalling_ic_handsfree_enable" : @"trtccalling_ic_handsfree_disable")] forState:UIControlStateNormal];
}

-(void)clickBack{
    if (self.onHangUp != nil) {
        self.onHangUp();
    }
    [self dismissViewControllerAnimated:true completion:nil];
}


#pragma mark - TRtcEngineDelegate

/**
 * WARNING 大多是一些可以忽略的事件通知，SDK内部会启动一定的补救机制
 */
- (void)onWarning:(TXLiteAVWarning)warningCode warningMsg:(NSString *)warningMsg {
    
}


/**
 * WARNING 大多是不可恢复的错误，需要通过 UI 提示用户
 */
- (void)onError:(TXLiteAVError)errCode errMsg:(NSString *)errMsg extInfo:(nullable NSDictionary *)extInfo {
    
    NSString *msg = [NSString stringWithFormat:@"didOccurError: %@[%d]", errMsg, errCode];
    [self toastTip:msg];
    //[self exitRoom];
    
}


- (void)onEnterRoom:(NSInteger)result {
    if (result >= 0) {
        NSString *msg = [NSString stringWithFormat:@"[%@]进房成功[%@]: elapsed[%ld]", _selfUserID, _roomID, (long)result];
        [self toastTip:msg];
        [self setRoomStatus:TRTC_ENTERED];
        
    }
    else {
        //[self exitRoom];
        
        NSString *msg = [NSString stringWithFormat:@"进房失败: [%ld]", (long)result];
        [self toastTip:msg];
    }
}


- (void)onExitRoom:(NSInteger)reason {
    NSString *msg = [NSString stringWithFormat:@"离开房间[%@]: reason[%ld]", _roomID, (long)reason];
    [self toastTip:msg];
}



- (void)onUserAudioAvailable:(NSString *)userId available:(BOOL)available
{
    NSLog(@"onUserAudioAvailable:userId:%@ alailable:%u", userId, available);
    
}


- (void)onUserVideoAvailable:(NSString *)userId available:(BOOL)available
{

    if (available) {
        // 启动远程画面的解码和显示逻辑，FillMode 可以设置是否显示黑边
        _mainViewUserId = userId;
        
       // [self.view addSubview:_remoteView];
       // [_localView sendSubviewToBack:_remoteView];
        [_trtc startRemoteView:userId view:_remoteView];
        [self relayout];
        [self updateTips];
    }
    else {
        [_trtc stopRemoteView:userId];
    }
        
     
    
    NSLog(@"onUserVideoAvailable:userId:%@ alailable:%u", userId, available);
    
}

-(void)updateTips{
    __block int timeout = self.totaltime; //倒计时时间
    dispatch_queue_t queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0);
    dispatch_source_t _timer = dispatch_source_create(DISPATCH_SOURCE_TYPE_TIMER, 0, 0,queue);
    dispatch_source_set_timer(_timer,dispatch_walltime(NULL, 0),1.0*NSEC_PER_SEC, 0); //每秒执行
    dispatch_source_set_event_handler(_timer, ^{
        if(timeout<=0){ //倒计时结束，关闭
                dispatch_source_cancel(_timer);
                //dispatch_release(_timer);
                dispatch_async(dispatch_get_main_queue(), ^{
                    //设置界面的按钮显示 根据自己需求设置
                    self->_expireTimeLabel.text=@"通话时间为0，结束通话！";
                });
        }else{
            int minutes = timeout / 60;
            int seconds = timeout % 60;
            NSString *strTime = [NSString stringWithFormat:@"剩余时间：%02d:%02d",minutes, seconds];
            dispatch_async(dispatch_get_main_queue(), ^{
                //设置界面的按钮显示 根据自己需求设置
                self->_expireTimeLabel.text = strTime;
            });
            timeout--;
        }
    });
    dispatch_resume(_timer);
}

- (void)onAudioRouteChanged:(TRTCAudioRoute)route fromRoute:(TRTCAudioRoute)fromRoute {
    NSLog(@"TRTC onAudioRouteChanged %ld -> %ld", (long)fromRoute, (long)route);
}



- (float)heightForString:(UITextView *)textView andWidth:(float)width {
    CGSize sizeToFit = [textView sizeThatFits:CGSizeMake(width, MAXFLOAT)];
    return sizeToFit.height;
}

- (void)toastTip:(NSString *)toastInfo {
    NSLog(@"%@", toastInfo);
   // return;
    
    _toastMsgCount++;
    
    CGRect frameRC = [[UIScreen mainScreen] bounds];
    frameRC.origin.y = frameRC.size.height - 110;
    frameRC.size.height -= 110;
    __block UITextView *toastView = [[UITextView alloc] init];
    
    toastView.editable = NO;
    toastView.selectable = NO;
    
    frameRC.size.height = [self heightForString:toastView andWidth:frameRC.size.width];
    
    // 避免新的tips将之前未消失的tips覆盖掉，现在是不断往上偏移
    frameRC.origin.y -=  _toastMsgHeight;
    _toastMsgHeight += frameRC.size.height;
    
    toastView.frame = frameRC;
    
    toastView.text = toastInfo;
    //toastView.backgroundColor = [UIColor whiteColor];
    toastView.alpha = 0.5;
    
    [self.view addSubview:toastView];
    
    dispatch_time_t popTime = dispatch_time(DISPATCH_TIME_NOW, 2 * NSEC_PER_SEC);
    
    __weak __typeof(self) weakSelf = self;
    dispatch_after(popTime, dispatch_get_main_queue(), ^() {
        [toastView removeFromSuperview];
        toastView = nil;
        if (weakSelf.toastMsgCount > 0) {
            weakSelf.toastMsgCount--;
        }
        if (weakSelf.toastMsgCount == 0) {
            weakSelf.toastMsgHeight = 0;
        }
    });
}


@end


