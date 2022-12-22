package com.lnight.videoplayerproject.presentation.upload_video_screen

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.*
import androidx.compose.material3.SnackbarResult.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.work.*
import com.google.accompanist.permissions.*
import com.lnight.videoplayerproject.common.WorkerKeys
import com.lnight.videoplayerproject.common.getVideoPath
import com.lnight.videoplayerproject.common.openAppDetails
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun UploadVideoScreen(
    navController: NavController,
    viewModel: MainViewModel,
    first: Boolean
) {
    val permissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberMultiplePermissionsState(
            listOf(
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        )
    } else {
        rememberMultiplePermissionsState(
            listOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        )
    }
    val videoItems = viewModel.videoItems
    val userVideoItems = viewModel.userVideoItems

    val context = LocalContext.current
    val selectVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let(viewModel::addSingleVideo)
        }
    )
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(key1 = lifecycleOwner, key2 = permissionState) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && permissionState.permissions.first().status.isGranted) {
                val urisWithData = context.getVideoPath()
                viewModel.addVideoUri(urisWithData)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val downloadRequest = OneTimeWorkRequestBuilder<DownloadVideoWorker>()
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED
                )
                .build()
        )
        .addTag("DownloadTag")
    val workManager = WorkManager.getInstance(context)

    val workInfo = workManager
        .getWorkInfosByTagLiveData("DownloadTag")
        .observeAsState()
        .value

    val downloadInfo: WorkInfo? = remember(key1 = workInfo) {
        workInfo?.firstOrNull()
    }
    var showDialog by remember {
        mutableStateOf(false)
    }

    var showEdittext by remember {
        mutableStateOf(false)
    }
    var start by remember {
        mutableStateOf(false)
    }

    var showSnackbar by remember {
        mutableStateOf(false)
    }
    DisposableEffect(key1 = lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                if (permissionState.permissions.first().status.shouldShowRationale && permissionState.permissions.size > 2) {
                    permissionState.permissions[1].launchPermissionRequest()
                } else if (!permissionState.permissions.first().status.shouldShowRationale) {
                    permissionState.launchMultiplePermissionRequest()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }

    }

    LaunchedEffect(key1 = permissionState) {
        showSnackbar = !permissionState.permissions.first().status.isGranted
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
            },
            confirmButton = {
                Column {

                    Text(
                        text = "Choose where to get the video from",
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            selectVideoLauncher.launch("video/*")
                            showDialog = false
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "select video from storage",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    Button(
                        onClick = {
                            showEdittext = true
                            showDialog = false
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "download video from url",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        )
    }
    var text by remember { mutableStateOf("") }

    if (showEdittext) {
        AlertDialog(
            onDismissRequest = {
                showEdittext = false
            },
            title = {
                Text(text = "Enter video url")
            },
            text = {
                Column {
                    TextField(
                        value = text,
                        onValueChange = { text = it }
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.padding(all = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val data = Data.Builder()
                                .putString(WorkerKeys.VIDEO_URI, text)
                                .build()
                            downloadRequest.setInputData(data)
                            val work = downloadRequest.build()
                            workManager
                                .beginUniqueWork(
                                    "download",
                                    ExistingWorkPolicy.KEEP,
                                    work
                                )
                                .enqueue()
                            text = ""
                            showEdittext = false
                            start = true
                        }
                    ) {
                        Text("Submit")
                    }

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            text = ""
                            showEdittext = false
                        }
                    ) {
                        Text("Dismiss")
                    }
                }
            }

        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        LaunchedEffect(key1 = downloadInfo) {
            if (downloadInfo?.state == WorkInfo.State.FAILED) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = downloadInfo.outputData.getString(WorkerKeys.ERROR_MSG)
                            ?: "Download failed, try again later"
                    )
                }
                workManager.pruneWork()
            } else if (downloadInfo?.state == WorkInfo.State.SUCCEEDED) {
                val localUri = downloadInfo.outputData.getString(WorkerKeys.LOCAL_VIDEO_URI)
                if (localUri != null) {
                    Log.e("TAG", localUri)
                    viewModel.addSingleVideo(localUri.toUri())
                }
                workManager.pruneWork()
            }
        }

        LaunchedEffect(key1 = showSnackbar) {
            if (showSnackbar && !first) {
                val result = snackbarHostState.showSnackbar(
                    message = if (permissionState.permissions.first().status.shouldShowRationale) "We can't show you all your files without permission" else "Permission fully denied. Go to settings to give permission",
                    actionLabel = "Give permission",
                    duration = SnackbarDuration.Long
                )
                showSnackbar = when (result) {
                    Dismissed -> {
                        false
                    }
                    ActionPerformed -> {
                        if (permissionState.permissions.first().status.shouldShowRationale) permissionState.permissions.first()
                            .launchPermissionRequest() else context.openAppDetails()
                        false
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(35.dp))

            Text(
                text = "Add video to watch!",
                fontSize = 30.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            IconButton(
                onClick = {
                    showDialog = true
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            ) {
                Icon(
                    imageVector = Icons.Default.FileOpen,
                    contentDescription = "select video",
                    modifier = Modifier.size(200.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                if (videoItems.isNotEmpty()) {
                    item {
                        Text(
                            text = "All video files",
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                    }
                }
                items(videoItems.size) { index ->
                    val item = videoItems.elementAt(index)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                BorderStroke(
                                    width = 0.8.dp,
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                navController.navigate("video_player_screen")
                                viewModel.playVideo(item.contentUri)
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.videoFrame != null) {
                            Image(
                                contentScale = ContentScale.FillBounds,
                                bitmap = item.videoFrame,
                                contentDescription = "file Image",
                                modifier = Modifier
                                    .size(width = 110.dp, height = 90.dp)
                                    .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = item.name,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Spacer(modifier = Modifier.height(15.dp))
                }
                if (userVideoItems.isNotEmpty() || downloadInfo?.state == WorkInfo.State.RUNNING) {
                    item {
                        Text(
                            text = "Your own uploads",
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                    }
                }
                items(userVideoItems.size) { index ->
                    val item = userVideoItems.elementAt(index)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                BorderStroke(
                                    width = 0.8.dp,
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                navController.navigate("video_player_screen")
                                viewModel.playVideo(item.contentUri)
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.videoFrame != null) {
                            Image(
                                contentScale = ContentScale.FillBounds,
                                bitmap = item.videoFrame,
                                contentDescription = "file Image",
                                modifier = Modifier
                                    .size(width = 110.dp, height = 90.dp)
                                    .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = item.name,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Spacer(modifier = Modifier.height(15.dp))
                }
                if (downloadInfo?.state == WorkInfo.State.RUNNING) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8f))
                            )
                            Spacer(modifier = Modifier.height(15.dp))
                        }
                    }
                }
            }
        }
    }
}