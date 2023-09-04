//
//  BNHErrorFactory.h
//  BinahAI
//
//  Created by Tal Lerman on 18/01/2023.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

typedef NSInteger BNHAlertCode;

@interface BNHErrorFactory : NSObject

+ (NSError *_Nonnull)errorForCode:(BNHAlertCode)code;

+ (NSError *_Nonnull)errorForCode:(BNHAlertCode)code
                   customUserInfo:(NSDictionary *_Nullable)customUserInfo
                  underlyingError:(NSError *_Nullable)underlyingError;

@end

NS_ASSUME_NONNULL_END
