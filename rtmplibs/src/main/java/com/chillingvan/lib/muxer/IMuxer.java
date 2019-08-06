package com.chillingvan.lib.muxer;

import android.media.MediaCodec;

import com.chillingvan.lib.publisher.StreamPublisher;

/**
 * Created by Chilling on 2017/5/21.
 */

public interface IMuxer {

    /**
     *
     * @return 1 if it is connected
     * 0 if it is not connected
     */
    int open(StreamPublisher.StreamPublisherParam params);

    void writeVideo(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo);

    void writeAudio(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo);

    int close();

    String getMediaPath();
}
