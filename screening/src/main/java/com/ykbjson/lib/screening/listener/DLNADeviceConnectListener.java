package com.ykbjson.lib.screening.listener;

import com.ykbjson.lib.screening.bean.DeviceInfo;

/**
 * Description：连接设备的回调接口
 * <BR/>
 * Creator：yankebin
 * <BR/>
 * CreatedAt：2019-07-09
 */
public interface DLNADeviceConnectListener {

    int TYPE_DLNA = 1;
    int TYPE_IM = 2;
    int TYPE_NEW_LELINK = 3;
    int CONNECT_INFO_CONNECT_SUCCESS = 100000;
    int CONNECT_INFO_CONNECT_FAILURE = 100001;
    int CONNECT_INFO_DISCONNECT = 212000;
    int CONNECT_INFO_DISCONNECT_SUCCESS = 212001;
    int CONNECT_ERROR_FAILED = 212010;
    int CONNECT_ERROR_IO = 212011;
    int CONNECT_ERROR_IM_WAITTING = 212012;
    int CONNECT_ERROR_IM_REJECT = 212013;
    int CONNECT_ERROR_IM_TIMEOUT = 212014;
    int CONNECT_ERROR_IM_BLACKLIST = 212015;

    void onConnect(DeviceInfo deviceInfo, int errorCode);

    void onDisconnect(DeviceInfo deviceInfo,int type,int errorCode);
}
