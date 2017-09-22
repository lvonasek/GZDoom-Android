#include <stdlib.h>
#include <jni.h>
#include <android/log.h>
#include <unistd.h>
#include <GLES/gl.h>
#include <string>


#include "SDL_android_extra.h"

extern "C"
{

#include "in_android.h"
#include "SDL_keycode.h"

#define JAVA_FUNC(x) Java_com_lucidvr_gzdoom_NativeLib_##x

bool shooting = false;

GLfloat   model[16];

void frameControls()
{
}


#define EXPORT_ME __attribute__ ((visibility("default")))

int argc=1;
const char * argv[32];

std::string game_path;

const char * getGamePath()
{
	return game_path.c_str();
}

int android_audio_rate;

void EXPORT_ME
JAVA_FUNC(init) ( JNIEnv* env,	jobject thiz,jint audio_rate,jobjectArray argsArray,jint lowRes,jstring game_path_ )
{
	android_audio_rate = audio_rate;

	argv[0] = "quake";
	int argCount = (env)->GetArrayLength( argsArray);
	LOGI("argCount = %d",argCount);
	for (int i=0; i<argCount; i++) {
		jstring string = (jstring) (env)->GetObjectArrayElement( argsArray, i);
		argv[argc] = (char *)(env)->GetStringUTFChars( string, 0);
		LOGI("arg = %s",argv[argc]);
		argc++;
	}


	game_path = (char *)(env)->GetStringUTFChars( game_path_, 0);

	LOGI("game_path = %s",getGamePath());

	setenv("HOME", getGamePath(),1);

	putenv("TIMIDITY_CFG=../timidity.cfg");

	chdir(getGamePath());

	SDL_SetSwapBufferCallBack(frameControls);

	//Now done in java to keep context etc
	SDL_SwapBufferPerformsSwap(true);

	PortableInit(argc,argv); //Never returns!!
}

void EXPORT_ME
JAVA_FUNC(loop) ( JNIEnv* env,	jobject thiz )
{
    PortableLoop();
}

__attribute__((visibility("default"))) jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
	LOGI("JNI_OnLoad");
	return JNI_VERSION_1_4;
}


void EXPORT_ME
JAVA_FUNC(keypress) (JNIEnv *env, jobject obj,jint down, jint keycode, jint unicode)
{
	PortableKeyEvent(down,keycode,unicode);

}

void EXPORT_ME
JAVA_FUNC(doAction) (JNIEnv *env, jobject obj,	jint state, jint action)
{
	PortableAction(state,action);
}

void EXPORT_ME
JAVA_FUNC(analogFwd) (JNIEnv *env, jobject obj,	jfloat v)
{
	PortableMoveFwd(v);
}

void EXPORT_ME
JAVA_FUNC(analogSide) (JNIEnv *env, jobject obj,jfloat v)
{
	PortableMoveSide(v);
}

void EXPORT_ME
JAVA_FUNC(analogPitch) (JNIEnv *env, jobject obj,
		jint mode,jfloat v)
		{
	PortableLookPitch(mode, v);
		}

void EXPORT_ME
JAVA_FUNC(analogYaw) (JNIEnv *env, jobject obj,	jint mode,jfloat v)
{
	PortableLookYaw(mode, v);
}

}
