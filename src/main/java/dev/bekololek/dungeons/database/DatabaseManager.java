package dev.bekololek.dungeons.database;

import dev.bekololek.dungeons.Main;
import dev.bekololek.dungeons.models.Party;
import dev.bekololek.dungeons.models.PlayerData;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class DatabaseManager {

    private final Main plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
    }

    public void initialize() throws SQLException {
        String dbType = plugin.getConfig().getString("database.type", "sqlite");

        HikariConfig config = new HikariConfig();

        if (dbType.equalsIgnoreCase("sqlite")) {
            setupSQLite(config);
        } else if (dbType.equalsIgnoreCase("mysql")) {
            setupMySQL(config);
        } else {
            throw new IllegalArgumentException("Invalid database type: " + dbType);
        }

        config.setMaximumPoolSize(plugin.getConfig().getInt("database.mysql.pool-size", 10));
        config.setConnectionTimeout(30000);
        config.setLeakDetectionThreshold(60000);

        dataSource = new HikariDataSource(config);
        createTables();

        plugin.getLogger().info("Database initialized successfully using " + dbType);
    }

    private void setupSQLite(HikariConfig config) {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        String dbFile = plugin.getConfig().getString("database.sqlite.file", "data.db");
        File dbPath = new File(dataFolder, dbFile);

        config.setJdbcUrl("jdbc:sqlite:" + dbPath.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    }

    private void setupMySQL(HikariConfig config) {
        String host = plugin.getConfig().getString("database.mysql.host", "localhost");
        int port = plugin.getConfig().getInt("database.mysql.port", 3306);
        String database = plugin.getConfig().getString("database.mysql.database", "dungeons");
        String username = plugin.getConfig().getString("database.mysql.username", "root");
        String password = plugin.getConfig().getString("database.mysql.password", "password");

        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
    }

    private void createTables() throws SQLException {
        try (Connection conn = getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS player_data (
                        player_id VARCHAR(36) PRIMARY KEY,
                        total_completions INTEGER DEFAULT 0,
                        total_playtime BIGINT DEFAULT 0,
                        last_seen BIGINT,
                        created_at BIGINT
                    )""");
            }

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS dungeon_cooldowns (
                        player_id VARCHAR(36),
                        dungeon_id VARCHAR(64),
                        expiry_time BIGINT,
                        PRIMARY KEY (player_id, dungeon_id)
                    )""");
            }

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS dungeon_completions (
                        player_id VARCHAR(36),
                        dungeon_id VARCHAR(64),
                        completion_count INTEGER DEFAULT 0,
                        PRIMARY KEY (player_id, dungeon_id)
                    )""");
            }

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS parties (
                        party_id VARCHAR(36) PRIMARY KEY,
                        leader_id VARCHAR(36),
                        created_at BIGINT
                    )""");
            }

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS party_members (
                        party_id VARCHAR(36),
                        player_id VARCHAR(36),
                        joined_at BIGINT,
                        PRIMARY KEY (party_id, player_id)
                    )""");
            }

            String historyTable = isUsingSQLite()
                    ? """
                    CREATE TABLE IF NOT EXISTS dungeon_completion_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        player_id VARCHAR(36),
                        dungeon_id VARCHAR(64),
                        completion_duration BIGINT,
                        completed_at BIGINT
                    )"""
                    : """
                    CREATE TABLE IF NOT EXISTS dungeon_completion_history (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        player_id VARCHAR(36),
                        dungeon_id VARCHAR(64),
                        completion_duration BIGINT,
                        completed_at BIGINT
                    )""";

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(historyTable);
            }

            plugin.getLogger().info("Database tables created successfully");
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource is not initialized");
        }
        return dataSource.getConnection();
    }

    // ==================== Player Data ====================

    public PlayerData loadPlayerData(UUID playerId) {
        try (Connection conn = getConnection()) {
            PlayerData data = new PlayerData(playerId);

            try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM player_data WHERE player_id = ?")) {
                stmt.setString(1, playerId.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        data.addPlaytime(rs.getLong("total_playtime"));
                    }
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM dungeon_cooldowns WHERE player_id = ?")) {
                stmt.setString(1, playerId.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String dungeonId = rs.getString("dungeon_id");
                        long expiryTime = rs.getLong("expiry_time");
                        long remainingSeconds = (expiryTime - System.currentTimeMillis()) / 1000;
                        if (remainingSeconds > 0) {
                            data.setDungeonCooldown(dungeonId, remainingSeconds);
                        }
                    }
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM dungeon_completions WHERE player_id = ?")) {
                stmt.setString(1, playerId.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String dungeonId = rs.getString("dungeon_id");
                        int count = rs.getInt("completion_count");
                        for (int i = 0; i < count; i++) {
                            data.incrementCompletion(dungeonId);
                        }
                    }
                }
            }

            return data;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player data for " + playerId, e);
            return new PlayerData(playerId);
        }
    }

    public void savePlayerData(PlayerData data) {
        try (Connection conn = getConnection()) {
            String upsert = isUsingSQLite()
                    ? """
                    INSERT INTO player_data (player_id, total_completions, total_playtime, last_seen, created_at)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT(player_id) DO UPDATE SET
                        total_completions = excluded.total_completions,
                        total_playtime = excluded.total_playtime,
                        last_seen = excluded.last_seen"""
                    : """
                    INSERT INTO player_data (player_id, total_completions, total_playtime, last_seen, created_at)
                    VALUES (?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        total_completions = VALUES(total_completions),
                        total_playtime = VALUES(total_playtime),
                        last_seen = VALUES(last_seen)""";

            try (PreparedStatement stmt = conn.prepareStatement(upsert)) {
                stmt.setString(1, data.getPlayerId().toString());
                stmt.setInt(2, data.getTotalCompletions());
                stmt.setLong(3, data.getTotalPlaytime());
                stmt.setLong(4, data.getLastSeen());
                stmt.setLong(5, System.currentTimeMillis());
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM dungeon_cooldowns WHERE player_id = ?")) {
                stmt.setString(1, data.getPlayerId().toString());
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO dungeon_cooldowns (player_id, dungeon_id, expiry_time) VALUES (?, ?, ?)")) {
                for (Map.Entry<String, Long> entry : data.getDungeonCooldowns().entrySet()) {
                    stmt.setString(1, data.getPlayerId().toString());
                    stmt.setString(2, entry.getKey());
                    stmt.setLong(3, entry.getValue());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM dungeon_completions WHERE player_id = ?")) {
                stmt.setString(1, data.getPlayerId().toString());
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO dungeon_completions (player_id, dungeon_id, completion_count) VALUES (?, ?, ?)")) {
                for (Map.Entry<String, Integer> entry : data.getDungeonCompletions().entrySet()) {
                    stmt.setString(1, data.getPlayerId().toString());
                    stmt.setString(2, entry.getKey());
                    stmt.setInt(3, entry.getValue());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player data for " + data.getPlayerId(), e);
        }
    }

    public void saveCompletionRecord(UUID playerId, String dungeonId, long completionDuration) {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO dungeon_completion_history (player_id, dungeon_id, completion_duration, completed_at) VALUES (?, ?, ?, ?)")) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, dungeonId);
                stmt.setLong(3, completionDuration);
                stmt.setLong(4, System.currentTimeMillis());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save completion record for " + playerId, e);
        }
    }

    public long getFastestCompletion(String dungeonId) {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT MIN(completion_duration) FROM dungeon_completion_history WHERE dungeon_id = ?")) {
                stmt.setString(1, dungeonId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get fastest completion for " + dungeonId, e);
        }
        return 0;
    }

    public long getPlayerFastestCompletion(UUID playerId, String dungeonId) {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT MIN(completion_duration) FROM dungeon_completion_history WHERE player_id = ? AND dungeon_id = ?")) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, dungeonId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get player fastest completion", e);
        }
        return 0;
    }

    // ==================== Party ====================

    public void saveParty(Party party) {
        try (Connection conn = getConnection()) {
            String upsert = isUsingSQLite()
                    ? """
                    INSERT INTO parties (party_id, leader_id, created_at) VALUES (?, ?, ?)
                    ON CONFLICT(party_id) DO UPDATE SET leader_id = excluded.leader_id"""
                    : """
                    INSERT INTO parties (party_id, leader_id, created_at) VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE leader_id = VALUES(leader_id)""";

            try (PreparedStatement stmt = conn.prepareStatement(upsert)) {
                stmt.setString(1, party.getPartyId().toString());
                stmt.setString(2, party.getLeaderId().toString());
                stmt.setLong(3, party.getCreatedAt());
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM party_members WHERE party_id = ?")) {
                stmt.setString(1, party.getPartyId().toString());
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO party_members (party_id, player_id, joined_at) VALUES (?, ?, ?)")) {
                for (UUID memberId : party.getMembers()) {
                    stmt.setString(1, party.getPartyId().toString());
                    stmt.setString(2, memberId.toString());
                    stmt.setLong(3, System.currentTimeMillis());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save party " + party.getPartyId(), e);
        }
    }

    public void deleteParty(UUID partyId) {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM party_members WHERE party_id = ?")) {
                stmt.setString(1, partyId.toString());
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM parties WHERE party_id = ?")) {
                stmt.setString(1, partyId.toString());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete party " + partyId, e);
        }
    }

    public List<Party> loadAllParties() {
        List<Party> parties = new ArrayList<>();
        try (Connection conn = getConnection()) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM parties")) {
                while (rs.next()) {
                    UUID partyId = UUID.fromString(rs.getString("party_id"));
                    UUID leaderId = UUID.fromString(rs.getString("leader_id"));
                    long createdAt = rs.getLong("created_at");

                    Set<UUID> members = new HashSet<>();
                    try (PreparedStatement memberStmt = conn.prepareStatement(
                            "SELECT player_id FROM party_members WHERE party_id = ?")) {
                        memberStmt.setString(1, partyId.toString());
                        try (ResultSet memberRs = memberStmt.executeQuery()) {
                            while (memberRs.next()) {
                                members.add(UUID.fromString(memberRs.getString("player_id")));
                            }
                        }
                    }

                    parties.add(new Party(partyId, leaderId, members, createdAt));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load parties", e);
        }
        return parties;
    }

    private boolean isUsingSQLite() {
        return plugin.getConfig().getString("database.type", "sqlite").equalsIgnoreCase("sqlite");
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection closed");
        }
    }
}
