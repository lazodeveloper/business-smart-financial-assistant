package com.bcp.ia.asistent.model.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "customer_cashflow")
public class CustomerCashflowEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cashflow_id", nullable = false, columnDefinition = "INT")
    private Long cashflowId;

    @JsonBackReference("customer-customerCashflow")
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_customer_id", value = ConstraintMode.NO_CONSTRAINT),
            columnDefinition = "INT")
    private CustomersEntity customers;

    @Column(name = "monthly_income_avg", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyIncomeAvg;

    @Column(name = "income_variability_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal incomeVariabilityPct;

    @Column(name = "essential_expenses_avg", nullable = false, precision = 10, scale = 2)
    private BigDecimal essentialExpensesAvg;
}
