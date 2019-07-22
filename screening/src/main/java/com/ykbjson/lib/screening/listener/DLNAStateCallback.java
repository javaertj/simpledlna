package com.ykbjson.lib.screening.listener;

/**
 * Description：DLNAManager初始化回调接口
 * <BR/>
 * Creator：yankebin
 * <BR/>
 * CreatedAt：2019-07-09
 */
public interface DLNAStateCallback {

    void onConnected();

    void onDisconnected();

}
