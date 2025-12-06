package com.smartparking.customer_service.repository;

import com.smartparking.customer_service.model.Vehicle;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcVehicleRepository implements VehicleRepository{
    private final JdbcTemplate jdbc;

    public JdbcVehicleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Vehicle> mapper = new RowMapper<>() {
        @Override
        public Vehicle mapRow(ResultSet rs, int rowNum) throws SQLException {
            Vehicle v = new Vehicle();
            v.setId(rs.getLong("vehicle_id"));
            v.setLicencePlate(rs.getString("licence_plate"));
            v.setCustomerId(rs.getLong("customer_id"));
            return v;
        }
    };

    @Override
    public Optional<Vehicle> findById(Long id) {
        var list = jdbc.query(
                "SELECT vehicle_id, licence_plate, customer_id " +
                        "FROM vehicle WHERE vehicle_id = ?",
                mapper,
                id
        );
        return list.stream().findFirst();
    }

    @Override
    public List<Vehicle> findByCustomerId(Long customerId) {
        return jdbc.query(
                "SELECT vehicle_id, licence_plate, customer_id " +
                        "FROM vehicle WHERE customer_id = ?",
                mapper,
                customerId
        );
    }

    @Override
    public List<Vehicle> findAll() {
        return jdbc.query(
                "SELECT vehicle_id, licence_plate, customer_id FROM vehicle",
                mapper
        );
    }

    @Override
    public List<Vehicle> findUnassigned() {
        return jdbc.query(
                "SELECT vehicle_id, licence_plate, customer_id " +
                        "FROM vehicle WHERE customer_id IS NULL",
                mapper
        );
    }

    @Override
    public Optional<Vehicle> findByLicencePlate(String licencePlate) {
        var list = jdbc.query(
                "SELECT vehicle_id, licence_plate, customer_id " +
                        "FROM vehicle WHERE licence_plate = ?",
                mapper,
                licencePlate
        );
        return list.stream().findFirst();
    }

    @Override
    public Vehicle save(Vehicle vehicle) {
        if (vehicle.getId() == null) {
            Long id = jdbc.queryForObject(
                    "INSERT INTO vehicle(licence_plate, customer_id) " +
                            "VALUES (?, ?) RETURNING vehicle_id",
                    Long.class,
                    vehicle.getLicencePlate(),
                    vehicle.getCustomerId()
            );
            vehicle.setId(id);
            return vehicle;
        } else {
            jdbc.update(
                    "UPDATE vehicle SET licence_plate = ?, customer_id = ? " +
                            "WHERE vehicle_id = ?",
                    vehicle.getLicencePlate(),
                    vehicle.getCustomerId(),
                    vehicle.getId()
            );
            return vehicle;
        }
    }
}
