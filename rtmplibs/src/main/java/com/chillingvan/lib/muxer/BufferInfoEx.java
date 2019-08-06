package com.chillingvan.lib.muxer;

import android.media.MediaCodec;

/**
 * Created by Chilling on 2017/12/23.
 */

public class BufferInfoEx {
    private MediaCodec.BufferInfo bufferInfo;
    private int totalTime;

    public BufferInfoEx(MediaCodec.BufferInfo bufferInfo, int totalTime) {
        this.bufferInfo = bufferInfo;
        this.totalTime = totalTime;
    }

    public MediaCodec.BufferInfo getBufferInfo() {
        return bufferInfo;
    }

    public int getTotalTime() {
        return totalTime;
    }
}
