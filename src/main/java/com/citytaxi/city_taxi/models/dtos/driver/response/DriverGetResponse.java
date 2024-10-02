package com.citytaxi.city_taxi.models.dtos.driver.response;

import com.citytaxi.city_taxi.models.enums.EDriverAvailabilityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DriverGetResponse {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private String driverLicense;
    private Integer avgRating;
    private EDriverAvailabilityStatus availability;
    private Double latitude;
    private Double longitude;
    private String locationName;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
