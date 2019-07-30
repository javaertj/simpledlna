

package com.ykbjson.lib.screenrecorder;

import android.media.MediaFormat;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Description：音频编码配置
 * <BR/>
 * Creator：yankebin
 * <BR/>
 * CreatedAt：2019-07-29
 */
class AudioEncodeConfig implements Parcelable {
    final String codecName;
    final String mimeType;
    final int bitRate;
    final int sampleRate;
    final int channelCount;
    final int profile;

    AudioEncodeConfig(String codecName, String mimeType,
                      int bitRate, int sampleRate, int channelCount, int profile) {
        this.codecName = codecName;
        this.mimeType = Objects.requireNonNull(mimeType);
        this.bitRate = bitRate;
        this.sampleRate = sampleRate;
        this.channelCount = channelCount;
        this.profile = profile;
    }

    protected AudioEncodeConfig(Parcel in) {
        codecName = in.readString();
        mimeType = in.readString();
        bitRate = in.readInt();
        sampleRate = in.readInt();
        channelCount = in.readInt();
        profile = in.readInt();
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(codecName);
        dest.writeString(mimeType);
        dest.writeInt(bitRate);
        dest.writeInt(sampleRate);
        dest.writeInt(channelCount);
        dest.writeInt(profile);
    }

    public static final Creator<AudioEncodeConfig> CREATOR = new Creator<AudioEncodeConfig>() {
        @Override
        public AudioEncodeConfig createFromParcel(Parcel in) {
            return new AudioEncodeConfig(in);
        }

        @Override
        public AudioEncodeConfig[] newArray(int size) {
            return new AudioEncodeConfig[size];
        }
    };
}
