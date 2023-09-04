/********* BinahAi.m Cordova Plugin Implementation *******/

#import <BinahAI/BinahAI.h>
#import <Cordova/CDV.h>
#import <UIKit/UIKit.h>
#import "BinahAi.h"
#import "CameraRenderController.h"

@implementation BinahAi

- (void)coolMethod:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult = nil;
    NSString* echo = [command.arguments objectAtIndex:0];

    if (echo != nil && [echo length] > 0) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:echo];
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    }

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)startCamera:(CDVInvokedUrlCommand*)command
{
    self.startCameraCommand = command;
    CDVPluginResult* pluginResult = nil;
    NSString* echo = @"Camera started!";

    dispatch_async(dispatch_get_main_queue(), ^{
        self.webView.opaque = NO;
        self.webView.backgroundColor = [UIColor clearColor];

        UIStoryboard *storyboard = [UIStoryboard storyboardWithName:@"CameraViewController" bundle:nil];
        CameraRenderController *cameraViewController = [storyboard instantiateInitialViewController];

        cameraViewController.delegate = self;

        cameraViewController.view.opaque = NO;
        cameraViewController.view.backgroundColor = [UIColor clearColor];

        UIViewController *rootViewController = [UIApplication sharedApplication].delegate.window.rootViewController;

        [rootViewController.view addSubview:cameraViewController.view];
        cameraViewController.view.frame = rootViewController.view.bounds;
        [rootViewController.view bringSubviewToFront:self.webView];
    });
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:echo];
    [pluginResult setKeepCallback:@YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.startCameraCommand.callbackId];
}

- (void)stopCamera:(CDVInvokedUrlCommand *)command
{
    
}

- (void)startScan:(CDVInvokedUrlCommand *)command
{
    self.startScanCommand = command;
    NSLog(@"START SCAN");
    NSInteger measurementDuration = 60;
    NSError *error = nil;
    [self.mSession startWithMeasurementDuration:measurementDuration error:&error];
    if(error != nil){
        NSLog(@"Received Error. Domain: %@ Code: %ld", error.domain, (long)error.code);
    }
}

- (void)stopScan:(CDVInvokedUrlCommand *)command
{
    if(self.mSession.state != BNHSessionStateReady){
        NSError *error = nil;
        [self.mSession stopAndReturnError:&error];
        if(error != nil){
            NSLog(@"Received Error. Domain: %@ Code: %ld", error.domain, (long)error.code);
        }
    }
}

- (void)imageValidation:(CDVInvokedUrlCommand *)command
{
    self.imageValidationCommand = command;
}

- (void) getSessionState:(CDVInvokedUrlCommand *)command
{
    self.getSessionStateCommand = command;
}

- (void)onCameraError:(NSError *)error {
    
}

- (void)onCameraStarted:(id<BNHSession>)session {
    self.mSession = session;
    NSLog(@"onCameraStarted");
}

- (void)onFinalResult:(NSArray *)vitalSignsResult {
    if([self startScanCommand] != nil){
        CDVPluginResult *startScanPluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:vitalSignsResult];
        [startScanPluginResult setKeepCallback:@NO];
        [self.commandDelegate sendPluginResult:startScanPluginResult callbackId:self.startScanCommand.callbackId];
    }
}

- (void)onImageValidation:(NSDictionary *)imageValidation {
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:imageValidation];
    [pluginResult setKeepCallback:@YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.imageValidationCommand.callbackId];
}

- (void)onSessionState:(NSNumber *)sessionState {
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:sessionState.stringValue];
    [pluginResult setKeepCallback:@YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.getSessionStateCommand.callbackId];
}

- (void)onStartScan:(NSDictionary *)vitalSign {
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:vitalSign];
    [pluginResult setKeepCallback:@YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.startScanCommand.callbackId];
    
}

- (void)onError:(NSMutableDictionary *)error {
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:error];
    [pluginResult setKeepCallback:@NO];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.startCameraCommand.callbackId];
}


- (void)onWarning:(NSMutableDictionary *)warning {
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:warning];
    [pluginResult setKeepCallback:@YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.startCameraCommand.callbackId];
}


@end
