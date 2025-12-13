package com.smartparking.accounts_service.repo;

import com.smartparking.accounts_service.model.LoginCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
public class JdbcLoginCodeRepository implements LoginCodeRepository {
    private final JdbcTemplate jdbc;

    public JdbcLoginCodeRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private final RowMapper<LoginCode> mapper = new RowMapper<>() {
        @Override
        public LoginCode mapRow(ResultSet rs, int rowNum) throws SQLException {
            LoginCode c = new LoginCode();
            c.setId(rs.getLong("code_id"));
            c.setCode(rs.getString("code"));
            c.setAccountId(rs.getLong("account_id"));
            c.setUsed(rs.getBoolean("is_used"));

            return c;
        }
    };

    @Override
    public LoginCode save(LoginCode code) {
        if (code.getId() == null) {
            // INSERT
            Long id = jdbc.queryForObject(
                    "INSERT INTO login_code(code, account_id, is_used) VALUES (?, ?, ?) RETURNING code_id",
                    Long.class,
                    code.getCode(),
                    code.getAccountId(),
                    code.isUsed()
            );

        } else {
            // UPDATE
            jdbc.update(
                    "UPDATE login_code SET code = ?, account_id = ?, is_used = ? WHERE code_id = ?",
                    code.getCode(),
                    code.getAccountId(),
                    code.isUsed(),
                    code.getId()
            );
        }
        return code;
    }

    @Override
    public Optional<LoginCode> findValidByCode(String codeValue) {
        var list = jdbc.query(
                "SELECT code_id, code, account_id, is_used FROM login_code WHERE code = ? AND is_used = FALSE",
                mapper,
                codeValue
        );
        return list.stream().findFirst();
    }

    @Override
    public Optional<LoginCode> findValidByAccountId(String accountid) {
        var list = jdbc.query(
                "SELECT code_id, code, account_id, is_used FROM login_code WHERE accountid = ? AND is_used = FALSE",
                mapper,
                accountid
        );
        return list.stream().findFirst();
    }

    @Override
    public LoginCode markUsed(LoginCode code) {
        jdbc.update(
                "UPDATE login_code SET is_used = TRUE WHERE code_id = ?",
                code.getCode()
        );
        return code;
    }
}

