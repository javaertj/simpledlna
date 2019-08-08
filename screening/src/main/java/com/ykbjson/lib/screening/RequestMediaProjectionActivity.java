package com.ykbjson.lib.screening;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ykbjson.lib.screening.listener.OnRequestMediaProjectionResultCallback;

/**
 * Description：获取录屏的MediaProjection的activity,显示一个像素
 * <BR/>
 * Creator：yankebin
 * <BR/>
 * CreatedAt：2019-08-06
 */
public class RequestMediaProjectionActivity extends AppCompatActivity {
    private static final int CODE_REQUEST_MEDIA_PROJECTION = 1012;
    static OnRequestMediaProjectionResultCallback resultCallback;
    private MediaProjectionManager mMediaProjectionManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_media_projection);
        //window大小设置为1个像素，用户无感知不可见
        Window window = getWindow();
        window.setGravity(Gravity.LEFT | Gravity.TOP);
        WindowManager.LayoutParams params = window.getAttributes();
        params.x = 0;
        params.y = 0;
        params.width = 1;
        params.height = 1;
        window.setAttributes(params);

        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        requestMediaProjection();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CODE_REQUEST_MEDIA_PROJECTION) {

            MediaProjection mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
            if (null == mediaProjection) {
                Toast.makeText(this, "你拒绝了录屏操作！", Toast.LENGTH_SHORT).show();
            } else if (null != resultCallback) {
                resultCallback.onMediaProjectionResult(mediaProjection);
            }
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        resultCallback = null;
        super.onDestroy();
    }

    private void requestMediaProjection() {
        Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, CODE_REQUEST_MEDIA_PROJECTION);
    }

    static void start(Context context) {
        context.startActivity(new Intent(context, RequestMediaProjectionActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
}
