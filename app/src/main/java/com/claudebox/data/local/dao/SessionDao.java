package com.claudebox.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.claudebox.data.local.entity.SessionEntity;

import java.util.List;

@Dao
public interface SessionDao {

    @Query("SELECT * FROM sessions ORDER BY lastActiveAt DESC")
    List<SessionEntity> getAll();

    @Query("SELECT * FROM sessions WHERE id = :id")
    SessionEntity getById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SessionEntity session);

    @Update
    void update(SessionEntity session);

    @Delete
    void delete(SessionEntity session);

    @Query("DELETE FROM sessions WHERE id = :id")
    void deleteById(String id);

    @Query("UPDATE sessions SET lastActiveAt = :lastActiveAt WHERE id = :id")
    void updateLastActiveAt(String id, long lastActiveAt);
}
