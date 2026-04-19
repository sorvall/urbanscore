package com.ecorating.client;

import java.util.Optional;

public interface AddressGeocoder {

    Optional<AddressResolution> resolveFromCoordinates(double latitude, double longitude);

    record AddressResolution(
            double latitude,
            double longitude,
            String formattedAddress,
            String unrestrictedAddress,
            String cityArea,
            String cityDistrict,
            String cityDistrictWithType,
            String area,
            String areaWithType,
            String region,
            String regionWithType
    ) {
        public String bestAddressLine() {
            if (unrestrictedAddress != null && !unrestrictedAddress.isBlank()) {
                return unrestrictedAddress.trim();
            }
            if (formattedAddress != null && !formattedAddress.isBlank()) {
                return formattedAddress.trim();
            }
            return null;
        }
    }
}
