package com.ecorating.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "green_zones")
public class GreenZone {

    public enum ZoneType {
        PARK,
        FOREST,
        SQUARE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "osm_id")
    private Long osmId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone_type", nullable = false, length = 20)
    private ZoneType zoneType;

    @JdbcTypeCode(SqlTypes.GEOGRAPHY)
    @Column(nullable = false, columnDefinition = "geography(Point,4326)")
    private Point location;

    @Column(name = "area_m2")
    private Double areaM2;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected GreenZone() {
    }

    public GreenZone(Long osmId, String name, ZoneType zoneType, Point location, Double areaM2) {
        this.osmId = osmId;
        this.name = name;
        this.zoneType = zoneType;
        this.location = location;
        this.areaM2 = areaM2;
    }

    public Long getId() {
        return id;
    }

    public Long getOsmId() {
        return osmId;
    }

    public String getName() {
        return name;
    }

    public ZoneType getZoneType() {
        return zoneType;
    }

    public Point getLocation() {
        return location;
    }

    public Double getAreaM2() {
        return areaM2;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setZoneType(ZoneType zoneType) {
        this.zoneType = zoneType;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public void setAreaM2(Double areaM2) {
        this.areaM2 = areaM2;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
