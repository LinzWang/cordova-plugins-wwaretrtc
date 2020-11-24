/*cordova Plugin Implementation *******/

#import "CDVWwaretrtc.h"
#import "TRTCVideoViewController.h"
#import "TRTCAudioViewController.h"

@implementation CDVWwaretrtc
#pragma mark -初始化
-(void)pluginInitialize{
    
  NSString* sdkappid = [[self.commandDelegate settings] objectForKey:@"sdkappid"];
    NSLog(@"sdkappid in init%@",[[self.commandDelegate settings] objectForKey:@"sdkappid"]);
  if(sdkappid && ![sdkappid isEqualToString:self.sdkappid]){
    self.sdkappid = sdkappid;
  }
  
}


#pragma mark -测试
- (void)testing:(CDVInvokedUrlCommand*)command
{
  CDVPluginResult* pluginResult = nil;
  NSDictionary* values = [command.arguments objectAtIndex:0];

 // NSString* isdebug = [values objectForKey:@"isDebug"];
  
  NSMutableDictionary* result = [NSMutableDictionary dictionaryWithCapacity:0];
  [result setDictionary:values];
  [result setObject:@(true) forKey:@"success"];
 
  if (values != nil ) {
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
  } else {
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Arg was null"];
  }

  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

-(void)enterroom:(CDVInvokedUrlCommand*)command
{

    NSDictionary *data = [command.arguments objectAtIndex:0];
    if (data == nil) {
      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"数据有误"];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      return;
    }
    //NSLog(@"data:%@",data);
    self->onFinishedCallbackId = command.callbackId;

    // TRTC相关参数设置
    TRTCParams *param = [[TRTCParams alloc] init];
    param.sdkAppId = (UInt32)[(NSString*)self.sdkappid intValue];
    param.userId = (NSString*)[data objectForKey: @"userid"];
    param.roomId = (UInt32)[(NSString*)[data objectForKey: @"roomid"] intValue];
    NSString *paramUsersig = [data objectForKey: @"usersig"];
    int totaltime=[[data objectForKey: @"estimatetime"] intValue]*60;
    NSString *roomtype = [data objectForKey:@"roomtype"];

    if(paramUsersig != nil && paramUsersig.length>0){
      param.userSig =paramUsersig;
    }else{
      param.userSig = [GenerateTestUserSig genTestUserSig:param.userId];
    }
    
    //param.privateMapKey = @"";
    param.role = TRTCRoleAnchor;
    if([roomtype  isEqual:@"audio"]){
           
     // 控制器参数
     TRTCAudioViewController *vc = [[TRTCAudioViewController alloc] init];
      NSLog(@"data apid:%u",(unsigned int)param.sdkAppId);
     [vc setParam:param];
     vc.guestimg = [data objectForKey:@"guestimg"];
     vc.guestname = [data objectForKey:@"guestname"];
     vc.totaltime = totaltime;
     //vc.param = param;
     // 视频通话场景

     //self->controller = vc;
     vc.modalPresentationStyle = UIModalPresentationCurrentContext;
     [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          [self.viewController presentViewController:vc animated:YES completion:nil];
     }];
        
    }else{
      // 控制器参数
     TRTCVideoViewController *vc = [[TRTCVideoViewController alloc] init];
     NSLog(@"data:%@",param);
     [vc setParam:param];
     vc.totaltime = totaltime;
     //vc.param = param;
     // 视频通话场景
     __weak CDVWwaretrtc *w_self = self;
      vc.onHangUp = ^{
          [w_self onHangUp];
      };
     //self->controller = vc;
     vc.modalPresentationStyle = UIModalPresentationCurrentContext;
     [[NSOperationQueue mainQueue] addOperationWithBlock:^{
          [self.viewController presentViewController:vc animated:YES completion:nil];
     }];
        
    }
     
        
}

-(void) onHangUp {
    if (self->onFinishedCallbackId) {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString: @"true"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:onFinishedCallbackId];
    }
}




-(void)checkPermission:(CDVInvokedUrlCommand*)command
{
  CDVPluginResult* pluginResult = nil;  
  NSMutableDictionary* result = [NSMutableDictionary dictionaryWithCapacity:0];
  //[result setDictionary:values];
  [result setObject:@(true) forKey:@"success"];
 
  pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];

  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId]; 
   
}

@end
