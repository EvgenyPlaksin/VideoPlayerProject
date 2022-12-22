package com.lnight.videoplayerproject.presentation

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lnight.videoplayerproject.presentation.play_video_screen.VideoPlayerScreen
import com.lnight.videoplayerproject.presentation.ui.theme.VideoPlayerProjectTheme
import com.lnight.videoplayerproject.presentation.upload_video_screen.MainViewModel
import com.lnight.videoplayerproject.presentation.upload_video_screen.UploadVideoScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPref = getSharedPreferences("firstPref", Context.MODE_PRIVATE)
        setContent {
            VideoPlayerProjectTheme {
                val viewModel = hiltViewModel<MainViewModel>()

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
                                viewModel = viewModel,
                                sharedPref.getBoolean("first", true)
                            )
                            sharedPref.edit().putBoolean("first", false).apply()
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