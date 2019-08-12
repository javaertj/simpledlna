package com.ykbjson.lib.screenrecorder;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
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
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
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

    private ScreenRecorderServiceProxy mScreenRecorderServiceProxy;

    @Override
    public void onCreate() {
        Loggers.DEBUG = BuildConfig.DEBUG;
        super.onCreate();
        mScreenRecorderServiceProxy = getScreenRecorderServiceProxy(this);
    }

    @Nullable
    @Override
    public final IBinder onBind(Intent intent) {
        return mScreenRecorderServiceProxy;
    }

    @Override
    public void onDestroy() {
        if (null != mScreenRecorderServiceProxy) {
            mScreenRecorderServiceProxy.destroyRecorder();
        }
        super.onDestroy();
    }

    /**
     * 重写用来自定义扩展
     *
     * @param context
     * @return
     */
    ScreenRecorderServiceProxy getScreenRecorderServiceProxy(Context context) {
        return new ScreenRecorderServiceProxy(context);
    }

    static class ScreenRecorderService implements IScreenRecorderService {
        static final int MSG_PREPARE_RECORDER = 1;
        static final int MSG_START_RECORDER = MSG_PREPARE_RECORDER + 1;
        static final int MSG_STOP_RECORDER = MSG_PREPARE_RECORDER + 2;
        static final int MSG_DESTROY_RECORDER = MSG_PREPARE_RECORDER + 3;

        VideoEncodeConfig mVideoEncodeConfig;
        AudioEncodeConfig mAudioEncodeConfig;
        MediaProjection mMediaProjection;
        ScreenRecorder mScreenRecorder;
        VirtualDisplay mVirtualDisplay;
        boolean mIsLandscape;
        IRecorderCallback mCallback;
        WorkHandler mHandler;
        Context mContext;

        ScreenRecorderService(Context context) {
            mContext = context;
            Utils.initMediaCodecInfo();
            HandlerThread handlerThread = new HandlerThread("ScreenRecorderService_" + System.currentTimeMillis());
            handlerThread.start();
            mHandler = new WorkHandler(this, handlerThread.getLooper());
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
            mHandler.obtainMessage(MSG_PREPARE_RECORDER,
                    new PrepareRecorderParam(mediaProjection, audioConfig, videoConfig)).sendToTarget();
        }

        void handlePrepareAndStartRecorder(PrepareRecorderParam extra) {
            //maybe recorder is running
            handleStopMediaProjection();
            mMediaProjection = extra.mediaProjection;
            mMediaProjection.registerCallback(mProjectionCallback, new Handler());
            handleStartRecorder(extra);
        }

        @Override
        public void startRecorder(VideoEncodeConfig videoConfig, AudioEncodeConfig audioConfig) {
            mHandler.obtainMessage(MSG_START_RECORDER,
                    new PrepareRecorderParam(mMediaProjection, audioConfig, videoConfig)).sendToTarget();
        }

        void handleStartRecorder(PrepareRecorderParam extra) {
            //maybe recorder is running
            handleStopRecorder(false);

            //prepare EncodeConfig
            mAudioEncodeConfig = extra.audioEncodeConfig;
            mVideoEncodeConfig = extra.videoEncodeConfig;
            if (null == mVideoEncodeConfig) {
                mVideoEncodeConfig = VideoEncodeConfig.Builder.create().build();
            }
            if (null == mAudioEncodeConfig) {
                mAudioEncodeConfig = AudioEncodeConfig.Builder.create().build();
            }
            //prepare ScreenRecorder
            prepareScreenRecorder();
            if (null == mScreenRecorder) {
                Log.e(TAG, "startRecorder,prepare ScreenRecorder failure");
                return;
            }
            if (null != mCallback) {
                mCallback.onPrepareRecord();
            }
            mScreenRecorder.start();
        }

        @Override
        public void startRecorder() {
            startRecorder(mVideoEncodeConfig, mAudioEncodeConfig);
        }

        @Override
        public void stopRecorder() {
            mHandler.sendEmptyMessage(MSG_STOP_RECORDER);
        }

        void handleStopMediaProjection() {
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

        void handleStopRecorder(boolean stopMediaProjection) {
            if (stopMediaProjection) {
                handleStopMediaProjection();
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
            mHandler.sendEmptyMessage(MSG_DESTROY_RECORDER);
        }

        void handleDestroyRecorder() {
            handleStopRecorder(true);
            if (null != mHandler) {
                mHandler.removeCallbacksAndMessages(null);
                mHandler.getLooper().quitSafely();
                mHandler = null;
            }
            mVideoEncodeConfig = null;
            mAudioEncodeConfig = null;
            mContext = null;
            if (null != mCallback) {
                mCallback.onDestroyRecord();
                mCallback = null;
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

        void onConfigurationChanged(Configuration newConfig) {
            mIsLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;
        }

        void prepareScreenRecorder() {
            if (mMediaProjection == null) {
                Log.e(TAG, "prepareScreenRecorder,media projection is null");
                return;
            }
            if (hasPermissions()) {
                MediaCodecInfo mediaCodecInfo = getVideoCodecInfo(mVideoEncodeConfig.codecName);
                MediaCodecInfo.CodecCapabilities capabilities = mediaCodecInfo.getCapabilitiesForType(VIDEO_AVC);
                MediaCodecInfo.VideoCapabilities videoCapabilities = capabilities.getVideoCapabilities();

                if (!videoCapabilities.isSizeSupported(mVideoEncodeConfig.width, mVideoEncodeConfig.height)) {
                    Log.w(TAG, "prepareScreenRecorder,unSupport size," + mVideoEncodeConfig.codecName +
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
                File file = new File(dir, "ScreenRecord_" + format.format(new Date()) + "_" + mVideoEncodeConfig.width
                        + "x" + mVideoEncodeConfig.height + ".mp4");
                Log.d(TAG, "prepareScreenRecorder,Create recorder with :" + mVideoEncodeConfig + " \n " + mAudioEncodeConfig + "\n " + file);
                mScreenRecorder = newRecorder(mMediaProjection, mVideoEncodeConfig, mAudioEncodeConfig, file);
            } else {
                throw new RuntimeException("Permission denied! Screen recorder is cancel.");
            }
        }


        ScreenRecorder newRecorder(MediaProjection mediaProjection, VideoEncodeConfig video,
                                   AudioEncodeConfig audio, File output) {
            final VirtualDisplay display = getOrCreateVirtualDisplay(mediaProjection, video);
            ScreenRecorder r = new ScreenRecorder(video, audio, display, output.getAbsolutePath());
            r.setCallback(mCallback);
            return r;
        }


        VirtualDisplay getOrCreateVirtualDisplay(MediaProjection mediaProjection, VideoEncodeConfig config) {
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


        boolean hasPermissions() {
            PackageManager pm = mContext.getPackageManager();
            String packageName = mContext.getPackageName();
            int granted = pm.checkPermission(RECORD_AUDIO, packageName) | pm.checkPermission(WRITE_EXTERNAL_STORAGE, packageName);
            return granted == PackageManager.PERMISSION_GRANTED;
        }


        MediaCodecInfo getVideoCodecInfo(String codecName) {
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

        MediaProjection.Callback mProjectionCallback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                stopRecorder();
            }
        };


        static class WorkHandler extends Handler {
            private ScreenRecorderService mService;

            WorkHandler(ScreenRecorderService service, Looper looper) {
                super(looper);
                mService = service;
            }

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                switch (msg.what) {
                    case MSG_PREPARE_RECORDER:
                        mService.handlePrepareAndStartRecorder((PrepareRecorderParam) msg.obj);
                        break;

                    case MSG_START_RECORDER:
                        mService.handleStartRecorder((PrepareRecorderParam) msg.obj);
                        break;

                    case MSG_STOP_RECORDER:
                        mService.handleStopRecorder(true);
                        break;

                    case MSG_DESTROY_RECORDER:
                        mService.handleDestroyRecorder();
                        break;

                    default:
                        break;

                }
            }
        }

        private static class PrepareRecorderParam {
            private MediaProjection mediaProjection;
            private AudioEncodeConfig audioEncodeConfig;
            private VideoEncodeConfig videoEncodeConfig;

            private PrepareRecorderParam(MediaProjection mediaProjection, AudioEncodeConfig audioEncodeConfig,
                                         VideoEncodeConfig videoEncodeConfig) {
                this.mediaProjection = mediaProjection;
                this.audioEncodeConfig = audioEncodeConfig;
                this.videoEncodeConfig = videoEncodeConfig;
            }
        }
    }

    static class ScreenRecorderServiceProxy extends Binder implements IScreenRecorderServiceProxy {

        IScreenRecorderService mScreenRecorderService;

        ScreenRecorderServiceProxy(Context context) {
            mScreenRecorderService = getScreenRecorderService(context);
        }

        /**
         * 重写用来自定义扩展
         *
         * @param context
         * @return
         */
        IScreenRecorderService getScreenRecorderService(Context context) {
            return new ScreenRecorderService(context);
        }

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
