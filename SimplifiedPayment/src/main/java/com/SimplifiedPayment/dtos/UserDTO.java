package com.SimplifiedPayment.dtos;

import com.SimplifiedPayment.domain.user.UserType;

import java.math.BigDecimal;

public record UserDTO(String firstName, String lastName, String Document, BigDecimal balance, String email, String password, UserType userType) {
}
