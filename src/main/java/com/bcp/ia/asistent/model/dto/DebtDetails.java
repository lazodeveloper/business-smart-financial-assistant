package com.bcp.ia.asistent.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
public class DebtDetails {
    private String productId;                     // Id del producto
    private String productType;                   // Tipo de producto
    private BigDecimal currentBalance;                // Saldo actual
    private BigDecimal annualRatePct;                 // Tasa anual (%)
    private BigDecimal minimumPayment;                // Pago mínimo
    private Integer finalTermMonths;              // Plazo final (meses)
    private BigDecimal totalInterest;                 // Interés total
    private Integer daysPastDue;                 // Días de atraso
    // campos de optimización del plan
    private Integer paymentPriority;              // Prioridad de pago
    private BigDecimal extraPaymentFromFCD = BigDecimal.valueOf(0.0);   // Pago extra desde FCD
    private BigDecimal totalRecommendedPayment = BigDecimal.valueOf(0.0);  // Pago total recomendado
    private BigDecimal estimatedLiquidationMonth;   // Mes estimado de liquidación
    private BigDecimal interestSavingsOnDebt = BigDecimal.valueOf(0.0);  // Ahorro de interés por deuda

    // Consolidación (Mejor Oferta)
    private BigDecimal consolidatedAmount;            // Monto de consolidación
    private Double newRatePct;                    // Nueva tasa (%) post-consolidación
    private Double newTermMonths;                 // Nuevo plazo (meses)
    private List<String> originalDebtsConsolidated; // Deudas originales consolidadas
    private String eligibilityConditionsMet;      // Condiciones de elegibilidad cumplidas
}
