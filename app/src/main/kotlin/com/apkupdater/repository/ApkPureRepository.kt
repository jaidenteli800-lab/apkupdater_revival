package com.apkupdater.repository

import android.util.Log
import com.apkupdater.data.apkpure.AppInfoForUpdate
import com.apkupdater.data.apkpure.AppUpdateResponse
import com.apkupdater.data.apkpure.DeviceHeader
import com.apkupdater.data.apkpure.GetAppUpdate
import com.apkupdater.data.apkpure.toAppUpdate
import com.apkupdater.data.ui.AppInstalled
import com.apkupdater.data.ui.getApp
import com.apkupdater.data.ui.getSignature
import com.apkupdater.prefs.Prefs
import com.apkupdater.service.ApkPureService
import com.google.gson.Gson
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow


class ApkPureRepository(
    gson: Gson,
    private val service: ApkPureService,
    private val prefs: Prefs,
) {

    private val header = gson.toJson(DeviceHeader())

    fun updates(apps: List<AppInstalled>) = flow {
        val info = apps.map { AppInfoForUpdate(it.packageName, it.versionCode) }
        val r = service.getAppUpdate(header, GetAppUpdate(info))
        val updates = r.app_update_response.asSequence()
            .filter { filterSignature(it.sign, apps.getSignature(it.package_name)) }
            .filter { filterPreRelease(it) }
            .filter { filterAlpha(it) }
            .filter { filterBeta(it) }
            .map { it.toAppUpdate(apps.getApp(it.package_name)) }
            .toList()
        emit(updates)
    }.catch {
        Log.e("ApkPureRepository", it.message, it)
        emit(emptyList())
    }

    fun search(text: String) = flow {
        val response = service.search(header, text)
        val info = response.data.data.asSequence()
            .mapNotNull { d ->
                d.data.firstOrNull()?.takeIf { !it.ad }?.app_info?.let {
                    AppInfoForUpdate(package_name = it.package_name, version_code = 0L, is_system = false)
                }
            }
            .toList()
        val r = service.getAppUpdate(header, GetAppUpdate(info))
        val updates = r.app_update_response.asSequence()
            .filter { filterAlpha(it) }
            .filter { filterBeta(it) }
            .map { it.toAppUpdate(null) }
            .toList()
        emit(Result.success(updates))
    }.catch {
        Log.e("ApkPureRepository", it.message, it)
        emit(Result.failure(it))
    }

    private fun filterPreRelease(update: AppUpdateResponse): Boolean {
        val name = update.version_name.lowercase()
        return when {
            prefs.ignorePreRelease.get() && (name.contains("alpha") || name.contains("beta") || name.contains("rc") || name.contains("dev") || name.contains("preview")) -> false
            prefs.ignoreAlpha.get() && name.contains("alpha") -> false
            prefs.ignoreBeta.get() && name.contains("beta") -> false
            else -> true
        }
    }

    private fun filterAlpha(update: AppUpdateResponse) = when {
        prefs.ignoreAlpha.get() && update.version_name.contains(other = "alpha", ignoreCase = true) -> false
        else -> true
    }

    private fun filterBeta(update: AppUpdateResponse) = when {
        prefs.ignoreBeta.get() && update.version_name.contains(other = "beta", ignoreCase = true) -> false
        else -> true
    }

    private fun filterSignature(signatures: List<String>, signature: String) = when {
        signatures.isEmpty() -> true
        signatures.contains(signature) -> true
        else -> false
    }

}
