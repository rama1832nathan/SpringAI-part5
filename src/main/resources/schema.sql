CREATE TABLE IF NOT EXISTS SPRING_AI_CHAT_MEMORY (
                                                     id              BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                     conversation_id VARCHAR(256) NOT NULL,
    content         TEXT         NOT NULL,
    type            VARCHAR(64)  NOT NULL,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
    );