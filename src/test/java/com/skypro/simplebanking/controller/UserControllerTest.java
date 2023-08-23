package com.skypro.simplebanking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skypro.simplebanking.dto.CreateUserRequest;
import com.skypro.simplebanking.entity.Account;
import com.skypro.simplebanking.entity.AccountCurrency;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.repository.AccountRepository;
import com.skypro.simplebanking.repository.UserRepository;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.util.ArrayList;

import static com.skypro.simplebanking.authentication.AuthenticationUp.authenticationUp;
import static com.skypro.simplebanking.authentication.AuthenticationUp.authenticationUpforUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AccountRepository accountRepository;

    @AfterEach
    void cleanData(){
        userRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @SneakyThrows
    @Test
    void createUserWhenRoleIsAdminTest(@Value("${app.security.admin-token}") String token){

        CreateUserRequest userRequestTest = getUserRequest();
        String username = userRequestTest.getUsername();

        String jsonUser = objectMapper.writeValueAsString(userRequestTest);;

        mockMvc.perform(post("/user")
                        .header("X-SECURITY-ADMIN-KEY", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.accounts").isNotEmpty());
    }

    @SneakyThrows
    @Test
    void createUserWhenRoleIsUserTest(){
        authenticationUp(false);
        CreateUserRequest userRequestTest = getUserRequest();
        String jsonUser = objectMapper.writeValueAsString(userRequestTest);

        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonUser))
                .andExpect(status().isForbidden());
    }

    @SneakyThrows
    @Test
    void getAllUsersWhenRoleIsUserTest(){
        authenticationUp(false);

        User userTest1 = addNewUser();
        User userTest2 = addNewUser();

        mockMvc.perform(get("/user/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").value(userTest1.getUsername()))
                .andExpect(jsonPath("$[0].id").value(userTest1.getId()))
                .andExpect(jsonPath("$[0].accounts").isNotEmpty())
                .andExpect(jsonPath("$[1].username").value(userTest2.getUsername()))
                .andExpect(jsonPath("$[1].id").value(userTest2.getId()))
                .andExpect(jsonPath("$[1].accounts").isNotEmpty());
    }

    @SneakyThrows
    @Test
    void getAllUsersWhenRoleIsAdminTest() {
        authenticationUp(true);

        mockMvc.perform(get("/user/list"))
                .andExpect(status().isForbidden());
    }

    @SneakyThrows
    @Test
    void getMyProfileWhenRoleIsUserTest(){
        User userTest = addNewUser();

        authenticationUpforUser(userTest, false);

        mockMvc.perform(get("/user/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.id").value(userTest.getId()))
                .andExpect(jsonPath("$.username").isNotEmpty())
                .andExpect(jsonPath("$.username").value(userTest.getUsername()))
                .andExpect(jsonPath("$.accounts").isNotEmpty());
    }

    @SneakyThrows
    @Test
    void getMyProfileWhenRoleIsAdminTest(){
        authenticationUp(true);

        mockMvc.perform(get("/user/me"))
                .andExpect(status().isForbidden());
    }

    public CreateUserRequest getUserRequest(){
        final String name = RandomStringUtils.randomAlphabetic(7);
        final String password  = RandomStringUtils.randomAlphanumeric(8);
        CreateUserRequest userRequestTest = new CreateUserRequest();
        userRequestTest.setUsername(name);
        userRequestTest.setPassword(password);
        return userRequestTest;
    }

    @SneakyThrows
    public User addNewUser(){
        final String name = RandomStringUtils.randomAlphabetic(7);;
        final String password  = RandomStringUtils.randomAlphanumeric(8);
        final Long amount = 1L;
        User userTest = new User();
        userTest.setUsername(name);
        userTest.setPassword(password);

        userRepository.save(userTest);

        User user = userRepository.findByUsername(name).get();
        user.setAccounts(new ArrayList<>());

        Account accountTest = new Account();
        accountTest.setUser(userTest);
        accountTest.setAmount(amount);
        accountTest.setAccountCurrency(AccountCurrency.RUB);
        user.getAccounts().add(accountTest);

        accountRepository.save(accountTest);

        return user;
    }
}
