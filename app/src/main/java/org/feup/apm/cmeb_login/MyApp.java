package org.feup.apm.cmeb_login;

import android.app.Application;
import android.os.Environment;
import android.util.Log;

import java.io.File;

public class MyApp extends Application {
    @Override
    public void onTerminate() {
        super.onTerminate();

        File cacheDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (cacheDir != null && cacheDir.isDirectory()) {
            for (File file : cacheDir.listFiles()) {
                file.delete();
                Log.d("MyApp", "Deleted file: " + file.getAbsolutePath());
            }
        }
    }
}
