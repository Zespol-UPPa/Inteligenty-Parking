package com.smartparking.parking_service.dto;

import java.util.List;

/**
 * DTO for creating a new parking with sections
 */
public class AddParkingRequest {

    private String name;
    private String address;
    private Long companyId;
    private List<SectionDto> sections;

    public static class SectionDto {
        private String prefix;        // A, B, C, etc.
        private Integer numberOfSpots; // How many spots in this section
        private Integer floorLevel;   // Which floor
        private Boolean reservable;   // Can spots be reserved?

        public SectionDto() {}

        public SectionDto(String prefix, Integer numberOfSpots, Integer floorLevel, Boolean reservable) {
            this.prefix = prefix;
            this.numberOfSpots = numberOfSpots;
            this.floorLevel = floorLevel;
            this.reservable = reservable;
        }

        // Getters and Setters
        public String getPrefix() { return prefix; }
        public void setPrefix(String prefix) { this.prefix = prefix; }

        public Integer getNumberOfSpots() { return numberOfSpots; }
        public void setNumberOfSpots(Integer numberOfSpots) { this.numberOfSpots = numberOfSpots; }

        public Integer getFloorLevel() { return floorLevel; }
        public void setFloorLevel(Integer floorLevel) { this.floorLevel = floorLevel; }

        public Boolean getReservable() { return reservable; }
        public void setReservable(Boolean reservable) { this.reservable = reservable; }
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }

    public List<SectionDto> getSections() { return sections; }
    public void setSections(List<SectionDto> sections) { this.sections = sections; }
}