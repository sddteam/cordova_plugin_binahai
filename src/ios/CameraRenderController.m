#import <Foundation/Foundation.h>
#import <GLKit/GLKit.h>
#import <OpenGLES/ES2/glext.h>
#import "CameraRenderController.h"
#import "BinahAi.h"

@implementation CameraRenderController
@synthesize context = _context;
@synthesize delegate;
static NSString *const licenseKey = @"668765-6009B5-426FAD-D62FC0-D89858-19B9FF";
static NSMutableDictionary *vitalHolder;
static dispatch_queue_t vitalHolderQueue;

+ (void)initialize{
    if(self == [CameraRenderController class]){
        vitalHolder = [NSMutableDictionary dictionary];
        vitalHolderQueue = dispatch_queue_create("com.binah.vitalHolderQueue", DISPATCH_QUEUE_SERIAL);
    }
}

- (CameraRenderController *)init{
    if(self = [super init]){
        self.renderLock = [[NSLock alloc] init];
    }
    return self;
}

- (void) viewDidLoad{
    [super viewDidLoad];
    
//    UIStoryboard *storyboard = [UIStoryboard storyboardWithName:@"CameraViewController" bundle:nil];
//
//    UIViewController *cameraViewController = [storyboard instantiateInitialViewController];
//
//    if(cameraViewController.presentingViewController){
//        [cameraViewController dismissViewControllerAnimated:NO completion:nil];
//    }
//
//    [self presentViewController:cameraViewController animated:YES completion:nil];
    [self createSession];

}

- (void)createSession{
    BNHLicenseDetails *licenseDetails = [[BNHLicenseDetails alloc] initWithLicenseKey:licenseKey];
    BNHFaceSessionBuilder *sessionBuilder = [[[[[BNHFaceSessionBuilder alloc] init]
                                               withImageListener:self]
                                              withVitalSignsListener:self]
                                             withSessionInfoListener:self];
    NSError *error = nil;
    id<BNHSession> _Nullable session = [sessionBuilder buildWithLicenseDetails:licenseDetails
                                                                         error:&error];
    if([self.delegate respondsToSelector:@selector(onCameraStarted:)]){
        [self.delegate onCameraStarted:session];
    }
    
    if(error != nil){
        NSLog(@"Received Error. Domain: %@ Code: %ld", error.domain, (long)error.code);
    }
}

- (void)onEnabledVitalSignsWithEnabledVitalSigns:(BNHSessionEnabledVitalSigns * _Nonnull)enabledVitalSigns {
    NSLog(@"Log");
}

- (void)onErrorWithErrorData:(BNHErrorData * _Nonnull)errorData {
    dispatch_async(dispatch_get_main_queue(), ^{
        NSLog(@"Received Error. Domain: %@ Code: %ld", errorData.domain, (long)errorData.code);
        NSMutableDictionary *errorDict =  [NSMutableDictionary dictionary];
        [errorDict setObject:@(errorData.code) forKey:@"code"];
        [errorDict setObject:errorData.domain forKey:@"domain"];
        
        if([self.delegate respondsToSelector:@selector(onError:)]){
            [self.delegate onError:errorDict];
        }
    });
}

- (void)onLicenseInfoWithLicenseInfo:(BNHLicenseInfo * _Nonnull)licenseInfo {
    NSLog(@"Log");
}

- (void)onSessionStateChangeWithSessionState:(enum BNHSessionState)sessionState {
    dispatch_async(dispatch_get_main_queue(), ^{
        [self.delegate onSessionState:@(sessionState)];
    });
    
}

- (void)onWarningWithWarningData:(BNHWarningData * _Nonnull)warningData {
    dispatch_async(dispatch_get_main_queue(), ^{
        NSLog(@"Received Warning. Domain: %@ Code: %ld", warningData.domain, (long)warningData.code);
        NSMutableDictionary *warningDict =  [NSMutableDictionary dictionary];
        [warningDict setObject:@(warningData.code) forKey:@"code"];
        [warningDict setObject:warningData.domain forKey:@"domain"];
        
        if([self.delegate respondsToSelector:@selector(onWarning:)]){
            [self.delegate onWarning:warningDict];
        }
    });
}

- (void)onImageWithImageData:(BNHImageData * _Nonnull)imageData {
    dispatch_async(dispatch_get_main_queue(), ^{
        self.cameraPreview.image = imageData.image;
        
        CGRect roi = imageData.roi;
        
        if(!CGRectIsNull(roi)){
            NSMutableDictionary *imageErrorCodeDict = [NSMutableDictionary dictionary];
            NSInteger imageValidity = imageData.imageValidity;
            if(imageValidity != BNHImageValidity.valid){
                [imageErrorCodeDict setObject:@(imageValidity) forKey:@"imageValidationError"];
            }else{
                [imageErrorCodeDict setObject:@(imageValidity) forKey:@"imageValidationError"];
            }
            
            [self.delegate onImageValidation:imageErrorCodeDict];
        }
    });
        
}

- (void)onFinalResultsWithResults:(BNHVitalSignsResults *)results {
    dispatch_async(dispatch_get_main_queue(), ^{
        NSArray<id<BNHVitalSign>> *vitalSigns = results.getResults;
        
        NSMutableDictionary<NSNumber *, NSString *> *signResults = [NSMutableDictionary dictionary];
        for(BNHVitalSignInt *sign in vitalSigns){
            //NSLog(@"%ld", (long)sign.value);
            //BNHVitalSignTypes *types;
            //NSArray<NSNumber *> *signTypes = types.all;
            
            if(sign.type == BNHVitalSignTypes.pulseRate){
                BNHVitalSignPulseRate *pulseRate = (BNHVitalSignPulseRate *) [results getResultOf:BNHVitalSignTypes.pulseRate];
                signResults[@(sign.type)] = [NSString stringWithFormat:@"%ld", (long)pulseRate.value];
                NSLog(@"Pulse rate: %ld", (long)pulseRate.value);
            }else if (sign.type == BNHVitalSignTypes.bloodPressure){
//                BNHBloodPressure *bloodPressure = (BNHBloodPressure *) [results getResultOf:BNHVitalSignTypes.bloodPressure];
//                NSString *systolic = [NSString stringWithFormat:@"%ld", (long)bloodPressure.systolic];
//                NSString *diastolic = [NSString stringWithFormat:@"%ld", (long)bloodPressure.diastolic];
//                signResults[@(sign.value)] = [NSString stringWithFormat:@"%@/%@", systolic, diastolic];
            }else if(sign.type == BNHVitalSignTypes.lfhf){
                BNHVitalSignLFHF *lfhf = (BNHVitalSignLFHF *) [results getResultOf:BNHVitalSignTypes.lfhf];
                signResults[@(sign.type)] = [NSString stringWithFormat:@"%ld", (long)lfhf.value];
            }else if(sign.type == BNHVitalSignTypes.meanRri){
                BNHVitalSignMeanRRI *meanRRI = (BNHVitalSignMeanRRI *) [results getResultOf:BNHVitalSignTypes.meanRri];
                signResults[@(sign.type)] = [NSString stringWithFormat:@"%ld", (long)meanRRI.value];
            }else if(sign.type == BNHVitalSignTypes.pnsIndex){
                BNHVitalSignPNSIndex *pnsIndex = (BNHVitalSignPNSIndex *) [results getResultOf:BNHVitalSignTypes.pnsIndex];
                signResults[@(sign.type)] = [NSString stringWithFormat:@"%ld", (long)pnsIndex.value];
            }else if(sign.type == BNHVitalSignTypes.pnsZone){
                BNHVitalSignPNSZone *pnsZone = (BNHVitalSignPNSZone *) [results getResultOf:BNHVitalSignTypes.pnsZone];
                signResults[@(sign.type)] = [NSString stringWithFormat:@"%ld", (long)pnsZone.value];
            }else if(sign.type == BNHVitalSignTypes.prq){
                BNHVitalSignPRQ *prq = (BNHVitalSignPRQ *) [results getResultOf:BNHVitalSignTypes.prq];
                signResults[@(sign.type)] = [NSString stringWithFormat:@"%ld", (long)prq.value];
            }else if(sign.type == BNHVitalSignTypes.rmssd){
                BNHVitalSignRMSSD *rmssd = (BNHVitalSignRMSSD *) [results getResultOf:BNHVitalSignTypes.rmssd];
                signResults[@(sign.type)] = [NSString stringWithFormat:@"%ld", (long)rmssd.value];
            }else if(sign.type == BNHVitalSignTypes.rri){
                BNHVitalSignRRI *rri = (BNHVitalSignRRI *) [results getResultOf:BNHVitalSignTypes.rri];
                signResults[@(sign.type)] = [NSString stringWithFormat:@"%ld", (long)rri.value];
            }else if(sign.type == BNHVitalSignTypes.respirationRate){
                BNHVitalSignRespirationRate *respirationRate = (BNHVitalSignRespirationRate *) [results getResultOf:BNHVitalSignTypes.respirationRate];
                signResults[@(sign.type)] = [NSString stringWithFormat:@"%ld", (long)respirationRate.value];
            }else if(sign.type == BNHVitalSignTypes.sd1){
                BNHVitalSignSD1 *sd1 = (BNHVitalSignSD1 *) [results getResultOf:BNHVitalSignTypes.sd1];
                signResults[@(sign.type)] = [NSString stringWithFormat:@"%ld", (long)sd1.value];
            }else if(sign.type == BNHVitalSignTypes.sd2){
                BNHVitalSignSD2 *sd2 = (BNHVitalSignSD2 *) [results getResultOf:BNHVitalSignTypes.sd2];
                signResults[@(sign.type)] = [NSString stringWithFormat:@"%ld", (long)sd2.value];
            }else if(sign.type == BNHVitalSignTypes.sdnn){
                BNHVitalSignSDNN *sdnn = (BNHVitalSignSDNN *) [results getResultOf:BNHVitalSignTypes.sdnn];
                signResults[@(sign.type)] = [NSString stringWithFormat:@"%ld", (long)sdnn.value];
            }else if(sign.type == BNHVitalSignTypes.snsIndex){
                BNHVitalSignSNSIndex *snsIndex = (BNHVitalSignSNSIndex *) [results getResultOf:BNHVitalSignTypes.snsIndex];
                signResults[@(sign.type)] = [NSString stringWithFormat:@"%ld", (long)snsIndex.value];
            }else if(sign.type == BNHVitalSignTypes.snsZone){
                BNHVitalSignSNSZone *snsZone = (BNHVitalSignSNSZone *) [results getResultOf:BNHVitalSignTypes.snsZone];
                signResults[@(sign.type)] = [NSString stringWithFormat:@"%ld", (long)snsZone.value];
            }else if(sign.type == BNHVitalSignTypes.stressIndex){
                BNHVitalSignStressLevel *stressLevel = (BNHVitalSignStressLevel *) [results getResultOf:BNHVitalSignTypes.stressLevel];
                signResults[@(sign.type)] = [NSString stringWithFormat:@"%ld", (long)stressLevel.value];
            }else if(sign.type == BNHVitalSignTypes.stressLevel){
                BNHVitalSignStressIndex *stressIndex = (BNHVitalSignStressIndex *) [results getResultOf:BNHVitalSignTypes.stressIndex];
                signResults[@(sign.type)] = [NSString stringWithFormat:@"%ld", (long)stressIndex.value];
            }else if(sign.type == BNHVitalSignTypes.wellnessIndex){
                BNHVitalSignWellnessIndex *wellnessIndex = (BNHVitalSignWellnessIndex *) [results getResultOf:BNHVitalSignTypes.wellnessIndex];
                signResults[@(sign.type)] = [NSString stringWithFormat:@"%ld", (long)wellnessIndex.value];
            }else if(sign.type == BNHVitalSignTypes.wellnessLevel){
                BNHVitalSignWellnessLevel *wellnessLevel = (BNHVitalSignWellnessLevel *) [results getResultOf:BNHVitalSignTypes.wellnessLevel];
                signResults[@(sign.type)] = [NSString stringWithFormat:@"%ld", (long)wellnessLevel.value];
            }else if(sign.type == BNHVitalSignTypes.oxygenSaturation){
                BNHVitalSignOxygenSaturation *oxygenSaturation = (BNHVitalSignOxygenSaturation *) [results getResultOf:BNHVitalSignTypes.oxygenSaturation];
                signResults[@(sign.type)] = [NSString stringWithFormat:@"%ld", (long)oxygenSaturation.value];
            }
        }
        
        NSDictionary<NSNumber *, NSString *> *signTypeNames = @{
            @(1): @"pulseRate",
            @(2): @"respirationRate",
            @(4): @"oxygenSaturation",
            @(8): @"sdnn",
            @(16): @"stressLevel",
            @(32): @"rri",
            @(64): @"bloodPressure",
            @(128): @"stressIndex",
            @(256): @"meanRri",
            @(512): @"rmssd",
            @(1024): @"sd1",
            @(2048): @"sd2",
            @(4096): @"prq",
            @(8192): @"pnsIndex",
            @(16384): @"pnsZone",
            @(32768): @"snsIndex",
            @(65536): @"snsIndex",
            @(131072): @"wellnessIndex",
            @(262144): @"wellnessLevel",
            @(524288): @"lfhf",
            @(1048576): @"HEMOGLOBIN",
            @(2097152): @"HEMOGLOBIN_A1C"
        };
        NSMutableArray *finalResult = [NSMutableArray array];
        for (NSNumber *key in signResults.allKeys) {
            NSString *signValue = signResults[key];
            NSString *signTypeName = signTypeNames[key];
            
            NSMutableDictionary *vitalSignDict = [NSMutableDictionary dictionary];
            [vitalSignDict setObject:signTypeName forKey:@"name"];
            [vitalSignDict setObject:signValue forKey:@"value"];
            [finalResult addObject:vitalSignDict];
        }
        [self.delegate onFinalResult:finalResult];
    });
}

- (void)onVitalSignWithVitalSign:(id<BNHVitalSign> _Nonnull)vitalSign {
    dispatch_async(vitalHolderQueue, ^{
        if(vitalSign.type == BNHVitalSignTypes.pulseRate){
            BNHVitalSignPulseRate *pulseRate = (BNHVitalSignPulseRate *) vitalSign;
            [vitalHolder setObject:@(pulseRate.value) forKey:@"pulseRate"];
        }else if(vitalSign.type == BNHVitalSignTypes.oxygenSaturation){
            BNHVitalSignOxygenSaturation *oxygenSaturation = (BNHVitalSignOxygenSaturation *) vitalSign;
            [vitalHolder setObject:@(oxygenSaturation.value) forKey:@"oxygenSaturation"];
        }else if(vitalSign.type == BNHVitalSignTypes.respirationRate){
            BNHVitalSignRespirationRate *respirationRate = (BNHVitalSignRespirationRate *) vitalSign;
            [vitalHolder setObject:@(respirationRate.value) forKey:@"respirationRate"];
        }
    });
    if([self.delegate respondsToSelector:@selector(onStartScan:)]){
        dispatch_async(dispatch_get_main_queue(), ^{
            [self.delegate onStartScan:vitalHolder];
        });
    }
}

- (void)showToastMessage:(NSString *)message {
    UIAlertController *alertController = [UIAlertController alertControllerWithTitle:nil
                                                                             message:message
                                                                      preferredStyle:UIAlertControllerStyleAlert];
    
    [self presentViewController:alertController animated:YES completion:nil];
    
    // Dismiss the alert after a short delay (e.g., 2 seconds)
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(2.0 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        [alertController dismissViewControllerAnimated:YES completion:nil];
    });
}

@end

