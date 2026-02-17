package com.zabora.subscription.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserData {
    private String userId;
    private String email;
    private String role;
}
