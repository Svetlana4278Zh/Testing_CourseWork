package com.skypro.simplebanking.authentication;

import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.entity.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthenticationUp {

    public static void authenticationUp(Boolean isAdmin) {

        BankingUserDetails userDetails;

        if (isAdmin) {
            userDetails = new BankingUserDetails(-1L, "admin", "****", true);
        } else {
            userDetails = new BankingUserDetails(1L, "user1", "password1", false);
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public static void authenticationUpforUser(User user, Boolean isAdmin){
        long idTest = user.getId();
        String usernameTest = user.getUsername();
        String passwordTest = user.getPassword();

        BankingUserDetails userDetails = new BankingUserDetails(idTest, usernameTest, passwordTest, isAdmin);

        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

    }
}
