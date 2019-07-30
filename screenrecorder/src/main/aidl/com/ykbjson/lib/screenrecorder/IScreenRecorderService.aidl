
package com.ykbjson.lib.screenrecorder;
import com.ykbjson.lib.screenrecorder.VideoEncodeConfig;
import com.ykbjson.lib.screenrecorder.AudioEncodeConfig;
import com.ykbjson.lib.screenrecorder.ICallback;

interface IScreenRecorderService {

    void registerScreenRecorderCallback(in ICallback callback);

    void onPrepare(in int resultCode,in Intent intent,in VideoEncodeConfig videoConfig,
    in AudioEncodeConfig audioCofig,in String savingFilePath);

    void startRecorder();

    void stopRecorder();

    void destroy();

    String getSavingFilePath();

    boolean hasPrepared();
}
