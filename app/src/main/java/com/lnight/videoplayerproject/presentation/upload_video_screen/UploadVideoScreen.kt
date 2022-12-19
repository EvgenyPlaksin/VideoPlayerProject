package com.lnight.videoplayerproject.presentation.upload_video_screen

import android.annotation.SuppressLint
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.*
import androidx.compose.material3.SnackbarResult.*
import androidx.compose.runtime.*
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.lnight.videoplayerproject.common.getVideoPath
import com.lnight.videoplayerproject.common.openAppDetails
import kotlinx.coroutines.launch


@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun UploadVideoScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    val permissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(
            android.Manifest.permission.READ_MEDIA_VIDEO
        )
    } else {
        rememberPermissionState(
            android.Manifest.permission.READ_EXTERNAL_STORAGE
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
            if(event == Lifecycle.Event.ON_RESUME && permissionState.status.isGranted) {
                val urisWithData = context.getVideoPath()
                viewModel.addVideoUri(urisWithData)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    when {
        !permissionState.status.isGranted && !permissionState.status.shouldShowRationale -> {
            LaunchedEffect(key1 = true) {
                permissionState.launchPermissionRequest()
            }
        }
        !permissionState.status.isGranted && permissionState.status.shouldShowRationale  -> {
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    content = {
                        LaunchedEffect(key1 = true) {
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "We can't show you all your files without permission",
                                    actionLabel = "Give permission"
                                )
                                when (result) {
                                    Dismissed -> Unit
                                    ActionPerformed ->  {
                                        context.openAppDetails()
                                    }
                                }
                            }
                        }
                    }
                )
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
            onClick = { selectVideoLauncher.launch("video/*") },
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
            if(videoItems.isNotEmpty()) {
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
                    if(item.videoFrame != null) {
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
            if(userVideoItems.isNotEmpty()) {
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
                    if(item.videoFrame != null) {
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
        }
    }
}