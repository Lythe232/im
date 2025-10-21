#include <string>
extern "C" {
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libswscale/swscale.h>
#include <libavdevice/avdevice.h>
#include <libavutil/imgutils.h>
}

#include <jni.h>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

#define WIDTH 640
#define HEIGHT 480
#define LOG_TAG "FFmpegDecoder"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// 用来保存视频解码后的一帧图像数据
AVFrame *frame = NULL;
AVFrame *rgb_frame = NULL;
uint8_t *buffer = NULL;
struct SwsContext *sws_ctx = NULL;

extern "C" JNIEXPORT void JNICALL
Java_com_example_ffmpegplayer_FFmpegDecoder_playVideo(JNIEnv *env, jobject thiz, jstring video_path, jobject surface) {
    const char *input_path = env->GetStringUTFChars(video_path, 0);
    AVFormatContext *fmt_ctx = nullptr;
    int video_stream_index = -1;
    AVCodecContext *codec_ctx = nullptr;
    const AVCodec *codec = nullptr;
    AVFrame *frame = nullptr;
    AVFrame *rgb_frame = nullptr;
    uint8_t *buffer = nullptr;
    struct SwsContext *sws_ctx = nullptr;
    AVPacket packet;

    // 注册所有编解码器
    avdevice_register_all();
    avformat_network_init();

    // 打开输入文件
    if (avformat_open_input(&fmt_ctx, input_path, nullptr, nullptr) < 0) {
        LOGE("Could not open input file: %s", input_path);
        return;
    }

    // 查找流信息
    if (avformat_find_stream_info(fmt_ctx, nullptr) < 0) {
        LOGE("Could not find stream information");
        return;
    }

    // 查找视频流
    for (int i = 0; i < fmt_ctx->nb_streams; i++) {
        if (fmt_ctx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            video_stream_index = i;
            break;
        }
    }

    if (video_stream_index == -1) {
        LOGE("No video stream found");
        return;
    }

    // 获取解码器
    codec = avcodec_find_decoder(fmt_ctx->streams[video_stream_index]->codecpar->codec_id);
    if (!codec) {
        LOGE("Codec not found");
        return;
    }

    codec_ctx = avcodec_alloc_context3(codec);
    if (!codec_ctx) {
        LOGE("Could not allocate codec context");
        return;
    }

    if (avcodec_parameters_to_context(codec_ctx, fmt_ctx->streams[video_stream_index]->codecpar) < 0) {
        LOGE("Could not copy codec parameters");
        return;
    }

    // 打开解码器
    if (avcodec_open2(codec_ctx, codec, nullptr) < 0) {
        LOGE("Could not open codec");
        return;
    }

    // 获取 Surface 对应的 ANativeWindow
    LOGE("______________________");
    LOGE("env: %p, thread id: %ld, surface: %ld" , env, (long)pthread_self(), (long)surface);
    if (env == nullptr) {
        LOGE("env is NULL!");
        return;
    }
    LOGE("env is NONNULL!");
    ANativeWindow *native_window = ANativeWindow_fromSurface(env, surface);
    if (!native_window) {
        LOGE("Could not get native window from Surface");
        return;
    }

    // 分配内存
    frame = av_frame_alloc();
    rgb_frame = av_frame_alloc();
    int num_bytes = av_image_get_buffer_size(AV_PIX_FMT_RGB24, codec_ctx->width, codec_ctx->height, 1);
    buffer = (uint8_t *)av_malloc(num_bytes * sizeof(uint8_t));
    av_image_fill_arrays(rgb_frame->data, rgb_frame->linesize, buffer, AV_PIX_FMT_RGB24, codec_ctx->width, codec_ctx->height, 1);

    // 初始化像素转换器
    sws_ctx = sws_getContext(codec_ctx->width, codec_ctx->height, codec_ctx->pix_fmt,
                             codec_ctx->width, codec_ctx->height, AV_PIX_FMT_RGB24,
                             SWS_BILINEAR, nullptr, nullptr, nullptr);

    // 解码和渲染
    while (av_read_frame(fmt_ctx, &packet) >= 0) {
        if (packet.stream_index == video_stream_index) {
            int ret = avcodec_send_packet(codec_ctx, &packet);
            if (ret < 0) {
                LOGE("Error sending packet to decoder");
                break;
            }

            while (avcodec_receive_frame(codec_ctx, frame) >= 0) {
                // 转换图像为 RGB
                sws_scale(sws_ctx, frame->data, frame->linesize, 0, codec_ctx->height, rgb_frame->data, rgb_frame->linesize);

                // 锁定 ANativeWindow 获取缓冲区
                ANativeWindow_Buffer buffer;
                if (ANativeWindow_lock(native_window, &buffer, nullptr) < 0) {
                    LOGE("Could not lock the native window");
                    return;
                }

                // 获取缓冲区的数据指针
                uint8_t *dst = (uint8_t *)buffer.bits;

                // 将解码后的 RGB 帧复制到缓冲区
                for (int y = 0; y < codec_ctx->height; ++y) {
                    memcpy(dst + y * buffer.stride, rgb_frame->data[0] + y * rgb_frame->linesize[0], codec_ctx->width * 3);
                }

                // 解锁并显示
                ANativeWindow_unlockAndPost(native_window);
            }
        }

        av_packet_unref(&packet);
    }

    // 释放资源
    av_frame_free(&frame);
    av_frame_free(&rgb_frame);
    avcodec_free_context(&codec_ctx);
    avformat_close_input(&fmt_ctx);
}