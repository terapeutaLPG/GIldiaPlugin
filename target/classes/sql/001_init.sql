-- Migracja 001: Inicjalizacja schematu bazy danych
-- Wykonane: 2025-08-30

-- Tabela gildii
CREATE TABLE IF NOT EXISTS guilds (
    id INT PRIMARY KEY AUTO_INCREMENT,
    tag VARCHAR(4) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL UNIQUE,
    leader_uuid BINARY(16) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    points INT DEFAULT 0,
    friendly_fire BOOLEAN DEFAULT TRUE,
    
    INDEX idx_leader (leader_uuid),
    INDEX idx_tag (tag),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tabela członków gildii
CREATE TABLE IF NOT EXISTS guild_members (
    id INT PRIMARY KEY AUTO_INCREMENT,
    guild_id INT NOT NULL,
    player_uuid BINARY(16) NOT NULL,
    role ENUM('LEADER', 'DEPUTY', 'MEMBER') DEFAULT 'MEMBER',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE KEY unique_member (guild_id, player_uuid),
    INDEX idx_player (player_uuid),
    INDEX idx_guild (guild_id),
    FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tabela sojuszy
CREATE TABLE IF NOT EXISTS guild_alliances (
    id INT PRIMARY KEY AUTO_INCREMENT,
    guild_id_a INT NOT NULL,
    guild_id_b INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE KEY unique_alliance (guild_id_a, guild_id_b),
    INDEX idx_guild_a (guild_id_a),
    INDEX idx_guild_b (guild_id_b),
    FOREIGN KEY (guild_id_a) REFERENCES guilds(id) ON DELETE CASCADE,
    FOREIGN KEY (guild_id_b) REFERENCES guilds(id) ON DELETE CASCADE,
    
    -- Zapobieganie duplikatom A-B i B-A
    CONSTRAINT check_different_guilds CHECK (guild_id_a != guild_id_b)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tabela zaproszeń do sojuszy
CREATE TABLE IF NOT EXISTS alliance_invites (
    id INT PRIMARY KEY AUTO_INCREMENT,
    from_guild_id INT NOT NULL,
    to_guild_id INT NOT NULL,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE KEY unique_invite (from_guild_id, to_guild_id),
    INDEX idx_from_guild (from_guild_id),
    INDEX idx_to_guild (to_guild_id),
    FOREIGN KEY (from_guild_id) REFERENCES guilds(id) ON DELETE CASCADE,
    FOREIGN KEY (to_guild_id) REFERENCES guilds(id) ON DELETE CASCADE,
    
    CONSTRAINT check_different_alliance_guilds CHECK (from_guild_id != to_guild_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tabela zaproszeń do gildii
CREATE TABLE IF NOT EXISTS join_invites (
    id INT PRIMARY KEY AUTO_INCREMENT,
    guild_id INT NOT NULL,
    player_uuid BINARY(16) NOT NULL,
    inviter_uuid BINARY(16) NOT NULL,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE KEY unique_join_invite (guild_id, player_uuid),
    INDEX idx_player (player_uuid),
    INDEX idx_guild (guild_id),
    INDEX idx_inviter (inviter_uuid),
    FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tabela migracji (tracking wykonanych migracji)
CREATE TABLE IF NOT EXISTS migration_history (
    id INT PRIMARY KEY AUTO_INCREMENT,
    migration_name VARCHAR(255) NOT NULL UNIQUE,
    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Zapisz wykonanie tej migracji
INSERT IGNORE INTO migration_history (migration_name) VALUES ('001_init.sql');

-- Indeksy dodatkowe dla wydajności
CREATE INDEX IF NOT EXISTS idx_guilds_created ON guilds(created_at);
CREATE INDEX IF NOT EXISTS idx_members_joined ON guild_members(joined_at);
CREATE INDEX IF NOT EXISTS idx_alliances_created ON guild_alliances(created_at);
CREATE INDEX IF NOT EXISTS idx_invites_sent ON alliance_invites(sent_at);
CREATE INDEX IF NOT EXISTS idx_join_invites_sent ON join_invites(sent_at);
