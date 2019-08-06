package com.chillingvan.lib.publisher;

import android.graphics.SurfaceTexture;

import com.chillingvan.canvasgl.glview.texture.GLMultiTexProducerView;
import com.chillingvan.canvasgl.glview.texture.GLTexture;
import com.chillingvan.canvasgl.glview.texture.gles.EglContextWrapper;
import com.chillingvan.canvasgl.glview.texture.gles.GLThread;
import com.chillingvan.lib.camera.CameraInterface;
import com.chillingvan.lib.encoder.video.H264Encoder;
import com.chillingvan.lib.muxer.IMuxer;

import java.io.IOException;
import java.util.List;

/**
 * Data Stream:
 * Camera -> SurfaceTexture of GLSurfaceTextureProducerView -> Surface of MediaCodec -> encode data(byte[]) -> RTMPMuxer -> Server
 *
 */

public class CameraStreamPublisher {

    private StreamPublisher streamPublisher;
    private IMuxer muxer;
    private GLMultiTexProducerView cameraPreviewTextureView;
    private CameraInterface instantVideoCamera;
    private OnSurfacesCreatedListener onSurfacesCreatedListener;

    public CameraStreamPublisher(IMuxer muxer, GLMultiTexProducerView cameraPreviewTextureView, CameraInterface instantVideoCamera) {
        this.muxer = muxer;
        this.cameraPreviewTextureView = cameraPreviewTextureView;
        this.instantVideoCamera = instantVideoCamera;
    }

    private void initCameraTexture() {
        cameraPreviewTextureView.setOnCreateGLContextListener(new GLThread.OnCreateGLContextListener() {
            @Override
            public void onCreate(EglContextWrapper eglContext) {
                streamPublisher = new StreamPublisher(eglContext, muxer);
            }
        });
        cameraPreviewTextureView.setSurfaceTextureCreatedListener(new GLMultiTexProducerView.SurfaceTextureCreatedListener() {
            @Override
            public void onCreated(List<GLTexture> producedTextureList) {
                if (onSurfacesCreatedListener != null) {
                    onSurfacesCreatedListener.onCreated(producedTextureList, streamPublisher);
                }
                GLTexture texture = producedTextureList.get(0);
                SurfaceTexture surfaceTexture = texture.getSurfaceTexture();
                streamPublisher.addSharedTexture(new GLTexture(texture.getRawTexture(), surfaceTexture));
                surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        cameraPreviewTextureView.requestRenderAndWait();
                        streamPublisher.drawAFrame();
                    }
                });

                instantVideoCamera.setPreview(surfaceTexture);
                instantVideoCamera.startPreview();
            }
        });
    }

    public void prepareEncoder(StreamPublisher.StreamPublisherParam param, H264Encoder.OnDrawListener onDrawListener) {
        streamPublisher.prepareEncoder(param, onDrawListener);
    }

    public void resumeCamera() {
        if (instantVideoCamera.isOpened()) return;

        instantVideoCamera.openCamera();
        initCameraTexture();
        cameraPreviewTextureView.onResume();
    }

    public boolean isStart() {
        return streamPublisher != null && streamPublisher.isStart();
    }

    public void pauseCamera() {
        if (!instantVideoCamera.isOpened()) return;

        instantVideoCamera.release();
        cameraPreviewTextureView.onPause();
    }

    public void startPublish() throws IOException {
        streamPublisher.start();
    }


    public void closeAll() {
        streamPublisher.close();
    }

    public void setOnSurfacesCreatedListener(OnSurfacesCreatedListener onSurfacesCreatedListener) {
        this.onSurfacesCreatedListener = onSurfacesCreatedListener;
    }

    public interface OnSurfacesCreatedListener {
        void onCreated(List<GLTexture> producedTextureList, StreamPublisher streamPublisher);
    }
}
