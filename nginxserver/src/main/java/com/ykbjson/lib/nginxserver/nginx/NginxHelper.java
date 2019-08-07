package com.ykbjson.lib.nginxserver.nginx;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.jrummyapps.android.shell.CommandResult;
import com.jrummyapps.android.shell.Shell;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Description：Ngiinx服务器帮助类
 * <BR/>
 * Creator：yankebin
 * <BR/>
 * CreatedAt：2019-08-01
 */
public class NginxHelper {

    private static final String TAG = "NginxHelper";
    private static final String EXE_FILE_RELATIVE_PATH = "/sbin/nginx";
    private static final String CONF_FILE_RELATIVE_PATH = "/conf/nginx.conf";

    private static String mNginxDir;

    public static CommandResult installNginxServer(@NonNull Context context) {
        mNginxDir = getAppDataDir(context) + "/nginx";
        copyFileOrDirFromAsset(context, "nginx");
        CommandResult result = Shell.SH.run("chmod -R 777 " + mNginxDir);
        Log.d(TAG, result.exitCode + "\n" + result.stdout + "\n" + result.stderr);

        return result;
    }

    public static CommandResult startNginxServer() {
        CommandResult result = Shell.SH.run(mNginxDir + EXE_FILE_RELATIVE_PATH + " -p " + mNginxDir + " -c " + mNginxDir + CONF_FILE_RELATIVE_PATH);
        Log.d(TAG, result.exitCode + "\n" + result.stdout + "\n" + result.stderr);
        return result;
    }

    public static CommandResult stopNginxServer() {
        CommandResult result = Shell.SH.run(mNginxDir + EXE_FILE_RELATIVE_PATH + " -p " + mNginxDir + " -s quit");
        Log.d(TAG, result.exitCode + "\n" + result.stdout + "\n" + result.stderr);
        return result;
    }


    private static String getAppDataDir(@NonNull Context context) {
        return context.getApplicationInfo().dataDir;
    }

    private static void copyFileOrDirFromAsset(@NonNull Context context, String path) {
        AssetManager assetManager = context.getAssets();
        String[] assets;
        try {
            assets = assetManager.list(path);
            if (null == assets || assets.length == 0) {
                copyFile(context, path);
            } else {
                String fullPath = getAppDataDir(context) + "/" + path;
                File dir = new File(fullPath);
                if (!dir.exists())
                    dir.mkdir();
                for (String asset : assets) {
                    copyFileOrDirFromAsset(context, path + "/" + asset);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void copyFile(@NonNull Context context, String filename) {
        Log.d(TAG, "copy file path : " + filename);
        AssetManager assetManager = context.getAssets();
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(filename);
            String newFileName = getAppDataDir(context) + "/" + filename;
            out = new FileOutputStream(newFileName);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != out) {
                    out.close();
                }
                if (null != in) {
                    in.close();
                }
            } catch (IOException e) {
                //ignored
            }
        }
    }

    public static String getRtmpLiveServerConfig() {
        return ":9577/live/";
    }

    public static String getHttpServerConfig() {
        return ":9572/";
    }
}
