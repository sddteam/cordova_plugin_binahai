//
//  BNHImageHelper.h
//  BinahAI
//
//  Created by Tal Lerman on 09/01/2023.
//

#import <BinahAI/BNHMatWrapper.h>

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <AVFoundation/AVFoundation.h>


NS_ASSUME_NONNULL_BEGIN

@interface BNHImageHelper : NSObject


+ (CGImageRef)imageFromSampleBuffer:(CMSampleBufferRef)sampleBuffer;

+ (UIImage *)getUIImageOutputSampleBuffer:(CMSampleBufferRef)sampleBuffer
                              andPosition:(AVCaptureDevicePosition)position;

+ (CIImage *)getCIImageFromCMSampleBuffer:(CMSampleBufferRef)sampleBuffer
                       includeAttachments:(BOOL)includeAttachments
                              andPosition:(AVCaptureDevicePosition)position;

+ (CIImage *)getModifiedCIImage:(CIImage *)ciImage;

+ (CVPixelBufferRef)getCVPixelBufferFromCIImage:(CIImage *)ciImage
                            withPixelFormatType:(OSType)pixelFormatType;

+ (NSDictionary *)getImageMetadata:(UIImage *)image;


#pragma mark - cv::Mat

+ (UIImage *)imageWithMatWrapper:(BNHMatWrapper *)matWrapper;

+ (BNHMatWrapper *)getMatWrapperFromOutputSampleBuffer:(CMSampleBufferRef)sampleBuffer
                                            colorSpace:(AVCaptureColorSpace)colorSpace;

+ (BNHMatWrapper *)getMatWrapperFromOutputPixelBuffer:(CVPixelBufferRef)pixelBuffer
                                           colorSpace:(AVCaptureColorSpace)colorSpace;

//void imageToMat(UIImage *image, cv::Mat &mat);

//void imageToMat16Bit(UIImage *image, cv::Mat &mat);
    

@end

NS_ASSUME_NONNULL_END
