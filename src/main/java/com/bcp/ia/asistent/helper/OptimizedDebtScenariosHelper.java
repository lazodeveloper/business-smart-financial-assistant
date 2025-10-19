package com.bcp.ia.asistent.helper;

import com.bcp.ia.asistent.model.dto.DebtDetails;
import com.bcp.ia.asistent.model.dto.Scenarios;
import com.bcp.ia.asistent.model.entity.CustomersEntity;
import com.nimbusds.jose.shaded.gson.Gson;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;

import static com.bcp.ia.asistent.helper.PaymentCalculatorHelper.*;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OptimizedDebtScenariosHelper {

    public static Scenarios optimizedDebtScenario(Scenarios minimDebtScenario, BigDecimal disposableCashFlow) {


        return Optional.ofNullable(minimDebtScenario)
                // 1️⃣ Calcular el total de pago mínimo
                .map(scenario ->
                        disposableCashFlow.subtract(scenario.getTotalMinPayment()
                ))
                // 2️⃣ Filtrar solo si el flujo de caja alcanza el pago mínimo
                .filter(excedentCash -> excedentCash.compareTo(BigDecimal.ZERO) > 0)
                // 3️⃣ Si alcanza, optimizamos
                .map(excedentCash ->  {

                    DebtDetails deudaPrioritaria = Optional.ofNullable(findMostDelinquentDebt(minimDebtScenario.getDebtDetails()))
                                    .orElseGet(() -> findDebtHighestInterest(minimDebtScenario.getDebtDetails()));
                   // deudaPrioritaria.setProductId("priorityDebt");
                    log.info("deudaPrioritaria" + new Gson().toJson(deudaPrioritaria));

                    BigDecimal interestSavingsOnDebt = deudaPrioritaria.getProductType().equals("tarjeta") ?
                            calcularAhorroInteresesCard(
                                    deudaPrioritaria.getCurrentBalance(),
                                    deudaPrioritaria.getAnnualRatePct(),
                                    deudaPrioritaria.getMinimumPayment(),
                                    excedentCash
                            )
                            : calcularAhorroInteresesConPagoExtra(
                                    deudaPrioritaria.getCurrentBalance(),
                                    deudaPrioritaria.getAnnualRatePct(),
                                    deudaPrioritaria.getFinalTermMonths(),
                                    excedentCash
                            );
                    log.info("interestSavingsOnDebt: " + interestSavingsOnDebt);

                    List<DebtDetails> updatedDebts = minimDebtScenario.getDebtDetails().stream()
                            .map(debt -> debt.getProductId().equals(deudaPrioritaria.getProductId())
                                    ? debt.toBuilder()
                                    .paymentPriority(1)
                                    .extraPaymentFromFCD(excedentCash)
                                    .totalRecommendedPayment(debt.getMinimumPayment().add(excedentCash))
                                    .interestSavingsOnDebt(interestSavingsOnDebt)
                                    .build()
                                    : debt.toBuilder()
                                    .paymentPriority(0)
                                    .extraPaymentFromFCD(BigDecimal.valueOf(0.0))
                                    .totalRecommendedPayment(BigDecimal.valueOf(0.0))
                                    .interestSavingsOnDebt(BigDecimal.valueOf(0.0))
                                    .build()
                            )
                            .toList();
                    log.info("updatedDebts: " + new Gson().toJson(updatedDebts));

                            return minimDebtScenario.toBuilder()
                                    .scenarioName("optimizedDebtScenario")
                                    .scenarioDescription("Escenario Optimizado: Aplicación de excedente al pago de deuda prioritaria")
                                    .totalExtraPayment(updatedDebts.stream()
                                            .map(DebtDetails::getExtraPaymentFromFCD)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add))
                                    .totalRecommendedPayment(updatedDebts.stream()
                                            .map(DebtDetails::getTotalRecommendedPayment)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add))
                                    .totalEstimatedInterestSavings(updatedDebts.stream()
                                            .map(DebtDetails::getInterestSavingsOnDebt)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add))
                                    .debtDetails(updatedDebts)
                                    .build();
                        })
                // 4️⃣ Si no cumple condiciones, devolver el escenario original
                .orElse(minimDebtScenario);
    }

    private static DebtDetails findMostDelinquentDebt(List<DebtDetails> debtDetails) {
        return debtDetails.stream()
                // 1️⃣ Filtrar solo deudas con mora (> 0 días)
                .filter(d -> Optional.ofNullable(d.getDaysPastDue()).orElse(0) > 0)
                // 2️⃣ Seleccionar la deuda con más días de mora, y si hay empate, la de mayor tasa de interés
                .max(Comparator
                        .comparing((DebtDetails d) -> Optional.ofNullable(d.getDaysPastDue()).orElse(0))
                        .thenComparing(d -> Optional.ofNullable(d.getAnnualRatePct()).orElse(BigDecimal.ZERO))
                )
                // 3️⃣ Si no hay ninguna con mora, devolver null
                .orElse(null);
    }

    private static DebtDetails findDebtHighestInterest(List<DebtDetails> debtDetails) {
        return debtDetails.stream()
                .filter(Objects::nonNull)
                .max(Comparator.comparing(
                        d -> Optional.ofNullable(d.getAnnualRatePct()).orElse(BigDecimal.ZERO)
                ))
                .orElse(null);
    }
}
