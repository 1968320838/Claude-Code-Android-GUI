package com.claudebox.domain.repository;

import com.claudebox.data.ssh.SSHConfig;
import com.claudebox.data.ssh.ConnectionState;
import kotlinx.coroutines.flow.Flow;

public interface TermuxRepository {
    Flow<ConnectionState> connect(SSHConfig config);
    void disconnect();
    Flow<String> executeCommand(String command);
    boolean isConnected();
}
