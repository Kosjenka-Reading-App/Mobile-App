//
// Created by ekulovic on 03/12/2018.
//

#ifndef SHOWCASEDEMO_ANDROIDCAPTURE_H
#define SHOWCASEDEMO_ANDROIDCAPTURE_H

#include "AndroidStreamCapture.h"
#include "AndroidImageCapture.h"

namespace VisageSDK
{
    class AndroidCapture {

    public:
        AndroidStreamCapture *cameraCapture = 0;
        AndroidImageCapture *imageCapture = 0;

    public:
        AndroidCapture();

        AndroidCapture(int width, int height, int format = VISAGE_FRAMEGRABBER_FMT_LUMINANCE);
        AndroidCapture(int width, int height, int orientation, int flip);

        ~AndroidCapture();

        VsImage *GrabFrame(long &timeStamp);

        void WriteFrame(unsigned char *imageData, int width, int height);
        void WriteFrameYUV420(unsigned char* imageDataChannel0, unsigned char* imageDataChannel1,
                         unsigned char* imageDataChannel2, long timestamp_A, int pixelStride);
    };
}

#endif //SHOWCASEDEMO_ANDROIDCAPTURE_H
