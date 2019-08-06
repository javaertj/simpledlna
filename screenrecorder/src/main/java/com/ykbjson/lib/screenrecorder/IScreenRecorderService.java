package com.ykbjson.lib.screenrecorder;

import android.media.projection.MediaProjection;

/**
 * Description：录屏服务
 * <BR/>
 * The default implementation requires permissions in <code>AndroidManifest.xml</code>:
 * </p>
 * <pre>{@code
 *  <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
 *  <uses-permission android:name="android.permission.RECORD_AUDIO"/>
 *  }</pre>
 * <p>
 * Creator：yankebin
 * <BR/>
 * CreatedAt：2019-07-31
 */
public interface IScreenRecorderService {

    void registerRecorderCallback(IRecorderCallback callback);

    void prepareAndStartRecorder(MediaProjection mediaProjection, VideoEncodeConfig videoConfig,
                                 AudioEncodeConfig audioConfig);

    void startRecorder(VideoEncodeConfig videoConfig, AudioEncodeConfig audioConfig);

    void startRecorder();

    void stopRecorder();

    void destroyRecorder();

    String getSavingFilePath();

    VideoEncodeConfig getVideoEncodeConfig();

    AudioEncodeConfig getAudioEncodeConfig();

    boolean hasPrepared();
}
