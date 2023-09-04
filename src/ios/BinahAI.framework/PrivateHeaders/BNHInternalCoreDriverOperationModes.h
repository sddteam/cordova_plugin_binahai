//
//  BNHInternalCoreDriverOperationModes.h
//  BinahAI
//
//  Created by Tal Lerman on 23/02/2023.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

typedef int BNHInternalCoreDriverOperationMode NS_SWIFT_NAME(InternalCoreDriverOperationMode);

NS_SWIFT_NAME(InternalCoreDriverOperationModes)
@interface BNHInternalCoreDriverOperationModes : NSObject

@property (class, nonatomic, readonly) BNHInternalCoreDriverOperationMode none;
@property (class, nonatomic, readonly) BNHInternalCoreDriverOperationMode measure;
@property (class, nonatomic, readonly) BNHInternalCoreDriverOperationMode signalLog;
@property (class, nonatomic, readonly) BNHInternalCoreDriverOperationMode recorder;
@property (class, nonatomic, readonly) BNHInternalCoreDriverOperationMode performance;
@property (class, nonatomic, readonly) BNHInternalCoreDriverOperationMode rgbTest;
@property (class, nonatomic, readonly) BNHInternalCoreDriverOperationMode videoTest;
@property (class, nonatomic, readonly) BNHInternalCoreDriverOperationMode objectDetectionLog;
@property (class, nonatomic, readonly) BNHInternalCoreDriverOperationMode vitalSignsSignalLog;
@property (class, nonatomic, readonly) BNHInternalCoreDriverOperationMode reportLog;
@property (class, nonatomic, readonly) BNHInternalCoreDriverOperationMode cameraMetadataLog;

@end

NS_ASSUME_NONNULL_END
