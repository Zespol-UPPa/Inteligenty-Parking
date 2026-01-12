package com.smartparking.company_service.repo;

import com.smartparking.company_service.model.Company;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcCompanyRepository implements CompanyRepository{
    private final JdbcTemplate jdbc;

    public JdbcCompanyRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Company> mapper = new RowMapper<Company>() {
        @Override
        public Company mapRow(ResultSet rs, int rowNum) throws SQLException {
            Company c = new Company();
            c.setId(rs.getLong("company_id"));
            c.setNameCompany(rs.getString("name_company"));
            c.setAddress(rs.getString("address"));
            c.setTaxId(rs.getString("tax_id"));
            return c;
        }
    };

    @Override
    public Optional<Company> findById(Long id) {
        var list = jdbc.query(
                "SELECT company_id, name_company, address, tax_id " +
                        "FROM company WHERE company_id = ?",
                mapper,
                id
        );
        return list.stream().findFirst();
    }

    @Override
    public Optional<Company> findByTaxId(String taxId) {
        var list = jdbc.query(
                "SELECT company_id, name_company, address, tax_id " +
                        "FROM company WHERE tax_id = ?",
                mapper,
                taxId
        );
        return list.stream().findFirst();
    }

    @Override
    public List<Company> findAll() {
        return jdbc.query(
                "SELECT company_id, name_company, address, tax_id FROM company",
                mapper
        );
    }

    //Implementaja metody dla programisty
    @Override
    public Company save(Company company) {
        if (company.getId() == null) {
            Long id = jdbc.queryForObject(
                    "INSERT INTO company(name_company, address, tax_id) " +
                            "VALUES (?, ?, ?) RETURNING company_id",
                    Long.class,
                    company.getNameCompany(),
                    company.getAddress(),
                    company.getTaxId()
            );
            company.setId(id);
            return company;
        } else {
            jdbc.update(
                    "UPDATE company SET name_company = ?, address = ?, tax_id = ? " +
                            "WHERE company_id = ?",
                    company.getNameCompany(),
                    company.getAddress(),
                    company.getTaxId(),
                    company.getId()
            );
            return company;
        }
    }

    @Override
    public String getNameById(Long id) {
        try {
            return jdbc.queryForObject(
                    "SELECT name_company FROM company WHERE company_id = ?",
                    String.class,
                    id
            );
        } catch (Exception e) {
            return null;
        }
    }
}

