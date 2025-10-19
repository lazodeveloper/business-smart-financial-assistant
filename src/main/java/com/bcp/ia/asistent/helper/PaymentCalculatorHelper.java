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
    // Constante para el divisor de la tasa anual a mensual (100 para porcentaje * 12 meses)
    private static final BigDecimal MONTHS_IN_YEAR_PERCENT = new BigDecimal("1200");
    // Para convertir el min_payment_pct
    private static final BigDecimal PERCENT_DIVISOR = new BigDecimal("100");
    // Límite para evitar bucles infinitos (50 años)
    private static final int MAX_ITERATIONS = 600;
    // Precisión para moneda (dos decimales)
    private static final int SCALE = 2;

    public static  BigDecimal getDisposableCashFlow(CustomersEntity customersEntity) {
        Set<CustomerCashflowEntity> cashflowSet = customersEntity.getCustomerCashflow();
        CustomerCashflowEntity cashflow = cashflowSet.iterator().next();

        BigDecimal ingresoMensual  = cashflow.getMonthlyIncomeAvg();
        BigDecimal variabilidadIngreso  = cashflow.getIncomeVariabilityPct();
        BigDecimal gastosEsenciales  = cashflow.getEssentialExpensesAvg();

        // Calcular la fracción de variabilidad: variabilidad / 100
        BigDecimal fraccionVariabilidad = variabilidadIngreso.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        // Calcular ingreso ajustado: ingresoMensual * (1 - variabilidad/100)
        BigDecimal ingresoAjustado = ingresoMensual.multiply(BigDecimal.ONE.subtract(fraccionVariabilidad));

        // Calcular monto disponible: ingresoAjustado - gastosEsenciales
        BigDecimal montoDisponible = ingresoAjustado.subtract(gastosEsenciales);

        // Aplicar margen de seguridad (opcional, 80%)
        BigDecimal montoSeguroDisponible = montoDisponible.multiply(BigDecimal.valueOf(0.8));

        // Si el resultado es negativo, devolvemos 0
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
            // Manejar tasa cero: pago principal / plazo
            return principal.divide(new BigDecimal(termMonths), 2, RoundingMode.HALF_UP);
        }

        // 1. Calcular la tasa de interés mensual 'i' (TasaAnual / 1200)
        BigDecimal monthlyRate = annualRatePct.divide(MONTHS_IN_YEAR_PERCENT, 10, RoundingMode.HALF_UP);

        // 2. Calcular el factor (1 + i)^n
        // Usamos Math.pow y luego lo convertimos a BigDecimal
        double factorDouble = Math.pow(monthlyRate.doubleValue() + 1, termMonths);
        BigDecimal factor = new BigDecimal(factorDouble);

        // 3. Numerador: i * (1 + i)^n
        BigDecimal numerator = monthlyRate.multiply(factor);

        // 4. Denominador: (1 + i)^n - 1
        BigDecimal denominator = factor.subtract(BigDecimal.ONE);

        // 5. Cuota = Principal * (Numerador / Denominador)
        BigDecimal monthlyPayment = principal.multiply(
                numerator.divide(denominator, 10, RoundingMode.HALF_UP)
        );

        // 6. Redondear a dos decimales (la precisión de la cuota)
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

        // 1. Obtener la Cuota Mensual Fija
        BigDecimal monthlyPayment = calculateMonthlyPayment(principal, annualRatePct, termMonths);

        // 2. Calcular el Pago Total = Cuota Mensual * Plazo
        BigDecimal totalPayments = monthlyPayment.multiply(new BigDecimal(termMonths));

        // 3. Calcular el Interés Total = Pago Total - Principal
        BigDecimal totalInterest = totalPayments.subtract(principal);

        // 4. Retornar el resultado con precisión de 2 decimales
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

        // 1. Calcular la tasa de interés mensual 'i' (TasaAnual / 1200)
        // Se asume que esta es la tasa efectiva que se aplica al balance para el período.
        BigDecimal monthlyRate = annualRatePct.divide(MONTHS_IN_YEAR_PERCENT, 10, RoundingMode.HALF_UP);

        // 2. Calcular el Interés Mensual del Período
        // Interés = Balance * Tasa Mensual
        BigDecimal periodicInterest = balance.multiply(monthlyRate);

        // 3. Calcular el Porcentaje Mínimo Requerido
        // Convertir porcentaje: minPaymentPct / 100 (ej: 5.00% -> 0.05)
        BigDecimal minPaymentFactor = minPaymentPct.divide(PERCENT_DIVISOR, 10, RoundingMode.HALF_UP);

        // Principal requerido = Balance * Factor Porcentual
        BigDecimal requiredPrincipalPayment = balance.multiply(minPaymentFactor);

        // 4. Calcular el Pago Mínimo Total
        // Pago Mínimo = Interés del Período + Porcentaje Requerido del Principal
        // NOTA: Algunas instituciones solo usan el mayor de estos dos o incluyen fees.
        // Aquí usamos la suma, que es el modelo más común para el mínimo total.
        BigDecimal minimumPayment = periodicInterest.add(requiredPrincipalPayment);

        // 5. Opcional: Establecer un pago mínimo absoluto (ej: $25), no implementado aquí.

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

        // Usamos copias para no modificar los valores originales
        BigDecimal remainingBalance = balance.setScale(2, RoundingMode.HALF_UP);
        int totalMonths = 0;

        // Tasa de interés mensual
        BigDecimal monthlyRate = annualRatePct.divide(MONTHS_IN_YEAR_PERCENT, 10, RoundingMode.HALF_UP);

        // Factor de pago mínimo (ej: 0.05)
        BigDecimal minPaymentFactor = minPaymentPct.divide(PERCENT_DIVISOR, 10, RoundingMode.HALF_UP);

        // La simulación se detiene si el balance es cero o si excede el límite
        while (remainingBalance.compareTo(BigDecimal.ZERO) > 0 && totalMonths < MAX_ITERATIONS) {

            totalMonths++;

            // 1. Calcular los intereses que se añaden al saldo este mes: Interés = Saldo * Tasa Mensual
            BigDecimal periodicInterest = remainingBalance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);

            // 2. Calcular el pago mínimo requerido
            // Pago Mínimo = (Saldo * Factor Porcentual) + Interés
            BigDecimal requiredPrincipalPayment = remainingBalance.multiply(minPaymentFactor);
            BigDecimal minimumPayment = periodicInterest.add(requiredPrincipalPayment).setScale(2, RoundingMode.HALF_UP);

            // Si el pago mínimo es menor que el saldo, pero el saldo es muy bajo,
            // el pago es el saldo restante (para el último mes)
            if (minimumPayment.compareTo(remainingBalance) > 0) {
                minimumPayment = remainingBalance;
            }

            // 3. Determinar qué parte del pago mínimo va a Principal
            // Pago al Principal = Pago Mínimo - Interés
            BigDecimal principalPaid = minimumPayment.subtract(periodicInterest);

            // 4. Actualizar el Saldo Restante
            remainingBalance = remainingBalance.subtract(principalPaid);

            // 5. Ajuste final (para evitar decimales pequeños negativos por el redondeo)
            if (remainingBalance.compareTo(BigDecimal.ZERO) < 0) {
                remainingBalance = BigDecimal.ZERO;
            }
        }

        // Si excedemos el límite, retornamos un valor especial (o lanzamos excepción)
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

        // Tasa de interés mensual y Factor de pago mínimo
        BigDecimal monthlyRate = annualRatePct.divide(MONTHS_IN_YEAR_PERCENT, 10, RoundingMode.HALF_UP);
        BigDecimal minPaymentFactor = minPaymentPct.divide(PERCENT_DIVISOR, 10, RoundingMode.HALF_UP);

        // La simulación se detiene si el balance es cero o si excede el límite de iteraciones
        while (remainingBalance.compareTo(BigDecimal.ZERO) > 0 && totalMonths < MAX_ITERATIONS) {

            totalMonths++;

            // 1. Calcular los intereses que se devengan este mes: Interés = Saldo * Tasa Mensual
            BigDecimal periodicInterest = remainingBalance.multiply(monthlyRate).setScale(SCALE, RoundingMode.HALF_UP);

            // Acumular los intereses generados
            totalInterestPaid = totalInterestPaid.add(periodicInterest);

            // 2. Calcular el Pago Mínimo Requerido (mismo cálculo que antes)
            BigDecimal requiredPrincipalPayment = remainingBalance.multiply(minPaymentFactor);
            BigDecimal minimumPayment = periodicInterest.add(requiredPrincipalPayment).setScale(SCALE, RoundingMode.HALF_UP);

            // Para el último mes, el pago es el saldo restante (capital + interés)
            if (minimumPayment.compareTo(remainingBalance.add(periodicInterest)) > 0) {
                minimumPayment = remainingBalance.add(periodicInterest);
            }

            // 3. Determinar qué parte del pago mínimo va a Principal
            BigDecimal principalPaid = minimumPayment.subtract(periodicInterest);

            // 4. Actualizar el Saldo Restante
            remainingBalance = remainingBalance.subtract(principalPaid);

            // 5. Ajuste final
            if (remainingBalance.compareTo(BigDecimal.ZERO) < 0) {
                remainingBalance = BigDecimal.ZERO;
            }
        }

        // Retornamos el total de intereses acumulados
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

        // Escenario 1: pagando solo el mínimo
        BigDecimal interesesBase = calcularInteresesTotales(balance, tasaMensual, minPayment);

        // Escenario 2: pagando mínimo + extra
        BigDecimal interesesOptimizado = calcularInteresesTotales(balance, tasaMensual,
                minPayment.add(extraPayment));

        // Ahorro = diferencia entre intereses
        return interesesBase.subtract(interesesOptimizado).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula el total de intereses pagados hasta liquidar la deuda.
     */
    private static BigDecimal calcularInteresesTotales(BigDecimal balance, BigDecimal tasaMensual, BigDecimal pago) {
        BigDecimal totalInteres = BigDecimal.ZERO;
        BigDecimal saldo = balance;

        int meses = 0;
        while (saldo.compareTo(BigDecimal.ZERO) > 0 && meses < 1000) { // límite de seguridad
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

        // Cuota base con fórmula de amortización francesa
        BigDecimal unoMasTasa = BigDecimal.ONE.add(tasaMensual);
        BigDecimal potencia = unoMasTasa.pow(plazoMeses);
        BigDecimal cuotaBase = montoPrestamo.multiply(tasaMensual).multiply(potencia)
                .divide(potencia.subtract(BigDecimal.ONE), 10, RoundingMode.HALF_UP);

        // Simular escenario base (sin pago extra)
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

        // Simular escenario con pago extra
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

        // Ahorro estimado en intereses
        BigDecimal ahorro = interesesBase.subtract(interesesExtra)
                .setScale(2, RoundingMode.HALF_UP);

        return ahorro.max(BigDecimal.ZERO);
    }
}
