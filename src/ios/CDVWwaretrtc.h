#import <Cordova/CDVPlugin.h>
#import <UIKit/UIKit.h>
#import <TXLiteAVSDK_TRTC/TRTCCloudDef.h>
#import "TRTCVideoViewController.h"
#import "GenerateTestUserSig.h"
#import "TRTCAudioViewController.h"

@interface CDVWwaretrtc : CDVPlugin {
    NSString *onFinishedCallbackId;
    TRTCVideoViewController *controller;
}


@property (nonatomic, strong) NSString* callbackID;
@property (nonatomic, strong) NSString* sdkappid;

- (void)testing:(CDVInvokedUrlCommand*)command;
- (void)enterroom:(CDVInvokedUrlCommand*)command;


@end

