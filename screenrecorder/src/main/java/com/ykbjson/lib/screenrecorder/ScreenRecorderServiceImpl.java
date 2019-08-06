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

import com.chillingvan.canvasgl.util.Loggers;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
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
        Loggers.DEBUG = BuildConfig.DEBUG;
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

        MediaProjection mMediaProjection;
        ScreenRecorder mScreenRecorder;
        VirtualDisplay mVirtualDisplay;
        boolean mIsLandscape;
        IRecorderCallback mCallback;

        ScreenRecorderService() {
            Utils.initMediaCodecInfo();
        }

        @Override
        public void registerRecorderCallback(IRecorderCallback callback) {
            mCallback = callback;
        }

        @Override
        public void prepareAndStartRecorder(MediaProjection mediaProjection, VideoEncodeConfig videoConfig,
                                            AudioEncodeConfig audioConfig) {
            // NOTE: Should pass this result data into a Service to run ScreenRecorder.
            if (mediaProjection == null) {
                Log.e(TAG, "prepareAndStartRecorder,media projection is null");
                return;
            }
            mMediaProjection = mediaProjection;
            mMediaProjection.registerCallback(mProjectionCallback, new Handler());
            startRecorder(videoConfig, audioConfig);
        }

        @Override
        public void startRecorder(VideoEncodeConfig videoConfig, AudioEncodeConfig audioConfig) {
            prepareScreenRecorder(mMediaProjection, videoConfig, audioConfig);
            if (null == mScreenRecorder) {
                Log.e(TAG, "startRecorder,ScreenRecorder has not been initialized yet");
                return;
            }
            if (null != mCallback) {
                mCallback.onPrepareRecorder();
            }
            registerReceiver(mStopActionReceiver, new IntentFilter(Notifications.ACTION_STOP));
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
                Log.e(TAG, "stopRecorder,ScreenRecorder has not been initialized yet");
                return;
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
                Log.e(TAG, "getSavingFilePath,ScreenRecorder has not been initialized yet");
                return "";
            }
            return mScreenRecorder.getSavedPath();
        }

        @Override
        public AudioEncodeConfig getAudioEncodeConfig() {
            if (null == mScreenRecorder) {
                Log.e(TAG, "getAudioEncodeConfig,ScreenRecorder has not been initialized yet");
                return null;
            }
            return mScreenRecorder.getAudioEncoder().getConfig();
        }

        @Override
        public VideoEncodeConfig getVideoEncodeConfig() {
            if (null == mScreenRecorder) {
                Log.e(TAG, "getVideoEncodeConfig,ScreenRecorder has not been initialized yet");
                return null;
            }
            return mScreenRecorder.getVideoEncoder().getConfig();
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
                Log.e(TAG, "prepareScreenRecorder,media projection is null");
                return;
            }
            if (hasPermissions()) {
                video = null == video ? VideoEncodeConfig.Builder.create().build() : video;
                audio = null == audio ? AudioEncodeConfig.Builder.create().build() : audio;

                MediaCodecInfo mediaCodecInfo = getVideoCodecInfo(video.codecName);
                MediaCodecInfo.CodecCapabilities capabilities = mediaCodecInfo.getCapabilitiesForType(VIDEO_AVC);
                MediaCodecInfo.VideoCapabilities videoCapabilities = capabilities.getVideoCapabilities();

                if (!videoCapabilities.isSizeSupported(video.width, video.height)) {
                    Log.w(TAG, "prepareScreenRecorder,unSupport size," + video.codecName +
                            " height range: " + videoCapabilities.getSupportedHeights() +
                            "\n width range: " + videoCapabilities.getSupportedHeights());
                    return;
                }

                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                        "Screenshots");
                if (!dir.exists() && !dir.mkdirs()) {
                    throw new RuntimeException("create file failure");
                }
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.CHINA);
                File file = new File(dir, "ScreenRecord_" + format.format(new Date()) + "_" + video.width
                        + "x" + video.height + ".mp4");
                Log.d(TAG, "prepareScreenRecorder,Create recorder with :" + video + " \n " + audio + "\n " + file);
                mScreenRecorder = newRecorder(mediaProjection, video, audio, file);
            } else {
                throw new RuntimeException("Permission denied! Screen recorder is cancel.");
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


        private boolean hasPermissions() {
            PackageManager pm = getPackageManager();
            String packageName = getPackageName();
            int granted = pm.checkPermission(RECORD_AUDIO, packageName) | pm.checkPermission(WRITE_EXTERNAL_STORAGE, packageName);
            return granted == PackageManager.PERMISSION_GRANTED;
        }


        private MediaCodecInfo getVideoCodecInfo(String codecName) {
            if (codecName == null) return null;
            final MediaCodecInfo[] infos = Utils.getmAvcCodecInfos();
            MediaCodecInfo codec = null;
            for (MediaCodecInfo info : infos) {
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
        public void registerRecorderCallback(IRecorderCallback callback) {
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
        public AudioEncodeConfig getAudioEncodeConfig() {
            return mScreenRecorderService.getAudioEncodeConfig();
        }

        @Override
        public VideoEncodeConfig getVideoEncodeConfig() {
            return mScreenRecorderService.getVideoEncodeConfig();
        }

        @Override
        public boolean hasPrepared() {
            return mScreenRecorderService.hasPrepared();
        }
    }
}
