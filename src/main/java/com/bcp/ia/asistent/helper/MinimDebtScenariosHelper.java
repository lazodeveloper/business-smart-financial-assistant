package com.bcp.ia.asistent.helper;

import com.bcp.ia.asistent.model.dto.DebtDetails;
import com.bcp.ia.asistent.model.dto.Scenarios;
import com.bcp.ia.asistent.model.entity.CardsEntity;
import com.bcp.ia.asistent.model.entity.CustomersEntity;
import com.bcp.ia.asistent.model.entity.LoansEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.bcp.ia.asistent.helper.PaymentCalculatorHelper.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MinimDebtScenariosHelper {

    public static Scenarios minimDebtScenario(CustomersEntity customersEntity) {

        List<DebtDetails> debtLoans = getDebtLoans(customersEntity.getLoans());
        List<DebtDetails> debtCards = getDebtCard(customersEntity.getCards());

        List<DebtDetails> debtDetails = Stream.concat(debtLoans.stream(), debtCards.stream()).toList();

        return Scenarios.builder()
                .scenarioName("minimDebtScenario")
                .scenarioDescription("Escenario Base: Pago Mínimo (El Costo Real)")
                .totalDebtAmount(debtDetails.stream()
                        .map(DebtDetails::getCurrentBalance)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .totalMinPayment(debtDetails.stream()
                        .map(DebtDetails::getMinimumPayment)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .totalTermMonths(debtDetails.stream().map(DebtDetails::getFinalTermMonths)
                        .filter(Objects::nonNull)
                        .mapToInt(Integer::intValue)
                        .sum())
                .totalInterestPaid(debtDetails.stream()
                                .map(DebtDetails::getTotalInterest)
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .totalAverageRate(calculateWeightedAverageRate(debtDetails))
                .debtDetails(debtDetails)
                .build();
    }

    private static List<DebtDetails> getDebtLoans(Set<LoansEntity> loansEntity) {
        return Optional.ofNullable(loansEntity)
                .orElse(Set.<LoansEntity>of())
                .stream()
                .map(loan -> DebtDetails.builder()
                        .productId("l-" + String.valueOf(loan.getLoanId()))
                        .productType(loan.getProductType())
                        .currentBalance(Optional.ofNullable(loan.getPrincipal()).orElse(BigDecimal.valueOf(0.0)))
                        .annualRatePct(Optional.ofNullable(loan.getAnnualRatePct()).orElse(BigDecimal.valueOf(0.0)))
                        .minimumPayment(calculateMonthlyPayment(
                                loan.getPrincipal(), loan.getAnnualRatePct(), loan.getRemainingTermMonths()))
                        .finalTermMonths(Optional.ofNullable(loan.getRemainingTermMonths()).orElse(0))
                        .totalInterest(calculateTotalInterestPaid(
                                loan.getPrincipal(), loan.getAnnualRatePct(), loan.getRemainingTermMonths()))
                        .interestSavingsOnDebt(BigDecimal.valueOf(0.0))
                        .daysPastDue(loan.getDaysPastDue())
                        .build())
                .collect(Collectors.toList());
    }

    private static List<DebtDetails> getDebtCard(Set<CardsEntity> cardsEntity) {
        return Optional.ofNullable(cardsEntity)
                .orElse(Set.<CardsEntity>of())
                .stream()
                .map(card -> {
                        BigDecimal cardMinimumPayment = calculateCardMinimumPayment(
                                card.getBalance(), card.getAnnualRatePct(), card.getMinPaymentPct());
                        Integer cardEstimatedTermMonths = calculateCardEstimatedTermMonths(card.getBalance(),
                                card.getAnnualRatePct(), cardMinimumPayment);

                        return DebtDetails.builder()
                        .productId("c-" + String.valueOf(card.getCardId()))
                        .productType("card")
                        .currentBalance(Optional.ofNullable(card.getBalance()).orElse(BigDecimal.valueOf(0.0)))
                        .annualRatePct(Optional.ofNullable(card.getAnnualRatePct()).orElse(BigDecimal.valueOf(0.0)))
                        .minimumPayment(cardMinimumPayment)
                        .finalTermMonths(cardEstimatedTermMonths)
                        .totalInterest(calculateCardTotalInterestPaid(card.getBalance(), card.getAnnualRatePct(),
                                cardMinimumPayment))
                        .interestSavingsOnDebt(BigDecimal.valueOf(0.0))
                        .daysPastDue(card.getDaysPastDue())
                        .build();
                })
                .collect(Collectors.toList());
    }

    public static BigDecimal calculateWeightedAverageRate(List<DebtDetails> debtDetails) {

        // Calcular el total de saldos
        BigDecimal totalSaldo = debtDetails.stream()
                .map(d -> Optional.ofNullable(d.getCurrentBalance()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalSaldo.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Calcular suma ponderada: (saldo * tasa)
        BigDecimal sumaPonderada = debtDetails.stream()
                .map(d -> {
                    BigDecimal saldo = Optional.ofNullable(d.getCurrentBalance()).orElse(BigDecimal.ZERO);
                    BigDecimal tasa = Optional.ofNullable(d.getAnnualRatePct()).orElse(BigDecimal.ZERO);
                    // Convertir de porcentaje (ej: 28.5 → 0.285)
                    BigDecimal tasaDecimal = tasa.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
                    return saldo.multiply(tasaDecimal);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tasa promedio ponderada = suma ponderada / total saldo → volver a porcentaje (x100)
        return sumaPonderada
                .divide(totalSaldo, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP); // redondeo final a 2 decimales
    }
}
