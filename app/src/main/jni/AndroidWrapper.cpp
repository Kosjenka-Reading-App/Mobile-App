#include <jni.h>
#include <EGL/egl.h>
#include <GLES/gl.h>
#include <vector>
#include <stdio.h>
#include <unistd.h>
#include "VisageTracker.h"
#include <VisageFaceAnalyser.h>
#include "VisageRendering.h"
#include "VisageGazeTracker.h"
//#include "AndroidImageCapture.h"
//#include "AndroidStreamCapture.h"
#include "AndroidCapture.h"
#include <numeric>
#include <iterator>
#include "LicenseString.h"
#include <cmath>

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

//static AndroidImageCapture *a_cap_image = 0;
//static AndroidStreamCapture *a_cap_camera = 0;
static AndroidCapture *androidCapture = 0;
static VsImage *drawImageBuffer = 0;
static VsImage *renderImage = 0;

void Sleep(int ms) { usleep(ms * 1000); }

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
static VisageGazeTracker *m_Tracker = 0;
static FaceData trackingData[MAX_FACES];
static VisageFaceAnalyser *m_Analyser = 0;
int *trackingStatus = 0;
int trackingTime;
int displayOptions = 0;
int analyserOptions = 0;


// ********************************
// Buffers for track->render communication
// ********************************

static FaceData trackingDataBuffer[MAX_FACES];
int trackingStatusBuffer[MAX_FACES];


// ********************************
// Variables used in rendering thread
// ********************************

static FaceData trackingDataRender[MAX_FACES];
int trackingStatusRender[MAX_FACES];
// Logo image
VsImage *logo = 0;


// ********************************
// Control flow variables
// ********************************

bool trackingOk = false;
bool isTracking = false;
bool orientationChanged = false;
bool trackerPaused = false;
bool trackerStopped = false;
//
int camOrientation;
int camHeight;
int camWidth;
int camFlip;
//
pthread_mutex_t displayRes_mutex;
pthread_mutex_t guardFrame_mutex;

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

int analyserInitialized = 0;


// ********************************
// JNI variables
// ********************************

JNIEnv *_env;
jobject _obj;
const char *_path;


//*******************************************
//*   Variables used for visage Analyser    *
//*******************************************

const int NUM_EMOTIONS = 7;
int currentFace = 0;

std::array<float, MAX_FACES> age;
std::array<int, MAX_FACES> gender;
std::array<std::array<float, NUM_EMOTIONS>, MAX_FACES> emotions;

bool ageActivated = false;
bool genderActivated = false;
bool emotionsActivated = false;


//**************************************************************************
//*   Variables and functions used in face selection for Visage Analyser.  *
//**************************************************************************


bool CalculateBoundingBox(int width, int height, FaceData *trackingData, VsRect *boundingBox,
                          bool flipY = false);

int SelectFaceForAnalyser();
int UserSelectedFace();
static float FPDistance(const VsPoint *fp1, const VsPoint *fp2);
static bool
GetFeaturePoint(VsPoint &point, FaceData *trackingData, int group, int index, int width, int height,
                bool flipY = false);

int *CountFaces();

int tapPositionX;
int tapPositionY;

float tapPositionXfloat;
float tapPositionYfloat;

bool displayText = false;
bool numFacesChanged = false;
int numTrackedFaces = 0;


//************************************************************
//*    Variables and functions used in wireframe animation.  *
//************************************************************

long durationTimeAnim = 1500;
long startTimeAnim;
bool animationEnabled[MAX_FACES] = {false};
VsRect *face;

void
AnimateWireframe(FaceData *faceData, int index, float alphaMin, float alphaMax, int glw, int glh);
void ResetWireframeAnimation(int index);


// ********************************
// Helper functions
// ********************************

/**
 * Callback method for license notification.
 *
 * Alerts the user that the license is not valid
 */
void AlertCallback(const char *warningMessage) {
    jclass dataClass = _env->FindClass("com/dsd/kosjenka/presentation/MainActivity");
    if (_env->ExceptionCheck())
        _env->ExceptionClear();
    if (dataClass != NULL) {
        jclass javaClassRef = (jclass) _env->NewGlobalRef(dataClass);
        jmethodID javaMethodRef = _env->GetMethodID(javaClassRef, "AlertDialogFunction",
                                                    "(Ljava/lang/String;)V");
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
    return (long) ((now.tv_sec * 1000000000LL + now.tv_nsec) / 1000000LL);
}

void ResetAnalyser(int index) {
    age[index] = -1.0;
    gender[index] = -1;
    std::fill(emotions[index].begin(), emotions[index].end(), 0.0f);
    if (m_Analyser) m_Analyser->resetStreamAnalysis(index);
}

bool AnalyseFace(VsImage *trackImage, int index) {

    if (trackingStatusBuffer[index] != TRACK_STAT_OK) {
        ResetAnalyser(index);
        return false;
    }

    AnalysisData analysisData;

    if (ageActivated) {
        analyserOptions |= VFA_AGE;
    } else {
        analyserOptions &= ~VFA_AGE;
    }

    if (genderActivated) {
        analyserOptions |= VFA_GENDER;
    } else {
        analyserOptions &= ~VFA_GENDER;
    }

    if (emotionsActivated) {
        analyserOptions |= VFA_EMOTION;
    } else {
        analyserOptions &= ~VFA_EMOTION;
    }

    m_Analyser->analyseStream(trackImage, trackingDataBuffer[index], analyserOptions, analysisData, index);

    if ((ageActivated && !analysisData.ageValid) || (genderActivated && !analysisData.genderValid) || (emotionsActivated && !analysisData.emotionsValid))
        return false;

    age[index] = analysisData.age;
    gender[index] = analysisData.gender;
    emotions[index] = analysisData.emotionProbabilities;
    return true;
}

static bool endsWith(const std::string &str, const std::string &suffix) {
    return str.size() >= suffix.size() &&
           0 == str.compare(str.size() - suffix.size(), suffix.size(), suffix);
}

// ********************************
// Wrapper function
// ********************************


/**
 * Method for initializing the tracker.
 *
 * This method creates a new VisageTracker objects and initializes the tracker.
 * @param configFilename - name of the configuration, along with the full path, to be used in tracking
 */
void Java_com_dsd_kosjenka_presentation_home_camera_VisageWrapper_TrackerInit(JNIEnv *env,
                                                                        jobject instance,
                                                                        jstring path,
                                                                        jstring configFilename) {

    _env = env;

    jclass VisageWrapperClass = env->GetObjectClass(instance);

    jfieldID fidCtx = env->GetFieldID(VisageWrapperClass, "ctx", "Landroid/content/Context;");
    jobject ctxObject = env->GetObjectField(instance, fidCtx);

    _obj = ctxObject;

    _path = env->GetStringUTFChars(path, 0);
    const char *_configFilename = env->GetStringUTFChars(configFilename, 0);

    //initialize licensing
    std::string pathToLicense(_path + std::string("/") + licenseKey);
    if (endsWith(licenseKey, ".vlc"))
        VisageSDK::initializeLicenseManager(_env, _obj, pathToLicense.c_str(), AlertCallback);
    else
        VisageSDK::initializeLicenseManager(_env, _obj, licenseKey.c_str(),
                                            AlertCallback); //for embedded license

    if (m_Tracker) {
        LOGI("m_tracker already initialised");
    } else {
        m_Tracker = new VisageSDK::VisageGazeTracker(
                (std::string(_path) + "/" + std::string(_configFilename)).c_str());
    }

    trackerStopped = false;
    trackerPaused = true;

    //Set up mutex for track->render thread synchronization
    pthread_mutex_destroy(&displayRes_mutex);
    pthread_mutex_init(&displayRes_mutex, NULL);

    //Set up mutex for tracking->stopping tracking synchronization
    pthread_mutex_destroy(&guardFrame_mutex);
    pthread_mutex_init(&guardFrame_mutex, NULL);

    //Delete previously allocated objects
    delete androidCapture;
    androidCapture = 0;

    LOGI("Configuration file %s", _configFilename);

    if(!face){
        face = new VsRect[MAX_FACES];
    }

    env->ReleaseStringUTFChars(configFilename, _configFilename);
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
int Java_com_dsd_kosjenka_presentation_home_camera_VisageWrapper_SetParameters(JNIEnv *env, jobject obj,
                                                                         jint width, jint height,
                                                                         jint orientation = 0,
                                                                         jint flip = 0) {

    pthread_mutex_lock(&guardFrame_mutex);
    pthread_mutex_lock(&displayRes_mutex);
    camOrientation = orientation;
    camHeight = height;
    camWidth = width;
    camFlip = flip;
    //Dispose of the previous drawImageBuffer
    if (!drawImageBuffer) {
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
    trackingOk = false;
    trackerStopped = false;

    for (int i = 0; i < MAX_FACES; i++) {
        trackingStatusRender[i] = TRACK_STAT_OFF;
        trackingStatusBuffer[i] = TRACK_STAT_OFF;
        ResetWireframeAnimation(i);
    }

    pthread_mutex_unlock(&displayRes_mutex);
    pthread_mutex_unlock(&guardFrame_mutex);

    for (int i = 0; i < MAX_FACES; i++) {
        ResetAnalyser(i);
    }

    //Reseting m_Tracker object before getting new frame source
    if(m_Tracker){
        m_Tracker->track(0,0,0,0);
    }
    return 0;
}

void Java_com_dsd_kosjenka_presentation_home_camera_VisageWrapper_InitOnlineGazeCalibration(JNIEnv *env,
                                                                            jobject obj) {
    if (!m_Tracker)
        return;

    m_Tracker->InitOnlineGazeCalibration();
    LOGI("InitOnlineGazeCalibration");
}

void Java_com_dsd_kosjenka_presentation_home_camera_VisageWrapper_AddGazeCalibrationPoint(JNIEnv *env,
                                                                                       jobject obj, jfloat x, jfloat y) {
    if (!m_Tracker)
        return;

    m_Tracker->AddGazeCalibrationPoint(x, y);
    LOGI("AddGazeCalibrationPoint");
}

void Java_com_dsd_kosjenka_presentation_home_camera_VisageWrapper_FinalizeOnlineGazeCalibration(JNIEnv *env,
                                                                               jobject obj) {
    if (!m_Tracker)
        return;

    m_Tracker->FinalizeOnlineGazeCalibration();
    LOGI("FinalizeOnlineGazeCalibration");
}

jobject
Java_com_dsd_kosjenka_presentation_home_camera_VisageWrapper_GetScreenSpaceGazeData(JNIEnv *env,
                                                                                    jobject obj) {
    if (m_Tracker && !trackerStopped && !trackerPaused){

        pthread_mutex_lock(&displayRes_mutex);

        ScreenSpaceGazeData data = trackingDataBuffer->gazeData;

        // get a reference to your class if you don't have it already
        jclass cls = env->FindClass("com/dsd/kosjenka/presentation/home/camera/VisageWrapper$ScreenSpaceGazeData");
        // get a reference to the constructor; the name is <init>
        jmethodID constructor = env->GetMethodID(cls, "<init>", "(IFFIF)V");

        jvalue args[5];

        for (int i = 0; i < MAX_FACES; i++) {
            if (trackingStatusBuffer[i] == TRACK_STAT_OFF){
                continue;
            }
//            LOGI("found face data");
            // set up the arguments
            args[0].i = data.index;
            args[1].f = data.x;
            args[2].f = data.y;
            args[3].i = data.inState;
            args[4].f = data.quality;
        }

        jobject gazeObject = env->NewObjectA(cls, constructor, args);

        pthread_mutex_unlock(&displayRes_mutex);

        return gazeObject;
    }
    return nullptr;
}

/**
 * Method for starting tracking from camera
 *
 * Initiates a while loop. Image is grabbed and track() function is called every iteration.
 * Copies data to the buffers for rendering.
 */
void Java_com_dsd_kosjenka_presentation_home_camera_VisageWrapper_TrackLoop(JNIEnv *env,
                                                                      jobject obj) {
    while (!trackerStopped) {
        if (m_Tracker && androidCapture && !trackerStopped && !trackerPaused) {
            pthread_mutex_lock(&guardFrame_mutex);
            long ts;
            VsImage *trackImage = androidCapture->GrabFrame(ts);

            if (trackerStopped || trackImage == 0) {
                pthread_mutex_unlock(&guardFrame_mutex);
                return;
            }

            long startTime = getTimeNsec();
            if (camOrientation == 90 || camOrientation == 270)
                trackingStatus = m_Tracker->track(camHeight, camWidth, trackImage->imageData,
                                                  trackingData, VISAGE_FRAMEGRABBER_FMT_RGB,
                                                  VISAGE_FRAMEGRABBER_ORIGIN_TL, 0, -1, MAX_FACES);
            else
                trackingStatus = m_Tracker->track(camWidth, camHeight, trackImage->imageData,
                                                  trackingData, VISAGE_FRAMEGRABBER_FMT_RGB,
                                                  VISAGE_FRAMEGRABBER_ORIGIN_TL, 0, -1, MAX_FACES);
            long endTime = getTimeNsec();
            trackingTime = (int) endTime - startTime;
            pthread_mutex_unlock(&guardFrame_mutex);

            //***
            //*** LOCK render thread while copying data for rendering ***
            //***
            pthread_mutex_lock(&displayRes_mutex);
            for (int i = 0; i < MAX_FACES; i++) {
                if (trackingStatus[i] == TRACK_STAT_OFF)
                    continue;
                trackingDataBuffer[i] = trackingData[i];
                trackingStatusBuffer[i] = trackingStatus[i];
                //Signalize that at least one face was tracked
                trackingOk = true;

            }

            isTracking = true;

            if (trackingOk) {
                vsCopy(trackImage, drawImageBuffer);
            }


            if (ageActivated || genderActivated || emotionsActivated) {

                int selectedFace = SelectFaceForAnalyser();

                if (currentFace != selectedFace) {
                    for (int i = 0; i < MAX_FACES; i++) {
                        ResetAnalyser(i);
                        ResetWireframeAnimation(i);
                    }
                    currentFace = selectedFace;
                }

                if (currentFace != -1)
                    AnalyseFace(drawImageBuffer, currentFace);
            }

            //***
            //*** UNLOCK render thread ***
            //***
            pthread_mutex_unlock(&displayRes_mutex);
        } else {
            Sleep(1);
        }
    }
    return;
}

bool Java_com_dsd_kosjenka_presentation_home_camera_VisageWrapper_DisplayTrackingStatus(JNIEnv *env,
                                                                                  jobject instance,
                                                                                  jint width,
                                                                                  jint height) {

    //***
    //*** LOCK track thread to copy data for rendering ***
    //***
    pthread_mutex_lock(&displayRes_mutex);
    if (!m_Tracker || trackerStopped || !isTracking || !drawImageBuffer || trackerPaused) {
        pthread_mutex_unlock(&displayRes_mutex);
        return false;
    }

    //copy image for rendering
    vsCopy(drawImageBuffer, renderImage);

    //copy faceData and statuses
    for (int i = 0; i < MAX_FACES; i++) {
        if (trackingStatusBuffer[i] == TRACK_STAT_OFF)
            continue;
        trackingDataRender[i] = trackingDataBuffer[i];
        trackingStatusRender[i] = trackingStatusBuffer[i];
    }
    int currentF = currentFace;

    glWidth = width;
    glHeight = height;

    //calculate aspect corrected width and height
    videoAspect = renderImage->width / (float) renderImage->height;

    float tmp;
    if (renderImage->width < renderImage->height) {
        tmp = glHeight;
        glHeight = (int) (glWidth / videoAspect);
        if (glHeight > tmp) {
            glWidth = (int) (glWidth * tmp / glHeight);
            glHeight = (int) tmp;
        }
    } else {
        tmp = glWidth;
        glWidth = (int) (glHeight * videoAspect);
        if (glWidth > tmp) {
            glHeight = (int) (glHeight * tmp / glWidth);
            glWidth = (int) tmp;
        }
    }

    int w = glWidth;
    int h = glHeight;
    //***
    //*** UNLOCK track thread ***
    //***
    pthread_mutex_unlock(&displayRes_mutex);



    //Render tracking results for the first face and display frame
    VisageRendering::DisplayResults(&trackingDataRender[0], trackingStatusRender[0], w,
                                    h, renderImage, DISPLAY_FRAME);
    if (logo)
        VisageRendering::DisplayLogo(logo, w, h);
    //Render tracking results for rest of the faces without rendering the frame
    for (int i = 0; i < MAX_FACES; i++) {
        if (trackingStatusRender[i] == TRACK_STAT_OK)
        {
            VisageRendering::DisplayResults(&trackingDataRender[i], trackingStatusRender[i],
                                                        w, h, renderImage, displayOptions);
        }
    }

    if (ageActivated || genderActivated || emotionsActivated) {
        if (currentF != -1 && trackingStatusRender[currentF] == TRACK_STAT_OK)
            AnimateWireframe(trackingDataRender, currentF, 0.2f, 0.6f, w, h);
    }

    return true;

}

/**
 * Stops the tracker and cleans memory
 */
void Java_com_dsd_kosjenka_presentation_home_camera_VisageWrapper_TrackerStop(JNIEnv *env, jobject obj) {
    if (m_Tracker) {
        trackerStopped = true;
        trackingOk = false;
        pthread_mutex_lock(&guardFrame_mutex);
        pthread_mutex_lock(&displayRes_mutex);
        for (int i = 0; i < MAX_FACES; i++) {
            trackingStatusRender[i] = TRACK_STAT_OFF;
            trackingStatusBuffer[i] = TRACK_STAT_OFF;
        }
        m_Tracker->stop();
        delete m_Tracker;
        m_Tracker = 0;
        vsReleaseImage(&drawImageBuffer);
        drawImageBuffer = 0;
        vsReleaseImage(&renderImage);
        renderImage = 0;
        VisageRendering::Reset();

        vsReleaseImage(&logo);
        logo = 0;

        pthread_mutex_unlock(&displayRes_mutex);
        pthread_mutex_unlock(&guardFrame_mutex);


    }

    if (m_Analyser) {
        delete m_Analyser;
        m_Analyser = 0;
    }
}

/**
* Writes raw image data into @ref VisageSDK::AndroidStreamCapture object. VisageTracker reads this image and performs tracking. User should call this
* function whenever new frame from camera is available. Data inside frame should be in Android NV21 (YUV420sp) format and @ref VisageSDK::AndroidStreamCapture
* will perform conversion to RGB.
*
* This function will reinitialize AndroidStreamCapture wrapper in case setParameter function was called, signaled by the orientationChanged flag. After creation,
* tracking will be resumed, signaled by trackerPaused flag.
* @param frame byte array with image data
*/
void Java_com_dsd_kosjenka_presentation_home_camera_VisageWrapper_WriteFrameStream(JNIEnv *env,
                                                                             jobject obj,
                                                                             jobject frameChannel0,
                                                                             jobject frameChannel1,
                                                                             jobject frameChannel2,
                                                                             jlong timestampA,
                                                                             jlong frameID,
                                                                             jint pixelStride) {

    if (trackerStopped)
        return;
    //Reinitialize if the parameters changed or initialize if it is the first time
    if (!androidCapture || orientationChanged) {
        delete androidCapture;
        androidCapture = new AndroidCapture(camWidth, camHeight, camOrientation, camFlip);
        orientationChanged = false;
        trackerPaused = false;
    }

    unsigned char *channel0 = (unsigned char *) env->GetDirectBufferAddress(frameChannel0);
    unsigned char *channel1 = (unsigned char *) env->GetDirectBufferAddress(frameChannel1);
    unsigned char *channel2 = (unsigned char *) env->GetDirectBufferAddress(frameChannel2);

    //Writes frame from Java to native
    androidCapture->WriteFrameYUV420(channel0, channel1, channel2, timestampA, pixelStride);
}


void Java_com_dsd_kosjenka_presentation_home_camera_VisageWrapper_SetDisplayOptions(JNIEnv *env,
                                                                              jobject instance,
                                                                              jint displayO) {

    pthread_mutex_lock(&guardFrame_mutex);
    displayOptions = displayO;

    int FA_AGE_CHECKED = displayOptions & DISPLAY_AGE;
    int FA_GEN_CHECKED = displayOptions & DISPLAY_GENDER;
    int FA_EMO_CHECKED = displayOptions & DISPLAY_EMOTIONS;

    ageActivated = FA_AGE_CHECKED && ((analyserInitialized & (int) VFA_AGE) == (int) VFA_AGE);

    genderActivated =
            FA_GEN_CHECKED && ((analyserInitialized & (int) VFA_GENDER) == (int) VFA_GENDER);

    emotionsActivated =
            FA_EMO_CHECKED && ((analyserInitialized & (int) VFA_EMOTION) == (int) VFA_EMOTION);

    pthread_mutex_unlock(&guardFrame_mutex);

}

void Java_com_dsd_kosjenka_presentation_home_camera_VisageWrapper_ToggleEars(JNIEnv *env, jobject instance, jstring path)
{
    const char *_path;
    _path = env->GetStringUTFChars(path, 0);

    VisageConfiguration temp = m_Tracker->getTrackerConfiguration();
    if (temp.getRefineEars() == 1)
    {
        std::string model_cfg = "vft/fm/jk_300.cfg";
        temp.setRefineEars(0);

        temp.setAuFittingModelConfiguration(model_cfg);
        temp.setPoseFittingModelConfiguration(model_cfg);
        temp.setMeshFittingModelConfiguration(model_cfg);

        VsCfgArr<float> smoothing_factors = temp.getSmoothingFactors();
        smoothing_factors[7] = -1.0;
        temp.setSmoothingFactors(smoothing_factors);
    }
    else
    {
        std::string model_cfg = "vft/fm/jk_300_wEars.cfg";
        temp.setRefineEars(1);
        temp.setAuFittingModelConfiguration(model_cfg);
        temp.setPoseFittingModelConfiguration(model_cfg);
        temp.setMeshFittingModelConfiguration(model_cfg);
        VsCfgArr<float> smoothing_factors = temp.getSmoothingFactors();
        smoothing_factors[7] = 1.5;
        temp.setSmoothingFactors(smoothing_factors);
    }
    m_Tracker->setTrackerConfiguration(temp);

    env->ReleaseStringUTFChars(path, _path);
}

void Java_com_dsd_kosjenka_presentation_home_camera_VisageWrapper_ToggleIris(JNIEnv *env, jobject instance)
{
    VisageConfiguration temp = m_Tracker->getTrackerConfiguration();
    if (temp.getProcessEyes() == 0)
    {
        temp.setProcessEyes(3);
    }
    else
    {
        temp.setProcessEyes(0);
    }
    m_Tracker->setTrackerConfiguration(temp);
}

void Java_com_dsd_kosjenka_presentation_home_camera_VisageWrapper_ToggleRefineLandmarks(JNIEnv *env, jobject instance,
                                                                      jboolean enableOrDisable)
{
    VisageConfiguration temp = m_Tracker->getTrackerConfiguration();
    (enableOrDisable) ? temp.setRefineLandmarks(1) : temp.setRefineLandmarks(0);
    m_Tracker->setTrackerConfiguration(temp);
}
/**
 * Method for "pausing" tracking
 *
 * Causes the tracking thread to run without doing any work - blocks the calls to the track function
 */
void Java_com_dsd_kosjenka_presentation_home_camera_VisageWrapper_PauseTracker(JNIEnv *env, jobject obj) {

    pthread_mutex_lock(&displayRes_mutex);
    pthread_mutex_lock(&guardFrame_mutex);

    trackerPaused = true;
    isTracking = false;

    pthread_mutex_unlock(&guardFrame_mutex);
    pthread_mutex_unlock(&displayRes_mutex);
}

void
Java_com_dsd_kosjenka_presentation_home_camera_VisageWrapper_ResumeTracker(JNIEnv *env,
                                                                     jobject instance) {

    pthread_mutex_lock(&displayRes_mutex);
    pthread_mutex_lock(&guardFrame_mutex);

    trackerPaused = false;
    isTracking = false;

    pthread_mutex_unlock(&guardFrame_mutex);
    pthread_mutex_unlock(&displayRes_mutex);

}

/**
* Writes raw image data into @ref VisageSDK::AndroidImageCapture object. VisageTracker reads this image and performs tracking.
* @param frame byte array with image data
* @param width image width
* @param height image height
*/
void Java_com_dsd_kosjenka_presentation_home_camera_VisageWrapper_WriteFrameImage(JNIEnv *env,
                                                                            jobject obj,
                                                                            jbyteArray frame,
                                                                            jint width,
                                                                            jint height) {
    pthread_mutex_lock(&displayRes_mutex);
    pthread_mutex_lock(&guardFrame_mutex);

    for (int i = 0; i < MAX_FACES; i++) {
        trackingStatusRender[i] = TRACK_STAT_OFF;
        trackingStatusBuffer[i] = TRACK_STAT_OFF;
    }

    if (!androidCapture || orientationChanged) {
        delete androidCapture;
        androidCapture = new AndroidCapture(width, height, VISAGE_FRAMEGRABBER_FMT_RGB);

        orientationChanged = false;
        trackerPaused = false;
    }

    jbyte *f = env->GetByteArrayElements(frame, 0);

    androidCapture->WriteFrame((unsigned char *) f, (int) width, (int) height);

    pthread_mutex_unlock(&guardFrame_mutex);
    pthread_mutex_unlock(&displayRes_mutex);

    env->ReleaseByteArrayElements(frame, f, 0);
}

jfloatArray
Java_com_dsd_kosjenka_presentation_home_camera_VisageWrapper_GetEmotions(JNIEnv *env, jclass type) {

    std::vector<float> emoTemp(NUM_EMOTIONS);

    if (std::all_of(emotions[currentFace].begin(), emotions[currentFace].end(), [](float em) { return em < 0.01; }) || currentFace == -1) {
        emoTemp.insert(emoTemp.end(), 7, -1.0f);
    } else {
        std::copy(emotions.at(currentFace).begin(), emotions.at(currentFace).end(), emoTemp.begin());
    }

    jfloat values[NUM_EMOTIONS];
    for (int i = 0; i < NUM_EMOTIONS; i++)
        values[i] = emoTemp[i];


    jfloatArray emotionsLocal;


    emotionsLocal = env->NewFloatArray(NUM_EMOTIONS);

    if (emotionsLocal == NULL) {
        LOGE("Out of memory error!");
        return NULL; /* out of memory error thrown */
    }

    env->SetFloatArrayRegion(emotionsLocal, 0, NUM_EMOTIONS, values);
    return emotionsLocal;

}

jfloat
Java_com_dsd_kosjenka_presentation_home_camera_VisageWrapper_GetAge(JNIEnv *env, jclass type) {
    if (age[currentFace] < 0 || currentFace == -1)
        return -1.0f;

    return (jfloat) age[currentFace];

}

jint Java_com_dsd_kosjenka_presentation_home_camera_VisageWrapper_GetGender(JNIEnv *env, jclass type) {

    if (gender[currentFace] < 0 || currentFace == -1)
        return -1;

    return (jint) gender[currentFace];
}


void Java_com_dsd_kosjenka_presentation_home_camera_VisageWrapper_DeallocateResources(JNIEnv *env,
                                                                                jobject instance) {
}

void
Java_com_dsd_kosjenka_presentation_home_camera_VisageWrapper_AllocateResources(JNIEnv *env,
                                                                         jobject instance) {
}

void
Java_com_dsd_kosjenka_presentation_home_camera_VisageWrapper_SendCoordinates(JNIEnv *env,
                                                                       jobject instance, jfloat x,
                                                                       jfloat y) {
    pthread_mutex_lock(&displayRes_mutex);

    tapPositionX = vsRound(x);
    tapPositionY = vsRound(y);

    pthread_mutex_unlock(&displayRes_mutex);
}


void Java_com_dsd_kosjenka_presentation_home_camera_VisageWrapper_ResetTextures(JNIEnv *env,
                                                                          jobject instance) {

    VisageRendering::Reset();

}

jboolean Java_com_dsd_kosjenka_presentation_home_camera_VisageWrapper_ShowInstruction(JNIEnv *env,
                                                                                jobject instance) {
    jboolean rtn_value = FALSE;

    pthread_mutex_lock(&displayRes_mutex);
    rtn_value = (jboolean) displayText;
    pthread_mutex_unlock(&displayRes_mutex);

    return rtn_value;
}

void Java_com_dsd_kosjenka_presentation_home_camera_VisageWrapper_InitAnalyser(JNIEnv *env, jobject instance)
{
    if(!m_Analyser)
    {
        m_Analyser = new VisageSDK::VisageFaceAnalyser();
        std::string analyzerPath = std::string(_path) + "/vfa";
        analyserInitialized = m_Analyser->init(analyzerPath.c_str());

        int num_threads = 1;
        int max_task_number = 2;
    }
}

void Java_com_dsd_kosjenka_presentation_home_camera_VisageWrapper_WriteLogoImage(JNIEnv *env, jclass type,
                                                                      jbyteArray logo_, jint width,
                                                                      jint height) {

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
 * Bounding box is calculated using several feature points, corners of the eyes, nose, and center of the upper lip.
 * It is positioned at the line connecting the center of the eyes and the nose tip.
 * Width and height of the bounding box are defined according to the widest side of the
 * triangle defined by vertexes: left eye center, right eye center and center of the upper lip.
 * Values of the bounding box are scaled to match screen coordinates and in respect to frame aspect.
 * In case you want BB to be represented in the normalized frame, pass 1 as the first two parameters of the function.
 *
 * @param width Frame width. ( In our case frame is stretched to match screen size, so we pass the width of the screen in pixels)
 * @param height Frame height. ( In our case is calculated according to the aspect of the frame and frame width)
 * @param trackingData Face data of the face to calculate bounding box.
 * @param boundingBox Object to store bounding box parameters
 * @param flipY Bool to indicate if feature points are flipped according to the x-axis.
 * @return True if the calculation was successful, false otherwise.
 */
bool CalculateBoundingBox(int width, int height, FaceData *trackingData, VsRect *boundingBox,
                          bool flipY) {
    //load crucial feature points
    VsPoint leye1, leye2, reye1, reye2, nose1, nose2, mouth;

    if (!(GetFeaturePoint(leye1, trackingData, 3, 11, width, height, flipY) &&
          GetFeaturePoint(leye2, trackingData, 3, 7, width, height, flipY) &&
          GetFeaturePoint(reye1, trackingData, 3, 12, width, height, flipY) &&
          GetFeaturePoint(reye2, trackingData, 3, 8, width, height, flipY) &&
          GetFeaturePoint(nose1, trackingData, 9, 5, width, height, flipY) &&
          GetFeaturePoint(nose2, trackingData, 9, 4, width, height, flipY) &&
          GetFeaturePoint(mouth, trackingData, 8, 1, width, height, flipY)))
        return false;

    //calculate mid-points
    VsPoint leye = vsPoint(vsRound((leye1.x + leye2.x) / 2.0f),
                           vsRound((leye1.y + leye2.y) / 2.0f));
    VsPoint reye = vsPoint(vsRound((reye1.x + reye2.x) / 2.0f),
                           vsRound((reye1.y + reye2.y) / 2.0f));
    VsPoint nose = vsPoint(vsRound((nose1.x + nose2.x) / 2.0f),
                           vsRound((nose1.y + nose2.y) / 2.0f));
    VsPoint centerEyes = vsPoint(vsRound((leye.x + reye.x) / 2.0f),
                                 vsRound((leye.y + reye.y) / 2.0f));

    if (centerEyes.y >= nose.y) {
        LOGI("Feature points are flipped\n");
    }

    //take the longest distance measure
    float distance = MAX(FPDistance(&leye, &mouth),
                         MAX(FPDistance(&reye, &mouth), FPDistance(&leye, &reye)));

    //get bb params
    boundingBox->width = vsRound(distance * 1.8f);
    boundingBox->height = vsRound(distance * 2.3f);
    boundingBox->x = vsRound(nose.x * 0.8f + centerEyes.x * 0.2f - boundingBox->width / 2.0f);
    boundingBox->y = vsRound(nose.y * 0.8f + centerEyes.y * 0.2f - boundingBox->height / 2.0f);

    return true;
}

/**
 * Euclidean distance of two points.
 * @param fp1
 * @param fp2
 * @return Euclidean distance represented with float
 */
float FPDistance(const VsPoint *fp1, const VsPoint *fp2) {
    return (float) sqrt(pow((float) (fp1->x - fp2->x), 2) + pow((float) (fp1->y - fp2->y), 2));
}

bool GetFeaturePoint(VsPoint &point, FaceData *trackingData, int group, int index, int width,
                     int height,
                     bool flipY) {
    const FeaturePoint &fp = trackingData->featurePoints2D->getFP(group, index);
    point.x = vsRound(fp.pos[0] * width);
    point.y = vsRound((flipY ? 1 - fp.pos[1] : fp.pos[1]) * height);
    return (bool) fp.defined;
}

/**
 * The method draws the wireframe of the face at the given index. The opacity of the wireframe changes during time creating animation. It is used to
 * highlight face chosen for estimations.
 * @param faceData Array of face data with latest values.
 * @param index Face index used for wireframe animation.
 * @param alphaMin Minimum opacity level.
 * @param alphaMax Maximum opacity level.
 */
void
AnimateWireframe(FaceData *faceData, int index, float alphaMin, float alphaMax, int glw, int glh) {

    if (!animationEnabled[index]) {
        startTimeAnim = getTimeNsec();
        animationEnabled[index] = true;
    }

    if (animationEnabled[index]) {
        long currTimeAnim = getTimeNsec();
        double weight = (float) (currTimeAnim - startTimeAnim) / (float) durationTimeAnim;
        double alpha = (alphaMax - alphaMin) / 2 * sin(2 * M_PI * weight - M_PI / 2) +
                       (alphaMax + alphaMin) / 2;

        VisageRendering::DisplayWireFrame(&faceData[index], glw, glh, alpha);

        if (weight > 1) {
            animationEnabled[index] = false;
        }
    }
}

/**
 * The method disables wireframe animation of the face at the given index.
 * @param index Index of the face.
 */
void ResetWireframeAnimation(int index) {
    animationEnabled[index] = false;
}

/**
 * Analyser display can host estimations for only one face at a time so we use this method to choose that face. If there is only one face in the frame, we
 * analyse that face. If there is more than one, we check whether the user tapped on the screen to select the face, otherwise, we choose
 * the smallest index with valid face data.
 * It should be used when trackingStatusBuffer is updated with the latest values and inside the displayRes_mutex lock.
 * @return Index of the selected face. If there are no faces in the frame, returns -1.
 */
int SelectFaceForAnalyser() {
    int *countedFaces = CountFaces();
    int selectedFace = -1;

    if (countedFaces[0] == 1) {
        selectedFace = countedFaces[1];
    } else {
        selectedFace = UserSelectedFace();
        if (selectedFace == -1)
            selectedFace = trackingStatusBuffer[currentFace] == TRACK_STAT_OK ? currentFace
                                                                              : countedFaces[1];

        if (countedFaces[0] != numTrackedFaces) {
            numTrackedFaces = countedFaces[0];
            if (!numFacesChanged) {
                numFacesChanged = true;
                displayText = true;
            }
        }
    }

    return selectedFace;
}

/**
 * Method checks whether user tapped on screen and selected face for the analyser.
 * It should be used when trackingStatusBuffer is updated with the latest values and inside the displayRes_mutex lock.
 * @return Index of the face user selected. If no tapping occurred, or the user didn't select face, returns -1.
 */
int UserSelectedFace() {
    int selectedFace = -1;

    if (tapPositionX == -1 || tapPositionY == -1)
        return selectedFace;

    float videoAspect = (float) drawImageBuffer->width / (float) drawImageBuffer->height;

    int winWidth = glWidth;
    int winHeight = vsRound(winWidth / videoAspect);


    for (int i = 0; i < MAX_FACES; i++) {
        //calculate bounding boxes for all found faces

        if (trackingStatusBuffer[i] == TRACK_STAT_OK) {
            CalculateBoundingBox(winWidth, winHeight, &trackingDataBuffer[i], &face[i]);

            if (tapPositionX > (face[i].x) && tapPositionX < (face[i].x + face[i].width) &&
                tapPositionY > (face[i].y) && tapPositionY < (face[i].y + face[i].height)) {
                //save index of clicked face
                selectedFace = i;

                displayText = false;
                tapPositionX = -1;
                tapPositionY = -1;
            }
        }
    }

    return selectedFace;
}

/**
 * This method counts tracked faces. It should be used when trackingStatusBuffer is updated with the latest values and inside the displayRes_mutex lock.
 * @return Array with two values, the first value is the number of tracked faces, second is the smallest index with tracking status ok.
 */
int *CountFaces() {

    static int count[2] = {0, -1};

    count[0] = 0;
    count[1] = -1;
    for (int i = MAX_FACES - 1; i >= 0; --i) {
        if (trackingStatusBuffer[i] == TRACK_STAT_OK) {
            count[0]++;
            count[1] = i;
        }
    }

    return count;
}

}
