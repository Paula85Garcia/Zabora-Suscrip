package com.zabora.subscription.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos del usuario autenticado extraidos de los headers del API Gateway.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserData {

    private Integer userId;
    private String email;
    private String role;
}
