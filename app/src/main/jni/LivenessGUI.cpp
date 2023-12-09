#include "LivenessGUI.h"
#include "VisageRendering.h"
#include <android/log.h>

#define  LOG_TAG    "LivenessGUI"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

namespace VisageSDK
{
    long getTimeNsec() {
    struct timespec now;
    clock_gettime(CLOCK_REALTIME, &now);
    return (long) ((now.tv_sec*1000000000LL + now.tv_nsec)/1000000LL);
    }

    bool LivenessGUI::getAnimationParams(float &effectValue)
    {
        currentTime = getTimeNsec();
        long timePassed = currentTime - startTime;
        bool imageChanged = false;
        if (timePassed > 3*ANIMATION_CYCLE_MS || startTime == 0)
        {
            startTime = getTimeNsec();
            timePassed = 0;
            displayNewContent = true;
        }
        else
            displayNewContent = false;

        if (timePassed < ANIMATION_CYCLE_MS)
           effectValue =  float(std::min((double)timePassed, (double)ANIMATION_CYCLE_MS)) / float(ANIMATION_CYCLE_MS);
        else if (timePassed > 2*ANIMATION_CYCLE_MS)
            effectValue = 1 - float(std::min((double)(timePassed-2*ANIMATION_CYCLE_MS), (double)ANIMATION_CYCLE_MS)) / float(ANIMATION_CYCLE_MS);
        else
          effectValue = 1;

        return displayNewContent;
    }

    bool LivenessGUI::loadImage(const char* fileName)
    {
        if (strcmp(fileName, "") != 0)
        {
            //TBD: code for loading image
          /*  if (images.find(fileName) == images.end())
            {
                //code for loading image
            } */
            return true;
        }
        else
            return false;
    }

    void LivenessGUI::setImage(const char* fileName, VsImage* img)
    {
        std::string path(fileName);
        images.insert( std::pair<std::string, VsImage*>(path, img) );

    }

    void LivenessGUI::promptUser(const char* displayText, const char* fileName, bool animate)
    {
        float effectValue = 1.0f;
        bool imageChanged;
        if (animate)
            displayNewContent = getAnimationParams(effectValue);
        else
            displayNewContent = true;

        if (displayNewContent)
        {
            strcpy(currentText, displayText);
            if (strcmp(currentImage, fileName) != 0)
                imageChanged = true;
            strcpy(currentImage, fileName);
        }

        if (strcmp(currentText, "") != 0)
            VisageRendering::DisplayText(currentText, effectValue, 7.0f);

        if (!loadImage(currentImage))
            return;
   //     std::string currIm(currentImage);
        if (images.count(currentImage) > 0)
            VisageRendering::DisplayImage(images.at(currentImage), effectValue, imageChanged);
    }
}
