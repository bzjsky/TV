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
import android.content.pm.SigningInfo;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.List;

public class Hook extends PackageManager {

    public static final String FONGMI_SIGN = "308203563082023ea003020102020900c4bfe2a129fb155a300d06092a864886f70d01010b05003058310b300906035504061302434e3110300e06035504081307556e6b6e6f776e3110300e06035504071307556e6b6e6f776e310b3009060355040a13025456310b3009060355040b13025456310b30090603550403130254563020170d3236313030383131323531345a180f32303534303232333131323531345a3058310b300906035504061302434e3110300e06035504081307556e6b6e6f776e3110300e06035504071307556e6b6e6f776e310b3009060355040a13025456310b3009060355040b13025456310b300906035504031302545630820122300d06092a864886f70d01010105000382010f003082010a0282010100bb53275c9bbda22271b982bba82007e8b6facf64d39aee916d26535bdbd36e5886998b9762540cacff9b5e9963d82c95c306799f93dbcf367cfb9190ada5cd52af78a66f43a0a64f5946853976aceba9b4cc6fa0d17be56de8856d78be99d9ef19dd3bf1a225e2a2ab451fc08be8b889d21e917732534ab7facc5167d0c94115d9b5c137faf37bf1aba60a7abfc7f4f878c6a8470e08fcc757853ce3e5a74350af7485c6bb44b1a9584fa4844a39e38fa5866bc1deb30d8a27b0b80d34089a630da72a52fc6083d5adc4243373474840bd98bcfe4fb452d94f11902d8da735411941620d140213bcfaa4e9d285c8a992bbfa9240a59d350da380d81a8a4ee3fd0203010001a321301f301d0603551d0e04160414163b0ca17ae5cc4fef9baafb722a78f98bdbc1d2300d06092a864886f70d01010b0500038201010015b7ad2284a60d354ef7304add6f0a0b94cb4fc25dbbe2ba125df5c2e94b368f0e1d2d6e08833fc3e177be571b69b8202a495872e66af276bab79a473b0d272f21ce778bdea37c03427dce9a7e5729ac7c74b0421e889736d730f34add2706cab1705efc3d965306c5734829d2a2ce15f1f406cfa0e41abee05dc605e6ce273ba1d8ab9b0c83221a66bd5273cb1f3754938a2fb6c3c0a73e7b188eff410006159b4b0a57a6c7bb5f227da11c7e2243084f05c798e5b51da8df3285551230b1e64746cb7aa55cc74ad259726cc8ddb132834c8844acf379b149ea1cbf415ce7a8b85318a752ff7bfb36d5d08d5c3c76cdf2374cd0193653766f54cd39b8e1c900";

    private final PackageManager base;
    private final String sign;
    private final String pkgn;

    public Hook(String sign, String pkgn) {
        this(null, sign, pkgn);
    }

    public Hook(PackageManager base, String pkgn) {
        this(base, FONGMI_SIGN, pkgn);
    }

    public Hook(PackageManager base, String sign, String pkgn) {
        this.base = base;
        this.sign = sign != null ? sign : FONGMI_SIGN;
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

    private void injectSignature(PackageInfo info, int flags) {
        if (info == null) return;
        info.signatures = new Signature[]{new Signature(sign)};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && (flags & PackageManager.GET_SIGNING_CERTIFICATES) != 0) {
            try {
                SigningInfo signingInfo = new SigningInfo();
                for (java.lang.reflect.Field field : SigningInfo.class.getDeclaredFields()) {
                    field.setAccessible(true);
                    if ("mSigningDetails".equals(field.getName())) {
                        Object details = field.get(signingInfo);
                        if (details != null) {
                            for (java.lang.reflect.Field df : details.getClass().getDeclaredFields()) {
                                df.setAccessible(true);
                                if (df.getType() == Signature[].class) df.set(details, info.signatures);
                            }
                        }
                    }
                }
                info.signingInfo = signingInfo;
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public PackageInfo getPackageInfo(@NonNull String packageName, int flags) {
        PackageInfo info = null;
        if (base != null) {
            try {
                info = base.getPackageInfo(mapPackage(packageName), flags);
            } catch (Throwable ignored) {
            }
        }
        if (info == null) {
            info = new PackageInfo();
            info.packageName = packageName;
            info.versionName = "5.6.1";
            info.versionCode = 561;
        }
        if ("com.fongmi.android.tv".equals(packageName) || "com.fongmi.android.mobile".equals(packageName)) {
            info.packageName = packageName;
        }
        ensureMetadata(info, packageName);
        injectSignature(info, flags);
        return info;
    }

    @NonNull
    @Override
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public PackageInfo getPackageInfo(@NonNull String packageName, @NonNull PackageInfoFlags flags) {
        return getPackageInfo(packageName, (int) flags.getValue());
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.O)
    public PackageInfo getPackageInfo(@NonNull VersionedPackage versionedPackage, int flags) {
        return getPackageInfo(versionedPackage.getPackageName(), flags);
    }

    @NonNull
    @Override
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public PackageInfo getPackageInfo(@NonNull VersionedPackage versionedPackage, @NonNull PackageInfoFlags flags) {
        return getPackageInfo(versionedPackage.getPackageName(), (int) flags.getValue());
    }

    @Override
    public String[] currentToCanonicalPackageNames(@NonNull String[] packageNames) {
        return new String[0];
    }

    @Override
    public String[] canonicalToCurrentPackageNames(@NonNull String[] packageNames) {
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
        return null;
    }

    @Override
    public int[] getPackageGids(@NonNull String packageName) {
        return new int[0];
    }

    @Override
    public int[] getPackageGids(@NonNull String packageName, int flags) {
        return new int[0];
    }

    @Override
    public int getPackageUid(@NonNull String packageName, int flags) {
        return 0;
    }

    @Override
    public PermissionInfo getPermissionInfo(@NonNull String permName, int flags) {
        return null;
    }

    @NonNull
    @Override
    public List<PermissionInfo> queryPermissionsByGroup(String permissionGroup, int flags) {
        return null;
    }

    @NonNull
    @Override
    public PermissionGroupInfo getPermissionGroupInfo(@NonNull String groupName, int flags) {
        return null;
    }

    @NonNull
    @Override
    public List<PermissionGroupInfo> getAllPermissionGroups(int flags) {
        return null;
    }

    @NonNull
    @Override
    public ApplicationInfo getApplicationInfo(@NonNull String packageName, int flags) {
        ApplicationInfo info = null;
        if (base != null) {
            try {
                info = base.getApplicationInfo(mapPackage(packageName), flags);
            } catch (Throwable ignored) {
            }
        }
        if (info == null) {
            info = new ApplicationInfo();
            info.packageName = packageName;
        }
        if ("com.fongmi.android.tv".equals(packageName) || "com.fongmi.android.mobile".equals(packageName)) {
            info.packageName = packageName;
        }
        if (info.metaData == null) info.metaData = new Bundle();
        return info;
    }

    @NonNull
    @Override
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public ApplicationInfo getApplicationInfo(@NonNull String packageName, @NonNull ApplicationInfoFlags flags) {
        return getApplicationInfo(packageName, (int) flags.getValue());
    }

    @NonNull
    @Override
    public ActivityInfo getActivityInfo(@NonNull ComponentName component, int flags) {
        return null;
    }

    @NonNull
    @Override
    public ActivityInfo getReceiverInfo(@NonNull ComponentName component, int flags) {
        return null;
    }

    @NonNull
    @Override
    public ServiceInfo getServiceInfo(@NonNull ComponentName component, int flags) {
        return null;
    }

    @NonNull
    @Override
    public ProviderInfo getProviderInfo(@NonNull ComponentName component, int flags) {
        return null;
    }

    @NonNull
    @Override
    public List<PackageInfo> getInstalledPackages(int flags) {
        return null;
    }

    @NonNull
    @Override
    public List<PackageInfo> getPackagesHoldingPermissions(@NonNull String[] permissions, int flags) {
        return null;
    }

    @Override
    public int checkPermission(@NonNull String permName, @NonNull String packageName) {
        return android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public boolean isPermissionRevokedByPolicy(@NonNull String permName, @NonNull String packageName) {
        return false;
    }

    @Override
    public boolean addPermission(@NonNull PermissionInfo info) {
        return false;
    }

    @Override
    public boolean addPermissionAsync(@NonNull PermissionInfo info) {
        return false;
    }

    @Override
    public void removePermission(@NonNull String permName) {
    }

    @Override
    public int checkSignatures(@NonNull String packageName1, @NonNull String packageName2) {
        return android.content.pm.PackageManager.SIGNATURE_MATCH;
    }

    @Override
    public int checkSignatures(int uid1, int uid2) {
        return android.content.pm.PackageManager.SIGNATURE_MATCH;
    }

    @Override
    public String[] getPackagesForUid(int uid) {
        return new String[0];
    }

    @Override
    public String getNameForUid(int uid) {
        return null;
    }

    @NonNull
    @Override
    public List<ApplicationInfo> getInstalledApplications(int flags) {
        return null;
    }

    @Override
    public boolean isInstantApp() {
        return false;
    }

    @Override
    public boolean isInstantApp(@NonNull String packageName) {
        return false;
    }

    @Override
    public int getInstantAppCookieMaxBytes() {
        return 0;
    }

    @NonNull
    @Override
    public byte[] getInstantAppCookie() {
        return new byte[0];
    }

    @Override
    public void clearInstantAppCookie() {
    }

    @Override
    public void updateInstantAppCookie(byte[] cookie) {
    }

    @Override
    public String[] getSystemSharedLibraryNames() {
        return new String[0];
    }

    @NonNull
    @Override
    public List<SharedLibraryInfo> getSharedLibraries(int flags) {
        return null;
    }

    @Override
    public ChangedPackages getChangedPackages(int sequenceNumber) {
        return null;
    }

    @NonNull
    @Override
    public FeatureInfo[] getSystemAvailableFeatures() {
        return new FeatureInfo[0];
    }

    @Override
    public boolean hasSystemFeature(@NonNull String featureName) {
        return false;
    }

    @Override
    public boolean hasSystemFeature(@NonNull String featureName, int version) {
        return false;
    }

    @Override
    public ResolveInfo resolveActivity(@NonNull Intent intent, int flags) {
        return null;
    }

    @NonNull
    @Override
    public List<ResolveInfo> queryIntentActivities(@NonNull Intent intent, int flags) {
        return null;
    }

    @NonNull
    @Override
    public List<ResolveInfo> queryIntentActivityOptions(ComponentName caller, Intent[] specifics, @NonNull Intent intent, int flags) {
        return null;
    }

    @NonNull
    @Override
    public List<ResolveInfo> queryBroadcastReceivers(@NonNull Intent intent, int flags) {
        return null;
    }

    @Override
    public ResolveInfo resolveService(@NonNull Intent intent, int flags) {
        return null;
    }

    @NonNull
    @Override
    public List<ResolveInfo> queryIntentServices(@NonNull Intent intent, int flags) {
        return null;
    }

    @NonNull
    @Override
    public List<ResolveInfo> queryIntentContentProviders(Intent intent, int flags) {
        return null;
    }

    @Override
    public ProviderInfo resolveContentProvider(@NonNull String authority, int flags) {
        return null;
    }

    @NonNull
    @Override
    public List<ProviderInfo> queryContentProviders(String processName, int uid, int flags) {
        return null;
    }

    @NonNull
    @Override
    public InstrumentationInfo getInstrumentationInfo(@NonNull ComponentName className, int flags) {
        return null;
    }

    @NonNull
    @Override
    public List<InstrumentationInfo> queryInstrumentation(@NonNull String targetPackage, int flags) {
        return null;
    }

    @Override
    public Drawable getDrawable(@NonNull String packageName, int resId, ApplicationInfo appInfo) {
        return null;
    }

    @NonNull
    @Override
    public Drawable getActivityIcon(@NonNull ComponentName activityName) {
        return null;
    }

    @NonNull
    @Override
    public Drawable getActivityIcon(@NonNull Intent intent) {
        return null;
    }

    @Override
    public Drawable getActivityBanner(@NonNull ComponentName activityName) {
        return null;
    }

    @Override
    public Drawable getActivityBanner(@NonNull Intent intent) {
        return null;
    }

    @NonNull
    @Override
    public Drawable getDefaultActivityIcon() {
        return null;
    }

    @NonNull
    @Override
    public Drawable getApplicationIcon(@NonNull ApplicationInfo info) {
        return null;
    }

    @NonNull
    @Override
    public Drawable getApplicationIcon(@NonNull String packageName) {
        return null;
    }

    @Override
    public Drawable getApplicationBanner(@NonNull ApplicationInfo info) {
        return null;
    }

    @Override
    public Drawable getApplicationBanner(@NonNull String packageName) {
        return null;
    }

    @Override
    public Drawable getActivityLogo(@NonNull ComponentName activityName) {
        return null;
    }

    @Override
    public Drawable getActivityLogo(@NonNull Intent intent) {
        return null;
    }

    @Override
    public Drawable getApplicationLogo(@NonNull ApplicationInfo info) {
        return null;
    }

    @Override
    public Drawable getApplicationLogo(@NonNull String packageName) {
        return null;
    }

    @NonNull
    @Override
    public Drawable getUserBadgedIcon(@NonNull Drawable drawable, @NonNull UserHandle user) {
        return null;
    }

    @NonNull
    @Override
    public Drawable getUserBadgedDrawableForDensity(@NonNull Drawable drawable, @NonNull UserHandle user, Rect badgeLocation, int badgeDensity) {
        return null;
    }

    @NonNull
    @Override
    public CharSequence getUserBadgedLabel(@NonNull CharSequence label, @NonNull UserHandle user) {
        return null;
    }

    @Override
    public CharSequence getText(@NonNull String packageName, int resId, ApplicationInfo appInfo) {
        return null;
    }

    @Override
    public XmlResourceParser getXml(@NonNull String packageName, int resId, ApplicationInfo appInfo) {
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
        return "TV";
    }

    @NonNull
    @Override
    public Resources getResourcesForActivity(@NonNull ComponentName activityName) {
        return null;
    }

    @NonNull
    @Override
    public Resources getResourcesForApplication(@NonNull ApplicationInfo app) {
        return null;
    }

    @NonNull
    @Override
    public Resources getResourcesForApplication(@NonNull String packageName) {
        return null;
    }

    @Override
    public void verifyPendingInstall(int id, int verificationCode) {
    }

    @Override
    public void extendVerificationTimeout(int id, int verificationCodeAtTimeout, long millisecondsToDelay) {
    }

    @Override
    public void setInstallerPackageName(@NonNull String targetPackage, String installerPackageName) {
    }

    @Override
    public String getInstallerPackageName(@NonNull String packageName) {
        return null;
    }

    @Override
    public void addPackageToPreferred(@NonNull String packageName) {
    }

    @Override
    public void removePackageFromPreferred(@NonNull String packageName) {
    }

    @NonNull
    @Override
    public List<PackageInfo> getPreferredPackages(int flags) {
        return null;
    }

    @Override
    public void addPreferredActivity(@NonNull IntentFilter filter, int match, ComponentName[] set, @NonNull ComponentName activity) {
    }

    @Override
    public void clearPackagePreferredActivities(@NonNull String packageName) {
    }

    @Override
    public int getPreferredActivities(@NonNull List<IntentFilter> outFilters, @NonNull List<ComponentName> outActivities, String packageName) {
        return 0;
    }

    @Override
    public void setComponentEnabledSetting(@NonNull ComponentName componentName, int newState, int flags) {
    }

    @Override
    public int getComponentEnabledSetting(@NonNull ComponentName componentName) {
        return android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
    }

    @Override
    public void setApplicationEnabledSetting(@NonNull String packageName, int newState, int flags) {
    }

    @Override
    public int getApplicationEnabledSetting(@NonNull String packageName) {
        return android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
    }

    @Override
    public boolean isSafeMode() {
        return false;
    }

    @Override
    public void setApplicationCategoryHint(@NonNull String packageName, int categoryHint) {
    }

    @NonNull
    @Override
    public PackageInstaller getPackageInstaller() {
        return null;
    }

    @Override
    public boolean canRequestPackageInstalls() {
        return false;
    }
}
