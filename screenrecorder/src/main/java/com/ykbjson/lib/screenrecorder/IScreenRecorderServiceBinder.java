package com.ykbjson.lib.screenrecorder;

/**
 * Description：录屏服务中转接口
 * <BR/>
 * Creator：yankebin
 * <BR/>
 * CreatedAt：2019-07-31
 */
interface IScreenRecorderServiceBinder extends IScreenRecorderService {
    IScreenRecorderService get();
}
