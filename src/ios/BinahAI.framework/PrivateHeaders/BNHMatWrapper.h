//
//  BNHMatWrapper.h
//  BinahAI
//
//  Created by Tal Lerman on 09/01/2023.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>


NS_ASSUME_NONNULL_BEGIN

@interface BNHMatWrapper : NSObject

@property (nonatomic, readonly) NSObject *matObj;

- (instancetype)init NS_UNAVAILABLE;
- (instancetype)new NS_UNAVAILABLE;
- (instancetype)initWithMat:(NSObject *)matObj;

@end

NS_ASSUME_NONNULL_END
