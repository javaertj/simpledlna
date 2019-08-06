package com.chillingvan.lib.muxer;

import android.media.MediaCodec;

import com.chillingvan.lib.publisher.StreamPublisher;

/**
 * Created by Chilling on 2017/12/23.
 */

public abstract class BaseMuxer implements IMuxer {
    protected StreamPublisher.StreamPublisherParam params;
    protected TimeIndexCounter videoTimeIndexCounter = new TimeIndexCounter();
    protected TimeIndexCounter audioTimeIndexCounter = new TimeIndexCounter();

    @Override
    public int open(StreamPublisher.StreamPublisherParam params) {
        this.params = params;
        videoTimeIndexCounter.reset();
        audioTimeIndexCounter.reset();
        return 0;
    }

    @Override
    public void writeVideo(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo) {
        videoTimeIndexCounter.calcTotalTime(bufferInfo.presentationTimeUs);
    }

    @Override
    public void writeAudio(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo) {
        audioTimeIndexCounter.calcTotalTime(bufferInfo.presentationTimeUs);
    }
}
