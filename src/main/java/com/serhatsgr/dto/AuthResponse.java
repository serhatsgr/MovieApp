package com.serhatsgr.dto;


public record AuthResponse(
        String accessToken,
        String refreshToken,
        String message,
        boolean success

) {

    //factory methods

   public static AuthResponse success(String accessToken, String refreshToken, String message) {
       return new AuthResponse(accessToken, refreshToken, message, true);
   }

   public static AuthResponse error(String message) {
       return new AuthResponse(null, null, message, false);
   }

}
