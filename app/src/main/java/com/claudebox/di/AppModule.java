package com.claudebox.di;

import com.claudebox.data.repository.ConnectionManager;
import com.claudebox.data.repository.TermuxRepositoryImpl;
import com.claudebox.domain.repository.TermuxRepository;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

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
}
