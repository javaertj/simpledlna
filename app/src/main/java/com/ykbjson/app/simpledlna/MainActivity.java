package com.ykbjson.app.simpledlna;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ykbjson.lib.screening.DLNAManager;
import com.ykbjson.lib.screening.DLNAPlayer;
import com.ykbjson.lib.screening.bean.DeviceInfo;
import com.ykbjson.lib.screening.bean.MediaInfo;
import com.ykbjson.lib.screening.listener.DLNAControlCallback;
import com.ykbjson.lib.screening.listener.DLNADeviceConnectListener;
import com.ykbjson.lib.screening.listener.DLNARegistryListener;
import com.ykbjson.lib.screening.listener.DLNAStateCallback;
import com.ykbjson.lib.screenrecorder.ICallback;
import com.ykbjson.lib.screenrecorder.IScreenRecorderService;
import com.ykbjson.lib.screenrecorder.ScreenRecorderServiceImpl;
import com.ykbjson.lib.simplepermission.PermissionsManager;
import com.ykbjson.lib.simplepermission.PermissionsRequestCallback;

import org.fourthline.cling.model.action.ActionInvocation;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements DLNADeviceConnectListener, ICallback {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int CODE_REQUEST_PERMISSION = 1010;
    private static final int CODE_REQUEST_MEDIA = 1011;
    private static final int CODE_REQUEST_MEDIA_PROJECTION = 1012;

    private int curItemType = MediaInfo.TYPE_UNKNOWN;
    private String mMediaPath;

    private DeviceInfo mDeviceInfo;
    private DLNAPlayer mDLNAPlayer;
    private DLNARegistryListener mDLNARegistryListener;

    private DevicesAdapter mDevicesAdapter;
    private ListView mDeviceListView;

    private IScreenRecorderService screenRecorderService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DLNAManager.setIsDebugMode(BuildConfig.DEBUG);
        PermissionsManager.getInstance().requestAllManifestPermissionsIfNecessary(CODE_REQUEST_PERMISSION,
                this, new PermissionsRequestCallback() {
                    @Override
                    public void onGranted(int requestCode, String permission) {
                        boolean hasPermission = PackageManager.PERMISSION_GRANTED ==
                                (checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                        & checkCallingOrSelfPermission(Manifest.permission.RECORD_AUDIO));
                        if (hasPermission) {
                            DLNAManager.getInstance().init(MainActivity.this, new DLNAStateCallback() {
                                @Override
                                public void onConnected() {
                                    Log.d(TAG, "DLNAManager ,onConnected");
                                    initDlna();
                                }

                                @Override
                                public void onDisconnected() {
                                    Log.d(TAG, "DLNAManager ,onDisconnected");
                                }
                            });
                        }
                    }

                    @Override
                    public void onDenied(int requestCode, String permission) {

                    }

                    @Override
                    public void onDeniedForever(int requestCode, String permission) {

                    }

                    @Override
                    public void onFailure(int requestCode, String[] deniedPermissions) {

                    }

                    @Override
                    public void onSuccess(int requestCode) {

                    }
                });

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDeviceListView = findViewById(R.id.deviceListView);
        final View emptyView = findViewById(R.id.layoutDeviceEmpty);
        mDeviceListView.setEmptyView(emptyView);
        mDeviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (TextUtils.isEmpty(mMediaPath) || curItemType == MediaInfo.TYPE_UNKNOWN) {
                    return;
                }
                DeviceInfo deviceInfo = mDevicesAdapter.getItem(position);
                if (null == deviceInfo) {
                    return;
                }
                mDLNAPlayer.connect(deviceInfo);
            }
        });
        mDevicesAdapter = new DevicesAdapter(MainActivity.this);
        mDeviceListView.setAdapter(mDevicesAdapter);


        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DLNAManager.getInstance().startBrowser();
            }
        });

        FloatingActionButton fabRecord = findViewById(R.id.fabRecord);
        fabRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              if(null!=screenRecorderService){
                  try {
                      screenRecorderService.stopRecorder();
                  } catch (RemoteException e) {
                      e.printStackTrace();
                  }
              }
            }
        });
    }


    @Override
    public void onConnect(DeviceInfo deviceInfo, int errorCode) {
        if (errorCode == CONNECT_INFO_CONNECT_SUCCESS) {
            mDeviceInfo = deviceInfo;
            Toast.makeText(this, "连接设备成功", Toast.LENGTH_SHORT).show();
            startPlay();
        }
    }

    @Override
    public void onDisconnect(DeviceInfo deviceInfo, int type, int errorCode) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_select_image:
                selectImage();
                break;

            case R.id.action_select_audio:
                selectAudio();
                break;

            case R.id.action_select_video:
                selectVideo();
                break;

            case R.id.action_screening_phone:
                screeningPhone();
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    private void selectVideo() {
        curItemType = MediaInfo.TYPE_VIDEO;
        Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, CODE_REQUEST_MEDIA);
    }

    private void selectAudio() {
        curItemType = MediaInfo.TYPE_AUDIO;
        Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, CODE_REQUEST_MEDIA);
    }

    private void selectImage() {
        curItemType = MediaInfo.TYPE_IMAGE;
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, CODE_REQUEST_MEDIA);
    }

    private void screeningPhone() {
        curItemType = MediaInfo.TYPE_VIDEO;
        try {
            if (!screenRecorderService.hasPrepared()) {
                requestMediaProjection();
            } else {
                screenRecorderService.startRecorder();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CODE_REQUEST_MEDIA) {
            if (resultCode != RESULT_OK && data == null) {
                return;
            }
            Uri uri = data.getData();
            String path;
            if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT) {
                path = Util.getRealPathFromUriAboveApi19(this, uri);
            } else {
                path = uri.getPath();
            }
            mMediaPath = path;
            Log.d(TAG, path);
            if (null != mDLNAPlayer && null != mDeviceInfo) {
                mDLNAPlayer.connect(mDeviceInfo);
            }
        } else if (requestCode == CODE_REQUEST_MEDIA_PROJECTION) {
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    "Screenshots");
            if (!dir.exists() && !dir.mkdirs()) {
                return;
            }
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss", Locale.CHINA);
            final File file = new File(dir, "ScreenRecord-" + format.format(new Date())
                    + ".mp4");
            try {
                screenRecorderService.onPrepare(resultCode, data, null, null, file.getAbsolutePath());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        try {
            screenRecorderService.destroy();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        unbindService(screenRecorderServiceConnection);
        mDLNAPlayer.disconnect();
        DLNAManager.getInstance().unregisterListener(mDLNARegistryListener);
        DLNAManager.getInstance().destroy();
        super.onDestroy();
    }

    @Override
    public IBinder asBinder() {
        return null;
    }

    @Override
    public void onStopRecord(String error) {
        Log.d(TAG, "ScreenRecorder onStopRecord");
    }

    @Override
    public void onStartRecord() throws RemoteException {
        Log.d(TAG, "ScreenRecorder onStartRecord");
        final String savingFilePath = screenRecorderService.getSavingFilePath();
        mMediaPath = savingFilePath;
        Log.d(TAG, savingFilePath);
        if (null != mDLNAPlayer && null != mDeviceInfo) {
            mDLNAPlayer.connect(mDeviceInfo);
        }
    }

    @Override
    public void onRecording(long presentationTimeUs) {
    }

    private void initScreenRecorder() {
        bindService(new Intent(this, ScreenRecorderServiceImpl.class), screenRecorderServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    private void requestMediaProjection() {
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, CODE_REQUEST_MEDIA_PROJECTION);
    }

    private ServiceConnection screenRecorderServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            screenRecorderService = (IScreenRecorderService) service;
            try {
                screenRecorderService.registerScreenRecorderCallback(MainActivity.this);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            screenRecorderService = null;
            screenRecorderServiceConnection = null;
        }
    };

    private void initDlna() {
        mDLNAPlayer = new DLNAPlayer(this);
        mDLNAPlayer.setConnectListener(this);
        mDLNARegistryListener = new DLNARegistryListener() {
            @Override
            public void onDeviceChanged(List<DeviceInfo> deviceInfoList) {
                mDevicesAdapter.clear();
                mDevicesAdapter.addAll(deviceInfoList);
                mDevicesAdapter.notifyDataSetChanged();
            }
        };

        DLNAManager.getInstance().registerListener(mDLNARegistryListener);
        initScreenRecorder();
    }

    /**
     * 开始播放
     */
    private void startPlay() {
        String sourceUrl = mMediaPath;
        final MediaInfo mediaInfo = new MediaInfo();
        mediaInfo.setMediaId(Base64.encodeToString(sourceUrl.getBytes(), Base64.NO_WRAP));
        mediaInfo.setMediaType(curItemType);
        mediaInfo.setUri(sourceUrl);
        mDLNAPlayer.setDataSource(mediaInfo);
        mDLNAPlayer.start(new DLNAControlCallback() {
            @Override
            public void onSuccess(@Nullable ActionInvocation invocation) {
                Toast.makeText(MainActivity.this, "投屏成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReceived(@Nullable ActionInvocation invocation, @Nullable Object... extra) {

            }

            @Override
            public void onFailure(@Nullable ActionInvocation invocation, int errorCode, @Nullable String errorMsg) {
                Toast.makeText(MainActivity.this, "投屏失败", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
