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
@Table(name = "cards")
public class CardsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_id", nullable = false, columnDefinition = "INT")
    private Long cardId;

    @JsonBackReference("customer-cards")
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_customer_id", value = ConstraintMode.NO_CONSTRAINT),
            columnDefinition = "INT")
    private CustomersEntity customers;

    @Column(name = "balance", nullable = false, precision = 10, scale = 2)
    private BigDecimal balance;

    @Column(name = "annual_rate_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal annualRatePct;

    @Column(name = "min_payment_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal minPaymentPct;

    @Column(name = "payment_due_day", nullable = false, columnDefinition = "INT")
    private Integer paymentDueDay;

    @Column(name = "days_past_due", nullable = false, columnDefinition = "INT")
    private Integer daysPastDue;
}
