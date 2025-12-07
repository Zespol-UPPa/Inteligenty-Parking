package com.smartparking.parking_service.repository;

import com.smartparking.parking_service.model.ParkingLocation;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcParkingLocationRepository implements ParkingLocationRepository {

    private final JdbcTemplate jdbc;

    public JdbcParkingLocationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<ParkingLocation> mapper = new RowMapper<>() {
        @Override
        public ParkingLocation mapRow(ResultSet rs, int rowNum) throws SQLException {
            ParkingLocation p = new ParkingLocation();
            p.setId(rs.getLong("parking_id"));
            p.setNameParking(rs.getString("name_parking"));
            p.setAddressLine(rs.getString("address_line"));
            p.setRefCompanyId(rs.getLong("ref_company_id"));
            return p;
        }
    };

    @Override
    public Optional<ParkingLocation> findById(Long id) {
        var list = jdbc.query(
                "SELECT parking_id, name_parking, address_line, ref_company_id " +
                        "FROM parking_location WHERE parking_id = ?",
                mapper,
                id
        );
        return list.stream().findFirst();
    }

    @Override
    public List<ParkingLocation> findByCompanyId(Long companyId) {
        return jdbc.query(
                "SELECT parking_id, name_parking, address_line, ref_company_id " +
                        "FROM parking_location WHERE ref_company_id = ?",
                mapper,
                companyId
        );
    }

    @Override
    public List<ParkingLocation> findAll() {
        return jdbc.query(
                "SELECT parking_id, name_parking, address_line, ref_company_id " +
                        "FROM parking_location",
                mapper
        );
    }

    @Override
    public ParkingLocation save(ParkingLocation location) {
        if (location.getId() == null) {
            Long id = jdbc.queryForObject(
                    "INSERT INTO parking_location(name_parking, address_line, ref_company_id) " +
                            "VALUES (?, ?, ?) RETURNING parking_id",
                    Long.class,
                    location.getNameParking(),
                    location.getAddressLine(),
                    location.getRefCompanyId()
            );
            location.setId(id);
            return location;
        } else {
            jdbc.update(
                    "UPDATE parking_location SET name_parking = ?, address_line = ?, ref_company_id = ? " +
                            "WHERE parking_id = ?",
                    location.getNameParking(),
                    location.getAddressLine(),
                    location.getRefCompanyId(),
                    location.getId()
            );
            return location;
        }
    }
}
