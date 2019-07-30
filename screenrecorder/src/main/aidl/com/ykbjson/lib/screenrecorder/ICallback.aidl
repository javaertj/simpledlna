// ICallback.aidl
package com.ykbjson.lib.screenrecorder;

// Declare any non-default types here with import statements

interface ICallback {

    void onStopRecord(in String error);

    void onStartRecord();

    void onRecording(in long presentationTimeUs);
}
