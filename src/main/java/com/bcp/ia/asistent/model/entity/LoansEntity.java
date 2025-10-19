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
@Table(name = "loans")
public class LoansEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loan_id", nullable = false, columnDefinition = "INT")
    private Long loanId;

    @JsonBackReference("customer-loans")
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_customer_id", value = ConstraintMode.NO_CONSTRAINT),
        columnDefinition = "INT")
    private CustomersEntity customers;

    @Column(name = "product_type", nullable = false, length = 50, columnDefinition = "VARCHAR(50)")
    private String productType;

    @Column(name = "principal", nullable = false, precision = 10, scale = 2)
    private BigDecimal principal;

    @Column(name = "annual_rate_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal annualRatePct;

    @Column(name = "remaining_term_months", nullable = false, columnDefinition = "INT")
    private Integer remainingTermMonths;

    @Column(name = "collateral", nullable = false, columnDefinition = "BIT")
    private Boolean collateral;

    @Column(name = "days_past_due", nullable = false, columnDefinition = "INT")
    private Integer daysPastDue;

}
