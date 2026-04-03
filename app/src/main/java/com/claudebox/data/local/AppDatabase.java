package com.claudebox.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.claudebox.data.local.dao.MessageDao;
import com.claudebox.data.local.dao.SessionDao;
import com.claudebox.data.local.entity.MessageEntity;
import com.claudebox.data.local.entity.SessionEntity;

@Database(
    entities = {SessionEntity.class, MessageEntity.class},
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract SessionDao sessionDao();
    public abstract MessageDao messageDao();
}
