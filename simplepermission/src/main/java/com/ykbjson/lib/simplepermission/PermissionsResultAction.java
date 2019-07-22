
package com.ykbjson.lib.simplepermission;

import android.content.pm.PackageManager;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * Desription：<p>This abstract class should be used to create an if/else action that the PermissionsManager
 * can execute when the permissions you request are granted or denied. Simple use involves
 * creating an anonymous instance of it and passing that instance to the
 * requestPermissionsIfNecessaryForResult method. The result will be sent back to you as
 * either onGranted (all permissions have been granted), or onDenied (a required permission
 * has been denied). Ideally you put your functionality in the onGranted method and notify
 * the user what won't work in the onDenied method.</p>
 * Creator：yankebin
 * CreatedAt：2018/11/7
 */
public class PermissionsResultAction {
    private final String TAG = getClass().getName();

    private Looper mLooper = Looper.getMainLooper();
    private final Set<String> mPermissions = new HashSet<>(1);
    private final Set<String> mDeniedPermissions = new HashSet<>(1);

    private final PermissionsRequestCallback mPermissionsRequestCallback;
    private final int mRequestCode;

    /**
     * Default Constructor
     */
    public PermissionsResultAction(int requestCode, @Nullable PermissionsRequestCallback permissionsRequestCallback) {
        mRequestCode = requestCode;
        mPermissionsRequestCallback = permissionsRequestCallback;
    }

    /**
     * Alternate Constructor. Pass the looper you wish the PermissionsResultAction
     * callbacks to be executed on if it is not the current Looper. For instance,
     * if you are making a permissions request from a background thread but wish the
     * callback to be on the UI thread, use this constructor to specify the UI Looper.
     *
     * @param looper the looper that the callbacks will be called using.
     */
    public PermissionsResultAction(int requestCode, @Nullable PermissionsRequestCallback permissionsRequestCallback, @NonNull Looper looper) {
        this(requestCode, permissionsRequestCallback);
        mLooper = looper;
    }

    /**
     * This method is called when a permission that have been
     * requested have been granted by the user. In this method
     * you should put your permission(s) sensitive code that can
     * only be executed with the required permissions.
     */
    public void onGranted(String permission) {
        if (null != mPermissionsRequestCallback) {
            mPermissionsRequestCallback.onGranted(mRequestCode, permission);
        }
    }

    /**
     * This method is called when a permission has been denied by
     * the user. It provides you with the permission that was denied
     * and will be executed on the Looper you pass to the constructor
     * of this class, or the Looper that this object was created on.
     *
     * @param permission the permission that was denied.
     */
    public void onDenied(String permission) {
        if (null != mPermissionsRequestCallback) {
            mPermissionsRequestCallback.onDenied(mRequestCode, permission);
        }
    }

    /**
     * This method is called when a permission has been denied by
     * the user forever. It provides you with the permission that was denied
     * and will be executed on the Looper you pass to the constructor
     * of this class, or the Looper that this object was created on.
     *
     * @param permission the permission that was denied.
     */
    public void onDeniedForever(String permission) {
        if (null != mPermissionsRequestCallback) {
            mPermissionsRequestCallback.onDeniedForever(mRequestCode, permission);
        }
    }

    /**
     * This method is called when all permissions has been check complete
     * but some permissions denied.
     *
     * @param deniedPermissions those denied permissions
     */
    public void onFailure(String[] deniedPermissions) {
        if (null != mPermissionsRequestCallback) {
            mPermissionsRequestCallback.onFailure(mRequestCode, deniedPermissions);
        }
    }

    /**
     * This method is called when all permissions has been check complete
     * and all permissions granted.
     */
    public void onSuccess() {
        if (null != mPermissionsRequestCallback) {
            mPermissionsRequestCallback.onSuccess(mRequestCode);
        }
    }

    /**
     * This method is used to determine if a permission not
     * being present on the current Android platform should
     * affect whether the PermissionsResultAction should continue
     * listening for events. By default, it returns true and will
     * simply ignore the permission that did not exist. Usually this will
     * work fine since most new permissions are introduced to
     * restrict what was previously allowed without permission.
     * If that is not the case for your particular permission you
     * request, override this method and return false to result in the
     * Action being denied.
     *
     * @param permission the permission that doesn't exist on this
     *                   Android version
     * @return return true if the PermissionsResultAction should
     * ignore the lack of the permission and proceed with exection
     * or false if the PermissionsResultAction should treat the
     * absence of the permission on the API level as a denial.
     */
    public synchronized boolean shouldIgnorePermissionNotFound(String permission) {
        if (PermissionsManager.getInstance().isEnableLog()) {
            Log.d(TAG, "Permission not found: " + permission);
        }
        return true;
    }


    @CallSuper
    protected synchronized final boolean onResult(final @NonNull String permission, int result) {
        if (result == PackageManager.PERMISSION_GRANTED) {
            return onResult(permission, Permissions.GRANTED);
        } else {
            return onResult(permission, Permissions.DENIED);
        }
    }

    /**
     * This method is called when a particular permission has changed.
     * This method will be called for all permissions, so this method determines
     * if the permission affects the state or not and whether it can proceed with
     * calling onGranted or if onDenied should be called.
     *
     * @param permission the permission that changed.
     * @param result     the result for that permission.
     * @return this method returns true if its primary action has been completed
     * and it should be removed from the data structure holding a reference to it.
     */
    @CallSuper
    protected synchronized final boolean onResult(final @NonNull String permission, Permissions result) {
        mPermissions.remove(permission);
        boolean onResult = false;
        if (result == Permissions.GRANTED) {
            getSchedule().scheduleDirect(new Runnable() {
                @Override
                public void run() {
                    PermissionsResultAction.this.onGranted(permission);
                }
            });
        } else if (result == Permissions.DENIED) {
            getSchedule().scheduleDirect(new Runnable() {
                @Override
                public void run() {
                    PermissionsResultAction.this.onDenied(permission);
                }
            });
        } else if (result == Permissions.NOT_FOUND) {
            if (shouldIgnorePermissionNotFound(permission)) {
                getSchedule().scheduleDirect(new Runnable() {
                    @Override
                    public void run() {
                        PermissionsResultAction.this.onGranted(permission);
                    }
                });
            } else {
                getSchedule().scheduleDirect(new Runnable() {
                    @Override
                    public void run() {
                        PermissionsResultAction.this.onDenied(permission);
                    }
                });
            }
        } else if (result == Permissions.USER_DENIED_FOREVER) {
            getSchedule().scheduleDirect(new Runnable() {
                @Override
                public void run() {
                    PermissionsResultAction.this.onDeniedForever(permission);
                }
            });
        }

        //标记不被通过的权限
        if (result == Permissions.DENIED || (result == Permissions.NOT_FOUND && !shouldIgnorePermissionNotFound(permission))) {
            mDeniedPermissions.add(permission);
        }
        if (mPermissions.isEmpty()) {
            getSchedule().scheduleDirect(new Runnable() {
                @Override
                public void run() {
                    if (mDeniedPermissions.isEmpty()) {
                        PermissionsResultAction.this.onSuccess();
                    } else {
                        final String[] deniedPermissions = (String[]) mDeniedPermissions.toArray(
                                new String[mDeniedPermissions.size()]);
                        PermissionsResultAction.this.onFailure(deniedPermissions);
                    }
                    //重置不被通过的权限存储
                    mDeniedPermissions.clear();
                }
            });
            onResult = true;
        }
        return onResult;
    }

    /**
     * This method registers the PermissionsResultAction object for the specified permissions
     * so that it will know which permissions to look for changes to. The PermissionsResultAction
     * will then know to look out for changes to these permissions.
     *
     * @param perms the permissions to listen for
     */
    @SuppressWarnings("WeakerAccess")
    @CallSuper
    protected synchronized final void registerPermissions(@NonNull String[] perms) {
        Collections.addAll(mPermissions, perms);
    }

    /**
     * 获取执行权限回调的线程调度器
     *
     * @return
     */
    protected synchronized Scheduler getSchedule() {
        return null == mLooper ? AndroidSchedulers.mainThread() : AndroidSchedulers.from(mLooper);
    }

    protected int getRequestCode() {
        return mRequestCode;
    }
}