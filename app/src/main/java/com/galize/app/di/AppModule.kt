package com.galize.app.di

import android.content.Context
import androidx.room.Room
import com.galize.app.ai.AiClient
import com.galize.app.ai.CloudAiClient
import com.galize.app.ai.LocalAiClient
import com.galize.app.ai.PromptBuilder
import com.galize.app.model.db.AppDatabase
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
