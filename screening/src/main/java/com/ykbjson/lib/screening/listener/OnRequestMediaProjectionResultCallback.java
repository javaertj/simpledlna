package com.ykbjson.lib.screening.listener;

import android.media.projection.MediaProjection;

import androidx.annotation.UiThread;

/**
 * Description：获取录屏的MediaProjection的回调
 * <BR/>
 * Creator：yankebin
 * <BR/>
 * CreatedAt：2019-08-06
 */
public interface OnRequestMediaProjectionResultCallback {
    @UiThread
    void onMediaProjectionResult(MediaProjection mediaProjection);
}
