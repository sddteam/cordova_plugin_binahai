//
//  BNHInternalVitalSign.h
//  BinahAI
//
//  Created by Tal Lerman on 22/02/2023.
//

#import <BinahAI/BNHVitalSignType.h>

#import <Foundation/Foundation.h>


@class BNHVitalSignConfidence;

NS_ASSUME_NONNULL_BEGIN

NS_SWIFT_NAME(InternalVitalSign)
@interface BNHInternalVitalSign : NSObject

@property (nonatomic) id value;
@property (nonatomic) BNHVitalSignType type;
@property (nonatomic, nullable) BNHVitalSignConfidence *confidence;

- (instancetype)init NS_UNAVAILABLE;
- (instancetype)new NS_UNAVAILABLE;

- (instancetype)initWithValue:(id)value
                         type:(BNHVitalSignType)type
                   confidence:(BNHVitalSignConfidence *_Nullable)confidence;

@end

NS_ASSUME_NONNULL_END
