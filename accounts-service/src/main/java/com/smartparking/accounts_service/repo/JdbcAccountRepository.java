package com.smartparking.accounts_service.repo;

import com.smartparking.accounts_service.model.Account;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
public class JdbcAccountRepository implements AccountRepository {
    private final JdbcTemplate jdbc;

    public JdbcAccountRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private final RowMapper<Account> mapper = new RowMapper<>() {
        @Override
        public Account mapRow(ResultSet rs, int rowNum) throws SQLException {
            Account a = new Account();
            // account_db.account schema:
            // account_id, email, password_hash, role_account, is_active
            a.setId(rs.getLong("account_id"));
            // In kodzie pole "username" odpowiada kolumnie "email"
            a.setUsername(rs.getString("email"));
            a.setPasswordHash(rs.getString("password_hash"));
            a.setRole(rs.getString("role_account"));
            return a;
        }
    };

    @Override
    public Optional<Account> findByUsername(String username) {
        // username w modelu = email w bazie account_db.account
        var list = jdbc.query(
                "SELECT account_id, email, password_hash, role_account " +
                        "FROM account WHERE email = ?",
                mapper,
                username
        );
        return list.stream().findFirst();
    }

    @Override
    public Account save(Account account) {
        if (account.getId() == null) {
            // INSERT do account_db.account
            jdbc.update(
                    "INSERT INTO account(email, password_hash, role_account, is_active) VALUES (?, ?, ?, TRUE)",
                    account.getUsername(),
                    account.getPasswordHash(),
                    account.getRole()
            );
            // pobierz wygenerowane account_id
            Long id = jdbc.queryForObject(
                    "SELECT account_id FROM account WHERE email = ?",
                    Long.class,
                    account.getUsername()
            );
            account.setId(id);
            return account;
        } else {
            jdbc.update(
                    "UPDATE account SET email = ?, password_hash = ?, role_account = ? WHERE account_id = ?",
                    account.getUsername(),
                    account.getPasswordHash(),
                    account.getRole(),
                    account.getId()
            );
            return account;
        }
    }
}

