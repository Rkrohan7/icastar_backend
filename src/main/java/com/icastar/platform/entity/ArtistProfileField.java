package com.icastar.platform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "artist_profile_fields")
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArtistProfileField extends BaseEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_profile_id", nullable = false)
    @JsonIgnore
    private ArtistProfile artistProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_type_field_id", nullable = false)
    private ArtistTypeField artistTypeField;

    @Column(name = "field_value", columnDefinition = "TEXT")
    private String fieldValue;
    
    // File handling is now managed by Document entity, not stored here
}
