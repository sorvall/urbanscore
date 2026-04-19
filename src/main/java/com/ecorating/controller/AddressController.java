package com.ecorating.controller;

import com.ecorating.client.AddressGeocoder;
import com.ecorating.client.AddressGeocoder.AddressResolution;
import com.ecorating.dto.AddressResponse;
import com.ecorating.dto.ApiResponse;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AddressController {

    private final AddressGeocoder addressGeocoder;

    public AddressController(AddressGeocoder addressGeocoder) {
        this.addressGeocoder = addressGeocoder;
    }

    /**
     * Адрес по координатам (DaData geolocate). Нужен для заполнения поля {@code address} перед {@code POST /report}.
     */
    @GetMapping("/address")
    public ApiResponse<AddressResponse> address(
            @RequestParam("lat") double lat, @RequestParam("lon") double lon) {
        Optional<AddressResolution> resolved = addressGeocoder.resolveFromCoordinates(lat, lon);
        String line = resolved.map(AddressResolution::bestAddressLine).orElse(null);
        if (line == null || line.isBlank()) {
            line = lat + ", " + lon;
        }
        return ApiResponse.ok(new AddressResponse(line));
    }
}
