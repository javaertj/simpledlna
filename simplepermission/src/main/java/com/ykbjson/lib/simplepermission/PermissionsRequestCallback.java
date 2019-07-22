package com.ykbjson.lib.simplepermission;

/**
 * Description：权限申请结果回调
 * Creator：yankebin
 * CreatedAt：2018/11/1
 */
public interface PermissionsRequestCallback {
    /**
     * This method is called when a permission that have been
     * requested have been granted by the user. In this method
     * you should put your permission(s) sensitive code that can
     * only be executed with the required permissions.
     */
    void onGranted(int requestCode, String permission);

    /**
     * This method is called when a permission has been denied by
     * the user. It provides you with the permission that was denied
     * and will be executed on the Looper you pass to the constructor
     * of this class, or the Looper that this object was created on.
     *
     * @param permission the permission that was denied.
     */
    void onDenied(int requestCode, String permission);

    /**
     * This method is called when a permission has been denied by
     * the user forever. It provides you with the permission that was denied
     * and will be executed on the Looper you pass to the constructor
     * of this class, or the Looper that this object was created on.
     *
     * @param permission the permission that was denied.
     */
    void onDeniedForever(int requestCode, String permission);

    /**
     * This method is called when all permissions has been check complete
     * but some permissions denied.
     *
     * @param deniedPermissions those denied permissions
     */
    void onFailure(int requestCode, String[] deniedPermissions);

    /**
     * This method is called when all permissions has been check complete
     * and all permissions granted.
     */
    void onSuccess(int requestCode);
}
