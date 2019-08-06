package com.ykbjson.lib.screenrecorder;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import androidx.annotation.NonNull;

import static com.ykbjson.lib.screenrecorder.ScreenRecorder.VIDEO_AVC;


/**
 * Description：视频编码配置
 * <BR/>
 * Creator：yankebin
 * <BR/>
 * CreatedAt：2019-07-29
 */
public class VideoEncodeConfig {
    final int width;
    final int height;
    final int bitrate;
    final int framerate;
    final int iframeInterval;
    final String codecName;
    final String mimeType;
    final int codecProfile;
    final int codecProfileLevel;

    private VideoEncodeConfig(Builder builder) {
        this(builder.width, builder.height, builder.bitrate, builder.framerate, builder.iframeInterval,
                builder.codecName, builder.mimeType, builder.codecProfile, builder.codecProfileLevel);
    }

    public VideoEncodeConfig(int width, int height, int bitrate, int framerate, int iframeInterval,
                             String codecName, String mimeType, int codecProfile, int codecProfileLevel) {
        this.width = width;
        this.height = height;
        this.bitrate = bitrate;
        this.framerate = framerate;
        this.iframeInterval = iframeInterval;
        this.codecName = codecName;
        this.mimeType = mimeType;
        this.codecProfile = codecProfile;
        this.codecProfileLevel = codecProfileLevel;
    }


    MediaFormat toFormat() {
        MediaFormat format = MediaFormat.createVideoFormat(mimeType, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, framerate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iframeInterval);
        if (codecProfile != 0 && codecProfileLevel != 0) {
            format.setInteger(MediaFormat.KEY_PROFILE, codecProfile);
            format.setInteger("level", codecProfileLevel);
        }
        // maybe useful
//         format.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 10_000_000);
        return format;
    }

    @Override
    public String toString() {
        return "VideoEncodeConfig{" +
                "width=" + width +
                ", height=" + height +
                ", bitrate=" + bitrate +
                ", framerate=" + framerate +
                ", iframeInterval=" + iframeInterval +
                ", codecName='" + codecName + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", codecProfileLevel=" + codecProfile + "-" + codecProfileLevel +
                '}';
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public static final class Builder {
        private int width = 1080;
        private int height = 1920;
        private int bitrate = 25000 * 1000;
        private int framerate = 60;
        private int iframeInterval = 30;
        private String codecName;
        private String mimeType = VIDEO_AVC;
        private int codecProfile;
        private int codecProfileLevel;

        public Builder() {
            final MediaCodecInfo[] infos = Utils.getmAvcCodecInfos();
            if (null != infos && infos.length > 0) {
                codecName = infos[0].getName();
                MediaCodecInfo.CodecCapabilities capabilities = infos[0].getCapabilitiesForType(VIDEO_AVC);
                MediaCodecInfo.CodecProfileLevel[] profiles = capabilities.profileLevels;
                if (profiles != null && profiles.length > 0) {
                    codecProfile = profiles[0].profile;
                    codecProfileLevel = profiles[0].level;
                }
            }
        }

        public static Builder create() {
            return new Builder();
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder height(int height) {
            this.height = height;
            return this;
        }

        public Builder bitrate(int bitrate) {
            this.bitrate = bitrate;
            return this;
        }

        public Builder framerate(int framerate) {
            this.framerate = framerate;
            return this;
        }

        public Builder iframeInterval(int iframeInterval) {
            this.iframeInterval = iframeInterval;
            return this;
        }

        public Builder codecName(@NonNull String codecName) {
            this.codecName = codecName;
            return this;
        }

        public Builder mimeType(@NonNull String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder codecProfile(int codecProfile) {
            this.codecProfile = codecProfile;
            return this;
        }

        public Builder codecProfileLevel(int codecProfileLevel) {
            this.codecProfileLevel = codecProfileLevel;
            return this;
        }

        public Builder codecProfile(@NonNull MediaCodecInfo.CodecProfileLevel codecProfile) {
            this.codecProfile = codecProfile.profile;
            this.codecProfileLevel = codecProfile.level;
            return this;
        }

        public VideoEncodeConfig build() {
            return new VideoEncodeConfig(this);
        }
    }
}
