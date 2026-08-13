package com.icastar.platform.dto.superadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncompleteProfileReminderResponseDto {

    private int totalTargeted;
    private int emailsSent;
    private int emailsFailed;
}
