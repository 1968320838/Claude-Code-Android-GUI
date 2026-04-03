package com.claudebox.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.claudebox.data.local.entity.MessageEntity;

import java.util.List;

@Dao
public interface MessageDao {

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    List<MessageEntity> getMessagesBySession(String sessionId);

    @Query("SELECT * FROM messages WHERE id = :id")
    MessageEntity getById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MessageEntity message);

    @Update
    void update(MessageEntity message);

    @Delete
    void delete(MessageEntity message);

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    void deleteBySessionId(String sessionId);

    @Query("DELETE FROM messages WHERE id = :id")
    void deleteById(String id);
}
