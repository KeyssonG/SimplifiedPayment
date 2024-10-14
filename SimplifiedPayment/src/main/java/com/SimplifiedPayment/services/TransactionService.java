package com.SimplifiedPayment.services;

import com.SimplifiedPayment.domain.transaction.Transaction;
import com.SimplifiedPayment.domain.user.User;
import com.SimplifiedPayment.dtos.TransactionDTO;
import com.SimplifiedPayment.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class TransactionService {

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionRepository repository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private NotificationService notificationService;

    public Transaction createTransaction(TransactionDTO transaction) throws Exception {
        User sender = this.userService.findUserBiId(transaction.senderId());
        User receiver = this.userService.findUserBiId(transaction.receiverId());

        userService.validateTransaction(sender, transaction.value());

        boolean isAuthorized = this.authorizeTransaction(sender, transaction.value());
        if(isAuthorized) {
            throw new Exception("Transação não autorizada");
        }

        Transaction newTransaction = new Transaction();
        newTransaction.setAmount(transaction.value());
        newTransaction.setSender(sender);
        newTransaction.setReceiver(receiver);
        newTransaction.setTimestamp(LocalDateTime.now());

        sender.setBalance((sender.getBalance().subtract(transaction.value())));
        receiver.setBalance(receiver.getBalance().add(transaction.value()));

        this.repository.save(newTransaction);
        this.userService.saveUser(sender);
        this.userService.saveUser(receiver);

        this.notificationService.sendNotification(sender, "Transação realizada com sucesso!");
        this.notificationService.sendNotification(receiver, "Transação recebida com sucesso!");

        return newTransaction;
    }

    public boolean authorizeTransaction(User sender, BigDecimal value) {
       ResponseEntity<Map> authorizationResponse = restTemplate.getForEntity("https://util.devi.tools/api/v2/authorize", Map.class);

       if (authorizationResponse.getStatusCode() == HttpStatus.OK) {
            String authorization = (String) authorizationResponse.getBody().get("true");
           return "Autorizado".equalsIgnoreCase(authorization);
       } else return false;
    }
}
