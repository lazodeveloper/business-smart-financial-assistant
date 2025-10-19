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

import java.io.PrintWriter;
import java.io.StringWriter;

@RestController
@Slf4j
@CrossOrigin(origins = {"http://localhost:4200", "https://mi-frontend-angular-app.azurewebsites.net"})
@RequiredArgsConstructor
@RequestMapping("/financial-strategy")
public class AsistentApiImpl {

    private final CustomerLoanService customerLoanService;

    @GetMapping("/customers/{id}")
    public  Mono<ResponseEntity<CustomersEntity>> getCustomer(@PathVariable("id") Integer customerId){

        //return Mono.just(ResponseEntity.ok("Hola Mundo"));
        return customerLoanService.getCustomer(customerId)
                .flatMap(customerEntity -> Mono.just(ResponseEntity.ok(customerEntity)))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
                .doOnError(throwable -> log.error(String.valueOf(throwable)))
                .onErrorResume(throwable -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .build()));
    }

    @GetMapping("/strategies/{id}")
    public  Mono<ResponseEntity<Strategies>> getStrategies(@PathVariable("id") Integer customerId){

        //return Mono.just(ResponseEntity.ok("Hola Mundo"));
        return customerLoanService.getStrategies(customerId)
                .flatMap(customerEntity -> Mono.just(ResponseEntity.ok(customerEntity)))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
                .doOnError(throwable -> log.info(String.valueOf(throwable)))
                .onErrorResume(throwable -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build()));
    }
}
