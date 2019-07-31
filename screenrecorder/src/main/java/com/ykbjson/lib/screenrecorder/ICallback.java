package com.ykbjson.lib.screenrecorder;

/**
 * Description：录屏状态回调接口
 * <BR/>
 * Creator：yankebin
 * <BR/>
 * CreatedAt：2019-07-31
 */
public interface ICallback {

    void onStopRecord(Throwable error);

    void onStartRecord();

    void onRecording(long presentationTimeUs);
}
