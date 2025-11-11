package com.smartparking.user_service.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Map<String, Object>> findById(Long id) {
        List<Map<String, Object>> list = jdbc.query(
                "SELECT a.id_account, a.email, c.first_name, c.last_name " +
                        "FROM account a LEFT JOIN customer c ON c.id_account = a.id_account " +
                        "WHERE a.id_account = ?",
                (rs, rowNum) -> Map.of(
                        "id_account", rs.getLong("id_account"),
                        "email", rs.getString("email"),
                        "first_name", rs.getString("first_name"),
                        "last_name", rs.getString("last_name")
                ), id);
        return list.stream().findFirst();
    }

    public int updateProfile(Long id, String firstName, String lastName) {
        return jdbc.update("UPDATE customer SET first_name = ?, last_name = ? WHERE id_account = ?", firstName, lastName, id);
    }
}


