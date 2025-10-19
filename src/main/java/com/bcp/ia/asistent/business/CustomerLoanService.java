package com.bcp.ia.asistent.business;

import com.bcp.ia.asistent.model.dto.Strategies;
import com.bcp.ia.asistent.model.entity.CustomersEntity;
import reactor.core.publisher.Mono;

public interface CustomerLoanService {
    Mono<CustomersEntity> getCustomer(Integer customerId);

    Mono<Strategies> getStrategies(Integer customerId);
}
