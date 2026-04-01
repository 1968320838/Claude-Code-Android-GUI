package com.claudebox.data.ssh;

public abstract class ConnectionState {
    private ConnectionState() {}

    public static final class Disconnected extends ConnectionState {
        public Disconnected() {}
    }

    public static final class Connecting extends ConnectionState {
        public Connecting() {}
    }

    public static final class Connected extends ConnectionState {
        public Connected() {}
    }

    public static final class Error extends ConnectionState {
        private final String message;

        public Error(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public static final class Reconnecting extends ConnectionState {
        private final int attempt;

        public Reconnecting(int attempt) {
            this.attempt = attempt;
        }

        public int getAttempt() {
            return attempt;
        }
    }
}
