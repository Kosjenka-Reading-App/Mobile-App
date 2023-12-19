
#include "AndroidCapture.h"

namespace VisageSDK {

    AndroidCapture::AndroidCapture() {

    }

    AndroidCapture::AndroidCapture(int width, int height,
                                   int format) {
        imageCapture = new AndroidImageCapture(width, height, format);
        cameraCapture = 0;

    }

    AndroidCapture::AndroidCapture(int width, int height, int orientation, int flip ) {
        cameraCapture = new AndroidStreamCapture(width, height, orientation, flip);
        imageCapture = 0;
    }

    AndroidCapture::~AndroidCapture() {
        if(imageCapture)
            delete imageCapture;
        if(cameraCapture)
            delete cameraCapture;

    }

    VsImage *AndroidCapture::GrabFrame(long &timeStamp) {
        if(imageCapture)
            return imageCapture->GrabFrame(timeStamp);
        if(cameraCapture)
            return cameraCapture->GrabFrame(timeStamp);
    }

    void AndroidCapture::WriteFrame(unsigned char *imageData, int width, int height) {
        if(imageCapture)
            imageCapture->WriteFrame(imageData, width, height);
    }

    void AndroidCapture::WriteFrameYUV420(unsigned char *imageDataChannel0,
                                          unsigned char *imageDataChannel1,
                                          unsigned char *imageDataChannel2, long timestamp_A, int pixelStride) {

        if(cameraCapture)
            cameraCapture->WriteFrameYUV420(imageDataChannel0, imageDataChannel1, imageDataChannel2, timestamp_A, pixelStride);

    }

}
