package com.bcp.ia.asistent.business.impl;

import com.bcp.ia.asistent.business.CustomerLoanService;
import com.bcp.ia.asistent.caller.OfferMockService;
import com.bcp.ia.asistent.caller.OpenAiAssistantService;
import com.bcp.ia.asistent.model.dto.Scenarios;
import com.bcp.ia.asistent.model.dto.Strategies;
import com.bcp.ia.asistent.model.entity.CreditScoreHistoryEntity;
import com.bcp.ia.asistent.model.entity.CustomersEntity;
import com.bcp.ia.asistent.repository.CustomerRepository;
import com.nimbusds.jose.shaded.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.util.List;

import static com.bcp.ia.asistent.helper.ConsolidationDebtScenariosHelper.consolidationDebtScenario;
import static com.bcp.ia.asistent.helper.MinimDebtScenariosHelper.minimDebtScenario;
import static com.bcp.ia.asistent.helper.OptimizedDebtScenariosHelper.optimizedDebtScenario;
import static com.bcp.ia.asistent.helper.PaymentCalculatorHelper.getDisposableCashFlow;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerLoanServiceImpl implements CustomerLoanService {

    private final OpenAiAssistantService openAiAssistantService;
    private final CustomerRepository customerRepository;
    private final OfferMockService offerMockService;

    @Override
    public Mono<CustomersEntity> getCustomer(Integer customerId) {

        return Mono.just(customerId)
                .flatMap(this::getCustomerLoans);
    }

    private Mono<CustomersEntity> getCustomerLoans(Integer customerId) {
        return Mono.fromCallable(() -> customerRepository.findCustomerById(customerId))
                .subscribeOn(Schedulers.boundedElastic())
                .switchIfEmpty(Mono.empty());
    }

    private Mono<CustomersEntity> getCustomerLoansCardsCash(Integer customerId) {
        return Mono.fromCallable(() -> customerRepository.findCustomerDebtById(customerId))
                .subscribeOn(Schedulers.boundedElastic())
                .switchIfEmpty(Mono.empty());
    }

    @Override
    public Mono<Strategies> getStrategies(Integer customerId) {
        return Mono.just(customerId)
                .flatMap(this::getCustomerLoansCardsCash)
                .flatMap(customersEntity -> {

                    BigDecimal disposableCashFlow = getDisposableCashFlow(customersEntity);
                    Integer creditScore = customersEntity.getCreditScoreHistory()
                            .stream()
                            .findFirst()
                            .map(CreditScoreHistoryEntity::getCreditScore)
                            .orElse(0);

                    return getScenarios(customersEntity, creditScore, disposableCashFlow)
                            .map(scenarios -> Strategies.builder()
                                    .scenarios(scenarios)
                                    .build())
                            .flatMap( strategies ->
                                    openAiAssistantService.generateFinancialReport(new Gson().toJson(strategies))
                                            .map(
                                            financialReport -> {
                                                strategies.setCustomer(customersEntity.getFullName());
                                                strategies.setDisposableCashFlow(disposableCashFlow);
                                                strategies.setCreditScore(creditScore);
                                                strategies.setRecommendedStrategy(financialReport);
                                                return strategies;
                                            }));
                });
    }

    private Mono<List<Scenarios>> getScenarios(CustomersEntity customersEntity, Integer creditScore, BigDecimal disposableCashFlow) {

        return offerMockService.getMockOffers()
                .collectList()
                .map(offers -> {
                    Scenarios minimScenario = minimDebtScenario(customersEntity);
                    Scenarios optimizedScenario = optimizedDebtScenario(minimScenario, disposableCashFlow);
                    Scenarios consolidationScenario = consolidationDebtScenario(creditScore, minimScenario, offers);
                    // retornamos lista inmutable
                    return List.of(minimScenario, optimizedScenario, consolidationScenario);
                });
    }
}
