package com.fongmi.android.tv;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.HandlerCompat;

import com.fongmi.android.tv.utils.Notify;
import com.fongmi.hook.Hook;
import com.github.catvod.Init;
import com.google.gson.Gson;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class App extends Application implements Application.ActivityLifecycleCallbacks {

    private static volatile App instance;
    private static final Set<ClassLoader> SPIDER_LOADERS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final Handler handler;
    private final Gson gson;
    private final long time;

    private Activity activity;
    private Hook hook;
    private PackageManager spiderPackageManager;

    public App() {
        instance = this;
        gson = new Gson();
        time = System.currentTimeMillis();
        handler = HandlerCompat.createAsync(Looper.getMainLooper());
    }

    public static App get() {
        return instance;
    }

    public static Gson gson() {
        return get().gson;
    }

    public static long time() {
        return get().time;
    }

    public static Activity activity() {
        return get().activity;
    }

    public static void post(Runnable runnable) {
        get().handler.post(runnable);
    }

    public static void post(Runnable runnable, long delayMillis) {
        get().handler.removeCallbacks(runnable);
        if (delayMillis >= 0) get().handler.postDelayed(runnable, delayMillis);
    }

    public static void removeCallbacks(Runnable runnable) {
        get().handler.removeCallbacks(runnable);
    }

    public static void removeCallbacks(Runnable... runnable) {
        for (Runnable r : runnable) get().handler.removeCallbacks(r);
    }

    public void setHook(Hook hook) {
        this.hook = hook;
    }

    public static void registerSpiderLoader(ClassLoader loader) {
        if (loader != null) SPIDER_LOADERS.add(loader);
    }

    public static boolean isSpiderCalling() {
        ClassLoader hostLoader = App.class.getClassLoader();
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement elem : stack) {
            String clsName = elem.getClassName();
            if (clsName.startsWith("android.webkit") || clsName.startsWith("org.chromium") || clsName.startsWith("com.android.webview")) {
                return false;
            }
            try {
                Class<?> caller = Class.forName(clsName);
                ClassLoader cl = caller.getClassLoader();
                if (cl != null && cl != hostLoader && cl != hostLoader.getParent()) {
                    if (SPIDER_LOADERS.contains(cl)) return true;
                    ClassLoader p = cl;
                    while (p != null) {
                        if (SPIDER_LOADERS.contains(p)) return true;
                        p = p.getParent();
                    }
                }
            } catch (Throwable ignored) {
            }
            if (clsName.startsWith("com.github.catvod") || clsName.startsWith("com.fongmi.quickjs") || clsName.startsWith("com.base.model") || clsName.startsWith("com.github.tvbox")) {
                return true;
            }
        }
        return false;
    }

    private PackageManager getSpiderPackageManager() {
        if (spiderPackageManager == null) {
            spiderPackageManager = new Hook(getBaseContext().getPackageManager(), getBaseContext().getPackageName());
        }
        return spiderPackageManager;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Init.set(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Notify.createChannel();
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public PackageManager getPackageManager() {
        if (hook != null) return hook;
        if (isSpiderCalling()) return getSpiderPackageManager();
        return getBaseContext().getPackageManager();
    }

    @Override
    public String getPackageName() {
        if (hook != null) return hook.getPackageName();
        if (isSpiderCalling()) return "com.fongmi.android.tv";
        return getBaseContext().getPackageName();
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        if (activity != activity()) this.activity = activity;
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        if (activity == activity()) this.activity = null;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }
}