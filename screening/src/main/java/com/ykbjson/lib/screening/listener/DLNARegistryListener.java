package com.ykbjson.lib.screening.listener;

import com.ykbjson.lib.screening.bean.DeviceInfo;

import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Description：DLNA设备注册回调
 * <BR/>
 * Creator：yankebin
 * <BR/>
 * CreatedAt：2019-07-09
 */
public abstract class DLNARegistryListener implements RegistryListener {
    private final DeviceType DMR_DEVICE_TYPE = new UDADeviceType("MediaRenderer");

    public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {

    }

    public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception ex) {

    }

    /**
     * Calls the {@link #onDeviceChanged(List)} method.
     *
     * @param registry The Cling registry of all devices and services know to the local UPnP stack.
     * @param device   A validated and hydrated device metadata graph, with complete service metadata.
     */
    public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
        onDeviceChanged(build(registry.getDevices()));
        onDeviceAdded(registry, device);
    }

    public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {

    }

    /**
     * Calls the {@link #onDeviceChanged(List)} method.
     *
     * @param registry The Cling registry of all devices and services know to the local UPnP stack.
     * @param device   A validated and hydrated device metadata graph, with complete service metadata.
     */
    public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
        onDeviceChanged(build(registry.getDevices()));
        onDeviceRemoved(registry, device);
    }

    /**
     * Calls the {@link #onDeviceChanged(List)} method.
     *
     * @param registry The Cling registry of all devices and services know to the local UPnP stack.
     * @param device   The local device added to the {@link org.fourthline.cling.registry.Registry}.
     */
    public void localDeviceAdded(Registry registry, LocalDevice device) {
        onDeviceChanged(build(registry.getDevices()));
        onDeviceAdded(registry, device);
    }

    /**
     * Calls the {@link #onDeviceChanged(List)} method.
     *
     * @param registry The Cling registry of all devices and services know to the local UPnP stack.
     * @param device   The local device removed from the {@link org.fourthline.cling.registry.Registry}.
     */
    public void localDeviceRemoved(Registry registry, LocalDevice device) {
        onDeviceChanged(build(registry.getDevices()));
        onDeviceRemoved(registry, device);
    }

    public void beforeShutdown(Registry registry) {

    }

    public void afterShutdown() {

    }

    public void onDeviceChanged(Collection<Device> deviceInfoList) {
        onDeviceChanged(build(deviceInfoList));
    }

    public abstract void onDeviceChanged(List<DeviceInfo> deviceInfoList);

    public void onDeviceAdded(Registry registry, Device device) {

    }

    public void onDeviceRemoved(Registry registry, Device device) {

    }

    private List<DeviceInfo> build(Collection<Device> deviceList) {
        final List<DeviceInfo> deviceInfoList = new ArrayList<>();
        for (Device device : deviceList) {
            //过滤不支持投屏渲染的设备
            if (null == device.findDevices(DMR_DEVICE_TYPE)) {
                continue;
            }
            final DeviceInfo deviceInfo = new DeviceInfo(device, getDeviceName(device));
            deviceInfoList.add(deviceInfo);
        }

        return deviceInfoList;
    }

    private String getDeviceName(Device device) {
        String name = "";
        if (device.getDetails() != null && device.getDetails().getFriendlyName() != null) {
            name = device.getDetails().getFriendlyName();
        } else {
            name = device.getDisplayString();
        }

        return name;
    }
}
