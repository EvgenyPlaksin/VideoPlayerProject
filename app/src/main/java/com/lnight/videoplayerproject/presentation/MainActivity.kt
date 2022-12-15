package com.lnight.videoplayerproject.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lnight.videoplayerproject.common.getVideoPath
import com.lnight.videoplayerproject.presentation.play_video_screen.VideoPlayerScreen
import com.lnight.videoplayerproject.presentation.ui.theme.VideoPlayerProjectTheme
import com.lnight.videoplayerproject.presentation.upload_video_screen.MainViewModel
import com.lnight.videoplayerproject.presentation.upload_video_screen.UploadVideoScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VideoPlayerProjectTheme {
                val viewModel = hiltViewModel<MainViewModel>()

                val context = LocalContext.current
                LaunchedEffect(key1 = true) {
                    val uris = context.getVideoPath()
                    uris.forEach { uriWithName ->
                        uriWithName.second?.let { viewModel.addVideoUri(it, uriWithName.first) }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "upload_video_screen") {
                        composable("upload_video_screen") {
                            UploadVideoScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                        composable("video_player_screen") {
                            VideoPlayerScreen(
                                navController = navController,
                                player = viewModel.player
                            )
                        }
                    }
                }
            }
        }
    }
}