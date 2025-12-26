package com.smartparking.admin_service.client;

import com.smartparking.admin_service.dto.IdResponse;
import com.smartparking.admin_service.dto.ParkingUsageDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class ParkingAdminClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ParkingAdminClient(RestTemplate restTemplate,
                              @Value("${PARKING_SERVICE_URL:http://parking-service:8083}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public long createLocation(String name, String address, Long companyId) {
        try {
            String url = baseUrl + "/parking/admin/locations?name={name}&address={address}&companyId={companyId}";
            ResponseEntity<IdResponse> response = restTemplate.postForEntity(
                    url,
                    null,
                    IdResponse.class,
                    Map.of("name", name, "address", address, "companyId", companyId)
            );
            IdResponse body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("Parking-service did not return location id");
            }
            return body.getId();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create location in parking-service: " + e.getMessage(), e);
        }
    }

    public long createSpot(Long locationId, String code, Integer floorLvl, boolean toReserved, String type) {
        try {
            String url = baseUrl + "/parking/admin/spots?locationId={locationId}&code={code}&floorLvl={floorLvl}&toReserved={toReserved}&type={type}";
            ResponseEntity<IdResponse> response = restTemplate.postForEntity(
                    url,
                    null,
                    IdResponse.class,
                    Map.of(
                            "locationId", locationId,
                            "code", code,
                            "floorLvl", floorLvl,
                            "toReserved", toReserved,
                            "type", type
                    )
            );
            IdResponse body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("Parking-service did not return spot id");
            }
            return body.getId();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create spot in parking-service: " + e.getMessage(), e);
        }
    }

    public List<ParkingUsageDto> usageReport() {
        try {
            String url = baseUrl + "/parking/admin/reports/usage";
            ResponseEntity<ParkingUsageDto[]> response =
                    restTemplate.getForEntity(url, ParkingUsageDto[].class);
            ParkingUsageDto[] body = response.getBody();
            if (body == null) {
                return List.of();
            }
            return List.of(body);
        } catch (Exception e) {
            return List.of();
        }
    }
}


