package com.bcp.ia.asistent.model.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payments_history")
public class PaymentsHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id", nullable = false, columnDefinition = "INT")
    private Long paymentId;

    @JsonBackReference("customer-paymentsHistory")
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_customer_id", value = ConstraintMode.NO_CONSTRAINT),
            columnDefinition = "INT")
    private CustomersEntity customers;

    @Column(name = "product_id", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private String productId;

    @Column(name = " product_type", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private String productType;

    @JsonDeserialize(using = LocalDateDeserializer.class)
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "payment_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal paymentAmount;

}
