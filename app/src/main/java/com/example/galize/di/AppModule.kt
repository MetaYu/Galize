package com.example.galize.di

import android.content.Context
import androidx.room.Room
import com.example.galize.ai.AiClient
import com.example.galize.ai.CloudAiClient
import com.example.galize.ai.LocalAiClient
import com.example.galize.ai.PromptBuilder
import com.example.galize.model.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePromptBuilder(): PromptBuilder = PromptBuilder()

    @Provides
    @Singleton
    @Named("cloud")
    fun provideCloudAiClient(promptBuilder: PromptBuilder): AiClient {
        return CloudAiClient(promptBuilder)
    }

    @Provides
    @Singleton
    @Named("local")
    fun provideLocalAiClient(): AiClient {
        return LocalAiClient()
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "galize_database"
        ).build()
    }
}
