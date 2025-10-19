package com.bcp.ia.asistent.helper;

import com.bcp.ia.asistent.model.entity.CustomerCashflowEntity;
import com.bcp.ia.asistent.model.entity.CustomersEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentCalculatorHelper {

    private static final BigDecimal MONTHS_IN_YEAR_PERCENT = new BigDecimal("1200");

    private static final BigDecimal PERCENT_DIVISOR = new BigDecimal("100");

    private static final int MAX_ITERATIONS = 600;

    private static final int SCALE = 2;

    public static  BigDecimal getDisposableCashFlow(CustomersEntity customersEntity) {
        Set<CustomerCashflowEntity> cashflowSet = customersEntity.getCustomerCashflow();
        CustomerCashflowEntity cashflow = cashflowSet.iterator().next();

        BigDecimal ingresoMensual  = cashflow.getMonthlyIncomeAvg();
        BigDecimal variabilidadIngreso  = cashflow.getIncomeVariabilityPct();
        BigDecimal gastosEsenciales  = cashflow.getEssentialExpensesAvg();

        BigDecimal fraccionVariabilidad = variabilidadIngreso.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        BigDecimal ingresoAjustado = ingresoMensual.multiply(BigDecimal.ONE.subtract(fraccionVariabilidad));

        BigDecimal montoDisponible = ingresoAjustado.subtract(gastosEsenciales);

        BigDecimal montoSeguroDisponible = montoDisponible.multiply(BigDecimal.valueOf(0.8));

        return montoSeguroDisponible.max(BigDecimal.ZERO);
    }

    /**
     * Calcula la cuota mensual fija de un préstamo (pago mínimo).
     *
     * @param principal Monto original del préstamo.
     * @param annualRatePct Tasa de interés anual en porcentaje (ej: 28.5).
     * @param termMonths Plazo restante en meses.
     * @return BigDecimal La cuota mensual mínima.
     */
    public static BigDecimal calculateMonthlyPayment(
            BigDecimal principal,
            BigDecimal annualRatePct,
            int termMonths) {

        if (annualRatePct.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(new BigDecimal(termMonths), 2, RoundingMode.HALF_UP);
        }

        BigDecimal monthlyRate = annualRatePct.divide(MONTHS_IN_YEAR_PERCENT, 10, RoundingMode.HALF_UP);

        double factorDouble = Math.pow(monthlyRate.doubleValue() + 1, termMonths);
        BigDecimal factor = new BigDecimal(factorDouble);

        BigDecimal numerator = monthlyRate.multiply(factor);

        BigDecimal denominator = factor.subtract(BigDecimal.ONE);

        BigDecimal monthlyPayment = principal.multiply(
                numerator.divide(denominator, 10, RoundingMode.HALF_UP)
        );

        return monthlyPayment.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula el monto total de intereses pagados durante la vida del préstamo.
     *
     * @param principal Monto original del préstamo.
     * @param annualRatePct Tasa de interés anual en porcentaje (ej: 28.5).
     * @param termMonths Plazo restante en meses.
     * @return BigDecimal El total de intereses pagados.
     */
    public static BigDecimal calculateTotalInterestPaid(
            BigDecimal principal,
            BigDecimal annualRatePct,
            int termMonths) {

        BigDecimal monthlyPayment = calculateMonthlyPayment(principal, annualRatePct, termMonths);

        BigDecimal totalPayments = monthlyPayment.multiply(new BigDecimal(termMonths));

        BigDecimal totalInterest = totalPayments.subtract(principal);

        return totalInterest.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula la cuota de pago mínimo de una tarjeta de crédito.
     * * El pago mínimo es la suma del interés del período más el porcentaje mínimo
     * del saldo adeudado.
     *
     * @param balance Saldo actual adeudado.
     * @param annualRatePct Tasa de interés anual en porcentaje (ej: 45.00).
     * @param minPaymentPct Porcentaje mínimo de pago requerido (ej: 5.00).
     * @return BigDecimal El monto de pago mínimo.
     */
    public static BigDecimal calculateCardMinimumPayment(
            BigDecimal balance,
            BigDecimal annualRatePct,
            BigDecimal minPaymentPct) {

        BigDecimal monthlyRate = annualRatePct.divide(MONTHS_IN_YEAR_PERCENT, 10, RoundingMode.HALF_UP);

        BigDecimal periodicInterest = balance.multiply(monthlyRate);

        BigDecimal minPaymentFactor = minPaymentPct.divide(PERCENT_DIVISOR, 10, RoundingMode.HALF_UP);

        BigDecimal requiredPrincipalPayment = balance.multiply(minPaymentFactor);

        BigDecimal minimumPayment = periodicInterest.add(requiredPrincipalPayment);


        return minimumPayment.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Estima el número de meses necesarios para pagar el saldo total de una tarjeta
     * de crédito pagando solo el mínimo requerido cada mes.
     *
     * @param balance Saldo inicial adeudado.
     * @param annualRatePct Tasa de interés anual en porcentaje (ej: 45.00).
     * @param minPaymentPct Porcentaje mínimo de pago requerido (ej: 5.00).
     * @return Integer El plazo estimado total en meses.
     */
    public static Integer calculateCardEstimatedTermMonths(
            BigDecimal balance,
            BigDecimal annualRatePct,
            BigDecimal minPaymentPct) {

        BigDecimal remainingBalance = balance.setScale(2, RoundingMode.HALF_UP);
        int totalMonths = 0;

        BigDecimal monthlyRate = annualRatePct.divide(MONTHS_IN_YEAR_PERCENT, 10, RoundingMode.HALF_UP);

        BigDecimal minPaymentFactor = minPaymentPct.divide(PERCENT_DIVISOR, 10, RoundingMode.HALF_UP);

        while (remainingBalance.compareTo(BigDecimal.ZERO) > 0 && totalMonths < MAX_ITERATIONS) {

            totalMonths++;

            BigDecimal periodicInterest = remainingBalance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);

            BigDecimal requiredPrincipalPayment = remainingBalance.multiply(minPaymentFactor);
            BigDecimal minimumPayment = periodicInterest.add(requiredPrincipalPayment).setScale(2, RoundingMode.HALF_UP);

            if (minimumPayment.compareTo(remainingBalance) > 0) {
                minimumPayment = remainingBalance;
            }

            BigDecimal principalPaid = minimumPayment.subtract(periodicInterest);

            remainingBalance = remainingBalance.subtract(principalPaid);

            if (remainingBalance.compareTo(BigDecimal.ZERO) < 0) {
                remainingBalance = BigDecimal.ZERO;
            }
        }

        if (totalMonths >= MAX_ITERATIONS) {
            System.err.println("Advertencia: El cálculo de plazo excedió el límite de " + MAX_ITERATIONS + " meses.");
            return MAX_ITERATIONS;
        }

        return totalMonths;
    }

    /**
     * Calcula el monto total de intereses pagados durante el plazo estimado
     * de la tarjeta de crédito, pagando solo el mínimo requerido cada mes.
     *
     * @param balance Saldo inicial adeudado.
     * @param annualRatePct Tasa de interés anual en porcentaje (ej: 45.00).
     * @param minPaymentPct Porcentaje mínimo de pago requerido (ej: 5.00).
     * @return BigDecimal El total de intereses pagados.
     */
    public static BigDecimal calculateCardTotalInterestPaid(
            BigDecimal balance,
            BigDecimal annualRatePct,
            BigDecimal minPaymentPct) {

        BigDecimal remainingBalance = balance.setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal totalInterestPaid = BigDecimal.ZERO;
        int totalMonths = 0;

        BigDecimal monthlyRate = annualRatePct.divide(MONTHS_IN_YEAR_PERCENT, 10, RoundingMode.HALF_UP);
        BigDecimal minPaymentFactor = minPaymentPct.divide(PERCENT_DIVISOR, 10, RoundingMode.HALF_UP);

        while (remainingBalance.compareTo(BigDecimal.ZERO) > 0 && totalMonths < MAX_ITERATIONS) {

            totalMonths++;

            BigDecimal periodicInterest = remainingBalance.multiply(monthlyRate).setScale(SCALE, RoundingMode.HALF_UP);

            totalInterestPaid = totalInterestPaid.add(periodicInterest);

            BigDecimal requiredPrincipalPayment = remainingBalance.multiply(minPaymentFactor);
            BigDecimal minimumPayment = periodicInterest.add(requiredPrincipalPayment).setScale(SCALE, RoundingMode.HALF_UP);

            if (minimumPayment.compareTo(remainingBalance.add(periodicInterest)) > 0) {
                minimumPayment = remainingBalance.add(periodicInterest);
            }

            BigDecimal principalPaid = minimumPayment.subtract(periodicInterest);

            remainingBalance = remainingBalance.subtract(principalPaid);

            if (remainingBalance.compareTo(BigDecimal.ZERO) < 0) {
                remainingBalance = BigDecimal.ZERO;
            }
        }

        return totalInterestPaid.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calcula el ahorro estimado en intereses al realizar un pago adicional mensual
     * en una tarjeta de crédito.
     *
     * @param balance Saldo actual de la tarjeta.
     * @param annualRatePct Tasa anual (%).
     * @param minPayment Pago mínimo mensual.
     * @param extraPayment Pago adicional mensual.
     * @return Ahorro estimado en intereses (BigDecimal).
     */
    public static BigDecimal calcularAhorroInteresesCard(
            BigDecimal balance,
            BigDecimal annualRatePct,
            BigDecimal minPayment,
            BigDecimal extraPayment
    ) {
        BigDecimal tasaMensual = annualRatePct.divide(BigDecimal.valueOf(12 * 100), 10, RoundingMode.HALF_UP);

        BigDecimal interesesBase = calcularInteresesTotales(balance, tasaMensual, minPayment);

        BigDecimal interesesOptimizado = calcularInteresesTotales(balance, tasaMensual,
                minPayment.add(extraPayment));

        return interesesBase.subtract(interesesOptimizado).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula el total de intereses pagados hasta liquidar la deuda.
     */
    private static BigDecimal calcularInteresesTotales(BigDecimal balance, BigDecimal tasaMensual, BigDecimal pago) {
        BigDecimal totalInteres = BigDecimal.ZERO;
        BigDecimal saldo = balance;

        int meses = 0;
        while (saldo.compareTo(BigDecimal.ZERO) > 0 && meses < 1000) {
            BigDecimal interesMes = saldo.multiply(tasaMensual);
            saldo = saldo.add(interesMes).subtract(pago);

            if (saldo.compareTo(BigDecimal.ZERO) < 0) saldo = BigDecimal.ZERO;

            totalInteres = totalInteres.add(interesMes);
            meses++;
        }

        return totalInteres.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula el ahorro estimado en intereses al realizar un pago adicional mensual
     * sobre un préstamo o crédito personal con cuotas fijas.
     *
     * @param montoPrestamo     Monto total del préstamo
     * @param tasaInteresAnual  Tasa de interés anual (por ejemplo, 12 para 12%)
     * @param plazoMeses        Plazo original del préstamo en meses
     * @param pagoExtraMensual  Monto adicional que el cliente paga cada mes
     * @return Ahorro estimado en intereses (BigDecimal)
     */
    public static BigDecimal calcularAhorroInteresesConPagoExtra(
            BigDecimal montoPrestamo,
            BigDecimal tasaInteresAnual,
            int plazoMeses,
            BigDecimal pagoExtraMensual
    ) {
        if (montoPrestamo == null || tasaInteresAnual == null || pagoExtraMensual == null)
            return BigDecimal.ZERO;

        BigDecimal tasaMensual = tasaInteresAnual
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

        BigDecimal unoMasTasa = BigDecimal.ONE.add(tasaMensual);
        BigDecimal potencia = unoMasTasa.pow(plazoMeses);
        BigDecimal cuotaBase = montoPrestamo.multiply(tasaMensual).multiply(potencia)
                .divide(potencia.subtract(BigDecimal.ONE), 10, RoundingMode.HALF_UP);

        BigDecimal saldoBase = montoPrestamo;
        BigDecimal interesesBase = BigDecimal.ZERO;
        int mesesBase = 0;

        while (saldoBase.compareTo(BigDecimal.ZERO) > 0 && mesesBase < 1000) {
            BigDecimal interes = saldoBase.multiply(tasaMensual);
            BigDecimal abonoCapital = cuotaBase.subtract(interes);
            saldoBase = saldoBase.subtract(abonoCapital).max(BigDecimal.ZERO);
            interesesBase = interesesBase.add(interes);
            mesesBase++;
        }

        BigDecimal saldoExtra = montoPrestamo;
        BigDecimal interesesExtra = BigDecimal.ZERO;
        int mesesExtra = 0;

        while (saldoExtra.compareTo(BigDecimal.ZERO) > 0 && mesesExtra < 1000) {
            BigDecimal interes = saldoExtra.multiply(tasaMensual);
            BigDecimal abonoCapital = cuotaBase.add(pagoExtraMensual).subtract(interes);

            if (abonoCapital.compareTo(saldoExtra) > 0) {
                abonoCapital = saldoExtra;
            }

            saldoExtra = saldoExtra.subtract(abonoCapital).max(BigDecimal.ZERO);
            interesesExtra = interesesExtra.add(interes);
            mesesExtra++;
        }

        BigDecimal ahorro = interesesBase.subtract(interesesExtra)
                .setScale(2, RoundingMode.HALF_UP);

        return ahorro.max(BigDecimal.ZERO);
    }
}
