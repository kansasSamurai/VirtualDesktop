package org.jwellman.demo.files;

public class LocationData {
    private int year;
    private String agencyId;
    private String companyId;
    private String locationId;
    private String locationName;

    public LocationData(int year, String agencyId, String companyId, String locationId, String locationName) {
        this.year = year;
        this.agencyId = agencyId;
        this.companyId = companyId;
        this.locationId = locationId;
        this.locationName = locationName;
    }

    public int getYear() {
        return year;
    }

    public String getAgencyId() {
        return agencyId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getLocationId() {
        return locationId;
    }

    public String getLocationName() {
        return locationName;
    }

    @Override
    public String toString() {
        return "LocationData{" +
                "year=" + year +
                ", agencyId='" + agencyId + '\'' +
                ", companyId='" + companyId + '\'' +
                ", locationId='" + locationId + '\'' +
                ", locationName='" + locationName + '\'' +
                '}';
    }
}
