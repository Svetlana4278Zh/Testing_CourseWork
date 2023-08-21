package com.skypro.simplebanking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skypro.simplebanking.dto.TransferRequest;
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
public class TransferControllerTest {
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
    void transferWhenRoleIsUserTest(){
        User userSender = addNewUserAndAccount();
        User userRecipient = addNewUserAndAccount();
        Account accountSender = userSender.getAccounts().stream().findFirst().get();

        authenticationUpforUser(userSender, false);

        String jsonTransferRequest = getJsonTransferRequest(userSender, userRecipient);

        mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTransferRequest))
                .andExpect(status().isOk());

        mockMvc.perform(get("/account/{id}", userSender.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountSender.getId()))
                .andExpect(jsonPath("$.amount").value(0L));
    }

    @SneakyThrows
    @Test
    void transferByAnotherUserTest(){
        User userSender = addNewUserAndAccount();
        User userRecipient = addNewUserAndAccount();

        authenticationUpforUser(userRecipient, false);

        String jsonTransferRequest = getJsonTransferRequest(userSender, userRecipient);

        mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTransferRequest))
                .andExpect(status().isNotFound());
    }

    @SneakyThrows
    @Test
    void transferWhenRoleIsAdminTest(){
        authenticationUp(true);

        User userSender = addNewUserAndAccount();
        User userRecipient = addNewUserAndAccount();

        String jsonTransferRequest = getJsonTransferRequest(userSender, userRecipient);

        mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTransferRequest))
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

    @SneakyThrows
    public String getJsonTransferRequest(User userSender, User userRecipient){
        Account accountSender = userSender.getAccounts().stream().findFirst().get();
        Account accountRecipient = userRecipient.getAccounts().stream().findFirst().get();

        long fromAccountId = accountSender.getId();
        long toUserId = userRecipient.getId();
        long toAccountId = accountRecipient.getId();
        long amount = accountSender.getAmount();

        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setFromAccountId(fromAccountId);
        transferRequest.setToUserId(toUserId);
        transferRequest.setToAccountId(toAccountId);
        transferRequest.setAmount(amount);

        return objectMapper.writeValueAsString(transferRequest);
    }
}
