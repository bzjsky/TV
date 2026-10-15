package com.fongmi.hook;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ChangedPackages;
import android.content.pm.FeatureInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.SharedLibraryInfo;
import android.content.pm.Signature;
import android.content.pm.VersionedPackage;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.Collections;
import java.util.List;

public class Hook extends PackageManager {

    private final PackageManager base;
    private final String sign;
    private final String pkgn;

    public Hook(String sign, String pkgn) {
        this(null, sign, pkgn);
    }

    public Hook(PackageManager base, String pkgn) {
        this(base, null, pkgn);
    }

    public Hook(PackageManager base, String sign, String pkgn) {
        this.base = base;
        this.sign = sign;
        this.pkgn = pkgn;
    }

    public String getPackageName() {
        return pkgn;
    }

    private String mapPackage(String name) {
        if ("com.fongmi.android.tv".equals(name) || "com.fongmi.android.mobile".equals(name)) {
            return pkgn != null ? pkgn : name;
        }
        return name;
    }


    private void ensureMetadata(PackageInfo info, String packageName) {
        if (info == null) return;
        if (info.applicationInfo == null) {
            info.applicationInfo = new ApplicationInfo();
            info.applicationInfo.packageName = info.packageName != null ? info.packageName : packageName;
        }
        if (info.applicationInfo.metaData == null) {
            info.applicationInfo.metaData = new Bundle();
        }
    }

    private ApplicationInfo ensureAppMetadata(ApplicationInfo info, String packageName) {
        if (info == null) {
            info = new ApplicationInfo();
            info.packageName = packageName;
        }
        if (info.metaData == null) {
            info.metaData = new Bundle();
        }
        return info;
    }

    @Override
    public PackageInfo getPackageInfo(@NonNull String packageName, int flags) throws NameNotFoundException {
        if (sign != null) {
            PackageInfo info = new PackageInfo();
            info.signatures = new Signature[]{new Signature(sign)};
            ensureMetadata(info, packageName);
            return info;
        }
        if (base != null) {
            try {
                PackageInfo info = base.getPackageInfo(mapPackage(packageName), flags);
                if ("com.fongmi.android.tv".equals(packageName) || "com.fongmi.android.mobile".equals(packageName)) {
                    info.packageName = packageName;
                }
                ensureMetadata(info, packageName);
                return info;
            } catch (Throwable e) {
                if (e instanceof NameNotFoundException) throw (NameNotFoundException) e;
                throw new NameNotFoundException(packageName);
            }
        }
        PackageInfo info = new PackageInfo();
        info.signatures = new Signature[]{new Signature(sign)};
        ensureMetadata(info, packageName);
        return info;
    }

    @NonNull
    @Override
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public PackageInfo getPackageInfo(@NonNull String packageName, @NonNull PackageInfoFlags flags) throws NameNotFoundException {
        if (sign != null) {
            PackageInfo info = new PackageInfo();
            info.signatures = new Signature[]{new Signature(sign)};
            ensureMetadata(info, packageName);
            return info;
        }
        if (base != null) {
            try {
                PackageInfo info = base.getPackageInfo(mapPackage(packageName), flags);
                if ("com.fongmi.android.tv".equals(packageName) || "com.fongmi.android.mobile".equals(packageName)) {
                    info.packageName = packageName;
                }
                ensureMetadata(info, packageName);
                return info;
            } catch (Throwable e) {
                if (e instanceof NameNotFoundException) throw (NameNotFoundException) e;
                throw new NameNotFoundException(packageName);
            }
        }
        PackageInfo info = new PackageInfo();
        info.signatures = new Signature[]{new Signature(sign)};
        ensureMetadata(info, packageName);
        return info;
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.O)
    public PackageInfo getPackageInfo(@NonNull VersionedPackage versionedPackage, int flags) throws NameNotFoundException {
        if (base != null) {
            try {
                String pkg = mapPackage(versionedPackage.getPackageName());
                PackageInfo info = base.getPackageInfo(new VersionedPackage(pkg, versionedPackage.getLongVersionCode()), flags);
                if ("com.fongmi.android.tv".equals(versionedPackage.getPackageName()) || "com.fongmi.android.mobile".equals(versionedPackage.getPackageName())) {
                    info.packageName = versionedPackage.getPackageName();
                }
                ensureMetadata(info, versionedPackage.getPackageName());
                return info;
            } catch (Throwable e) {
                if (e instanceof NameNotFoundException) throw (NameNotFoundException) e;
                throw new NameNotFoundException(versionedPackage.getPackageName());
            }
        }
        return getPackageInfo(versionedPackage.getPackageName(), flags);
    }

    @NonNull
    @Override
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public PackageInfo getPackageInfo(@NonNull VersionedPackage versionedPackage, @NonNull PackageInfoFlags flags) throws NameNotFoundException {
        if (base != null) {
            try {
                String pkg = mapPackage(versionedPackage.getPackageName());
                PackageInfo info = base.getPackageInfo(new VersionedPackage(pkg, versionedPackage.getLongVersionCode()), flags);
                if ("com.fongmi.android.tv".equals(versionedPackage.getPackageName()) || "com.fongmi.android.mobile".equals(versionedPackage.getPackageName())) {
                    info.packageName = versionedPackage.getPackageName();
                }
                ensureMetadata(info, versionedPackage.getPackageName());
                return info;
            } catch (Throwable e) {
                if (e instanceof NameNotFoundException) throw (NameNotFoundException) e;
                throw new NameNotFoundException(versionedPackage.getPackageName());
            }
        }
        return getPackageInfo(versionedPackage.getPackageName(), (int) flags.getValue());
    }

    @Override
    public String[] currentToCanonicalPackageNames(@NonNull String[] packageNames) {
        if (base != null) {
            try {
                return base.currentToCanonicalPackageNames(packageNames);
            } catch (Throwable ignored) {
            }
        }
        return new String[0];
    }

    @Override
    public String[] canonicalToCurrentPackageNames(@NonNull String[] packageNames) {
        if (base != null) {
            try {
                return base.canonicalToCurrentPackageNames(packageNames);
            } catch (Throwable ignored) {
            }
        }
        return new String[0];
    }

    @Override
    public Intent getLaunchIntentForPackage(@NonNull String packageName) {
        if (base != null) {
            try {
                return base.getLaunchIntentForPackage(mapPackage(packageName));
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Override
    public Intent getLeanbackLaunchIntentForPackage(@NonNull String packageName) {
        if (base != null) {
            try {
                return base.getLeanbackLaunchIntentForPackage(mapPackage(packageName));
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Override
    public int[] getPackageGids(@NonNull String packageName) {
        if (base != null) {
            try {
                return base.getPackageGids(mapPackage(packageName));
            } catch (Throwable ignored) {
            }
        }
        return new int[0];
    }

    @Override
    public int[] getPackageGids(@NonNull String packageName, int flags) {
        if (base != null) {
            try {
                return base.getPackageGids(mapPackage(packageName), flags);
            } catch (Throwable ignored) {
            }
        }
        return new int[0];
    }

    @Override
    public int getPackageUid(@NonNull String packageName, int flags) {
        if (base != null) {
            try {
                return base.getPackageUid(mapPackage(packageName), flags);
            } catch (Throwable ignored) {
            }
        }
        return 0;
    }

    @Override
    public PermissionInfo getPermissionInfo(@NonNull String permName, int flags) {
        if (base != null) {
            try {
                return base.getPermissionInfo(permName, flags);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public List<PermissionInfo> queryPermissionsByGroup(String permissionGroup, int flags) {
        if (base != null) {
            try {
                return base.queryPermissionsByGroup(permissionGroup, flags);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public PermissionGroupInfo getPermissionGroupInfo(@NonNull String groupName, int flags) {
        if (base != null) {
            try {
                return base.getPermissionGroupInfo(groupName, flags);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public List<PermissionGroupInfo> getAllPermissionGroups(int flags) {
        if (base != null) {
            try {
                return base.getAllPermissionGroups(flags);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public ApplicationInfo getApplicationInfo(@NonNull String packageName, int flags) throws NameNotFoundException {
        if (base != null) {
            try {
                ApplicationInfo info = base.getApplicationInfo(mapPackage(packageName), flags);
                if ("com.fongmi.android.tv".equals(packageName) || "com.fongmi.android.mobile".equals(packageName)) {
                    info.packageName = packageName;
                }
                return ensureAppMetadata(info, packageName);
            } catch (Throwable e) {
                if (e instanceof NameNotFoundException) throw (NameNotFoundException) e;
                throw new NameNotFoundException(packageName);
            }
        }
        return ensureAppMetadata(null, packageName);
    }

    @NonNull
    @Override
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public ApplicationInfo getApplicationInfo(@NonNull String packageName, @NonNull ApplicationInfoFlags flags) throws NameNotFoundException {
        if (base != null) {
            try {
                ApplicationInfo info = base.getApplicationInfo(mapPackage(packageName), flags);
                if ("com.fongmi.android.tv".equals(packageName) || "com.fongmi.android.mobile".equals(packageName)) {
                    info.packageName = packageName;
                }
                return ensureAppMetadata(info, packageName);
            } catch (Throwable e) {
                if (e instanceof NameNotFoundException) throw (NameNotFoundException) e;
                throw new NameNotFoundException(packageName);
            }
        }
        return ensureAppMetadata(null, packageName);
    }

    @NonNull
    @Override
    public ActivityInfo getActivityInfo(@NonNull ComponentName component, int flags) {
        if (base != null) {
            try {
                return base.getActivityInfo(component, flags);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public ActivityInfo getReceiverInfo(@NonNull ComponentName component, int flags) {
        if (base != null) {
            try {
                return base.getReceiverInfo(component, flags);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public ServiceInfo getServiceInfo(@NonNull ComponentName component, int flags) {
        if (base != null) {
            try {
                return base.getServiceInfo(component, flags);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public ProviderInfo getProviderInfo(@NonNull ComponentName component, int flags) {
        if (base != null) {
            try {
                return base.getProviderInfo(component, flags);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public List<PackageInfo> getInstalledPackages(int flags) {
        if (base != null) {
            try {
                return base.getInstalledPackages(flags);
            } catch (Throwable ignored) {
            }
        }
        return Collections.emptyList();
    }

    @NonNull
    @Override
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public List<PackageInfo> getInstalledPackages(@NonNull PackageInfoFlags flags) {
        if (base != null) {
            try {
                return base.getInstalledPackages(flags);
            } catch (Throwable ignored) {
            }
        }
        return Collections.emptyList();
    }

    @NonNull
    @Override
    public List<PackageInfo> getPackagesHoldingPermissions(@NonNull String[] permissions, int flags) {
        if (base != null) {
            try {
                return base.getPackagesHoldingPermissions(permissions, flags);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Override
    public int checkPermission(@NonNull String permName, @NonNull String packageName) {
        if (base != null) {
            try {
                return base.checkPermission(permName, mapPackage(packageName));
            } catch (Throwable ignored) {
            }
        }
        return android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public boolean isPermissionRevokedByPolicy(@NonNull String permName, @NonNull String packageName) {
        if (base != null) {
            try {
                return base.isPermissionRevokedByPolicy(permName, mapPackage(packageName));
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    @Override
    public boolean addPermission(@NonNull PermissionInfo info) {
        if (base != null) {
            try {
                return base.addPermission(info);
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    @Override
    public boolean addPermissionAsync(@NonNull PermissionInfo info) {
        if (base != null) {
            try {
                return base.addPermissionAsync(info);
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    @Override
    public void removePermission(@NonNull String permName) {
        if (base != null) {
            try {
                base.removePermission(permName);
                return;
            } catch (Throwable ignored) {
            }
        }
        
    }

    @Override
    public int checkSignatures(@NonNull String packageName1, @NonNull String packageName2) {
        if (base != null) {
            try {
                return base.checkSignatures(packageName1, packageName2);
            } catch (Throwable ignored) {
            }
        }
        return android.content.pm.PackageManager.SIGNATURE_MATCH;
    }

    @Override
    public int checkSignatures(int uid1, int uid2) {
        if (base != null) {
            try {
                return base.checkSignatures(uid1, uid2);
            } catch (Throwable ignored) {
            }
        }
        return android.content.pm.PackageManager.SIGNATURE_MATCH;
    }

    @Override
    public String[] getPackagesForUid(int uid) {
        if (base != null) {
            try {
                return base.getPackagesForUid(uid);
            } catch (Throwable ignored) {
            }
        }
        return new String[0];
    }

    @Override
    public String getNameForUid(int uid) {
        if (base != null) {
            try {
                return base.getNameForUid(uid);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public List<ApplicationInfo> getInstalledApplications(int flags) {
        if (base != null) {
            try {
                return base.getInstalledApplications(flags);
            } catch (Throwable ignored) {
            }
        }
        return Collections.emptyList();
    }

    @NonNull
    @Override
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public List<ApplicationInfo> getInstalledApplications(@NonNull ApplicationInfoFlags flags) {
        if (base != null) {
            try {
                return base.getInstalledApplications(flags);
            } catch (Throwable ignored) {
            }
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isInstantApp() {
        if (base != null) {
            try {
                return base.isInstantApp();
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    @Override
    public boolean isInstantApp(@NonNull String packageName) {
        if (base != null) {
            try {
                return base.isInstantApp(mapPackage(packageName));
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    @Override
    public int getInstantAppCookieMaxBytes() {
        if (base != null) {
            try {
                return base.getInstantAppCookieMaxBytes();
            } catch (Throwable ignored) {
            }
        }
        return 0;
    }

    @NonNull
    @Override
    public byte[] getInstantAppCookie() {
        if (base != null) {
            try {
                return base.getInstantAppCookie();
            } catch (Throwable ignored) {
            }
        }
        return new byte[0];
    }

    @Override
    public void clearInstantAppCookie() {
        if (base != null) {
            try {
                base.clearInstantAppCookie();
                return;
            } catch (Throwable ignored) {
            }
        }
        
    }

    @Override
    public void updateInstantAppCookie(byte[] cookie) {
        if (base != null) {
            try {
                base.updateInstantAppCookie(cookie);
                return;
            } catch (Throwable ignored) {
            }
        }
        
    }

    @Override
    public String[] getSystemSharedLibraryNames() {
        if (base != null) {
            try {
                return base.getSystemSharedLibraryNames();
            } catch (Throwable ignored) {
            }
        }
        return new String[0];
    }

    @NonNull
    @Override
    public List<SharedLibraryInfo> getSharedLibraries(int flags) {
        if (base != null) {
            try {
                return base.getSharedLibraries(flags);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Override
    public ChangedPackages getChangedPackages(int sequenceNumber) {
        if (base != null) {
            try {
                return base.getChangedPackages(sequenceNumber);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public FeatureInfo[] getSystemAvailableFeatures() {
        if (base != null) {
            try {
                return base.getSystemAvailableFeatures();
            } catch (Throwable ignored) {
            }
        }
        return new FeatureInfo[0];
    }

    @Override
    public boolean hasSystemFeature(@NonNull String featureName) {
        if (base != null) {
            try {
                return base.hasSystemFeature(featureName);
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    @Override
    public boolean hasSystemFeature(@NonNull String featureName, int version) {
        if (base != null) {
            try {
                return base.hasSystemFeature(featureName, version);
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    @Override
    public ResolveInfo resolveActivity(@NonNull Intent intent, int flags) {
        if (base != null) {
            try {
                return base.resolveActivity(intent, flags);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public List<ResolveInfo> queryIntentActivities(@NonNull Intent intent, int flags) {
        if (base != null) {
            try {
                return base.queryIntentActivities(intent, flags);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public List<ResolveInfo> queryIntentActivityOptions(ComponentName caller, Intent[] specifics, @NonNull Intent intent, int flags) {
        if (base != null) {
            try {
                return base.queryIntentActivityOptions(caller, specifics, intent, flags);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public List<ResolveInfo> queryBroadcastReceivers(@NonNull Intent intent, int flags) {
        if (base != null) {
            try {
                return base.queryBroadcastReceivers(intent, flags);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Override
    public ResolveInfo resolveService(@NonNull Intent intent, int flags) {
        if (base != null) {
            try {
                return base.resolveService(intent, flags);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public List<ResolveInfo> queryIntentServices(@NonNull Intent intent, int flags) {
        if (base != null) {
            try {
                return base.queryIntentServices(intent, flags);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public List<ResolveInfo> queryIntentContentProviders(Intent intent, int flags) {
        if (base != null) {
            try {
                return base.queryIntentContentProviders(intent, flags);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Override
    public ProviderInfo resolveContentProvider(@NonNull String authority, int flags) {
        if (base != null) {
            try {
                return base.resolveContentProvider(authority, flags);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public List<ProviderInfo> queryContentProviders(String processName, int uid, int flags) {
        if (base != null) {
            try {
                return base.queryContentProviders(processName, uid, flags);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public InstrumentationInfo getInstrumentationInfo(@NonNull ComponentName className, int flags) {
        if (base != null) {
            try {
                return base.getInstrumentationInfo(className, flags);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public List<InstrumentationInfo> queryInstrumentation(@NonNull String targetPackage, int flags) {
        if (base != null) {
            try {
                return base.queryInstrumentation(targetPackage, flags);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Override
    public Drawable getDrawable(@NonNull String packageName, int resId, ApplicationInfo appInfo) {
        if (base != null) {
            try {
                return base.getDrawable(mapPackage(packageName), resId, appInfo);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public Drawable getActivityIcon(@NonNull ComponentName activityName) {
        if (base != null) {
            try {
                return base.getActivityIcon(activityName);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public Drawable getActivityIcon(@NonNull Intent intent) {
        if (base != null) {
            try {
                return base.getActivityIcon(intent);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Override
    public Drawable getActivityBanner(@NonNull ComponentName activityName) {
        if (base != null) {
            try {
                return base.getActivityBanner(activityName);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Override
    public Drawable getActivityBanner(@NonNull Intent intent) {
        if (base != null) {
            try {
                return base.getActivityBanner(intent);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public Drawable getDefaultActivityIcon() {
        if (base != null) {
            try {
                return base.getDefaultActivityIcon();
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public Drawable getApplicationIcon(@NonNull ApplicationInfo info) {
        if (base != null) {
            try {
                return base.getApplicationIcon(info);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public Drawable getApplicationIcon(@NonNull String packageName) {
        if (base != null) {
            try {
                return base.getApplicationIcon(mapPackage(packageName));
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Override
    public Drawable getApplicationBanner(@NonNull ApplicationInfo info) {
        if (base != null) {
            try {
                return base.getApplicationBanner(info);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Override
    public Drawable getApplicationBanner(@NonNull String packageName) {
        if (base != null) {
            try {
                return base.getApplicationBanner(mapPackage(packageName));
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Override
    public Drawable getActivityLogo(@NonNull ComponentName activityName) {
        if (base != null) {
            try {
                return base.getActivityLogo(activityName);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Override
    public Drawable getActivityLogo(@NonNull Intent intent) {
        if (base != null) {
            try {
                return base.getActivityLogo(intent);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Override
    public Drawable getApplicationLogo(@NonNull ApplicationInfo info) {
        if (base != null) {
            try {
                return base.getApplicationLogo(info);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Override
    public Drawable getApplicationLogo(@NonNull String packageName) {
        if (base != null) {
            try {
                return base.getApplicationLogo(mapPackage(packageName));
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public Drawable getUserBadgedIcon(@NonNull Drawable drawable, @NonNull UserHandle user) {
        if (base != null) {
            try {
                return base.getUserBadgedIcon(drawable, user);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public Drawable getUserBadgedDrawableForDensity(@NonNull Drawable drawable, @NonNull UserHandle user, Rect badgeLocation, int badgeDensity) {
        if (base != null) {
            try {
                return base.getUserBadgedDrawableForDensity(drawable, user, badgeLocation, badgeDensity);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public CharSequence getUserBadgedLabel(@NonNull CharSequence label, @NonNull UserHandle user) {
        if (base != null) {
            try {
                return base.getUserBadgedLabel(label, user);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Override
    public CharSequence getText(@NonNull String packageName, int resId, ApplicationInfo appInfo) {
        if (base != null) {
            try {
                return base.getText(mapPackage(packageName), resId, appInfo);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Override
    public XmlResourceParser getXml(@NonNull String packageName, int resId, ApplicationInfo appInfo) {
        if (base != null) {
            try {
                return base.getXml(mapPackage(packageName), resId, appInfo);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public CharSequence getApplicationLabel(@NonNull ApplicationInfo info) {
        if (base != null) {
            try {
                return base.getApplicationLabel(info);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public Resources getResourcesForActivity(@NonNull ComponentName activityName) {
        if (base != null) {
            try {
                return base.getResourcesForActivity(activityName);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public Resources getResourcesForApplication(@NonNull ApplicationInfo app) {
        if (base != null) {
            try {
                return base.getResourcesForApplication(app);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    @Override
    public Resources getResourcesForApplication(@NonNull String packageName) {
        if (base != null) {
            try {
                return base.getResourcesForApplication(mapPackage(packageName));
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Override
    public void verifyPendingInstall(int id, int verificationCode) {
        if (base != null) {
            try {
                base.verifyPendingInstall(id, verificationCode);
                return;
            } catch (Throwable ignored) {
            }
        }
        
    }

    @Override
    public void extendVerificationTimeout(int id, int verificationCodeAtTimeout, long millisecondsToDelay) {
        if (base != null) {
            try {
                base.extendVerificationTimeout(id, verificationCodeAtTimeout, millisecondsToDelay);
                return;
            } catch (Throwable ignored) {
            }
        }
        
    }

    @Override
    public void setInstallerPackageName(@NonNull String targetPackage, String installerPackageName) {
        if (base != null) {
            try {
                base.setInstallerPackageName(targetPackage, installerPackageName);
                return;
            } catch (Throwable ignored) {
            }
        }
        
    }

    @Override
    public String getInstallerPackageName(@NonNull String packageName) {
        if (base != null) {
            try {
                String installer = base.getInstallerPackageName(mapPackage(packageName));
                return installer != null ? installer : "";
            } catch (Throwable ignored) {
            }
        }
        return "";
    }

    @Override
    public void addPackageToPreferred(@NonNull String packageName) {
        if (base != null) {
            try {
                base.addPackageToPreferred(mapPackage(packageName));
                return;
            } catch (Throwable ignored) {
            }
        }
        
    }

    @Override
    public void removePackageFromPreferred(@NonNull String packageName) {
        if (base != null) {
            try {
                base.removePackageFromPreferred(mapPackage(packageName));
                return;
            } catch (Throwable ignored) {
            }
        }
        
    }

    @NonNull
    @Override
    public List<PackageInfo> getPreferredPackages(int flags) {
        if (base != null) {
            try {
                return base.getPreferredPackages(flags);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Override
    public void addPreferredActivity(@NonNull IntentFilter filter, int match, ComponentName[] set, @NonNull ComponentName activity) {
        if (base != null) {
            try {
                base.addPreferredActivity(filter, match, set, activity);
                return;
            } catch (Throwable ignored) {
            }
        }
        
    }

    @Override
    public void clearPackagePreferredActivities(@NonNull String packageName) {
        if (base != null) {
            try {
                base.clearPackagePreferredActivities(mapPackage(packageName));
                return;
            } catch (Throwable ignored) {
            }
        }
        
    }

    @Override
    public int getPreferredActivities(@NonNull List<IntentFilter> outFilters, @NonNull List<ComponentName> outActivities, String packageName) {
        if (base != null) {
            try {
                return base.getPreferredActivities(outFilters, outActivities, mapPackage(packageName));
            } catch (Throwable ignored) {
            }
        }
        return 0;
    }

    @Override
    public void setComponentEnabledSetting(@NonNull ComponentName componentName, int newState, int flags) {
        if (base != null) {
            try {
                base.setComponentEnabledSetting(componentName, newState, flags);
                return;
            } catch (Throwable ignored) {
            }
        }
        
    }

    @Override
    public int getComponentEnabledSetting(@NonNull ComponentName componentName) {
        if (base != null) {
            try {
                return base.getComponentEnabledSetting(componentName);
            } catch (Throwable ignored) {
            }
        }
        return android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
    }

    @Override
    public void setApplicationEnabledSetting(@NonNull String packageName, int newState, int flags) {
        if (base != null) {
            try {
                base.setApplicationEnabledSetting(mapPackage(packageName), newState, flags);
                return;
            } catch (Throwable ignored) {
            }
        }
        
    }

    @Override
    public int getApplicationEnabledSetting(@NonNull String packageName) {
        if (base != null) {
            try {
                return base.getApplicationEnabledSetting(mapPackage(packageName));
            } catch (Throwable ignored) {
            }
        }
        return android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
    }

    @Override
    public boolean isSafeMode() {
        if (base != null) {
            try {
                return base.isSafeMode();
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    @Override
    public void setApplicationCategoryHint(@NonNull String packageName, int categoryHint) {
        if (base != null) {
            try {
                base.setApplicationCategoryHint(mapPackage(packageName), categoryHint);
                return;
            } catch (Throwable ignored) {
            }
        }
        
    }

    @NonNull
    @Override
    public PackageInstaller getPackageInstaller() {
        if (base != null) {
            try {
                return base.getPackageInstaller();
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Override
    public boolean canRequestPackageInstalls() {
        if (base != null) {
            try {
                return base.canRequestPackageInstalls();
            } catch (Throwable ignored) {
            }
        }
        return false;
    }
}
