
package com.ykbjson.lib.screenrecorder;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Parcel;
import android.os.Parcelable;


/**
 * Description：视频编码配置
 * <BR/>
 * Creator：yankebin
 * <BR/>
 * CreatedAt：2019-07-29
 */
public class VideoEncodeConfig implements Parcelable {
    final int width;
    final int height;
    final int bitrate;
    final int framerate;
    final int iframeInterval;
    final String codecName;
    final String mimeType;
    final int codecProfile;
    final int codecProfileLevel;


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

    protected VideoEncodeConfig(Parcel in) {
        width = in.readInt();
        height = in.readInt();
        bitrate = in.readInt();
        framerate = in.readInt();
        iframeInterval = in.readInt();
        codecName = in.readString();
        mimeType = in.readString();
        codecProfile = in.readInt();
        codecProfileLevel = in.readInt();
    }

    public static final Creator<VideoEncodeConfig> CREATOR = new Creator<VideoEncodeConfig>() {
        @Override
        public VideoEncodeConfig createFromParcel(Parcel in) {
            return new VideoEncodeConfig(in);
        }

        @Override
        public VideoEncodeConfig[] newArray(int size) {
            return new VideoEncodeConfig[size];
        }
    };

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(width);
        dest.writeInt(height);
        dest.writeInt(bitrate);
        dest.writeInt(framerate);
        dest.writeInt(iframeInterval);
        dest.writeString(codecName);
        dest.writeString(mimeType);
        dest.writeInt(codecProfile);
        dest.writeInt(codecProfileLevel);
    }
}
