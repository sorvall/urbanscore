package com.ecorating.controller;

import com.ecorating.client.AddressGeocoder;
import com.ecorating.client.AddressGeocoder.AddressResolution;
import com.ecorating.dto.AddressResponse;
import com.ecorating.dto.ApiResponse;
import com.ecorating.dto.GeocodeResponse;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class AddressController {

    private final AddressGeocoder addressGeocoder;

    public AddressController(AddressGeocoder addressGeocoder) {
        this.addressGeocoder = addressGeocoder;
    }

    /**
     * Геокодирование строки адреса (DaData suggest) — координаты и нормализованная строка для карты и отчёта.
     */
    @GetMapping("/geocode")
    public ApiResponse<GeocodeResponse> geocode(@RequestParam("q") String q) {
        if (q == null || q.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Пустой запрос адреса");
        }
        Optional<AddressResolution> resolved = addressGeocoder.resolveFromAddressQuery(q.trim());
        if (resolved.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Адрес не найден. Уточните запрос.");
        }
        AddressResolution r = resolved.get();
        String line = r.bestAddressLine();
        if (line == null || line.isBlank()) {
            line = q.trim();
        }
        return ApiResponse.ok(new GeocodeResponse(line, r.latitude(), r.longitude()));
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
