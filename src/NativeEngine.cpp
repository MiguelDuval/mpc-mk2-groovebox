    if (pad < 0 || pad >= 16 || layer < 0 || layer >= 8) {
        return toJString(env, "Sample load failed: invalid pad or layer");
    }

    const jsize length = env->GetArrayLength(data);
    if (length <= 0) {
        return toJString(env, "Sample load failed: empty byte array");
    }

    jbyte* bytes = env->GetByteArrayElements(data, nullptr);
    if (bytes == nullptr) {
        return toJString(env, "Sample load failed: JNI access error");
    }

    const auto* raw = reinterpret_cast<const std::uint8_t*>(bytes);
    const auto result =
            mpc::audio::AudioEngine::instance().loadSampleForPadLayer(
                    std::span<const std::uint8_t>(
                            raw,
                            static_cast<std::size_t>(length)),
                    static_cast<std::uint8_t>(pad),
                    static_cast<std::uint8_t>(layer));

    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
    return toJString(env, result);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioSetPadSampleRegion(
        JNIEnv* env, jobject /* thiz */, jint pad, jint layer,
        jlong startFrame, jlong endFrame)
{
    if (env == nullptr) {
        return nullptr;
    }

    if (pad < 0 || pad >= 16 || layer < 0 || layer >= 8
            || startFrame < 0 || endFrame < 0) {
        return toJString(env, "Sample region change failed: invalid pad, layer or frame");
    }

    return toJString(
            env,
            mpc::audio::AudioEngine::instance().setPadSampleRegion(
                    static_cast<std::uint8_t>(pad),
                    static_cast<std::uint8_t>(layer),
                    static_cast<std::size_t>(startFrame),
                    static_cast<std::size_t>(endFrame)));
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioGetPadSampleRegionStart(
        JNIEnv* /* env */, jobject /* thiz */, jint pad, jint layer)
{
    if (pad < 0 || pad >= 16 || layer < 0 || layer >= 8) {
        return 0;
    }

    return static_cast<jlong>(
            mpc::audio::AudioEngine::instance().padSampleRegion(
                    static_cast<std::uint8_t>(pad),
                    static_cast<std::uint8_t>(layer)).startFrame);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioGetPadSampleRegionEnd(
        JNIEnv* /* env */, jobject /* thiz */, jint pad, jint layer)
{
    if (pad < 0 || pad >= 16 || layer < 0 || layer >= 8) {
        return 0;
    }

    return static_cast<jlong>(
            mpc::audio::AudioEngine::instance().padSampleRegion(
                    static_cast<std::uint8_t>(pad),
                    static_cast<std::uint8_t>(layer)).endFrame);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioGetPadSampleFrameCount(
        JNIEnv* /* env */, jobject /* thiz */, jint pad, jint layer)
{
    if (pad < 0 || pad >= 16 || layer < 0 || layer >= 8) {
        return 0;
    }

    return static_cast<jlong>(
            mpc::audio::AudioEngine::instance().padSampleFrameCount(
                    static_cast<std::uint8_t>(pad),
                    static_cast<std::uint8_t>(layer)));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioGetPadSampleName(
        JNIEnv* env, jobject /* thiz */, jint pad, jint layer)
{
    if (env == nullptr) {
        return nullptr;
    }

    if (pad < 0 || pad >= 16 || layer < 0 || layer >= 8) {
        return toJString(env, "");
    }

    const auto& state = mpc::MpcCore::instance().projectState();
    const auto& sampleLayer =
            state.activeDrumProgram()
                    .pad(static_cast<std::size_t>(pad))
                    .layer(static_cast<std::size_t>(layer));
    const auto* sample = state.findSample(sampleLayer.sample);
    if (sample == nullptr || sample->name.empty()) {
        return toJString(env, "");
    }

    return toJString(env, sample->name);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioChopPadSampleToPads(
        JNIEnv* env, jobject /* thiz */, jint sourcePad, jint sourceLayer, jint chopCount)
{
    if (env == nullptr) {
        return nullptr;
    }

    if (sourcePad < 0 || sourcePad >= 16
            || sourceLayer < 0 || sourceLayer >= 8
            || chopCount < 0 || chopCount > 255) {
        return toJString(env, "Chop failed: invalid source or chop count");
    }
