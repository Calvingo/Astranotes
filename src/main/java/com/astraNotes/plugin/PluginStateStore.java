package com.astraNotes.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

/**
 * Stores plugin state in a separate database table, isolated from note data.
 * REQ-SEC-6: Plugin state isolation and auditability
 */
public class PluginStateStore {
    private static final Logger logger = LoggerFactory.getLogger(PluginStateStore.class);
    private final Connection connection;

    public PluginStateStore(Connection connection) {
        this.connection = connection;
    }

    public Optional<String> getState(String pluginId, String stateKey) throws SQLException {
        String sql = "SELECT state_value FROM plugin_state WHERE plugin_id = ? AND state_key = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, pluginId);
            pstmt.setString(2, stateKey);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("state_value"));
                }
            }
        }
        return Optional.empty();
    }

    public void saveState(String pluginId, String stateKey, String stateValue) throws SQLException {
        String sql = "INSERT INTO plugin_state(plugin_id, state_key, state_value, updated_at) VALUES (?, ?, ?, ?) " +
                     "ON CONFLICT(plugin_id, state_key) DO UPDATE SET state_value = excluded.state_value, updated_at = excluded.updated_at;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, pluginId);
            pstmt.setString(2, stateKey);
            pstmt.setString(3, stateValue);
            pstmt.setLong(4, Instant.now().getEpochSecond());
            pstmt.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                logger.error("Rollback failed while saving plugin state", rollbackEx);
            }
            throw e;
        }
    }
}
