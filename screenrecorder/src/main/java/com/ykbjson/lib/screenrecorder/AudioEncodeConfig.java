package com.ykbjson.lib.screenrecorder;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import androidx.annotation.NonNull;

import java.util.Objects;

import static com.ykbjson.lib.screenrecorder.ScreenRecorder.AUDIO_AAC;

/**
 * Description：音频编码配置
 * <BR/>
 * Creator：yankebin
 * <BR/>
 * CreatedAt：2019-07-29
 */
public class AudioEncodeConfig {
    final String codecName;
    final String mimeType;
    final int bitRate;
    final int sampleRate;
    final int channelCount;
    final int profile;


    private AudioEncodeConfig(Builder builder) {
        this(builder.codecName, builder.mimeType, builder.bitrate, builder.simpleRate,
                builder.channelCount, builder.profile);
    }

    public AudioEncodeConfig(String codecName, String mimeType,
                             int bitRate, int sampleRate, int channelCount, int profile) {
        this.codecName = codecName;
        this.mimeType = Objects.requireNonNull(mimeType);
        this.bitRate = bitRate;
        this.sampleRate = sampleRate;
        this.channelCount = channelCount;
        this.profile = profile;
    }


    MediaFormat toFormat() {
        MediaFormat format = MediaFormat.createAudioFormat(mimeType, sampleRate, channelCount);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, profile);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        //format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4096 * 4);
        return format;
    }

    @Override
    public String toString() {
        return "AudioEncodeConfig{" +
                "codecName='" + codecName + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", bitRate=" + bitRate +
                ", sampleRate=" + sampleRate +
                ", channelCount=" + channelCount +
                ", profile=" + profile +
                '}';
    }

    public static final class Builder {
        private String codecName;
        private String mimeType = AUDIO_AAC;
        private int bitrate = 80 * 1000;
        private int simpleRate = 44100;
        private int channelCount = 2;
        private int profile = MediaCodecInfo.CodecProfileLevel.AACObjectMain;

        public Builder() {
            final MediaCodecInfo[] infos = Utils.getmAacCodecInfos();
            if (null != infos && infos.length > 0) {
                codecName = infos[0].getName();
            }
        }

        public static Builder create() {
            return new Builder();
        }

        public Builder codecName(@NonNull String codecName) {
            this.codecName = codecName;
            return this;
        }

        public Builder mimeType(@NonNull String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder bitrate(int bitrate) {
            this.bitrate = bitrate;
            return this;
        }

        public Builder simpleRate(int simpleRate) {
            this.simpleRate = simpleRate;
            return this;
        }

        public Builder channelCount(int channelCount) {
            this.channelCount = channelCount;
            return this;
        }

        public Builder profile(int profile) {
            this.profile = profile;
            return this;
        }

        public AudioEncodeConfig build() {
            return new AudioEncodeConfig(this);
        }
    }
}
