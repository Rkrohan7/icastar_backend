package com.icastar.platform.dto.casting;

import com.icastar.platform.entity.JobApplication;
import lombok.Data;

@Data
public class SelectedArtistDto {
    private Long userId;
    private Long artistProfileId;
    private String name;
    private String avatarUrl;
    private JobApplication.ApplicationStatus status;

    public SelectedArtistDto() {}

    public SelectedArtistDto(JobApplication application) {
        if (application.getArtist() != null) {
            this.artistProfileId = application.getArtist().getId();
            this.userId = application.getArtist().getUser().getId();
            this.name = application.getArtist().getUser().getFirstName() + " " +
                       application.getArtist().getUser().getLastName();
            this.avatarUrl = application.getArtist().getProfileUrl();
        } else {
            // Guest applicant
            this.name = application.getGuestFullName();
        }
        this.status = application.getStatus();
    }
}