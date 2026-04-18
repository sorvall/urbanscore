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
@Table(name = "hazard_objects")
public class HazardObject {

    public enum HazardType {
        FACTORY,
        INCINERATOR,
        LANDFILL,
        TPP
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "hazard_type", nullable = false, length = 20)
    private HazardType hazardType;

    @JdbcTypeCode(SqlTypes.GEOGRAPHY)
    @Column(nullable = false, columnDefinition = "geography(Point,4326)")
    private Point location;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 100)
    private String source;

    /** global_id строки data.mos.ru — для повторного импорта без дублей. */
    @Column(name = "source_global_id")
    private Long sourceGlobalId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected HazardObject() {
    }

    public HazardObject(String name, HazardType hazardType, Point location, String description, String source) {
        this(name, hazardType, location, description, source, null);
    }

    public HazardObject(
            String name,
            HazardType hazardType,
            Point location,
            String description,
            String source,
            Long sourceGlobalId
    ) {
        this.name = name;
        this.hazardType = hazardType;
        this.location = location;
        this.description = description;
        this.source = source;
        this.sourceGlobalId = sourceGlobalId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public HazardType getHazardType() {
        return hazardType;
    }

    public Point getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    public String getSource() {
        return source;
    }

    public Long getSourceGlobalId() {
        return sourceGlobalId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
