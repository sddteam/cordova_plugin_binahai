//
//  BNHVitalSignTypesBitmaskCode.h
//  BinahAI
//
//  Created by Tal Lerman on 20/02/2022.
//  Copyright © 2022 binah.ai. All rights reserved.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

typedef int64_t BNHVitalSignTypeBitmaskCode NS_SWIFT_NAME(VitalSignTypeBitmaskCode);

NS_SWIFT_NAME(VitalSignTypesBitmaskCode)
@interface BNHVitalSignTypesBitmaskCode : NSObject

@property (class, nonatomic, readonly) BNHVitalSignTypeBitmaskCode none;
@property (class, nonatomic, readonly) BNHVitalSignTypeBitmaskCode pulseRate;
@property (class, nonatomic, readonly) BNHVitalSignTypeBitmaskCode respirationRate;
@property (class, nonatomic, readonly) BNHVitalSignTypeBitmaskCode oxygenSaturation;
@property (class, nonatomic, readonly) BNHVitalSignTypeBitmaskCode sdnn;
@property (class, nonatomic, readonly) BNHVitalSignTypeBitmaskCode stressLevel;
@property (class, nonatomic, readonly) BNHVitalSignTypeBitmaskCode rri;
@property (class, nonatomic, readonly) BNHVitalSignTypeBitmaskCode stressIndex;
@property (class, nonatomic, readonly) BNHVitalSignTypeBitmaskCode bloodPressure;
@property (class, nonatomic, readonly) BNHVitalSignTypeBitmaskCode meanRri;
@property (class, nonatomic, readonly) BNHVitalSignTypeBitmaskCode rmssd;
@property (class, nonatomic, readonly) BNHVitalSignTypeBitmaskCode sd1;
@property (class, nonatomic, readonly) BNHVitalSignTypeBitmaskCode sd2;
@property (class, nonatomic, readonly) BNHVitalSignTypeBitmaskCode prq;
@property (class, nonatomic, readonly) BNHVitalSignTypeBitmaskCode pnsIndex;
@property (class, nonatomic, readonly) BNHVitalSignTypeBitmaskCode pnsZone;
@property (class, nonatomic, readonly) BNHVitalSignTypeBitmaskCode snsIndex;
@property (class, nonatomic, readonly) BNHVitalSignTypeBitmaskCode snsZone;
@property (class, nonatomic, readonly) BNHVitalSignTypeBitmaskCode wellnessIndex;
@property (class, nonatomic, readonly) BNHVitalSignTypeBitmaskCode wellnessLevel;
@property (class, nonatomic, readonly) BNHVitalSignTypeBitmaskCode lfhf;
@property (class, nonatomic, readonly) BNHVitalSignTypeBitmaskCode hemoglobin;
@property (class, nonatomic, readonly) BNHVitalSignTypeBitmaskCode hemoglobinA1C;

@end

NS_ASSUME_NONNULL_END
