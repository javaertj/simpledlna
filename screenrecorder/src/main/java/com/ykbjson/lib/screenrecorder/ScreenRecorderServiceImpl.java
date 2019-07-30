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
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;

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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mScreenRecorderService = new ScreenRecorderService();
        return mScreenRecorderService;
    }

    private final class ScreenRecorderService extends IScreenRecorderService.Stub {

        private final int DEFAULT_AUDIO_BITRATE = 80 * 1000;
        private final int DEFAULT_AUDIO_SAMPLERATE = 44100;
        private final int DEFAULT_AUDIO_CHANNEL_COUNT = 2;
        private final int DEFAULT_AUDIO_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectMain;

        private final int DEFAULT_VIDEO_BITRATE = 25000 * 1000;
        private final int DEFAULT_VIDEO_FRAMERATE = 60;
        private final int DEFAULT_VIDEO_IFRAME = 30;
        private final int DEFAULT_VIDEO_WIDTH = 1080;
        private final int DEFAULT_VIDEO_HEIGHT = 1920;


        private MediaProjectionManager mMediaProjectionManager;
        private ScreenRecorder mScreenRecorder;
        private MediaProjection mMediaProjection;
        private VirtualDisplay mVirtualDisplay;
        private boolean mIsLandscape;
        private ICallback mCallback;

        private MediaCodecInfo[] mAvcCodecInfos; // avc codecs
        private MediaCodecInfo[] mAacCodecInfos; // aac codecs

        private ScreenRecorderService() {
            Utils.findEncodersByTypeAsync(VIDEO_AVC, infos -> mAvcCodecInfos = infos);
            Utils.findEncodersByTypeAsync(AUDIO_AAC, infos -> mAacCodecInfos = infos);
        }

        @Override
        public void registerScreenRecorderCallback(ICallback callback) throws RemoteException {
            mCallback = callback;
        }

        @Override
        public void onPrepare(int resultCode, Intent intent, VideoEncodeConfig videoConfig, AudioEncodeConfig audioCofig, String savingFilePath) throws RemoteException {
            // NOTE: Should pass this result data into a Service to run ScreenRecorder.
            // The following codes are merely exemplary.
            mMediaProjectionManager = (MediaProjectionManager) getApplicationContext().getSystemService(MEDIA_PROJECTION_SERVICE);
            MediaProjection mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, intent);
            if (mediaProjection == null) {
                throw new RemoteException("media projection is null");
            }
            mMediaProjection = mediaProjection;
            mMediaProjection.registerCallback(mProjectionCallback, new Handler());
            startCapturing(mediaProjection, savingFilePath, videoConfig, audioCofig);
        }

        @Override
        public void startRecorder() throws RemoteException {
            if (null == mScreenRecorder) {
                throw new RemoteException("ScreenRecorder has not been initialized yet");
            }
            mScreenRecorder.start();
            registerReceiver(mStopActionReceiver, new IntentFilter(Notifications.ACTION_STOP));
        }

        @Override
        public void stopRecorder() throws RemoteException {
            if (null == mScreenRecorder) {
                throw new RemoteException("ScreenRecorder has not been initialized yet");
            }
            mScreenRecorder.quit();
            unregisterReceiver(mStopActionReceiver);
        }

        @Override
        public void destroy() throws RemoteException {
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
        public String getSavingFilePath() throws RemoteException {
            if (null == mScreenRecorder) {
                throw new RemoteException("ScreenRecorder has not been initialized yet");
            }
            return mScreenRecorder.getSavedPath();
        }

        @Override
        public boolean hasPrepared() {
            return null != mScreenRecorder;
        }


        private void onConfigurationChanged(Configuration newConfig) {
            mIsLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;
        }

        private void startCapturing(MediaProjection mediaProjection, String savingFilePath,
                                    VideoEncodeConfig video, AudioEncodeConfig audio) throws RemoteException {
            video = null == video ? createVideoConfig() : video;
            audio = null == audio ? createAudioConfig() : audio;
            final File file = new File(savingFilePath);
            Log.d(TAG, "Create recorder with :" + video + " \n " + audio + "\n " + file);
            mScreenRecorder = newRecorder(mediaProjection, video, audio, file);
            if (hasPermissions()) {
                startRecorder();
            } else {
                throw new RemoteException("Permission denied! Screen recorder is cancel");
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

        private final MediaProjection.Callback mProjectionCallback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                if (mScreenRecorder != null) {
                    try {
                        stopRecorder();
                    } catch (RemoteException e) {
                        //ignored
                    }
                }
            }
        };

        private final BroadcastReceiver mStopActionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Notifications.ACTION_STOP.equals(intent.getAction())) {
                    if (null!=mScreenRecorderService){
                        try {
                            stopRecorder();
                        } catch (RemoteException e) {
                            //ignored
                        }
                    }
                }
            }
        };
    }
}
