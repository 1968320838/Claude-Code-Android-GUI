package com.claudebox.data.repository;

import com.claudebox.data.local.dao.MessageDao;
import com.claudebox.data.local.dao.SessionDao;
import com.claudebox.data.local.entity.MessageEntity;
import com.claudebox.data.local.entity.SessionEntity;
import com.claudebox.domain.model.Message;
import com.claudebox.domain.model.Session;
import com.claudebox.domain.repository.SessionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SessionRepositoryImpl implements SessionRepository {

    private final SessionDao sessionDao;
    private final MessageDao messageDao;

    @Inject
    public SessionRepositoryImpl(SessionDao sessionDao, MessageDao messageDao) {
        this.sessionDao = sessionDao;
        this.messageDao = messageDao;
    }

    @Override
    public List<Session> getSessions() {
        List<SessionEntity> entities = sessionDao.getAll();
        List<Session> sessions = new ArrayList<>();
        for (SessionEntity entity : entities) {
            sessions.add(entityToSession(entity));
        }
        return sessions;
    }

    @Override
    public Session createSession() {
        String id = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        String name = "Session " + (sessionDao.getAll().size() + 1);

        SessionEntity entity = new SessionEntity(id, name, now, now);
        sessionDao.insert(entity);

        return new Session(id, name, now, now);
    }

    @Override
    public void deleteSession(String id) {
        // Messages are deleted automatically via CASCADE
        sessionDao.deleteById(id);
    }

    @Override
    public Session getSession(String id) {
        SessionEntity entity = sessionDao.getById(id);
        return entity != null ? entityToSession(entity) : null;
    }

    /**
     * Update session's last active timestamp.
     */
    public void updateLastActive(String id) {
        sessionDao.updateLastActiveAt(id, System.currentTimeMillis());
    }

    /**
     * Get messages for a session.
     */
    public List<Message> getMessages(String sessionId) {
        List<MessageEntity> entities = messageDao.getMessagesBySession(sessionId);
        List<Message> messages = new ArrayList<>();
        for (MessageEntity entity : entities) {
            messages.add(entityToMessage(entity));
        }
        return messages;
    }

    /**
     * Add a message to a session.
     */
    public void addMessage(Message message) {
        messageDao.insert(messageToEntity(message));
    }

    /**
     * Delete all messages in a session.
     */
    public void deleteMessages(String sessionId) {
        messageDao.deleteBySessionId(sessionId);
    }

    private Session entityToSession(SessionEntity entity) {
        return new Session(
            entity.getId(),
            entity.getName(),
            entity.getCreatedAt(),
            entity.getLastActiveAt()
        );
    }

    private SessionEntity sessionToEntity(Session session) {
        return new SessionEntity(
            session.getId(),
            session.getName(),
            session.getCreatedAt(),
            session.getLastActiveAt()
        );
    }

    private Message entityToMessage(MessageEntity entity) {
        return new Message(
            entity.getId(),
            entity.getSessionId(),
            entity.getContent(),
            entity.getRawContent(),
            entity.isFromUser(),
            entity.getTimestamp()
        );
    }

    private MessageEntity messageToEntity(Message message) {
        return new MessageEntity(
            message.getId(),
            message.getSessionId(),
            message.getContent(),
            message.getRawContent(),
            message.isFromUser(),
            message.getTimestamp()
        );
    }
}
