#ifndef __AndroidCameraCapture_h__
#define __AndroidCameraCapture_h__

#include <pthread.h>
#include <cerrno>
#include "vs_main.h"
#include <vector>
#include <cmath>

#define VISAGE_FRAMEGRABBER_FMT_RGB 0
#define VISAGE_FRAMEGRABBER_FMT_BGR 1
#define VISAGE_FRAMEGRABBER_FMT_LUMINANCE 2
#define VISAGE_FRAMEGRABBER_FMT_RGBA 3
#define VISAGE_FRAMEGRABBER_FMT_BGRA 4

#define VISAGE_FRAMEGRABBER_ORIGIN_TL 0
#define VISAGE_FRAMEGRABBER_ORIGIN_BL 1

using namespace std;

namespace VisageSDK
{

/** AndroidStreamCapture demonstrates use of raw camera image
 * input to track from Android camera.
 * @ref GrabFrame method will be periodically called to get new frame.
 * For inputing new frame, @ref WriteFrame should be used. This method expects frame in
 * Android camera NV21 format (YUV420sp). YUV420sp to RGB converting, rotation and flipping
 * is done in @ref GrabFrame.
 */
class AndroidStreamCapture {

public:

	bool frameArrived;

	/** Constructor.
	 *
	 */
	AndroidStreamCapture();

	/** Constructor.
	*
	* @param width width of image
	* @param height height of image
	* @param orientation Orientation of image. Allowed values are 0, 90, 180, 270
	* @param flip Flip image horizontaly.
	*/
	AndroidStreamCapture(int width, int height, int orientation=0, int flip = 0);

	/** Destructor.
	 *
	 */
	~AndroidStreamCapture(void);

	/**
	 *
	 * This function is called periodically to get the new video frame to process.
	 *
	 */
	VsImage *GrabFrame(long &timeStamp);

	void WriteFrameYUV420(unsigned char* imageDataChannel0, unsigned char* imageDataChannel1,
						unsigned char* imageDataChannel2, long timestamp_A, int pixelStride);

	void YUV_NV21_TO_RGB(unsigned char* yuv, VsImage* buff, int width, int height);

	int clamp(int x);
	void rotateYUV(const unsigned char* input, unsigned char* output, int width, int height, int rotation, bool flip);


private:

	/**
	* Convert default Android camera output format (YUV420sp) to RGB.
	*/
	void YUV420toRGB(unsigned char* dataChannel0, unsigned char* dataChannel1, unsigned char* dataChannel2, VsImage* buff, int width, int height, int pixelStride);
	void convertYUV420toARGB(int y, int u, int v, char *r, char *g, char *b);

    std::vector <std::pair<VsImage*, long>> _buffers;

    int _wb;
    int _rb;
    int _ub;

    pthread_mutex_t mutex;
    pthread_mutex_t paramMutex;

	VsImage* bufferN;
	VsImage* bufferT;

	unsigned char *data;
	int orientation;
	int flip;
	int pts;
    int writeCount;
    int grabCount;
	int width, height;

	pthread_cond_t cond;
};

}

#endif // __AndroidCameraCapture_h__
