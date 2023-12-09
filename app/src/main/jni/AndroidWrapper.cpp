#include <jni.h>
#include <EGL/egl.h> 
#include <GLES/gl.h>
#include <vector>
#include <stdio.h>
#include <unistd.h>
#include "VisageTracker.h"
#include "VisageRendering.h"
#include "AndroidImageCapture.h"
#include "AndroidCameraCapture.h"
#include "VisageLivenessAction.h"
#include "VisageLivenessBrowRaise.h"
#include "VisageLivenessSmile.h"
#include "VisageLivenessBlink.h"
#include "LivenessGUI.h"
#include <random>
#include "LicenseString.h"

#include <android/log.h>
#define  LOG_TAG    "TrackerWrapper"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

using namespace VisageSDK;

// neccessary prototype declaration for licensing
namespace VisageSDK
{
	int initializeLicenseManager(JNIEnv* env, jobject obj, const char *licenseKeyFileName, void (*alertFunction)(const char*) = 0);
}

static AndroidImageCapture *a_cap_image = 0;
static AndroidCameraCapture *a_cap_camera = 0;
static VsImage *drawImageBuffer = 0;
static VsImage *renderImage = 0;

static float m_fps = 0;

void Sleep(int ms) {usleep(ms*1000);}

/** \file AndroidWrapper.cpp
 * Implementation of simple interface around visage|SDK VisageTracker functionality.
 *
 * In order for Android application, which uses Java as its primary programming language, to communicate with visage|SDK functionality, 
 * which uses C++ as its primary language it is necessary to use Java Native Interface as a framework between the two.  
 *
 * Key members of wrapper are:
 * - m_Tracker: the VisageTracker object
 * - trackingData: the TrackingData object used for retrieving and holding tracking data
 * - displayTrackingResults: method that demonstrates how to acquire, use and display tracking data and 3D face model
 * 
 */

extern "C" {

// ********************************
// Variables used in tracking thread
// ********************************
const int MAX_FACES = 4;
static VisageTracker *m_Tracker = 0;
static FaceData trackingData[MAX_FACES];
int *trackingStatus = 0;
float displayFramerate = 0;
int timestamp = 0;
int trackingTime;

// ********************************
// Buffers for track->render communication
// ********************************
static FaceData trackingDataBuffer[MAX_FACES];
int trackingStatusBuffer[MAX_FACES];

// ********************************
// Variables used for liveness
// ********************************

/**
* Maximal number of states that the actions will have.
* It should be set to the number of states that the action with maximal number of states has.
*/
static const int MAX_ACTION_STATES = 5;
/**
* Number of actions that will need to be verified in order to confirm liveness.
*/
static const int NUM_ACTIONS = 3;

/**
* Structure that holds the action and its accompanying messages and images that will be displayed to user during action verification
*/
struct Action
{
    VisageLivenessAction *action;
    std::string images[MAX_ACTION_STATES];
    std::string messages[MAX_ACTION_STATES];
};

/**
* Array of actions that will need to be verified.
*/
Action actions[NUM_ACTIONS];

/**
* VisageLivenessBrowRaise object.
* Used to verify that the user has raised its eyebrows.
*/
VisageLivenessAction *BrowRaiseAction;

/**
* VisageLivenessSmile object.
* Used to verify that the user has smiled.
*/
VisageLivenessAction *SmileAction;

/**
* VisageLivenessBlink object.
* Used to verify that the user has blinked.
*/
VisageLivenessAction *BlinkAction;

bool liveness = false;

int currAction = 0;
int actionState = -1;
int timePassed = 0;
long startTime_liveness;
long currentTime_liveness;

LivenessGUI *GUI;



// ********************************
// Variables used in rendering thread
// ********************************
static FaceData trackingDataRender[MAX_FACES];
int trackingStatusRender[MAX_FACES];
// Logo image
VsImage* logo = 0;
VsImage* faceContourImage = 0;
VsImage* fontImage = 0;
const char* faceContourImagePath = 0;

// ********************************
// Control flow variables
// ********************************
bool trackingOk = false;
bool isTracking = false;
bool orientationChanged = false;
bool trackerPaused = false;
bool trackerStopped;
//
int camOrientation;
int camHeight;
int camWidth;
int camFlip;
//
pthread_mutex_t displayRes_mutex;
pthread_mutex_t guardFrame_mutex;
//

GLuint texIds[3];

/**
* Texture ID for displaying frames from the tracker.
*/
GLuint frameTexId = 0;

GLuint instructionsTexId = 0;

/**
* Texture coordinates for displaying frames from the tracker.
*/
float xTexCoord;

/**
* Texture coordinates for displaying frames from the tracker.
*/
float yTexCoord;

/**
* Size of the texture for displaying frames from the tracker.
*/
int xTexSize;

/**
* Size of the texture for displaying frames from the tracker.
*/
int yTexSize;

/**
* Aspect of the video.
*/
float videoAspect;

/**
* Size of the OpenGL view.
*/
int glWidth;

/**
* Size of the OpenGL view.
*/
int glHeight;

// ********************************
// JNI variables
// ********************************
JNIEnv* _env;
jobject _obj;


// ********************************
// Helper functions
// ********************************

/**
 * Callback method for license notification.
 *
 * Alerts the user that the license is not valid
 */
void AlertCallback(const char* warningMessage)
{
	jclass dataClass = _env->FindClass("com/dsd/kosjenka/presentation/home/camera/CameraActivity");
	if (_env->ExceptionCheck())
			_env->ExceptionClear();
	if (dataClass != NULL)
	{
		jclass javaClassRef = (jclass) _env->NewGlobalRef(dataClass);
		jmethodID javaMethodRef = _env->GetMethodID(javaClassRef, "AlertDialogFunction", "(Ljava/lang/String;)V");
		if (_env->ExceptionCheck())
			_env->ExceptionClear();
		jstring message = _env->NewStringUTF(warningMessage);
		if (javaMethodRef != 0)
			_env->CallVoidMethod(_obj, javaMethodRef, message);

		_env->DeleteGlobalRef(javaClassRef);
		_env->DeleteLocalRef(message);
	}
}

/**
 * Simple timer function
 */
long getTimeNsec() {
	struct timespec now;
	clock_gettime(CLOCK_REALTIME, &now);
	return (long) ((now.tv_sec*1000000000LL + now.tv_nsec)/1000000LL);
}

void SetLivenessActions()
{
    BrowRaiseAction = new VisageLivenessBrowRaise();
    SmileAction = new VisageLivenessSmile();
    BlinkAction = new VisageLivenessBlink();

    actions[0].action = BrowRaiseAction;
    actions[0].images[VisageLivenessBrowRaise::STATE_ACTION_INITIALIZING] = "";
    actions[0].messages[VisageLivenessBrowRaise::STATE_ACTION_INITIALIZING] = "";
    actions[0].images[VisageLivenessBrowRaise::STATE_WAIT_FOR_FRONTAL] = "";
    actions[0].messages[VisageLivenessBrowRaise::STATE_WAIT_FOR_FRONTAL] = "Keep your head frontal!";
    actions[0].images[VisageLivenessBrowRaise::STATE_WAIT_FOR_STABLE] = "";
    actions[0].messages[VisageLivenessBrowRaise::STATE_WAIT_FOR_STABLE] = "Keep your head stable!";
    actions[0].images[VisageLivenessBrowRaise::STATE_WAIT_FOR_ACTION] = "";
    actions[0].messages[VisageLivenessBrowRaise::STATE_WAIT_FOR_ACTION] = "Raise your eyebrows!";
    actions[0].images[VisageLivenessBrowRaise::STATE_ACTION_VERIFIED] = "";
    actions[0].messages[VisageLivenessBrowRaise::STATE_ACTION_VERIFIED] = "";

    actions[1].action = SmileAction;
    actions[1].images[VisageLivenessSmile::STATE_ACTION_INITIALIZING] = "";
    actions[1].messages[VisageLivenessSmile::STATE_ACTION_INITIALIZING] = "";
    actions[1].images[VisageLivenessSmile::STATE_WAIT_FOR_FRONTAL] = "";
    actions[1].messages[VisageLivenessSmile::STATE_WAIT_FOR_FRONTAL] = "Keep your head frontal!";
    actions[1].images[VisageLivenessSmile::STATE_WAIT_FOR_STABLE] = "";
    actions[1].messages[VisageLivenessSmile::STATE_WAIT_FOR_STABLE] = "Keep your head stable!";
    actions[1].images[VisageLivenessSmile::STATE_WAIT_FOR_ACTION] = "";
    actions[1].messages[VisageLivenessSmile::STATE_WAIT_FOR_ACTION] = "Smile!";
    actions[1].images[VisageLivenessSmile::STATE_ACTION_VERIFIED] = "";
    actions[1].messages[VisageLivenessSmile::STATE_ACTION_VERIFIED] = "";

    actions[2].action = BlinkAction;
    actions[2].images[VisageLivenessBlink::STATE_ACTION_INITIALIZING] = "";
    actions[2].messages[VisageLivenessBlink::STATE_ACTION_INITIALIZING] = "";
    actions[2].images[VisageLivenessBlink::STATE_WAIT_FOR_FRONTAL] = "";
    actions[2].messages[VisageLivenessBlink::STATE_WAIT_FOR_FRONTAL] = "Keep your head frontal!";
    actions[2].images[VisageLivenessBlink::STATE_WAIT_FOR_STABLE] = "";
    actions[2].messages[VisageLivenessBlink::STATE_WAIT_FOR_STABLE] = "Keep your head stable!";
    actions[2].images[VisageLivenessBlink::STATE_WAIT_FOR_ACTION] = "";
    actions[2].messages[VisageLivenessBlink::STATE_WAIT_FOR_ACTION] = "Blink!";
    actions[2].images[VisageLivenessBlink::STATE_ACTION_VERIFIED] = "";
    actions[2].messages[VisageLivenessBlink::STATE_ACTION_VERIFIED] = "";
}

void RandomizeActions()
{
   std::random_device rd;
    std::mt19937 g(rd());

    std::shuffle(&actions[0], &actions[3], g);
}

// ********************************

// ********************************
// Wrapper function
// ********************************


static bool endsWith(const std::string& str, const std::string& suffix)
{
	return str.size() >= suffix.size() && 0 == str.compare(str.size()-suffix.size(), suffix.size(), suffix);
}

/**
 * Creates VsImage object for logo received from TrackerActivity
 *
 * @param logo_ - pixel data from loaded logo
 * @param width - logo width
 * @param height - logo height
 */

void
Java_com_visagetechnologies_visagetrackerdemo_TrackerActivity_WriteLogoImage(JNIEnv *env, jclass type, jbyteArray logo_, jint width, jint height) {

	jbyte *logoBytes = env->GetByteArrayElements(logo_, 0);

	pthread_mutex_lock(&displayRes_mutex);

	if (logo) {

		pthread_mutex_unlock(&displayRes_mutex);
		env->ReleaseByteArrayElements(logo_, logoBytes, 0);
		return;
	}

	logo = vsCreateImage(vsSize(width, height), VS_DEPTH_8U, 4);
	memcpy(logo->imageData, logoBytes, logo->imageSize);

	pthread_mutex_unlock(&displayRes_mutex);

	env->ReleaseByteArrayElements(logo_, logoBytes, 0);
}

/**
 * Creates VsImage object for face countour image received from TrackerActivity
 *
 * @param faceContourImage_ - pixel data from loaded image
 * @param width - image width
 * @param height - image height
 */
void Java_com_visagetechnologies_visagetrackerdemo_TrackerActivity_WriteFaceContourImage(JNIEnv *env, jclass type,
																			  jstring path, jbyteArray faceContourImage_, jint width,
																			  jint height) {

	jbyte *faceContourImageBytes = env->GetByteArrayElements(faceContourImage_, 0);

	pthread_mutex_lock(&displayRes_mutex);

	if (faceContourImage) {

		pthread_mutex_unlock(&displayRes_mutex);
		env->ReleaseByteArrayElements(faceContourImage_, faceContourImageBytes, 0);
		return;
	}

	faceContourImage = vsCreateImage(vsSize(width, height), VS_DEPTH_8U, 4);
	memcpy(faceContourImage->imageData, faceContourImageBytes, faceContourImage->imageSize);
	faceContourImagePath = env->GetStringUTFChars(path, 0);

    GUI->setImage(faceContourImagePath, faceContourImage);

	pthread_mutex_unlock(&displayRes_mutex);

	env->ReleaseByteArrayElements(faceContourImage_, faceContourImageBytes, 0);
}

/**
 * Creates VsImage object for font image received from TrackerActivity
 *
 * @param fontImage_ - pixel data from loaded image
 * @param width - image width
 * @param height - image height
 */
void Java_com_visagetechnologies_visagetrackerdemo_TrackerActivity_WriteFontImage(JNIEnv *env, jclass type,
																						  jbyteArray fontImage_, jint width,
																						  jint height) {
	jbyte *fontImageBytes = env->GetByteArrayElements(fontImage_, 0);

	pthread_mutex_lock(&displayRes_mutex);

	if (fontImage) {

		pthread_mutex_unlock(&displayRes_mutex);
		env->ReleaseByteArrayElements(fontImage_, fontImageBytes, 0);
		return;
	}

	fontImage = vsCreateImage(vsSize(width, height), VS_DEPTH_8U, 4);
	memcpy(fontImage->imageData, fontImageBytes, fontImage->imageSize);
	pthread_mutex_unlock(&displayRes_mutex);

	env->ReleaseByteArrayElements(fontImage_, fontImageBytes, 0);
}

/**
 * Method for initializing the tracker.
 *
 * This method creates a new VisageTracker objects and initializes the tracker.
 * @param configFilename - name of the configuration, along with the full path, to be used in tracking
 */
void Java_com_dsd_kosjenka_presentation_home_camera_CameraActivity_TrackerInit(JNIEnv *env, jobject obj, jstring pathToAssets, jstring configFilename)
{
    liveness = false;
	_env = env;
	_obj = obj;
	const char *_pathToAssets = env->GetStringUTFChars(pathToAssets, 0);
	trackerStopped = false;

    //example how to initialize license key
    std::string pathToLicense(_pathToAssets + std::string("/") + licenseKey);
	if (endsWith(licenseKey, ".vlc"))
        initializeLicenseManager(env, obj, pathToLicense.c_str(), AlertCallback);
	else
        initializeLicenseManager(env, obj, licenseKey.c_str(), AlertCallback); //for embedded license

	if (fontImage)
		VisageRendering::SetFontTexture((VsImage*)fontImage);

	//Set up mutex for track->render thread synchronization
	pthread_mutex_destroy(&displayRes_mutex);
	pthread_mutex_init(&displayRes_mutex, NULL);
	//Set up mutex for tracking->stopping tracking synchronization
	pthread_mutex_destroy(&guardFrame_mutex);
	pthread_mutex_init(&guardFrame_mutex, NULL);

	//Delete previously allocated objects
	delete a_cap_camera;
	a_cap_camera = 0;
	a_cap_image = 0;

    const char *_configFilename = env->GetStringUTFChars(configFilename, 0);

    std::string pathToConfig = _pathToAssets + std::string("/") + _configFilename;
	//Initialize tracker
	m_Tracker = new VisageTracker((_pathToAssets + std::string("/Facial Features Tracker.cfg")).c_str());

//	LOGI("%s", _configFilename);
	env->ReleaseStringUTFChars(pathToAssets, _pathToAssets);
}

void Java_com_visagetechnologies_visagetrackerdemo_TrackerActivity_LivenessInit(JNIEnv *env, jobject obj)
{
    GUI = new LivenessGUI();
    liveness = true;
    SetLivenessActions();
    RandomizeActions();

}

void Java_com_visagetechnologies_visagetrackerdemo_TrackerActivity_RestartLiveness(JNIEnv *env, jobject obj)
{
    for (int i = 0; i < NUM_ACTIONS; i++)
        actions[i].action->reset();

    currAction = 0;
    RandomizeActions();
}

/**
 * Method for "pausing" tracking
 *
 * Causes the tracking thread to run without doing any work - blocks the calls to the track function
 */
void Java_com_visagetechnologies_visagetrackerdemo_TrackerActivity_PauseTracker(JNIEnv *env, jobject obj)
{
	trackerPaused = true;

if (liveness)
    {
        for (int i = 0; i < NUM_ACTIONS; i++)
            actions[i].action->reset();

        currAction = 0;

        RandomizeActions();
    }
}

/**
 * Method for starting tracking in image
 *
 * Initiates a while loop. Image is grabbed and track() function is called every iteration.
 * Copies data to the buffers for rendering.
 */
void Java_com_visagetechnologies_visagetrackerdemo_TrackerActivity_TrackFromImage(JNIEnv *env, jobject obj, jint width, jint height )
{
	while(!trackerStopped)
	{
		if (m_Tracker && a_cap_image && !trackerStopped)
		{
			pthread_mutex_lock(&guardFrame_mutex);
			long ts;
			drawImageBuffer = a_cap_image->GrabFrame(ts);
			if (!renderImage)
				renderImage = vsCloneImage(drawImageBuffer);
			if (trackerStopped || drawImageBuffer == 0)
			{
				pthread_mutex_unlock(&guardFrame_mutex);
				return;
			}
			long startTime = getTimeNsec();
			trackingStatus = m_Tracker->track(width, height,  (const char*)drawImageBuffer->imageData, trackingData, VISAGE_FRAMEGRABBER_FMT_RGB, VISAGE_FRAMEGRABBER_ORIGIN_TL, 0, -1, MAX_FACES);
			long endTime = getTimeNsec();
			trackingTime = (int)(endTime - startTime);
			LOGE("Tracking time is %d", trackingTime);
			pthread_mutex_unlock(&guardFrame_mutex);
			pthread_mutex_lock(&displayRes_mutex);
			for (int i=0; i<MAX_FACES; i++)
			{
				if (trackingStatus[i] == TRACK_STAT_OFF)
					continue;
				std::swap(trackingDataBuffer[i], trackingData[i]);
				trackingStatusBuffer[i] = trackingStatus[i];
				trackingOk = true;

			}
			isTracking = true;
			pthread_mutex_unlock(&displayRes_mutex);
		}
		else
			Sleep(100);

			}
	return;
}

/**
 * Method that sets frame parameters
 *
 * Called initially before tracking starts and every time orientation changes. Creates buffer of
 * correct sizes and sets orientationChanged to true
 *
 * @param width - width of the received frame
 * @param height - height of the received frame
 * @param orientation - orientation of the frame derived from camera and screen orientation
 * @param flip - 1 if frame is mirrored, 0 if not
 */
void Java_com_dsd_kosjenka_presentation_home_camera_CameraActivity_setParameters(JNIEnv *env, jobject obj, jint width, jint height, jint orientation, jint flip)
{
	camOrientation = orientation;
	camHeight = height;
	camWidth = width;
	camFlip = flip;

	//Dispose of the previous drawImageBuffer
	if (!drawImageBuffer)
	{
		vsReleaseImage(&drawImageBuffer);
		drawImageBuffer = 0;
	}

	//Depending on the camera orientation (landscape or portrait), create a drawImageBuffer buffer for storing pixels that will be used in the tracking thread
	if (camOrientation == 90 || camOrientation == 270)
		drawImageBuffer = vsCreateImage(vsSize(height, width), VS_DEPTH_8U, 3);
	else
		drawImageBuffer = vsCreateImage(vsSize(width, height), VS_DEPTH_8U, 3);

	//Dispose of the previous drawImage
	vsReleaseImage(&renderImage);
	renderImage = 0;

	//Create a renderImage buffer based on the drawImageBuffer which will be used in the rendering thread
	//NOTE: Copying imageData between track and draw buffers is protected with mutexes
	renderImage = vsCloneImage(drawImageBuffer);

	orientationChanged = true;

//	return 0;
}

/**
 * Method for starting tracking from camera
 *
 * Initiates a while loop. Image is grabbed and track() function is called every iteration.
 * Copies data to the buffers for rendering.
 */
void Java_com_dsd_kosjenka_presentation_home_camera_CameraActivity_TrackFromCam(JNIEnv *env, jobject obj)
{
	while (!trackerStopped)
	{
		if (m_Tracker && a_cap_camera && !trackerStopped && !trackerPaused)
		{
			pthread_mutex_lock(&guardFrame_mutex);
			long ts;
			VsImage *trackImage = a_cap_camera->GrabFrame(ts);
			if (trackerStopped || trackImage == 0)
			{
				pthread_mutex_unlock(&guardFrame_mutex);
				return;
			}
			long startTime = getTimeNsec();
			if (camOrientation == 90 || camOrientation == 270)
				trackingStatus = m_Tracker->track(camHeight, camWidth, trackImage->imageData, trackingData, VISAGE_FRAMEGRABBER_FMT_RGB, VISAGE_FRAMEGRABBER_ORIGIN_TL, 0, -1, MAX_FACES);
			else
				trackingStatus = m_Tracker->track(camWidth, camHeight, trackImage->imageData, trackingData, VISAGE_FRAMEGRABBER_FMT_RGB, VISAGE_FRAMEGRABBER_ORIGIN_TL, 0, -1, MAX_FACES);
			long endTime = getTimeNsec();
			trackingTime = (int)endTime - startTime;

			pthread_mutex_unlock(&guardFrame_mutex);

			//***
			//*** LOCK render thread while copying data for rendering ***
			//***
			pthread_mutex_lock(&displayRes_mutex);
			for (int i=0; i<MAX_FACES; i++)
			{
				if (trackingStatus[i] == TRACK_STAT_OFF)
					continue;
				std::swap(trackingDataBuffer[i], trackingData[i]);
				trackingStatusBuffer[i] = trackingStatus[i];
				//Signalize that at least one face was tracked
				trackingOk = true;

			}
			isTracking = true;
            vsCopy(trackImage, drawImageBuffer);

			//***
			//*** UNLOCK render thread ***
			//***
			pthread_mutex_unlock(&displayRes_mutex);
		}
		else
			Sleep(1);
	}
	return;
}


void Java_com_visagetechnologies_visagetrackerdemo_TrackerActivity_PerformLiveness(JNIEnv *env, jobject obj)
{
    for (int i = 0; i < NUM_ACTIONS; i++)
		actions[i].action->reset();
	timePassed = 0;
    startTime_liveness = getTimeNsec();

	currAction = 0;
    actionState = -1;

	while (!trackerStopped)
	{
		if (m_Tracker && a_cap_camera && !trackerStopped && !trackerPaused)
		{
			pthread_mutex_lock(&guardFrame_mutex);
			long ts;
			VsImage *trackImage = a_cap_camera->GrabFrame(ts);
			if (trackerStopped || trackImage == 0)
			{
				pthread_mutex_unlock(&guardFrame_mutex);
				return;
			}
			if (currAction >= NUM_ACTIONS)
			{
			    vsCopy(trackImage, drawImageBuffer);
                trackingStatusBuffer[0] = trackingStatus[0];
                pthread_mutex_unlock(&guardFrame_mutex);
                continue;
			}
			long startTime = getTimeNsec();
			if (camOrientation == 90 || camOrientation == 270)
				trackingStatus = m_Tracker->track(camHeight, camWidth, trackImage->imageData, trackingData, VISAGE_FRAMEGRABBER_FMT_RGB, VISAGE_FRAMEGRABBER_ORIGIN_TL, 0, -1);
			else
				trackingStatus = m_Tracker->track(camWidth, camHeight, trackImage->imageData, trackingData, VISAGE_FRAMEGRABBER_FMT_RGB, VISAGE_FRAMEGRABBER_ORIGIN_TL, 0, -1);
			long endTime = getTimeNsec();

			pthread_mutex_unlock(&guardFrame_mutex);

			trackingTime = (int)endTime - startTime;

			if (trackingStatus[0] == TRACK_STAT_OK)
            {
                actionState = actions[currAction].action->update(trackingData, trackImage);
                currentTime_liveness = getTimeNsec();
                timePassed = (int)currentTime_liveness - startTime_liveness;
            }
            else
            {
                //reset liveness detection if face is lost
                for (int i = 0; i < NUM_ACTIONS; i++)
                    actions[i].action->reset();
                currAction = 0;
                RandomizeActions();
            }

			if (actionState == 0)
            {
                currAction++;
                startTime_liveness = getTimeNsec();
            }

			//***
			//*** LOCK render thread while copying data for rendering ***
			//***
			pthread_mutex_lock(&displayRes_mutex);
            if (trackingStatus[0] != TRACK_STAT_OFF)
            {
                vsCopy(trackImage, drawImageBuffer);
                trackingStatusBuffer[0] = trackingStatus[0];
                trackingOk = true;
            }
			isTracking = true;

			//***
			//*** UNLOCK render thread ***
			//***
			pthread_mutex_unlock(&displayRes_mutex);
		}
		else
			Sleep(1);


	}
	return;
}

void ReleaseActions()
{
	BrowRaiseAction = 0;
	delete BrowRaiseAction;

	SmileAction = 0;
	delete SmileAction;

	BlinkAction = 0;
	delete BlinkAction;
}


/**
 * Stops the tracker and cleans memory
 */
void Java_com_dsd_kosjenka_presentation_home_camera_CameraActivity_TrackerStop(JNIEnv *env, jobject obj)
{
	if (m_Tracker)
	{
		trackerStopped = true;
		trackingOk =false;
		pthread_mutex_lock(&guardFrame_mutex);
		for (int i=0; i<MAX_FACES; i++)
			{
				trackingStatusRender[i] = TRACK_STAT_OFF;
				trackingStatusBuffer[i] = TRACK_STAT_OFF;
				trackingStatus[i] = TRACK_STAT_OFF;
			}
		m_Tracker->stop();
		delete m_Tracker;
		m_Tracker = 0;
		vsReleaseImage(&drawImageBuffer);
		drawImageBuffer = 0;
		vsReleaseImage(&renderImage);
		renderImage = 0;
		VisageRendering::Reset();
		pthread_mutex_unlock(&guardFrame_mutex);

	}

	if (liveness)
	    ReleaseActions();
}


/**
* Writes raw image data into @ref VisageSDK::AndroidImageCapture object. VisageTracker reads this image and performs tracking.
* @param frame byte array with image data
* @param width image width
* @param height image height
*/
void Java_com_visagetechnologies_visagetrackerdemo_ImageTrackerView_WriteFrameImage(JNIEnv *env, jobject obj, jbyteArray frame, jint width, jint height) 
{
	if (!a_cap_image)
		a_cap_image = new AndroidImageCapture(width, height, VISAGE_FRAMEGRABBER_FMT_RGB);
	jbyte *f = env->GetByteArrayElements(frame, 0);
	a_cap_image->WriteFrame((unsigned char *)f, (int)width, (int)height);
	env->ReleaseByteArrayElements(frame, f, 0);
}


/**
* Writes raw image data into @ref VisageSDK::AndroidCameraCapture object. VisageTracker reads this image and performs tracking. User should call this 
* function whenever new frame from camera is available. Data inside frame should be in Android NV21 (YUV420sp) format and @ref VisageSDK::AndroidCameraCapture
* will perform conversion to RGB.
*
* This function will reinitialize AndroidCameraCapture wrapper in case setParameter function was called, signaled by the orientationChanged flag. After creation,
* tracking will be resumed, signaled by trackerPaused flag.
* @param frame byte array with image data
*/
void Java_com_dsd_kosjenka_presentation_home_camera_CameraActivity_WriteFrameCamera(JNIEnv *env, jobject obj, jbyteArray frame)
{
	if (trackerStopped)
		return;

	//Reinitialize if the parameters changed or initialize if it is the first time
	if (!a_cap_camera || orientationChanged)
	{
		delete a_cap_camera;
		a_cap_camera = new AndroidCameraCapture(camWidth, camHeight, camOrientation, camFlip);
		orientationChanged = false;
		trackerPaused = false;
	}
	//
	jbyte *f = env->GetByteArrayElements(frame, 0);
	//Write frames from Java to native
	a_cap_camera->WriteFrameYUV((unsigned char *)f);
	env->ReleaseByteArrayElements(frame, f, 0);
}


/**
 * Method for displaying tracking results.
 *
 * This method is periodically called by the application rendering thread to get and display tracking results.
 * The results are retrieved using VisageSDK::TrackingData structure and displayed OpenGL ES for visual data (frames from camera and 3D face model).
 * It shows how to properly interpret tracking data and setup the OpenGL scene to display 3D face model retrieved from the tracker correctly aligned to the video frame.
 *
 * @param width width of GLSurfaceView used for rendering.
 * @param height height of GLSurfaceView used for rendering.
 */
jboolean Java_com_dsd_kosjenka_presentation_home_camera_TrackerRenderer_displayTrackingStatus(JNIEnv* env, jobject obj, jint width, jint height)
{
	glWidth = width;
	glHeight = height;

	if (!m_Tracker || trackerStopped || !isTracking || !drawImageBuffer || trackerPaused)
		return false;
	//***
	//*** LOCK track thread to copy data for rendering ***
	//***
	pthread_mutex_lock(&displayRes_mutex);
	//copy image for rendering
	vsCopy(drawImageBuffer, renderImage);
	//copy faceData and statuses
	if (!liveness)
	{
	    for (int i=0; i<MAX_FACES; i++)
        	{
        		if (trackingStatusBuffer[i] == TRACK_STAT_OFF)
        			continue;
        		trackingDataRender[i] = trackingDataBuffer[i];
        		trackingStatusRender[i] = trackingStatusBuffer[i];
        	}
	}
	else
	    trackingStatusRender[0] = trackingStatusBuffer[0];
	//***
	//*** UNLOCK track thread ***
	//***
	pthread_mutex_unlock(&displayRes_mutex);

	//calculate aspect corrected width and height
	videoAspect = renderImage->width / (float) renderImage->height;

	float tmp;
	if(renderImage->width < renderImage->height)
	{
		tmp = glHeight;
		glHeight = glWidth / videoAspect;
		if (glHeight > tmp)
		{
			glWidth  = glWidth*tmp/glHeight;
			glHeight = tmp;
		}
	}
	else
	{
		tmp = glWidth;
		glWidth = glHeight * videoAspect;
		if (glWidth > tmp)
		{
			glHeight  = glHeight*tmp/glWidth;
			glWidth = tmp;
		}
	}

	//Render tracking results for the first face and display frame
	VisageRendering::DisplayResults(&trackingDataRender[0], trackingStatusRender[0], glWidth, glHeight, renderImage, DISPLAY_FRAME);
	if (logo)
		VisageRendering::DisplayLogo(logo, glWidth, glHeight);
	//Render tracking results for rest of the faces without rendering the frame
	if (!liveness)
	{
	    for (int i=0; i<MAX_FACES; i++)
        	{
        		if(trackingStatusRender[i] == TRACK_STAT_OK)

        			VisageRendering::DisplayResults(&trackingDataRender[i], trackingStatusRender[i], glWidth, glHeight, renderImage, DISPLAY_DEFAULT - DISPLAY_FRAME);
        	}
	}
	else
	{
	    if (currAction < NUM_ACTIONS)
        {
            if (trackingStatusRender[0] == TRACK_STAT_OK)
                GUI->promptUser(actions[currAction].messages[actionState].c_str(), actions[currAction].images[actionState].c_str(), true);
            else
            {
                // if tracker was unable to find face, display bounding box as instruction on where face needs to be positioned
                GUI->promptUser("", faceContourImagePath, true);
            }
        }
        else
        {
            //keep displaying frame after liveness is confirmed
            GUI->promptUser("Liveness confirmed!", "", true);
        }
	}

	return true;
}



/**
* Method for getting frame rate information from the tracker.
*
* @return float value of frame rate obtained from the tracker.
*/
float Java_com_dsd_kosjenka_presentation_home_camera_CameraActivity_GetFps(JNIEnv* env, jobject obj)
{
	return trackingData[0].frameRate;
}

/**
* Method for getting frame rate information from the tracker.
*
* @return float value of frame rate obtained from the tracker.
*/
int Java_com_dsd_kosjenka_presentation_home_camera_CameraActivity_GetTrackTime(JNIEnv* env, jobject obj)
{
	return trackingTime;
}


/**
* Method for getting the tracking status information from tracker.
*
* @return tracking status information as string.
*/
JNIEXPORT jstring JNICALL Java_com_dsd_kosjenka_presentation_home_camera_CameraActivity_GetStatus(JNIEnv* env, jobject obj)
{
	char* msg;

	for (int i=0; i<MAX_FACES; i++)
	{
		if (trackingStatusBuffer[i] == TRACK_STAT_OK)
			return env->NewStringUTF("OK");
	}

	for (int i=0; i<MAX_FACES; i++)
	{
		if (trackingStatusBuffer[i] == TRACK_STAT_RECOVERING)
			return env->NewStringUTF("RECOVERING");
	}

	for (int i=0; i<MAX_FACES; i++)
	{
		if (trackingStatusBuffer[i] == TRACK_STAT_INIT)
			return env->NewStringUTF("INITIALIZING");
	}

	return env->NewStringUTF("OFF");
}

float Java_com_visagetechnologies_visagetrackerdemo_TrackerRenderer_getTrackerFps( JNIEnv*  env )
{
	return m_fps;
}


float Java_com_dsd_kosjenka_presentation_home_camera_CameraActivity_GetDisplayFps(JNIEnv* env,
                                                                                  jobject thiz)
{
	return displayFramerate;
}


jboolean Java_com_dsd_kosjenka_presentation_home_camera_TrackerView_IsAutoStopped(JNIEnv* env,jobject thiz)
{
	//RESET VARIABLES IN WRAPPER
	if (m_Tracker)
   {

		for (int i=0; i<MAX_FACES; i++)
		{
			if (trackingStatus[i] != TRACK_STAT_OK)
				return true;
		}
		return false;

   }
   else
		return false;
}

}