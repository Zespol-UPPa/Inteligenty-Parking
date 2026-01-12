package com.smartparking.worker_service.repo;
import com.smartparking.worker_service.model.Worker;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
@Repository
public class JdbcWorkerRepository implements WorkerRepository {
    private final JdbcTemplate jdbc;

    public JdbcWorkerRepository(JdbcTemplate jdbc) {this.jdbc = jdbc;}

    private final RowMapper<Worker> mapper = new RowMapper<>() {
        @Override
        public Worker mapRow(ResultSet rs, int rowNum) throws SQLException {
            Worker a = new Worker();
            a.setId(rs.getLong("worker_id"));
            a.setFirstName(rs.getString("first_name"));
            a.setLastName(rs.getString("last_name"));
            a.setPhoneNumber(rs.getString("phone_number"));
            a.setPeselNumber(rs.getString("pesel_number"));
            a.setRefAccountId(rs.getLong("ref_account_id"));
            a.setRefCompanyId(rs.getLong("ref_company_id"));
            a.setRefParkingId(rs.getLong("ref_parking_id"));
            return a;
        }
    };

    @Override
    public Optional<Worker> findById(Long id) {
        var list = jdbc.query(
                "SELECT worker_id, first_name, last_name, phone_number, pesel_number, " +
                        "ref_account_id, ref_company_id , ref_parking_id " +
                        "FROM worker WHERE worker_id = ?",
                mapper,
                id
        );
        return list.stream().findFirst();
    }
    @Override
    public Optional<Worker> findByAccountId(Long accountId) {
        var list = jdbc.query(
                "SELECT worker_id, first_name, last_name, phone_number, pesel_number, " +
                        "ref_account_id, ref_company_id , ref_parking_id " +
                        "FROM worker WHERE ref_account_id = ?",
                mapper,
                accountId
        );
        return list.stream().findFirst();
    }
    @Override
    public List<Worker> findAll() {
        return jdbc.query(
                "SELECT worker_id, first_name, last_name, phone_number, pesel_number, " +
                        "ref_account_id, ref_company_id , ref_parking_id " +
                        "FROM worker",
                mapper
        );
    }

    @Override
    public Worker save(Worker worker) {
        if (worker.getId() == null) {
            Long id = jdbc.queryForObject(
                    "INSERT INTO worker(first_name, last_name, phone_number, pesel_number, " +
                            "ref_account_id, ref_company_id , ref_parking_id) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING worker_id",
                    Long.class,
                    worker.getFirstName(),
                    worker.getLastName(),
                    worker.getPhoneNumber(),
                    worker.getPeselNumber(),
                    worker.getRefAccountId(),
                    worker.getRefCompanyId(),
                    worker.getRefParkingId()
            );

            worker.setId(id);
            return worker;
        } else {
            jdbc.update(
                    "UPDATE worker SET first_name = ?, last_name = ?, phone_number = ?, " +
                            "pesel_number = ?, ref_account_id = ?, ref_company_id = ? , ref_parking_id = ? " +
                            "WHERE worker_id = ?",
                    worker.getFirstName(),
                    worker.getLastName(),
                    worker.getPhoneNumber(),
                    worker.getPeselNumber(),
                    worker.getRefAccountId(),
                    worker.getRefCompanyId(),
                    worker.getRefParkingId(),
                    worker.getId()
            );
            return worker;
        }
    }

    @Override
    public void updatePersonalData(Long accountId, String firstName, String lastName,
                                   String phoneNumber, String peselNumber) {
        int rowsAffected = jdbc.update(
                "UPDATE worker SET first_name = ?, last_name = ?, phone_number = ?, pesel_number = ? " +
                        "WHERE ref_account_id = ?",
                firstName,
                lastName,
                phoneNumber,
                peselNumber,
                accountId
        );

        if (rowsAffected == 0) {
            throw new RuntimeException("Worker not found for accountId: " + accountId);
        }
    }

    @Override
    public List<Worker> findByCompanyId(Long companyId) {
        return jdbc.query(
                "SELECT worker_id, first_name, last_name, phone_number, pesel_number, " +
                        "ref_account_id, ref_company_id, ref_parking_id " +
                        "FROM worker WHERE ref_company_id = ?",
                mapper,
                companyId
        );
    }
}
