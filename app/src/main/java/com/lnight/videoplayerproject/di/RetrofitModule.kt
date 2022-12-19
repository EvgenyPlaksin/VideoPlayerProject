package com.lnight.videoplayerproject.di

import com.lnight.videoplayerproject.data.remote.VideoApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import retrofit2.Retrofit

@Module
@InstallIn(ViewModelComponent::class)
object RetrofitModule {

    @Provides
    @ViewModelScoped
    fun provideVideoApi(): VideoApi {
        return Retrofit.Builder()
            .baseUrl("http://localhost/")
            .build()
            .create(VideoApi::class.java)
    }

}