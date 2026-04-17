package com.ecorating.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NearestLandscapingDto(
        @JsonProperty("work_name")
        String workName,
        @JsonProperty("work_essence")
        String workEssence,
        @JsonProperty("detailed_work")
        String detailedWork,
        @JsonProperty("address")
        String address,
        @JsonProperty("adm_area")
        String admArea,
        @JsonProperty("district")
        String district,
        @JsonProperty("year_of_work")
        Integer yearOfWork,
        @JsonProperty("work_status")
        String workStatus,
        @JsonProperty("volume")
        String volume,
        @JsonProperty("latitude")
        double latitude,
        @JsonProperty("longitude")
        double longitude,
        @JsonProperty("distance_meters")
        double distanceMeters,
        @JsonProperty("source")
        String source
) {
}
