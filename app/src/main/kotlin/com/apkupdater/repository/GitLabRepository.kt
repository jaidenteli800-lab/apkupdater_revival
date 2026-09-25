package com.apkupdater.repository

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.apkupdater.data.gitlab.GitLabApps
import com.apkupdater.data.gitlab.GitLabAssets
import com.apkupdater.data.ui.AppInstalled
import com.apkupdater.data.ui.AppUpdate
import com.apkupdater.data.ui.GitLabSource
import com.apkupdater.data.ui.Link
import com.apkupdater.data.ui.getApp
import com.apkupdater.service.GitLabService
import com.apkupdater.util.combine
import com.apkupdater.util.filterVersionTag
import io.github.g00fy2.versioncompare.Version
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow


class GitLabRepository(
    private val service: GitLabService,
) {

    fun updates(apps: List<AppInstalled>) = flow {
        val checks = mutableListOf<Flow<List<AppUpdate>>>()

        GitLabApps.forEach { app ->
            apps.find { it.packageName == app.packageName }?.let {
                checks.add(checkApp(apps, app.user, app.repo, app.packageName, it.version))
            }
        }

        if (checks.isEmpty()) {
            emit(emptyList())
        } else {
            checks.combine { all ->
                emit(all.flatMap { it })
            }.collect()
        }
    }.catch {
        emit(emptyList())
        Log.e("GitLabRepository", "Error fetching releases.", it)
    }

    fun search(text: String) = flow {
        val checks = mutableListOf<Flow<List<AppUpdate>>>()

        GitLabApps.forEach { app ->
            if (app.repo.contains(other = text, ignoreCase = true) 
                || app.user.contains(other = text, ignoreCase = true) 
                || app.packageName.contains(other = text, ignoreCase = true)) {
                checks.add(checkApp(null, app.user, app.repo, app.packageName, "?"))
            }
        }

        if (checks.isEmpty()) {
            emit(Result.success(emptyList()))
        } else {
            checks.combine { all ->
                val r = all.flatMap { it }
                emit(Result.success(r))
            }.collect()
        }
    }.catch {
        emit(Result.failure(it))
        Log.e("GitLabRepository", "Error searching.", it)
    }

    private fun checkApp(
        apps: List<AppInstalled>?,
        user: String,
        repo: String,
        packageName: String,
        currentVersion: String
    ) = flow {
        val releases = service.getReleases(user, repo)

        if (releases.isNotEmpty() && Version(filterVersionTag(releases[0].tag_name)) > Version(currentVersion)) {
            val app = apps?.getApp(packageName)
            val apk = findApkAsset(releases[0].assets)
            if (apk.isNotEmpty()) {
                emit(listOf(
                    AppUpdate(
                        name = repo,
                        packageName = packageName,
                        version = releases[0].tag_name,
                        oldVersion = app?.version ?: "?",
                        versionCode = 0L,
                        oldVersionCode = app?.versionCode ?: 0L,
                        source = GitLabSource,
                        link = Link.Url(apk),
                        whatsNew = releases[0].description,
                        iconUri = if (apps == null) releases[0].author.avatar_url.toUri() else Uri.EMPTY
                    )
                ))
            } else {
                emit(emptyList())
            }
        } else {
            emit(emptyList())
        }
    }.catch {
        emit(emptyList())
        Log.e("GitLabRepository", "Error fetching releases for $packageName.", it)
    }

    private fun findApkAsset(assets: GitLabAssets): String {
        return assets.links.find { it.url.endsWith(".apk", ignoreCase = true) }?.url.orEmpty()
    }

}
