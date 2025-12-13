package com.smartparking.admin_service.repo;
import com.smartparking.admin_service.model.Admin;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcAdminRepository implements AdminRepository {
    private final JdbcTemplate jdbc;

    public JdbcAdminRepository(JdbcTemplate jdbc) {this.jdbc = jdbc;}

    private final RowMapper<Admin> mapper = new RowMapper<>() {
        @Override
        public Admin mapRow(ResultSet rs, int rowNum) throws SQLException {
            Admin a = new Admin();
            a.setId(rs.getLong("admin_id"));
            a.setFirstName(rs.getString("first_name"));
            a.setLastName(rs.getString("last_name"));
            a.setPhoneNumber(rs.getString("phone_number"));
            a.setPeselNumber(rs.getString("pesel_number"));
            a.setRefAccountId(rs.getLong("ref_account_id"));
            a.setRefCompanyId(rs.getLong("ref_company_id"));
            return a;
        }
    };

    @Override
    public Optional<Admin> findById(Long id) {
        var list = jdbc.query(
                "SELECT admin_id, first_name, last_name, phone_number, pesel_number, " +
                        "ref_account_id, ref_company_id " +
                        "FROM admin WHERE admin_id = ?",
                mapper,
                id
        );
        return list.stream().findFirst();
    }
    @Override
    public Optional<Admin> findByAccountId(Long accountId) {
        var list = jdbc.query(
                "SELECT admin_id, first_name, last_name, phone_number, pesel_number, " +
                        "ref_account_id, ref_company_id " +
                        "FROM admin WHERE ref_account_id = ?",
                mapper,
                accountId
        );
        return list.stream().findFirst();
    }
    @Override
    public List<Admin> findAll() {
        return jdbc.query(
                "SELECT admin_id, first_name, last_name, phone_number, pesel_number, " +
                        "ref_account_id, ref_company_id " +
                        "FROM admin",
                mapper
        );
    }

    @Override
    public Admin save(Admin admin) {
        if (admin.getId() == null) {
            Long id = jdbc.queryForObject(
                    "INSERT INTO admin(first_name, last_name, phone_number, pesel_number, " +
                            "ref_account_id, ref_company_id) " +
                            "VALUES (?, ?, ?, ?, ?, ?) RETURNING admin_id",
                    Long.class,
                    admin.getFirstName(),
                    admin.getLastName(),
                    admin.getPhoneNumber(),
                    admin.getPeselNumber(),
                    admin.getRefAccountId(),
                    admin.getRefCompanyId()
            );

            admin.setId(id);
            return admin;
        } else {
            jdbc.update(
                    "UPDATE admin SET first_name = ?, last_name = ?, phone_number = ?, " +
                            "pesel_number = ?, ref_account_id = ?, ref_company_id = ? " +
                            "WHERE admin_id = ?",
                    admin.getFirstName(),
                    admin.getLastName(),
                    admin.getPhoneNumber(),
                    admin.getPeselNumber(),
                    admin.getRefAccountId(),
                    admin.getRefCompanyId(),
                    admin.getId()
            );
            return admin;
        }
    }

}
