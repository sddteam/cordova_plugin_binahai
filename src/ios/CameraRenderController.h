#import <GLKit/GLKit.h>
#import <UIKit/UIKit.h>
#import <BinahAI/BinahAI.h>


@protocol ImagePreviewListener <NSObject>

- (void)onStartScan:(NSDictionary *)vitalSign;
- (void)onFinalResult: (NSArray *)vitalSignsResult;
- (void)onImageValidation: (NSDictionary *)imageValidation;
- (void)onSessionState: (NSNumber *)sessionState;
- (void)onCameraStarted:(id<BNHSession>)session;
- (void)onCameraError:(NSError *)error;
- (void)onWarning:(NSMutableDictionary *)warning;
- (void)onError:(NSMutableDictionary *)error;

@end

@interface CameraRenderController : UIViewController <BNHSessionInfoListener, BNHImageListener, BNHVitalSignsListener> {
    CVOpenGLESTextureCacheRef _videoTextureCache;
    GLuint _renderBuffer;
    
}
@property (unsafe_unretained, nonatomic) IBOutlet UIImageView *cameraPreview;
@property (weak, nonatomic) IBOutlet UILabel *measureState;
@property (nonatomic, weak) id<ImagePreviewListener> delegate;
@property (nonatomic) CIContext *ciContext;
@property (nonatomic) EAGLContext *context;
//@property (nonatomic, assign) id delegate;
@property (nonatomic) NSLock *renderLock;

@end
