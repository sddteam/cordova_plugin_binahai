#import <Cordova/CDV.h>
#import <Cordova/CDVPlugin.h>
#import <Cordova/CDVInvokedUrlCommand.h>
#import "CameraRenderController.h"

@interface BinahAi : CDVPlugin <ImagePreviewListener>

- (void) coolMethod:(CDVInvokedUrlCommand*)command;
- (void) startCamera:(CDVInvokedUrlCommand*)command;
- (void) stopCamera:(CDVInvokedUrlCommand*)command;
- (void) startScan:(CDVInvokedUrlCommand*)command;
- (void) stopScan:(CDVInvokedUrlCommand*)command;
- (void) imageValidation:(CDVInvokedUrlCommand*)command;
- (void) getSessionState:(CDVInvokedUrlCommand*)command;

@property (nonatomic, strong) CDVInvokedUrlCommand *startScanCommand;
@property (nonatomic, strong) CDVInvokedUrlCommand *startCameraCommand;
@property (nonatomic, strong) CDVInvokedUrlCommand *getSessionStateCommand;
@property (nonatomic, strong) CDVInvokedUrlCommand *imageValidationCommand;

@property (nonatomic) CameraRenderController *cameraRenderController;
@property (nonatomic) id<BNHSession> mSession;

@end
