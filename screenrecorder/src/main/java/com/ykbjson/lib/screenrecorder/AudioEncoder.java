package com.ykbjson.lib.screenrecorder;

import android.media.MediaFormat;


/**
 * Description：音频编码器
 * <BR/>
 * Creator：yankebin
 * <BR/>
 * CreatedAt：2019-07-29
 */
class AudioEncoder extends BaseEncoder {
    private final AudioEncodeConfig mConfig;

    AudioEncoder(AudioEncodeConfig config) {
        super(config.codecName);
        this.mConfig = config;
    }

    @Override
    protected MediaFormat createMediaFormat() {
        return mConfig.toFormat();
    }

    AudioEncodeConfig getConfig() {
        return mConfig;
    }
}
