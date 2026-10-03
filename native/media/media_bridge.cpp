#include <jni.h>
#include <windows.h>
#include <algorithm>
#include <atomic>
#include <cmath>
#include <cstring>
#include <memory>
#include <stdexcept>
#include <string>
#include <vector>
extern "C" {
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libavutil/hwcontext.h>
#include <libavutil/display.h>
#include <libavutil/pixdesc.h>
#include <libavutil/opt.h>
#include <libswscale/swscale.h>
#include <libswresample/swresample.h>
}

namespace {
void checked(int result, const char* operation) {
    if (result >= 0) return;
    char error[AV_ERROR_MAX_STRING_SIZE]; av_strerror(result, error, sizeof(error));
    throw std::runtime_error(std::string(operation) + ": " + error);
}
void fail(JNIEnv* env, const std::exception& error) { env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), error.what()); }
std::string utf8(JNIEnv* env, jstring value) {
    const jchar* chars = env->GetStringChars(value, nullptr);
    int count = env->GetStringLength(value);
    int size = WideCharToMultiByte(CP_UTF8, WC_ERR_INVALID_CHARS, reinterpret_cast<const wchar_t*>(chars), count, nullptr, 0, nullptr, nullptr);
    std::string result(size, '\0');
    if (size > 0) WideCharToMultiByte(CP_UTF8, WC_ERR_INVALID_CHARS, reinterpret_cast<const wchar_t*>(chars), count, result.data(), size, nullptr, nullptr);
    env->ReleaseStringChars(value, chars);
    if (size == 0) throw std::runtime_error("Invalid source path");
    return result;
}
struct Decoder {
    AVFormatContext* format = nullptr;
    AVCodecContext* codec = nullptr;
    AVPacket* packet = av_packet_alloc();
    AVFrame* frame = av_frame_alloc();
    AVFrame* transferred = av_frame_alloc();
    SwsContext* scale = nullptr;
    SwrContext* resample = nullptr;
    AVBufferRef* hardware = nullptr;
    AVPixelFormat hardwareFormat = AV_PIX_FMT_NONE;
    AVStream* stream = nullptr;
    std::atomic<bool> cancelled{false};
    bool pending = false, draining = false, ended = false, audio = false, still = false, fallback = false;
    int maxEdge = 0, width = 0, height = 0, rotation = 0;
    int64_t nextAudioUs = AV_NOPTS_VALUE;
    std::string note;
    ~Decoder() {
        sws_freeContext(scale); swr_free(&resample); av_frame_free(&frame); av_frame_free(&transferred);
        av_packet_free(&packet); avcodec_free_context(&codec); avformat_close_input(&format); av_buffer_unref(&hardware);
    }
    static int interrupt(void* opaque) { return static_cast<Decoder*>(opaque)->cancelled.load() ? 1 : 0; }
    static AVPixelFormat pixelFormat(AVCodecContext* context, const AVPixelFormat* options) {
        auto* self = static_cast<Decoder*>(context->opaque);
        for (auto p = options; *p != AV_PIX_FMT_NONE; ++p) if (*p == self->hardwareFormat) return *p;
        if (self->fallback) for (auto p = options; *p != AV_PIX_FMT_NONE; ++p) {
            const AVPixFmtDescriptor* desc = av_pix_fmt_desc_get(*p);
            if (desc && !(desc->flags & AV_PIX_FMT_FLAG_HWACCEL)) { self->note = "D3D11VA unavailable for negotiated format; software fallback"; return *p; }
        }
        return AV_PIX_FMT_NONE;
    }
    bool receive() {
        while (!cancelled.load()) {
            int rc = avcodec_receive_frame(codec, frame);
            if (rc >= 0) return true;
            if (rc == AVERROR_EOF) { ended = true; return false; }
            if (rc != AVERROR(EAGAIN)) checked(rc, "receive frame");
            if (pending) {
                checked(avcodec_send_packet(codec, packet), "send packet"); av_packet_unref(packet); pending = false;
            } else if (draining) { throw std::runtime_error("Decoder requested packets after drain"); }
            else {
                while (true) { rc = av_read_frame(format, packet); if (rc < 0 || packet->stream_index == stream->index) break;
                    av_packet_unref(packet);
                }
                if (rc == AVERROR_EOF) { checked(avcodec_send_packet(codec, nullptr), "drain decoder"); draining = true; }
                else { checked(rc, "read packet"); pending = true; }
            }
        }
        return false;
    }
    int64_t timeUs() const {
        int64_t pts = frame->best_effort_timestamp;
        if (pts == AV_NOPTS_VALUE) pts = frame->pts;
        if (pts == AV_NOPTS_VALUE) throw std::runtime_error("Decoded frame has no timestamp");
        const int64_t start = stream->start_time == AV_NOPTS_VALUE ? 0 : stream->start_time;
        return av_rescale_q(pts - start, stream->time_base, AV_TIME_BASE_Q);
    }
};
Decoder& decoder(jlong value) { if (!value) throw std::runtime_error("Closed decoder"); return *reinterpret_cast<Decoder*>(value); }
}

extern "C" JNIEXPORT jstring JNICALL Java_com_pinwheel_media_LibavNative_configuration(JNIEnv* env, jobject) {
    return env->NewStringUTF(avcodec_configuration());
}
extern "C" JNIEXPORT jlong JNICALL Java_com_pinwheel_media_LibavNative_open(JNIEnv* env, jobject, jstring source, jint track, jint backend, jint maxEdge, jboolean fallback) {
    try {
        auto self = std::make_unique<Decoder>(); self->audio = track == 1; self->maxEdge = maxEdge; self->fallback = fallback;
        self->format = avformat_alloc_context(); if (!self->format || !self->frame || !self->packet || !self->transferred) throw std::bad_alloc();
        self->format->interrupt_callback = {Decoder::interrupt, self.get()};
        const std::string path = utf8(env, source);
        checked(avformat_open_input(&self->format, path.c_str(), nullptr, nullptr), "open media");
        checked(avformat_find_stream_info(self->format, nullptr), "probe media");
        const AVCodec* implementation = nullptr;
        int index = av_find_best_stream(self->format, self->audio ? AVMEDIA_TYPE_AUDIO : AVMEDIA_TYPE_VIDEO, -1, -1, &implementation, 0);
        if (index == AVERROR_STREAM_NOT_FOUND) return 0;
        checked(index, "find stream"); self->stream = self->format->streams[index];
        self->codec = avcodec_alloc_context3(implementation); if (!self->codec) throw std::bad_alloc();
        checked(avcodec_parameters_to_context(self->codec, self->stream->codecpar), "codec parameters");
        self->codec->thread_count = 2;
        self->width = self->codec->width; self->height = self->codec->height;
        const std::string demux = self->format->iformat->name;
        self->still = !self->audio && (demux.find("image2") != std::string::npos || demux.find("_pipe") != std::string::npos);
        auto* matrix = av_packet_side_data_get(self->stream->codecpar->coded_side_data, self->stream->codecpar->nb_coded_side_data, AV_PKT_DATA_DISPLAYMATRIX);
        if (matrix && matrix->size >= 9 * sizeof(int32_t)) {
            double angle = av_display_rotation_get(reinterpret_cast<const int32_t*>(matrix->data));
            if (std::isfinite(angle)) self->rotation = (static_cast<int>(std::round(-angle)) % 360 + 360) % 360;
        }
        if (!self->audio && !self->still && backend == 0) {
            for (int i = 0; const auto* config = avcodec_get_hw_config(implementation, i); ++i) {
                if ((config->methods & AV_CODEC_HW_CONFIG_METHOD_HW_DEVICE_CTX) && config->device_type == AV_HWDEVICE_TYPE_D3D11VA) {
                    self->hardwareFormat = config->pix_fmt; break;
                }
            }
            int rc = self->hardwareFormat == AV_PIX_FMT_NONE ? AVERROR(ENOSYS) : av_hwdevice_ctx_create(&self->hardware, AV_HWDEVICE_TYPE_D3D11VA, nullptr, nullptr, 0);
            if (rc < 0) {
                if (!self->fallback) checked(rc, "D3D11VA required");
                self->hardwareFormat = AV_PIX_FMT_NONE; self->note = "D3D11VA device/codec unavailable; software fallback";
            } else { self->codec->hw_device_ctx = av_buffer_ref(self->hardware); self->codec->opaque = self.get(); self->codec->get_format = Decoder::pixelFormat; self->note = "D3D11VA with CPU transfer for ANGLE upload"; }
        }
        checked(avcodec_open2(self->codec, implementation, nullptr), "open codec");
        if (!self->audio && (self->width <= 0 || self->height <= 0 || static_cast<int64_t>(self->width) * self->height > 25000000)) throw std::runtime_error("Unsupported video dimensions");
        if (self->audio) {
            AVChannelLayout stereo = AV_CHANNEL_LAYOUT_STEREO;
            checked(swr_alloc_set_opts2(&self->resample, &stereo, AV_SAMPLE_FMT_FLT, 48000, &self->codec->ch_layout, self->codec->sample_fmt, self->codec->sample_rate, 0, nullptr), "audio resampler");
            checked(swr_init(self->resample), "initialize resampler");
        }
        return reinterpret_cast<jlong>(self.release());
    } catch (const std::exception& error) { fail(env, error); return 0; }
}
extern "C" JNIEXPORT jlongArray JNICALL Java_com_pinwheel_media_LibavNative_description(JNIEnv* env, jobject, jlong handle) {
    try {
        auto& d = decoder(handle); jlong values[] = { d.format->duration == AV_NOPTS_VALUE ? 0 : d.format->duration, d.width, d.height,
            d.audio ? 48000 : 0, d.audio ? 2 : 0, d.still ? 1 : 0, d.hardware ? 1 : 0, d.rotation };
        auto result = env->NewLongArray(8); env->SetLongArrayRegion(result, 0, 8, values); return result;
    } catch (const std::exception& error) { fail(env, error); return nullptr; }
}
extern "C" JNIEXPORT jstring JNICALL Java_com_pinwheel_media_LibavNative_note(JNIEnv* env, jobject, jlong handle) {
    try { return env->NewStringUTF(decoder(handle).note.c_str()); } catch (const std::exception& error) { fail(env, error); return nullptr; }
}
extern "C" JNIEXPORT void JNICALL Java_com_pinwheel_media_LibavNative_seek(JNIEnv* env, jobject, jlong handle, jlong target) {
    try {
        auto& d = decoder(handle);
        if (!d.still) {
            int64_t timestamp = av_rescale_q(target, AV_TIME_BASE_Q, d.stream->time_base) + (d.stream->start_time == AV_NOPTS_VALUE ? 0 : d.stream->start_time);
            checked(av_seek_frame(d.format, d.stream->index, timestamp, AVSEEK_FLAG_BACKWARD), "seek to previous keyframe");
        } else checked(av_seek_frame(d.format, d.stream->index, 0, AVSEEK_FLAG_BACKWARD), "rewind still");
        avcodec_flush_buffers(d.codec); av_packet_unref(d.packet); av_frame_unref(d.frame); av_frame_unref(d.transferred);
        d.pending = d.draining = d.ended = false; d.nextAudioUs = AV_NOPTS_VALUE;
        if (d.resample) { swr_close(d.resample); checked(swr_init(d.resample), "reset audio resampler"); }
    } catch (const std::exception& error) { fail(env, error); }
}
extern "C" JNIEXPORT jobject JNICALL Java_com_pinwheel_media_LibavNative_video(JNIEnv* env, jobject, jlong handle) {
    try {
        auto& d = decoder(handle); if (!d.receive()) return nullptr;
        int64_t pts = d.timeUs(); AVFrame* image = d.frame;
        if (image->format == d.hardwareFormat && d.hardwareFormat != AV_PIX_FMT_NONE) {
            av_frame_unref(d.transferred); checked(av_hwframe_transfer_data(d.transferred, image, 0), "transfer D3D11VA frame"); image = d.transferred;
        }
        int w = image->width, h = image->height;
        if (d.maxEdge > 0 && std::max(w, h) > d.maxEdge) { double factor = static_cast<double>(d.maxEdge) / std::max(w, h); w = std::max(2, static_cast<int>(std::round(w * factor))); h = std::max(2, static_cast<int>(std::round(h * factor))); }
        d.scale = sws_getCachedContext(d.scale, image->width, image->height, static_cast<AVPixelFormat>(image->format), w, h, AV_PIX_FMT_RGBA, SWS_BILINEAR, nullptr, nullptr, nullptr);
        if (!d.scale) throw std::runtime_error("RGBA conversion unavailable");
        const int* coefficients = sws_getCoefficients(image->colorspace == AVCOL_SPC_BT709 ? SWS_CS_ITU709 : SWS_CS_ITU601);
        checked(sws_setColorspaceDetails(d.scale, coefficients, image->color_range == AVCOL_RANGE_JPEG, coefficients, 1, 0, 1 << 16, 1 << 16), "video color matrix");
        std::vector<uint8_t> bytes(static_cast<size_t>(w) * h * 4); uint8_t* outputs[] = {bytes.data(), nullptr, nullptr, nullptr}; int strides[] = {w * 4, 0, 0, 0};
        if (sws_scale(d.scale, image->data, image->linesize, 0, image->height, outputs, strides) != h) throw std::runtime_error("Incomplete video conversion");
        auto pixels = env->NewByteArray(static_cast<jsize>(bytes.size())); if (!pixels) return nullptr;
        env->SetByteArrayRegion(pixels, 0, static_cast<jsize>(bytes.size()), reinterpret_cast<const jbyte*>(bytes.data()));
        auto type = env->FindClass("com/pinwheel/media/NativeVideo"); auto result = env->NewObject(type, env->GetMethodID(type, "<init>", "(JII[B)V"), static_cast<jlong>(pts), w, h, pixels);
        av_frame_unref(d.frame); return result;
    } catch (const std::exception& error) { fail(env, error); return nullptr; }
}
extern "C" JNIEXPORT jobject JNICALL Java_com_pinwheel_media_LibavNative_audio(JNIEnv* env, jobject, jlong handle) {
    try {
        auto& d = decoder(handle); if (!d.receive()) return nullptr;
        int64_t pts = d.timeUs(); int64_t delay = swr_get_delay(d.resample, d.codec->sample_rate);
        int capacity = static_cast<int>(av_rescale_rnd(delay + d.frame->nb_samples, 48000, d.codec->sample_rate, AV_ROUND_UP));
        std::vector<float> values(static_cast<size_t>(capacity) * 2); uint8_t* destination[] = { reinterpret_cast<uint8_t*>(values.data()) };
        int samples = swr_convert(d.resample, destination, capacity, const_cast<const uint8_t**>(d.frame->extended_data), d.frame->nb_samples);
        checked(samples, "convert audio");
        if (d.nextAudioUs == AV_NOPTS_VALUE) d.nextAudioUs = pts - av_rescale(delay, AV_TIME_BASE, d.codec->sample_rate);
        pts = d.nextAudioUs; d.nextAudioUs += av_rescale(samples, AV_TIME_BASE, 48000);
        auto pcm = env->NewFloatArray(samples * 2); if (!pcm) return nullptr; env->SetFloatArrayRegion(pcm, 0, samples * 2, values.data());
        auto type = env->FindClass("com/pinwheel/media/NativeAudio"); auto result = env->NewObject(type, env->GetMethodID(type, "<init>", "(J[F)V"), static_cast<jlong>(pts), pcm);
        av_frame_unref(d.frame); return result;
    } catch (const std::exception& error) { fail(env, error); return nullptr; }
}
extern "C" JNIEXPORT void JNICALL Java_com_pinwheel_media_LibavNative_cancel(JNIEnv*, jobject, jlong handle) { if (handle) reinterpret_cast<Decoder*>(handle)->cancelled.store(true); }
extern "C" JNIEXPORT void JNICALL Java_com_pinwheel_media_LibavNative_close(JNIEnv*, jobject, jlong handle) { delete reinterpret_cast<Decoder*>(handle); }
