package com.fongmi.android.tv;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.startup.Initializer;

import com.fongmi.android.tv.event.EventIndex;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.activity.CrashActivity;
import com.github.catvod.bean.Doh;
import com.github.catvod.net.OkHttp;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;

import org.greenrobot.eventbus.EventBus;

import java.util.Collections;
import java.util.List;

import android.os.Looper;

import com.fongmi.android.tv.utils.Notify;

import cat.ereza.customactivityoncrash.config.CaocConfig;

public class Startup implements Initializer<Void> {

    private static volatile long lastToastTime;

    @NonNull
    @Override
    public Void create(@NonNull Context context) {
        CaocConfig.Builder.create().trackActivities(true).backgroundMode(CaocConfig.BACKGROUND_MODE_SILENT).errorActivity(CrashActivity.class).apply();
        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            boolean isSpider = false;
            Throwable cause = throwable;
            while (cause != null) {
                for (StackTraceElement elem : cause.getStackTrace()) {
                    if (elem.getClassName().startsWith("com.github.catvod") || elem.getClassName().startsWith("com.fongmi.quickjs") || elem.getClassName().startsWith("com.base.model")) {
                        isSpider = true;
                        break;
                    }
                }
                if (isSpider) break;
                cause = cause.getCause();
            }
            if (isSpider && thread != Looper.getMainLooper().getThread()) {
                Logger.e(throwable, "Suppressed background spider exception on " + thread.getName());
                long now = System.currentTimeMillis();
                if (now - lastToastTime > 3000) {
                    lastToastTime = now;
                    String msg = throwable.getMessage() != null ? throwable.getMessage() : throwable.getClass().getSimpleName();
                    App.post(() -> Notify.show("爬虫后台异常: " + msg));
                }
                return;
            }
            if (defaultHandler != null) defaultHandler.uncaughtException(thread, throwable);
        });
        Logger.addLogAdapter(new AndroidLogAdapter(PrettyFormatStrategy.newBuilder().methodCount(0).showThreadInfo(false).tag("TV").build()));
        EventBus.builder().addIndex(new EventIndex()).installDefaultEventBus();
        OkHttp.dns().setDoh(() -> Doh.objectFrom(Setting.getDoh()));
        return null;
    }

    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies() {
        return Collections.emptyList();
    }
}
