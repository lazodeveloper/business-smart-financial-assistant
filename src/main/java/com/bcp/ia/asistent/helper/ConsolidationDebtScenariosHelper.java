package com.bcp.ia.asistent.helper;

import com.bcp.ia.asistent.model.dto.ConsolidationOffer;
import com.bcp.ia.asistent.model.dto.DebtDetails;
import com.bcp.ia.asistent.model.dto.Scenarios;
import com.bcp.ia.asistent.model.entity.CustomersEntity;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

public class ConsolidationDebtScenariosHelper {

    public static Scenarios consolidationDebtScenario(Integer creditScore,  Scenarios minimDebtScenario, List<ConsolidationOffer> consolidationOffer) {


        ConsolidationOffer findBestOffer = findBestOffer(consolidationOffer, minimDebtScenario, creditScore);
        BigDecimal minPayment = calcularCuotaMensual(minimDebtScenario.getTotalDebtAmount(),
                findBestOffer.getNewRatePct(),
                findBestOffer.getMaxTermMonths());
        BigDecimal totalInterestPaid = calcularInteresTotalConsolidado(
                minimDebtScenario.getTotalDebtAmount(),
                findBestOffer.getNewRatePct(),
                findBestOffer.getMaxTermMonths());

        return Scenarios.builder()
                .scenarioName("consolidationDebtScenario")
                .scenarioDescription("Consolidación de Deudas: el ahorro proviene de la reducción de la tasa y " +
                        "la simplificación, comparado directamente con el baseline.")
                .totalAverageRate(findBestOffer.getNewRatePct())
                .totalPointreduction(minimDebtScenario.getTotalAverageRate().subtract(findBestOffer
                        .getNewRatePct()).setScale(2, RoundingMode.HALF_UP))
                .totalMinPayment(minPayment)
                .totalIncreaseQuota(minPayment.subtract(minimDebtScenario.getTotalMinPayment()).abs())
                .totalTermMonths(findBestOffer.getMaxTermMonths())
                .termSavingsVsBaseline(minimDebtScenario.getTotalTermMonths() - findBestOffer.getMaxTermMonths())
                .totalInterestPaid(totalInterestPaid)
                .totalEstimatedInterestSavings(minimDebtScenario.getTotalInterestPaid().subtract(totalInterestPaid))
                .build();
    }

    /**
     * Devuelve la primera mejor oferta elegible (según orden del stream).
     * Todo implementado con Streams y lambdas.
     *
     * @param offers List of available offers (from bank_offers.json)
     * @param minimDebtScenario List of customer debts (loans + cards)
     * @param creditScore customer's credit score (nullable)
     * @return Optional<Offer> with an eligible offer (first found) or empty if none
     */
    private static ConsolidationOffer findBestOffer(List<ConsolidationOffer> offers, Scenarios minimDebtScenario, Integer creditScore) {

        return offers.stream()
                // Para cada oferta -> determinar si es elegible usando only stream/lambda ops
                .filter(offer -> {
                    List<DebtDetails>  debtDetails = minimDebtScenario.getDebtDetails();
                    // 1) filtrar deudas que sean del tipo elegible para esta oferta
                    BigDecimal totalEligibleBalance = debtDetails.stream()
                            .filter(debt ->
                                    offer.getProductTypesEligible().contains(debt.getProductType()))
                            .map(debt -> minimDebtScenario.getTotalDebtAmount())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    // 2) condición: total balance debe caber dentro de la oferta
                    boolean balanceOk = totalEligibleBalance.compareTo(offer.getMaxConsolidatedBalance()) <= 0;

                    // 3) interpretar condiciones textuales y evaluarlas:
                    //    - "No mora >30 días": para esta oferta requerimos que ninguna deuda elegible tenga daysPastDue > 30
                    boolean noMoraOver30 = debtDetails.stream()
                            .noneMatch(debt -> debt.getDaysPastDue() > 30);

                    //    - "Score > 650 y sin mora activa": chequeo de score y que ninguna deuda elegible tenga daysPastDue > 0
                    boolean scoreCondition;
                    if (offer.getConditions().toLowerCase().contains("score > 650")) {
                        scoreCondition = (creditScore > 650) &&
                                debtDetails.stream()
                                        //.filter(d -> offer.productTypesEligible.contains(d.productType))
                                        .noneMatch(debt -> debt.getDaysPastDue() > 0);
                    } else {
                        // si no requiere score específico, lo consideramos satisfecho
                        scoreCondition = true;
                    }

                    // 4) otras condiciones textuales simples: si pide "No mora >30 días" evaluamos esa
                    boolean conditionTextSatisfied;
                    if (offer.getConditions().toLowerCase().contains("no mora >30")) {
                        conditionTextSatisfied = noMoraOver30;
                    } else {
                        conditionTextSatisfied = true;
                    }

                    // 5) resultado final: todas las condiciones relevantes deben cumplirse
                    return balanceOk && scoreCondition && conditionTextSatisfied;
                })
                // opcional: podrías ordenar por mejor tasa (más baja) para devolver la "mejor" oferta
                //.sorted(Comparator.comparing(o -> o.newRatePct))
                .min(Comparator.comparing(ConsolidationOffer::getNewRatePct))
                .orElse(null);
    }

    private static BigDecimal calcularCuotaMensual(BigDecimal monto, BigDecimal tasaAnualPct, Integer plazoMeses) {

        MathContext mc = new MathContext(15, RoundingMode.HALF_UP);

        // 1️⃣ Tasa mensual: (tasa anual / 12) / 100
        BigDecimal tasaMensual = tasaAnualPct
                .divide(BigDecimal.valueOf(12), mc)
                .divide(BigDecimal.valueOf(100), mc);

        // 2️⃣ (1 + i)^n
        BigDecimal unoMasI = BigDecimal.ONE.add(tasaMensual, mc);
        BigDecimal potencia = unoMasI.pow(plazoMeses, mc);

        // 3️⃣ Fórmula: P * [i * (1+i)^n] / [(1+i)^n - 1]
        BigDecimal numerador = monto.multiply(tasaMensual, mc).multiply(potencia, mc);
        BigDecimal denominador = potencia.subtract(BigDecimal.ONE, mc);

        return numerador.divide(denominador, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal calcularInteresTotalConsolidado(BigDecimal montoConsolidado,
                                                             BigDecimal tasaAnualPct,
                                                             int plazoMeses) {
        if (montoConsolidado == null || tasaAnualPct == null || plazoMeses <= 0) {
            return BigDecimal.ZERO;
        }

        // 1️⃣ Convertir tasa anual a tasa mensual (ejemplo: 17.5% → 0.0145833)
        BigDecimal tasaMensual = tasaAnualPct
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

        // 2️⃣ Calcular cuota mensual: P * r / (1 - (1 + r)^(-n))
        BigDecimal unoMasR = BigDecimal.ONE.add(tasaMensual);
        BigDecimal potencia = unoMasR.pow(-plazoMeses, new java.math.MathContext(10));
        BigDecimal denominador = BigDecimal.ONE.subtract(potencia);
        BigDecimal cuota = montoConsolidado.multiply(tasaMensual).divide(denominador, 10, RoundingMode.HALF_UP);

        // 3️⃣ Calcular interés total pagado
        BigDecimal totalPagado = cuota.multiply(BigDecimal.valueOf(plazoMeses));
        BigDecimal interesTotal = totalPagado.subtract(montoConsolidado);

        // 4️⃣ Redondear a 2 decimales
        return interesTotal.setScale(2, RoundingMode.HALF_UP);
    }
}
