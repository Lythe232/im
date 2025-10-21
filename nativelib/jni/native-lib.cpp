#include <jni.h>


// Java对象全局引用
JavaVM* gJavaVM = nullptr;
jobject gJavaObj = nullptr;

extern "C" JNIEXPORT void JNICALL
Java_com_example_ffmpegplayer_FFmpegDecoder_playVideo(JNIEnv *env, jobject thiz, jstring video_path, jobject surface);
extern "C"
JNIEXPORT void JNICALL
Java_com_lythe_nativelib_MainActivity_playVideo(JNIEnv *env, jobject thiz, jstring video,
                                                jobject surface) {
    Java_com_example_ffmpegplayer_FFmpegDecoder_playVideo(env, thiz, video, surface);
}
