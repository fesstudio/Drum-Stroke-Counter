package com.drummer.speed.di

import android.content.Context
import android.media.AudioManager
import com.drummer.speed.audio.engine.AudioFocusManager
import com.drummer.speed.audio.engine.CalibrationEngine
import com.drummer.speed.audio.engine.CountdownEngine
import com.drummer.speed.audio.engine.MetronomeEngine
import com.drummer.speed.audio.engine.StrokeDetector
import com.drummer.speed.data.local.AppDatabase
import com.drummer.speed.util.AudioConfig
import com.drummer.speed.data.local.HistoryDao
import com.drummer.speed.data.repository.DrumRepository
import com.drummer.speed.data.repository.UpdateRepository
import com.drummer.speed.domain.repository.IHistoryRepository
import com.drummer.speed.domain.repository.IUpdateRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideHistoryDao(database: AppDatabase): HistoryDao {
        return database.historyDao()
    }

    @Provides
    @Singleton
    fun provideHistoryRepository(repository: DrumRepository): IHistoryRepository {
        return repository
    }

    @Provides
    @Singleton
    fun provideUpdateRepository(repository: UpdateRepository): IUpdateRepository {
        return repository
    }

    @Provides
    @Singleton
    fun provideAudioManager(@ApplicationContext context: Context): AudioManager {
        return context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    @Provides
    @Singleton
    fun provideStrokeDetector(): StrokeDetector = StrokeDetector(AudioConfig.SAMPLE_RATE)

    @Provides
    @Singleton
    fun provideMetronomeEngine(): MetronomeEngine = MetronomeEngine()

    @Provides
    @Singleton
    fun provideCountdownEngine(): CountdownEngine = CountdownEngine()

    @Provides
    @Singleton
    fun provideAudioFocusManager(@ApplicationContext context: Context): AudioFocusManager {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return AudioFocusManager(audioManager)
    }
}
