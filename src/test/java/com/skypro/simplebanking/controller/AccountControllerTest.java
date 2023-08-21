package com.skypro.simplebanking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skypro.simplebanking.dto.BalanceChangeRequest;
import com.skypro.simplebanking.entity.Account;
import com.skypro.simplebanking.entity.AccountCurrency;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.repository.AccountRepository;
import com.skypro.simplebanking.repository.UserRepository;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
public class AccountControllerTest {

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
    void getUserAccountWhenRoleIsUserTest(){
        User userTest = addNewUserAndAccount();
        Account accountTest = userTest.getAccounts().stream().findFirst().get();

        authenticationUpforUser(userTest, false);

        mockMvc.perform(get("/account/{id}", accountTest.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountTest.getId()))
                .andExpect(jsonPath("$.amount").value(accountTest.getAmount()))
                .andExpect(jsonPath("$.currency").value(accountTest.getAccountCurrency().toString()));
    }

    @SneakyThrows
    @Test
    void getUserAccountWhenRoleIsAdminTest(){
        authenticationUp(true);
        User userTest = addNewUserAndAccount();
        Account accountTest = userTest.getAccounts().stream().findFirst().get();

        mockMvc.perform(get("/account/{id}", accountTest.getId()))
                .andExpect(status().isForbidden());
    }

    @SneakyThrows
    @Test
    void depositToAccountWhenRoleIsUserTest(){
        User userTest = addNewUserAndAccount();
        Account accountTest = userTest.getAccounts().stream().findFirst().get();

        authenticationUpforUser(userTest, false);

        long amountChange = 10L;
        long amountExpected = accountTest.getAmount() + amountChange;
        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest();
        balanceChangeRequest.setAmount(amountChange);
        String jsonBalanceChange = objectMapper.writeValueAsString(balanceChangeRequest);

        mockMvc.perform(post("/account/deposit/{id}", accountTest.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBalanceChange))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountTest.getId()))
                .andExpect(jsonPath("$.amount").value(amountExpected))
                .andExpect(jsonPath("$.currency").value(accountTest.getAccountCurrency().toString()));
    }

    @SneakyThrows
    @Test
    void depositToAccountByAnotherUserTest() {
        authenticationUp(false);
        User userTest = addNewUserAndAccount();
        Account accountTest = userTest.getAccounts().stream().findFirst().get();

        long amountChange = 10L;
        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest();
        balanceChangeRequest.setAmount(amountChange);
        String jsonBalanceChange = objectMapper.writeValueAsString(balanceChangeRequest);

        mockMvc.perform(post("/account/deposit/{id}", accountTest.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBalanceChange))
                .andExpect(status().isNotFound());
    }

    @SneakyThrows
    @Test
    void depositToAccountWhenRoleIsAdminTest(){
        authenticationUp(true);
        User userTest = addNewUserAndAccount();
        Account accountTest = userTest.getAccounts().stream().findFirst().get();

        long amountChange = 10L;
        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest();
        balanceChangeRequest.setAmount(amountChange);
        String jsonBalanceChange = objectMapper.writeValueAsString(balanceChangeRequest);

        mockMvc.perform(post("/account/deposit/{id}", accountTest.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBalanceChange))
                .andExpect(status().isForbidden());
    }

    @SneakyThrows
    @Test
    void withdrawFromAccountWhenRoleIsUserTest(){
        User userTest = addNewUserAndAccount();
        Account accountTest = userTest.getAccounts().stream().findFirst().get();

        authenticationUpforUser(userTest, false);

        long amountChange = accountTest.getAmount();
        long amountExpected = accountTest.getAmount() - amountChange;
        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest();
        balanceChangeRequest.setAmount(amountChange);
        String jsonBalanceChange = objectMapper.writeValueAsString(balanceChangeRequest);

        mockMvc.perform(post("/account/withdraw/{id}", accountTest.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBalanceChange))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountTest.getId()))
                .andExpect(jsonPath("$.amount").value(amountExpected))
                .andExpect(jsonPath("$.currency").value(accountTest.getAccountCurrency().toString()));
    }

    @SneakyThrows
    @Test
    void withdrawFromAccountByAnotherUserTest() {
        authenticationUp(false);
        User userTest = addNewUserAndAccount();
        Account accountTest = userTest.getAccounts().stream().findFirst().get();

        long amountChange = accountTest.getAmount();
        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest();
        balanceChangeRequest.setAmount(amountChange);
        String jsonBalanceChange = objectMapper.writeValueAsString(balanceChangeRequest);

        mockMvc.perform(post("/account/withdraw/{id}", accountTest.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBalanceChange))
                .andExpect(status().isNotFound());
    }

    @SneakyThrows
    @Test
    void withdrawFromAccountInvalidAmountTest(){
        User userTest = addNewUserAndAccount();
        Account accountTest = userTest.getAccounts().stream().findFirst().get();

        authenticationUpforUser(userTest, false);

        long amountChange = accountTest.getAmount() + 10;
        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest();
        balanceChangeRequest.setAmount(amountChange);
        String jsonBalanceChange = objectMapper.writeValueAsString(balanceChangeRequest);

        mockMvc.perform(post("/account/withdraw/{id}", accountTest.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBalanceChange))
                .andExpect(status().isBadRequest());
    }

    @SneakyThrows
    @Test
    void withdrawFromAccountWhenRoleIsAdminTest(){
        authenticationUp(true);

        User userTest = addNewUserAndAccount();
        Account accountTest = userTest.getAccounts().stream().findFirst().get();

        long amountChange = 1L;
        BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest();
        balanceChangeRequest.setAmount(amountChange);
        String jsonBalanceChange = objectMapper.writeValueAsString(balanceChangeRequest);

        mockMvc.perform(post("/account/withdraw/{id}", accountTest.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBalanceChange))
                .andExpect(status().isForbidden());
    }

    @SneakyThrows
    public User addNewUserAndAccount(){
        final String name = RandomStringUtils.randomAlphabetic(7);;
        final String password  = RandomStringUtils.randomAlphanumeric(8);
        final long amount = 1L;
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
