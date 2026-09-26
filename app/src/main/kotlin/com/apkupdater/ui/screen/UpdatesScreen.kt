package com.apkupdater.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apkupdater.R
import com.apkupdater.data.ui.AppUpdate
import com.apkupdater.data.ui.UpdateStage
import com.apkupdater.data.ui.UpdatesUiState
import com.apkupdater.prefs.Prefs
import com.apkupdater.ui.component.EmptyGrid
import com.apkupdater.ui.component.InstalledGrid
import com.apkupdater.ui.component.RefreshIcon
import com.apkupdater.ui.component.TvInstalledGrid
import com.apkupdater.ui.component.TvUpdateItem
import com.apkupdater.ui.component.UpdateItem
import com.apkupdater.ui.theme.statusBarColor
import com.apkupdater.util.formatBytes
import com.apkupdater.viewmodel.InstallDialogState
import com.apkupdater.viewmodel.UpdatesViewModel
import org.koin.compose.koinInject


@Composable
fun UpdatesScreen(viewModel: UpdatesViewModel) {
	LaunchedEffect(Unit) {
		viewModel.refresh(load = false)
	}

	val dialogState by viewModel.dialogState.collectAsStateWithLifecycle()
	when (val dialog = dialogState) {
		is InstallDialogState.PersistentApp -> {
			AlertDialog(
				onDismissRequest = { viewModel.dismissDialog() },
				title = { Text("Cannot Install Persistent App") },
				text = { Text("This is a persistent system app (${dialog.appName}) and cannot be updated directly. Persistent apps are protected by Android OS and cannot be replaced via standard package installation.") },
				confirmButton = {
					Button(onClick = { viewModel.dismissDialog() }) {
						Text("OK")
					}
				}
			)
		}
		is InstallDialogState.PermissionRequired -> {
			AlertDialog(
				onDismissRequest = { viewModel.dismissDialog() },
				title = { Text("Permission Required") },
				text = { Text("APKUpdater needs permission to install unknown apps. Press Continue to open Android System Settings, enable 'Allow from this source' for APKUpdater, then return to complete installation.") },
				confirmButton = {
					Button(onClick = { viewModel.openPermissionSettings() }) {
						Text("Continue to Settings")
					}
				},
				dismissButton = {
					Button(onClick = { viewModel.dismissDialog() }) {
						Text("Cancel")
					}
				}
			)
		}
		else -> {}
	}

	when (val state = viewModel.state().collectAsStateWithLifecycle().value) {
		is UpdatesUiState.Loading -> UpdatesScreenLoading(viewModel, state.stage, state.progress)
		is UpdatesUiState.Error -> UpdatesScreenError(viewModel, state.message)
		is UpdatesUiState.Success -> UpdatesScreenSuccess(viewModel, state.updates)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesTopBar(viewModel: UpdatesViewModel) = TopAppBar(
	title = {
		Text(stringResource(R.string.tab_updates))
	},
	colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.statusBarColor()),
	windowInsets = WindowInsets(0),
	actions = {
		Button(
			onClick = { viewModel.installAll() },
			modifier = Modifier.padding(end = 4.dp)
		) {
			Text("UPDATE ALL")
		}
		IconButton(onClick = { viewModel.refresh() }) {
			RefreshIcon(stringResource(R.string.refresh_updates))
		}
	},
	navigationIcon = {
		Box(Modifier.minimumInteractiveComponentSize().size(40.dp), Alignment.Center) {
			Icon(Icons.Filled.ThumbUp, "Tab Icon")
		}
	},
)

@Composable
fun UpdatesScreenLoading(viewModel: UpdatesViewModel, stage: UpdateStage, progress: Float) = Column {
	UpdatesTopBar(viewModel)
	Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			modifier = Modifier.padding(16.dp),
		) {
			Text(
				text = when (stage) {
					UpdateStage.CONNECTING -> stringResource(R.string.update_stage_connecting)
					UpdateStage.FETCHING -> stringResource(R.string.update_stage_fetching)
					UpdateStage.CHECKING -> stringResource(R.string.update_stage_checking)
					UpdateStage.READY -> stringResource(R.string.update_stage_ready)
				},
				style = MaterialTheme.typography.bodyLarge
			)
			Spacer(modifier = Modifier.height(16.dp))
			LinearProgressIndicator(
				progress = { progress },
				modifier = Modifier.fillMaxWidth().height(8.dp),
			)
		}
	}
}

@Composable
fun UpdatesScreenError(viewModel: UpdatesViewModel, message: String?) = Column {
	UpdatesTopBar(viewModel)
	Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
		Column(horizontalAlignment = Alignment.CenterHorizontally) {
			Text(text = message ?: stringResource(R.string.something_went_wrong))
			Spacer(modifier = Modifier.height(16.dp))
			Button(onClick = { viewModel.refresh() }) {
				Text(stringResource(R.string.retry))
			}
		}
	}
}

@Composable
fun UpdatesScreenSuccess(
	viewModel: UpdatesViewModel,
	updates: List<AppUpdate>
) = Column {
	val handler = LocalUriHandler.current
	val prefs = koinInject<Prefs>()
	val tv = prefs.androidTvUi.get()
	val isRoot = prefs.rootInstall.get()

	UpdatesTopBar(viewModel)

    val installingApps = updates.filter { (it.isInstalling || it.error != null) }
    AnimatedVisibility(
        visible = installingApps.isNotEmpty(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                .padding(12.dp)
        ) {
            if (!isRoot) {
                Text(
                    text = "Please stay in the app during updates to manually approve each package installation prompt.",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Text(
                text = "Global Progress",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            installingApps.forEach { app ->
                Column(
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = app.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = { viewModel.cancelInstall(app.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(16.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    app.error?.let { errorMsg ->
                         Text(
                            text = errorMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    } ?: run {
                        val progressValue = if (app.total > 0) app.progress.toFloat() / app.total.toFloat() else 0f
                        val progressPercent = (progressValue * 100).toInt()
                        
                        LinearProgressIndicator(
                            progress = { progressValue },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            strokeCap = StrokeCap.Round
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = app.status,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (app.total > 0) {
                                Text(
                                    text = "${app.progress.formatBytes()} / ${app.total.formatBytes()} ($progressPercent%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

	when {
		updates.isEmpty() -> EmptyGrid(stringResource(R.string.no_updates_found))
		tv -> TvGrid(viewModel, updates, handler)
		!tv -> Grid(viewModel, updates, handler)
	}
}

@Composable
fun TvGrid(
	viewModel: UpdatesViewModel,
	updates: List<AppUpdate>,
	handler: UriHandler
) = TvInstalledGrid {
	items(updates) { update ->
		TvUpdateItem(
			update,
			{ viewModel.install(update, handler) },
			{ viewModel.ignoreVersion(update.id) }
		)
	}
}

@Composable
fun Grid(
	viewModel: UpdatesViewModel,
	updates: List<AppUpdate>,
	handler: UriHandler
) = InstalledGrid {
	items(updates) { update ->
		UpdateItem(update) {
			viewModel.install(update, handler)
		}
	}
}
