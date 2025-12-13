package com.smartparking.customer_service.repository;

import com.smartparking.customer_service.model.Customer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcCustomerRepository implements CustomerRepository {
    private final JdbcTemplate jdbc;

    public JdbcCustomerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Customer> mapper = new RowMapper<>() {
        @Override
        public Customer mapRow(ResultSet rs, int rowNum) throws SQLException {
            Customer c = new Customer();
            c.setId(rs.getLong("customer_id"));
            c.setFirstName(rs.getString("first_name"));
            c.setLastName(rs.getString("last_name"));
            c.setRefAccountId(rs.getLong("ref_account_id"));
            return c;
        }
    };
    @Override
    public Optional<Customer> findById(Long id) {
        var list = jdbc.query(
                "SELECT customer_id, first_name, last_name, ref_account_id " +
                        "FROM customer WHERE customer_id = ?",
                mapper,
                id
        );
        return list.stream().findFirst();
    }

    @Override
    public Optional<Customer> findByAccountId(Long refAccountId) {
        var list = jdbc.query(
                "SELECT customer_id, first_name, last_name, ref_account_id " +
                        "FROM customer WHERE ref_account_id = ?",
                mapper,
                refAccountId
        );
        return list.stream().findFirst();
    }

    @Override
    public List<Customer> findAll() {
        return jdbc.query(
                "SELECT customer_id, first_name, last_name, ref_account_id FROM customer",
                mapper
        );
    }

    @Override
    public Customer save(Customer customer) {
        if (customer.getId() == null) {
            Long id = jdbc.queryForObject(
                    "INSERT INTO customer(first_name, last_name, ref_account_id) " +
                            "VALUES (?, ?, ?) RETURNING customer_id",
                    Long.class,
                    customer.getFirstName(),
                    customer.getLastName(),
                    customer.getRefAccountId()
            );
            customer.setId(id);
            return customer;
        } else {
            jdbc.update(
                    "UPDATE customer SET first_name = ?, last_name = ?, ref_account_id = ? " +
                            "WHERE customer_id = ?",
                    customer.getFirstName(),
                    customer.getLastName(),
                    customer.getRefAccountId(),
                    customer.getId()
            );
            return customer;
        }
    }
}

