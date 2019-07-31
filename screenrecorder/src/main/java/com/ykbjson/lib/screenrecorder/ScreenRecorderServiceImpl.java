package com.ykbjson.lib.screenrecorder;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodecInfo;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.ykbjson.lib.screenrecorder.ScreenRecorder.AUDIO_AAC;
import static com.ykbjson.lib.screenrecorder.ScreenRecorder.VIDEO_AVC;

/**
 * Description：录屏服务
 * <BR/>
 * Creator：yankebin
 * <BR/>
 * CreatedAt：2019-07-30
 */
public class ScreenRecorderServiceImpl extends Service {
    private static final String TAG = "ScreenRecorderService";

    private ScreenRecorderService mScreenRecorderService;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (null != mScreenRecorderService) {
            mScreenRecorderService.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mScreenRecorderService = new ScreenRecorderService();
    }

    @Nullable
    @Override
    public final IBinder onBind(Intent intent) {
        return new ScreenRecorderServiceBinder();
    }

    @Override
    public void onDestroy() {
        mScreenRecorderService.destroyRecorder();
        super.onDestroy();
    }

    protected class ScreenRecorderService implements IScreenRecorderService {

        final int DEFAULT_AUDIO_BITRATE = 80 * 1000;
        final int DEFAULT_AUDIO_SAMPLERATE = 44100;
        final int DEFAULT_AUDIO_CHANNEL_COUNT = 2;
        final int DEFAULT_AUDIO_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectMain;

        final int DEFAULT_VIDEO_BITRATE = 25000 * 1000;
        final int DEFAULT_VIDEO_FRAMERATE = 60;
        final int DEFAULT_VIDEO_IFRAME = 30;
        final int DEFAULT_VIDEO_WIDTH = 1080;
        final int DEFAULT_VIDEO_HEIGHT = 1920;


        MediaProjection mMediaProjection;
        ScreenRecorder mScreenRecorder;
        VirtualDisplay mVirtualDisplay;
        boolean mIsLandscape;
        ICallback mCallback;

        MediaCodecInfo[] mAvcCodecInfos; // avc codecs
        MediaCodecInfo[] mAacCodecInfos; // aac codecs

        ScreenRecorderService() {
            Utils.findEncodersByTypeAsync(VIDEO_AVC, infos -> mAvcCodecInfos = infos);
            Utils.findEncodersByTypeAsync(AUDIO_AAC, infos -> mAacCodecInfos = infos);
        }

        @Override
        public void registerRecorderCallback(ICallback callback) {
            mCallback = callback;
        }

        @Override
        public void prepareAndStartRecorder(MediaProjection mediaProjection, VideoEncodeConfig videoConfig,
                                            AudioEncodeConfig audioConfig) {
            // NOTE: Should pass this result data into a Service to run ScreenRecorder.
            if (mediaProjection == null) {
                throw new RuntimeException("media projection is null");
            }
            mMediaProjection = mediaProjection;
            mMediaProjection.registerCallback(mProjectionCallback, new Handler());
            startRecorder(videoConfig, audioConfig);
        }

        @Override
        public void startRecorder(VideoEncodeConfig videoConfig, AudioEncodeConfig audioConfig) {
            registerReceiver(mStopActionReceiver, new IntentFilter(Notifications.ACTION_STOP));
            prepareScreenRecorder(mMediaProjection, videoConfig, audioConfig);
            mScreenRecorder.start();
        }

        @Override
        public void startRecorder() {
            startRecorder(null, null);
        }

        @Override
        public void stopRecorder() {
            try {
                unregisterReceiver(mStopActionReceiver);
            } catch (Exception e) {
                //ignored
            }

            if (null == mScreenRecorder) {
                throw new RuntimeException("ScreenRecorder has not been initialized yet");
            }
            mScreenRecorder.quit();
            mScreenRecorder = null;
        }

        @Override
        public void destroyRecorder() {
            stopRecorder();
            if (mVirtualDisplay != null) {
                mVirtualDisplay.setSurface(null);
                mVirtualDisplay.release();
                mVirtualDisplay = null;
            }
            if (mMediaProjection != null) {
                mMediaProjection.unregisterCallback(mProjectionCallback);
                mMediaProjection.stop();
                mMediaProjection = null;
            }
        }

        @Override
        public String getSavingFilePath() {
            if (null == mScreenRecorder) {
                throw new RuntimeException("ScreenRecorder has not been initialized yet");
            }
            return mScreenRecorder.getSavedPath();
        }

        @Override
        public boolean hasPrepared() {
            return null != mMediaProjection;
        }


        private void onConfigurationChanged(Configuration newConfig) {
            mIsLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;
        }

        private void prepareScreenRecorder(MediaProjection mediaProjection, VideoEncodeConfig video, AudioEncodeConfig audio) {
            if (mediaProjection == null) {
                throw new RuntimeException("media projection is null");
            }
            if (hasPermissions()) {
                video = null == video ? createVideoConfig() : video;
                audio = null == audio ? createAudioConfig() : audio;

                MediaCodecInfo mediaCodecInfo = getVideoCodecInfo(video.codecName);
                MediaCodecInfo.CodecCapabilities capabilities = mediaCodecInfo.getCapabilitiesForType(VIDEO_AVC);
                MediaCodecInfo.VideoCapabilities videoCapabilities = capabilities.getVideoCapabilities();

                if (!videoCapabilities.isSizeSupported(video.width, video.height)) {
                    throw new RuntimeException("unSupport size," + video.codecName +
                            " height range: " + videoCapabilities.getSupportedHeights() +
                            "\n width range: " + videoCapabilities.getSupportedHeights());
                }

                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                        "Screenshots");
                if (!dir.exists() && !dir.mkdirs()) {
                    throw new RuntimeException("create file failure");
                }
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
                File file = new File(dir, "ScreenRecord_" + format.format(new Date()) + "_" + video.width
                        + "x" + video.height + ".mp4");
                Log.d(TAG, "Create recorder with :" + video + " \n " + audio + "\n " + file);
                mScreenRecorder = newRecorder(mediaProjection, video, audio, file);
            } else {
                throw new RuntimeException("Permission denied! Screen recorder is cancel");
            }
        }


        private ScreenRecorder newRecorder(MediaProjection mediaProjection, VideoEncodeConfig video,
                                           AudioEncodeConfig audio, File output) {
            final VirtualDisplay display = getOrCreateVirtualDisplay(mediaProjection, video);
            ScreenRecorder r = new ScreenRecorder(video, audio, display, output.getAbsolutePath());
            r.setCallback(mCallback);
            return r;
        }


        private VirtualDisplay getOrCreateVirtualDisplay(MediaProjection mediaProjection, VideoEncodeConfig config) {
            if (mVirtualDisplay == null) {
                mVirtualDisplay = mediaProjection.createVirtualDisplay("ScreenRecorder-display0",
                        config.width, config.height, 1 /*dpi*/,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                        null /*surface*/, null, null);
            } else {
                // resize if size not matched
                Point size = new Point();
                mVirtualDisplay.getDisplay().getSize(size);
                if (size.x != config.width || size.y != config.height) {
                    mVirtualDisplay.resize(config.width, config.height, 1);
                }
            }
            return mVirtualDisplay;
        }


        private AudioEncodeConfig createAudioConfig() {
            String codec = mAacCodecInfos[0].getName();
            if (codec == null) {
                return null;
            }
            return new AudioEncodeConfig(codec, AUDIO_AAC, DEFAULT_AUDIO_BITRATE, DEFAULT_AUDIO_SAMPLERATE,
                    DEFAULT_AUDIO_CHANNEL_COUNT, DEFAULT_AUDIO_PROFILE);
        }

        private VideoEncodeConfig createVideoConfig() {
            final String codec = mAvcCodecInfos[0].getName();
            if (codec == null) {
                // no selected codec ??
                return null;
            }
            int profile = 0;
            int profileLevel = 0;
            MediaCodecInfo.CodecCapabilities capabilities = mAvcCodecInfos[0].getCapabilitiesForType(VIDEO_AVC);
            MediaCodecInfo.CodecProfileLevel[] profiles = capabilities.profileLevels;
            if (profiles != null && profiles.length > 0) {
                profile = profiles[0].profile;
                profileLevel = profiles[0].level;
            }
            return new VideoEncodeConfig(DEFAULT_VIDEO_WIDTH, DEFAULT_VIDEO_HEIGHT, DEFAULT_VIDEO_BITRATE,
                    DEFAULT_VIDEO_FRAMERATE, DEFAULT_VIDEO_IFRAME, codec, VIDEO_AVC,
                    profile, profileLevel);
        }

        private boolean hasPermissions() {
            PackageManager pm = getPackageManager();
            String packageName = getPackageName();
            int granted = pm.checkPermission(RECORD_AUDIO, packageName) | pm.checkPermission(WRITE_EXTERNAL_STORAGE, packageName);
            return granted == PackageManager.PERMISSION_GRANTED;
        }


        private MediaCodecInfo getVideoCodecInfo(String codecName) {
            if (codecName == null) return null;
            if (mAvcCodecInfos == null) {
                mAvcCodecInfos = Utils.findEncodersByType(VIDEO_AVC);
            }
            MediaCodecInfo codec = null;
            for (MediaCodecInfo info : mAvcCodecInfos) {
                if (info.getName().equals(codecName)) {
                    codec = info;
                    break;
                }
            }
            return codec;
        }

        private final MediaProjection.Callback mProjectionCallback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                stopRecorder();
            }
        };

        private final BroadcastReceiver mStopActionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Notifications.ACTION_STOP.equals(intent.getAction())) {
                    stopRecorder();
                }
            }
        };
    }

    class ScreenRecorderServiceBinder extends Binder implements IScreenRecorderServiceBinder {

        @Override
        public IScreenRecorderService get() {
            return mScreenRecorderService;
        }

        @Override
        public void registerRecorderCallback(ICallback callback) {
            mScreenRecorderService.registerRecorderCallback(callback);
        }

        @Override
        public void prepareAndStartRecorder(MediaProjection mediaProjection, VideoEncodeConfig videoConfig, AudioEncodeConfig audioConfig) {
            mScreenRecorderService.prepareAndStartRecorder(mediaProjection, videoConfig, audioConfig);
        }

        @Override
        public void startRecorder(VideoEncodeConfig videoConfig, AudioEncodeConfig audioConfig) {
            mScreenRecorderService.startRecorder(videoConfig, audioConfig);
        }

        @Override
        public void startRecorder() {
            mScreenRecorderService.startRecorder();
        }

        @Override
        public void stopRecorder() {
            mScreenRecorderService.stopRecorder();
        }

        @Override
        public void destroyRecorder() {
            mScreenRecorderService.destroyRecorder();
        }

        @Override
        public String getSavingFilePath() {
            return mScreenRecorderService.getSavingFilePath();
        }

        @Override
        public boolean hasPrepared() {
            return mScreenRecorderService.hasPrepared();
        }
    }
}
