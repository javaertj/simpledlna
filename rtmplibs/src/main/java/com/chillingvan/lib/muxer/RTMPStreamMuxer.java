package com.chillingvan.lib.muxer;

import android.media.MediaCodec;
import android.text.TextUtils;

import com.chillingvan.canvasgl.util.Loggers;
import com.chillingvan.lib.publisher.StreamPublisher;

import net.butterflytv.rtmp_client.RTMPMuxer;

import java.util.Locale;

/**
 * Created by Chilling on 2017/5/29.
 */

public class RTMPStreamMuxer extends BaseMuxer {
    private RTMPMuxer rtmpMuxer;
    private FrameSender frameSender;

    public RTMPStreamMuxer() {
        super();
    }

    /**
     * @return 1 if it is connected
     * 0 if it is not connected
     */
    @Override
    public synchronized int open(final StreamPublisher.StreamPublisherParam params) {
        super.open(params);

        if (TextUtils.isEmpty(params.outputUrl)) {
            throw new IllegalArgumentException("Param outputUrl is empty");
        }

        rtmpMuxer = new RTMPMuxer();
        // -2 Url format error; -3 Connect error.
        int open = rtmpMuxer.open(params.outputUrl, params.width, params.height);
        Loggers.d("RTMPStreamMuxer", String.format(Locale.CHINA, "open: open: %d", open));
        int connected = rtmpMuxer.isConnected();
        Loggers.d("RTMPStreamMuxer", String.format(Locale.CHINA, "open: isConnected: %d", connected));

        Loggers.d("RTMPStreamMuxer", String.format("open: %s", params.outputUrl));
        if (!TextUtils.isEmpty(params.outputFilePath)) {
            rtmpMuxer.file_open(params.outputFilePath);
            rtmpMuxer.write_flv_header(true, true);
        }

        frameSender = new FrameSender(new FrameSender.FrameSenderCallback() {
            @Override
            public void onStart() {
            }

            @Override
            public void onSendVideo(FramePool.Frame sendFrame) {
                int result = rtmpMuxer.writeVideo(sendFrame.data, 0, sendFrame.length, sendFrame.bufferInfo.getTotalTime());
                Loggers.d("RTMPStreamMuxer", "writeVideo result : " + result);
            }

            @Override
            public void onSendAudio(FramePool.Frame sendFrame) {
                int result = rtmpMuxer.writeAudio(sendFrame.data, 0, sendFrame.length, sendFrame.bufferInfo.getTotalTime());
                Loggers.d("RTMPStreamMuxer", "writeAudio result : " + result);
            }

            @Override
            public void close() {
                if (rtmpMuxer != null) {
                    if (!TextUtils.isEmpty(params.outputFilePath)) {
                        rtmpMuxer.file_close();
                    }
                    rtmpMuxer.close();
                    rtmpMuxer = null;
                }

            }
        });
        frameSender.sendStartMessage();
        return connected;
    }

    @Override
    public void writeVideo(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo) {
        super.writeVideo(buffer, offset, length, bufferInfo);
        Loggers.d("RTMPStreamMuxer", "writeVideo: " + " time:" + videoTimeIndexCounter.getTimeIndex() + " offset:" + offset + " length:" + length);
        if (null == frameSender) {
            return;
        }
        frameSender.sendAddFrameMessage(buffer, offset, length, new BufferInfoEx(bufferInfo, videoTimeIndexCounter.getTimeIndex()), FramePool.Frame.TYPE_VIDEO);
    }


    @Override
    public void writeAudio(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo) {
        super.writeAudio(buffer, offset, length, bufferInfo);
        Loggers.d("RTMPStreamMuxer", "writeAudio: " + " time:" + videoTimeIndexCounter.getTimeIndex() + " offset:" + offset + " length:" + length);
        if (null == frameSender) {
            return;
        }
        frameSender.sendAddFrameMessage(buffer, offset, length, new BufferInfoEx(bufferInfo, audioTimeIndexCounter.getTimeIndex()), FramePool.Frame.TYPE_AUDIO);
    }

    @Override
    public synchronized int close() {
        if (frameSender != null) {
            frameSender.sendCloseMessage();
        }

        return 0;
    }

    @Override
    public String getMediaPath() {
        return params.outputUrl;
    }

}
