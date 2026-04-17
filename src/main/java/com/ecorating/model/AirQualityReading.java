package com.ecorating.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "air_quality_readings")
public class AirQualityReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lon;

    @JdbcTypeCode(SqlTypes.GEOGRAPHY)
    @Column(nullable = false, columnDefinition = "geography(Point,4326)")
    private Point location;

    @Column(name = "pm25")
    private Double pm25;

    @Column(name = "pm10")
    private Double pm10;

    @Column(name = "no2")
    private Double no2;

    @Column(name = "aqi_score", nullable = false)
    private Integer aqiScore;

    @Column(nullable = false, length = 100)
    private String source;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected AirQualityReading() {
    }

    public AirQualityReading(
            Double lat,
            Double lon,
            Point location,
            Double pm25,
            Double pm10,
            Double no2,
            Integer aqiScore,
            String source,
            LocalDateTime recordedAt
    ) {
        this.lat = lat;
        this.lon = lon;
        this.location = location;
        this.pm25 = pm25;
        this.pm10 = pm10;
        this.no2 = no2;
        this.aqiScore = aqiScore;
        this.source = source;
        this.recordedAt = recordedAt;
    }

    public Long getId() {
        return id;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLon() {
        return lon;
    }

    public Point getLocation() {
        return location;
    }

    public Double getPm25() {
        return pm25;
    }

    public Double getPm10() {
        return pm10;
    }

    public Double getNo2() {
        return no2;
    }

    public Integer getAqiScore() {
        return aqiScore;
    }

    public String getSource() {
        return source;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
