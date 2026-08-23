package com.medcare.app;
import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import com.medcare.app.utils.PreferencesManager;
public class MedCareApp extends Application {
    private int startedActivityCount = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(Activity activity) {
                startedActivityCount++;
            }

            @Override
            public void onActivityStopped(Activity activity) {
                startedActivityCount--;
                handler.removeCallbacks(backgroundCheck);
                if (startedActivityCount == 0) {
                    if (!activity.isChangingConfigurations()) {
                        new PreferencesManager(MedCareApp.this)
                                .setLastBackgroundTime(System.currentTimeMillis());
                        handler.postDelayed(backgroundCheck, 500);
                    }
                }
            }

            @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
            @Override public void onActivityResumed(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });
    }

    private final Runnable backgroundCheck = new Runnable() {
        @Override
        public void run() {
            if (startedActivityCount == 0) {
                new PreferencesManager(MedCareApp.this).setLastBackgroundTime(System.currentTimeMillis());
            }
        }
    };
}
