#ifndef __LivenessGUI_h__
#define __LivenessGUI_h__

#include <stdio.h>
#include "vs_main.h"
#include <map>

namespace VisageSDK
{
    class LivenessGUI
    {
        private:
            long currentTime;
            long startTime = 0;
            char currentText[200] = {0};
            char currentImage[200] = {0};
            //  there are three stages of animation - fade in, no transparency, and fade out; each of these lasts ANIMATION_CYCLE_MS
            const int ANIMATION_CYCLE_MS = 700;
            bool displayNewContent;
            std::map<std::string, VsImage*> images;

            bool getAnimationParams(float &effectValue);

            bool loadImage(const char* fileName);



        public:
            void promptUser(const char* displayText, const char* fileName, bool animate);
            void setImage(const char* fileName, VsImage* img);

    };
}

#endif //__LivenessGUI_h__
