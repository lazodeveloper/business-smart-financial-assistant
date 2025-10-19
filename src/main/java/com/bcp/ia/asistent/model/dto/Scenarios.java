package com.bcp.ia.asistent.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
public class Scenarios {
    private String scenarioName;            // Nombre del escenario
    private String  scenarioDescription;    // Descripción del escenario
    private BigDecimal totalDebtAmount;      // Suma total de la deuda
    private BigDecimal totalMinPayment; // Pago mínimo total
    private Integer totalTermMonths;        // Plazo total en meses
    private BigDecimal totalAverageRate; // Tasa % promedio total
    private BigDecimal totalInterestPaid;       // Interés total pagado
    private BigDecimal totalExtraPayment;  // Pago extra total
    private BigDecimal totalRecommendedPayment; // Pago total recomendado
    private BigDecimal totalEstimatedInterestSavings; // Ahorro total estimado en intereses
    //private BigDecimal totalMinimumPaymentConsolidation;  // Pago mínimo total post-consolidación
    private BigDecimal totalPointreduction; // Reducción total de puntos
    private BigDecimal totalIncreaseQuota; // Aumento total de la cuota

    private Double savingsVsBaseline;       // Ahorro vs línea base
    private Integer termSavingsVsBaseline;  // Ahorro en plazo vs línea base
    private List<DebtDetails> debtDetails;        // Detalles de la deuda
}
