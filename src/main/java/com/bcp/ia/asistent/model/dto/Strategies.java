package com.bcp.ia.asistent.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
public class Strategies {
    private String customer;
    private BigDecimal disposableCashFlow;
    private Integer creditScore;
    private List<Scenarios> scenarios;
    private String recommendedStrategy;
}
