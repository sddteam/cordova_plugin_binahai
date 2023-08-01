//
//  BNHInternalCoreProcessResult.h
//  BinahAI
//
//  Created by Tal Lerman on 23/02/2023.
//

#import <Foundation/Foundation.h>

@class BNHRoiData;
@class BNHFaceAngles;


NS_ASSUME_NONNULL_BEGIN

NS_SWIFT_NAME(InternalCoreProcessResult)
@interface BNHInternalCoreProcessResult : NSObject

@property (nonatomic) CGRect roi;
@property (nonatomic) NSInteger imageValidity;
@property (nonatomic, nullable) BNHFaceAngles *angles;
@property (nonatomic, nullable) NSNumber *faceDistance;

- (instancetype)init NS_UNAVAILABLE;
- (instancetype)new NS_UNAVAILABLE;

- (instancetype)initWithROI:(CGRect)roi
               imageVailidy:(NSInteger)imageValidity
                 faceAngles:(BNHFaceAngles *_Nullable)angles
               faceDistance:(NSNumber *_Nullable)faceDistance;

@end

NS_ASSUME_NONNULL_END
