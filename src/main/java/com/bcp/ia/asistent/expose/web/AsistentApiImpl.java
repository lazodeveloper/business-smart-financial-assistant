package com.bcp.ia.asistent.expose.web;

import com.bcp.ia.asistent.business.CustomerLoanService;
import com.bcp.ia.asistent.model.dto.Strategies;
import com.bcp.ia.asistent.model.entity.CustomersEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
@CrossOrigin(origins = {"http://localhost:4200", "https://salmon-river-028790710.3.azurestaticapps.net"})
@RequiredArgsConstructor
@RequestMapping("/financial-strategy")
public class AsistentApiImpl {

    private final CustomerLoanService customerLoanService;

    @GetMapping("/customers/{id}")
    public  Mono<ResponseEntity<CustomersEntity>> getCustomer(@PathVariable("id") Integer customerId){

        return customerLoanService.getCustomer(customerId)
                .flatMap(customerEntity -> Mono.just(ResponseEntity.ok(customerEntity)))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
                .doOnError(throwable -> log.error(String.valueOf(throwable)))
                .onErrorResume(throwable -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .build()));
    }

    @GetMapping("/strategies/{id}")
    public  Mono<ResponseEntity<Strategies>> getStrategies(@PathVariable("id") Integer customerId){

        return customerLoanService.getStrategies(customerId)
                .flatMap(customerEntity -> Mono.just(ResponseEntity.ok(customerEntity)))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
                .doOnError(throwable -> log.info(String.valueOf(throwable)))
                .onErrorResume(throwable -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build()));
    }
}
