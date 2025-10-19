package com.bcp.ia.asistent.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ConsolidationOffer {
    private String offerId;                         // Id de la oferta
    private List<String> productTypesEligible;      // Tipos de producto elegibles
    private BigDecimal maxConsolidatedBalance;      // Monto máximo consolidable
    private BigDecimal newRatePct;                  // Nueva tasa anual (%) propuesta
    private Integer maxTermMonths;                  // Plazo máximo en meses
    private String conditions;                      // Condiciones de la oferta
}
