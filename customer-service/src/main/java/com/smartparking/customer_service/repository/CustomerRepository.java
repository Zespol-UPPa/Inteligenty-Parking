package com.smartparking.customer_service.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class CustomerRepository {
    private final JdbcTemplate jdbc;

    public CustomerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Map<String, Object>> findById(Long id) {
        List<Map<String, Object>> list = jdbc.query(
                // customer-service korzysta wyłącznie z customer_db.
                // W tej bazie nie ma tabeli account – mamy customer(ref_account_id, first_name, last_name).
                // Dlatego pobieramy dane z customer i zostawiamy email jako null.
                "SELECT c.ref_account_id AS account_id, NULL::varchar AS email, c.first_name, c.last_name " +
                        "FROM customer c " +
                        "WHERE c.ref_account_id = ?",
                (rs, rowNum) -> Map.of(
                        "account_id", rs.getLong("account_id"),
                        "email", rs.getString("email"),
                        "first_name", rs.getString("first_name"),
                        "last_name", rs.getString("last_name")
                ), id);
        return list.stream().findFirst();
    }

    public int updateProfile(Long id, String firstName, String lastName) {
        return jdbc.update("UPDATE customer SET first_name = ?, last_name = ? WHERE ref_account_id = ?", firstName, lastName, id);
    }
}

