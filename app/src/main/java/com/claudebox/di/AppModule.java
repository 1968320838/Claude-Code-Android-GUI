package com.claudebox.di;

import android.content.Context;

import androidx.room.Room;

import com.claudebox.data.local.AppDatabase;
import com.claudebox.data.local.ConfigManager;
import com.claudebox.data.local.dao.MessageDao;
import com.claudebox.data.local.dao.SessionDao;
import com.claudebox.data.repository.ConnectionManager;
import com.claudebox.data.repository.SessionRepositoryImpl;
import com.claudebox.data.repository.TermuxRepositoryImpl;
import com.claudebox.domain.repository.SessionRepository;
import com.claudebox.domain.repository.TermuxRepository;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    @Provides
    @Singleton
    public AppDatabase provideAppDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(
            context,
            AppDatabase.class,
            "claudebox_database"
        ).build();
    }

    @Provides
    @Singleton
    public SessionDao provideSessionDao(AppDatabase database) {
        return database.sessionDao();
    }

    @Provides
    @Singleton
    public MessageDao provideMessageDao(AppDatabase database) {
        return database.messageDao();
    }

    @Provides
    @Singleton
    public TermuxRepository provideTermuxRepository() {
        return new TermuxRepositoryImpl();
    }

    @Provides
    @Singleton
    public ConnectionManager provideConnectionManager(TermuxRepository termuxRepository) {
        return ConnectionManager.getInstance(termuxRepository);
    }

    @Provides
    @Singleton
    public SessionRepository provideSessionRepository(SessionDao sessionDao, MessageDao messageDao) {
        return new SessionRepositoryImpl(sessionDao, messageDao);
    }
}
