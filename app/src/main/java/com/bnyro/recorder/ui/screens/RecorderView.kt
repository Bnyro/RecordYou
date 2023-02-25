package com.bnyro.recorder.ui.screens

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.res.Configuration
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bnyro.recorder.R
import com.bnyro.recorder.enums.Recorder
import com.bnyro.recorder.enums.RecorderState
import com.bnyro.recorder.ui.common.ClickableIcon
import com.bnyro.recorder.ui.components.AudioVisualizer
import com.bnyro.recorder.ui.components.SettingsBottomSheet
import com.bnyro.recorder.ui.models.RecorderModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecorderView(
    initialRecorder: Recorder
) {
    val recorderModel: RecorderModel = viewModel()
    val context = LocalContext.current
    val mProjectionManager =
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    val orientation = LocalConfiguration.current.orientation

    var showBottomSheet by remember {
        mutableStateOf(false)
    }
    var showPlayerScreen by remember {
        mutableStateOf(false)
    }
    var recordScreenMode by remember {
        mutableStateOf(false)
    }

    val requestRecording = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@rememberLauncherForActivityResult
        recorderModel.startVideoRecorder(context, result)
    }

    fun requestScreenRecording() {
        if (!recorderModel.hasScreenRecordingPermissions(context)) return
        requestRecording.launch(
            mProjectionManager.createScreenCaptureIntent()
        )
    }

    LaunchedEffect(Unit) {
        when (initialRecorder) {
            Recorder.AUDIO -> {
                recorderModel.startAudioRecorder(context)
            }

            Recorder.SCREEN -> {
                recordScreenMode = true
                requestScreenRecording()
            }

            Recorder.NONE -> {}
        }
    }

    Scaffold { pV ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(pV)
        ) {
            if (recorderModel.recordedAmplitudes.isNotEmpty()) {
                AudioVisualizer(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 80.dp)
                )
            } else {
                Text(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(
                            top = if (orientation == Configuration.ORIENTATION_LANDSCAPE) 50.dp else 200.dp
                        ),
                    text = stringResource(
                        if (recordScreenMode) R.string.record_screen else R.string.record_sound
                    ),
                    fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                    fontWeight = MaterialTheme.typography.headlineLarge.fontWeight
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 25.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                recorderModel.recordedTime?.let {
                    Text(
                        text = DateUtils.formatElapsedTime(it),
                        fontSize = MaterialTheme.typography.titleMedium.fontSize
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ClickableIcon(imageVector = Icons.Default.Settings) {
                        showBottomSheet = true
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    FloatingActionButton(
                        onClick = {
                            when {
                                recorderModel.recorderState == RecorderState.ACTIVE -> recorderModel.stopRecording()
                                recordScreenMode -> requestScreenRecording()
                                else -> recorderModel.startAudioRecorder(context)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = when {
                                recorderModel.recorderState == RecorderState.ACTIVE -> Icons.Default.Stop
                                recordScreenMode -> Icons.Default.Videocam
                                else -> Icons.Default.Mic
                            },
                            contentDescription = null
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && recorderModel.recorderState != RecorderState.IDLE) {
                        ClickableIcon(
                            imageVector = if (recorderModel.recorderState == RecorderState.PAUSED) {
                                Icons.Default.PlayArrow
                            } else {
                                Icons.Default.Pause
                            }
                        ) {
                            if (recorderModel.recorderState == RecorderState.PAUSED) {
                                recorderModel.resumeRecording()
                            } else {
                                recorderModel.pauseRecording()
                            }
                        }
                    } else {
                        ClickableIcon(
                            imageVector = Icons.Default.VideoLibrary
                        ) {
                            showPlayerScreen = true
                        }
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))
                ClickableIcon(
                    imageVector = if (recordScreenMode) Icons.Default.ExpandMore else Icons.Default.ExpandLess
                ) {
                    recordScreenMode = !recordScreenMode
                }
            }
        }
    }

    if (showBottomSheet) {
        SettingsBottomSheet {
            showBottomSheet = false
        }
    }
    if (showPlayerScreen) {
        PlayerScreen {
            showPlayerScreen = false
        }
    }
}
