package com.example.lifequest.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.lifequest.data.local.entity.AllowedApp

object AppUtils {
    // インストール済みアプリ一覧（システムアプリなどを適宜除外）
    fun getInstalledApps(context: Context): List<AllowedApp> {
        val pm = context.packageManager
        val apps = pm.getInstalledPackages(PackageManager.GET_META_DATA)
        val allowedApps = mutableListOf<AllowedApp>()

        for (packageInfo in apps) {
            // ランチャー起動可能なアプリのみ対象
            if (pm.getLaunchIntentForPackage(packageInfo.packageName) != null) {
                // ★修正: applicationInfo が null の場合はスキップする (?: continue)
                val appInfo = packageInfo.applicationInfo ?: continue

                // システムアプリを除外したい場合 (アップデートされたシステムアプリは許可するなど調整可)
                // if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) { ... }

                val label = pm.getApplicationLabel(appInfo).toString()
                allowedApps.add(AllowedApp(packageInfo.packageName, label))
            }
        }
        return allowedApps.sortedBy { it.label }
    }

    // デフォルトのホームアプリ（ランチャー）のパッケージ名を取得
    fun getDefaultLauncherPackage(context: Context): String? {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }
}