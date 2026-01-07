package com.smartparking.accounts_service.repo;

import com.smartparking.accounts_service.model.VerificationToken;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcVerificationTokenRepository implements VerificationTokenRepository {
    private final JdbcTemplate jdbc;

    public JdbcVerificationTokenRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<VerificationToken> mapper = new RowMapper<>() {
        @Override
        public VerificationToken mapRow(ResultSet rs, int rowNum) throws SQLException {
            VerificationToken token = new VerificationToken();
            token.setId(rs.getLong("token_id"));
            token.setToken(rs.getString("token"));
            token.setAccountId(rs.getLong("account_id"));
            
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                token.setCreatedAt(createdAt.toInstant());
            }
            
            Timestamp expiresAt = rs.getTimestamp("expires_at");
            if (expiresAt != null) {
                token.setExpiresAt(expiresAt.toInstant());
            }
            
            token.setIsUsed(rs.getBoolean("is_used"));
            return token;
        }
    };

    @Override
    public VerificationToken save(VerificationToken token) {
        if (token.getId() == null) {
            Long id = jdbc.queryForObject(
                    "INSERT INTO verification_token(token, account_id, created_at, expires_at, is_used) " +
                            "VALUES (?, ?, ?, ?, ?) RETURNING token_id",
                    Long.class,
                    token.getToken(),
                    token.getAccountId(),
                    Timestamp.from(token.getCreatedAt() != null ? token.getCreatedAt() : Instant.now()),
                    Timestamp.from(token.getExpiresAt()),
                    token.getIsUsed() != null ? token.getIsUsed() : false
            );
            token.setId(id);
            return token;
        } else {
            jdbc.update(
                    "UPDATE verification_token SET token = ?, account_id = ?, created_at = ?, expires_at = ?, is_used = ? " +
                            "WHERE token_id = ?",
                    token.getToken(),
                    token.getAccountId(),
                    Timestamp.from(token.getCreatedAt()),
                    Timestamp.from(token.getExpiresAt()),
                    token.getIsUsed(),
                    token.getId()
            );
            return token;
        }
    }

    @Override
    public Optional<VerificationToken> findByToken(String token) {
        var list = jdbc.query(
                "SELECT token_id, token, account_id, created_at, expires_at, is_used " +
                        "FROM verification_token WHERE token = ?",
                mapper,
                token
        );
        return list.stream().findFirst();
    }

    @Override
    public List<VerificationToken> findByAccountId(Long accountId) {
        return jdbc.query(
                "SELECT token_id, token, account_id, created_at, expires_at, is_used " +
                        "FROM verification_token WHERE account_id = ?",
                mapper,
                accountId
        );
    }

    @Override
    public void deleteExpired() {
        jdbc.update(
                "DELETE FROM verification_token WHERE expires_at < NOW() - INTERVAL '7 days'"
        );
    }

    @Override
    public void markAsUsed(Long tokenId) {
        jdbc.update(
                "UPDATE verification_token SET is_used = TRUE WHERE token_id = ?",
                tokenId
        );
    }

    @Override
    public void deleteByAccountId(Long accountId) {
        jdbc.update(
                "DELETE FROM verification_token WHERE account_id = ?",
                accountId
        );
    }
}

