package com.lnight.videoplayerproject.presentation.upload_video_screen

import android.media.MediaMetadataRetriever
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.lnight.videoplayerproject.common.getVideoPath

@Composable
fun UploadVideoScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    val videoItems by viewModel.videoItems.collectAsState()
    val context = LocalContext.current
    val selectVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let(viewModel::addVideoUri)
        }
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(key1 = lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if(event == Lifecycle.Event.ON_START) {
                val uris = context.getVideoPath()
                uris.forEach { uriWithData ->
                    uriWithData.second?.let {
                        viewModel.addVideoUri(
                            it,
                            uriWithData.first
                        )
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
            items(
                count = videoItems.size,
                key = {
                    videoItems[it].contentUri
                }
            ) { int ->
                val item = videoItems[int]
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
                    val mMMR = MediaMetadataRetriever()
                    mMMR.setDataSource(context, item.contentUri)
                    val btm by remember {
                        mutableStateOf(mMMR.frameAtTime)
                    }
                    if(btm != null) {
                        Image(
                            contentScale = ContentScale.FillBounds,
                            bitmap = btm!!.asImageBitmap(),
                            contentDescription = "file image",
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
                }
                Spacer(modifier = Modifier.height(15.dp))
            }
        }
    }
}