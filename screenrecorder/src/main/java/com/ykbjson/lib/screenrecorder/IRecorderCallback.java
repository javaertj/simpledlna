package com.ykbjson.lib.screenrecorder;

import android.media.MediaCodec;

/**
 * Description：录屏状态回调接口
 * <BR/>
 * Creator：yankebin
 * <BR/>
 * CreatedAt：2019-07-31
 */
public interface IRecorderCallback {

    void onPrepareRecord();

    void onStartRecord();

    void onRecording(long presentationTimeUs);

    void onStopRecord(Throwable error);

    void onDestroyRecord();

    void onMuxVideo(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo);

    void onMuxAudio(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo);
}
