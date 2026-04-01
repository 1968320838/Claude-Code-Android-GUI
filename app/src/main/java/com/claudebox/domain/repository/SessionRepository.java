package com.claudebox.domain.repository;

import com.claudebox.domain.model.Session;
import java.util.List;

public interface SessionRepository {
    List<Session> getSessions();
    Session createSession();
    void deleteSession(String id);
    Session getSession(String id);
}
