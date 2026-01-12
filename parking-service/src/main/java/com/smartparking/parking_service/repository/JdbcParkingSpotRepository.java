package com.smartparking.parking_service.repository;

import com.smartparking.parking_service.model.ParkingSpot;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcParkingSpotRepository implements ParkingSpotRepository {

    private final JdbcTemplate jdbc;

    public JdbcParkingSpotRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<ParkingSpot> mapper = new RowMapper<>() {
        @Override
        public ParkingSpot mapRow(ResultSet rs, int rowNum) throws SQLException {
            ParkingSpot s = new ParkingSpot();
            s.setId(rs.getLong("spot_id"));
            s.setCode(rs.getString("code"));
            s.setFloorLvl(rs.getInt("floor_lvl"));
            s.setToReserved(rs.getBoolean("to_reserved"));
            s.setType(rs.getString("type"));
            s.setParkingId(rs.getLong("id_parking"));
            return s;
        }
    };

    @Override
    public Optional<ParkingSpot> findById(Long id) {
        var list = jdbc.query(
                "SELECT spot_id, code, floor_lvl, to_reserved, type, id_parking " +
                        "FROM parking_spot WHERE spot_id = ?",
                mapper,
                id
        );
        return list.stream().findFirst();
    }

    @Override
    public List<ParkingSpot> findByParkingId(Long parkingId) {
        return jdbc.query(
                "SELECT spot_id, code, floor_lvl, to_reserved, type, id_parking " +
                        "FROM parking_spot WHERE id_parking = ?",
                mapper,
                parkingId
        );
    }

    @Override
    public List<ParkingSpot> findAll() {
        return jdbc.query(
                "SELECT spot_id, code, floor_lvl, to_reserved, type, id_parking " +
                        "FROM parking_spot",
                mapper
        );
    }

    @Override
    public Long countAllSpotsByParkingId(Long parkingId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM parking_spot WHERE id_parking = ?",
                Long.class,
                parkingId
        );
    }


    @Override
    public ParkingSpot save(ParkingSpot spot) {
        if (spot.getId() == null) {
            Long id = jdbc.queryForObject(
                    "INSERT INTO parking_spot(code, floor_lvl, to_reserved, type, id_parking) " +
                            "VALUES (?, ?, ?, 'Available', ?) RETURNING spot_id",
                    Long.class,
                    spot.getCode(),
                    spot.getFloorLvl(),
                    spot.isToReserved(),
                    spot.getParkingId()
            );
            spot.setId(id);
            return spot;
        } else {
            jdbc.update(
                    "UPDATE parking_spot SET code = ?, floor_lvl = ?, to_reserved = ?, type = ?, id_parking = ? " +
                            "WHERE spot_id = ?",
                    spot.getCode(),
                    spot.getFloorLvl(),
                    spot.isToReserved(),
                    spot.getType(),
                    spot.getParkingId(),
                    spot.getId()
            );
            return spot;
        }
    }
}
