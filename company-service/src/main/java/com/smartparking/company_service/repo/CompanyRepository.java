package com.smartparking.company_service.repo;

import com.smartparking.company_service.model.Company;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class CompanyRepository {
    private final JdbcTemplate jdbc;

    public CompanyRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Company> MAPPER = new RowMapper<Company>() {
        @Override
        public Company mapRow(ResultSet rs, int rowNum) throws SQLException {
            Company c = new Company();
            c.setCompanyId(rs.getInt("company_id"));
            c.setNameCompany(rs.getString("name_company"));
            c.setAddress(rs.getString("address"));
            c.setTaxId(rs.getString("tax_id"));
            return c;
        }
    };

    public Company save(Company c) {
        jdbc.update("INSERT INTO company(name_company, address, tax_id) VALUES (?, ?, ?)",
                c.getNameCompany(), c.getAddress(), c.getTaxId());
        return c;
    }

    public List<Company> findAll() {
        return jdbc.query("SELECT company_id, name_company, address, tax_id FROM company", MAPPER);
    }
}

