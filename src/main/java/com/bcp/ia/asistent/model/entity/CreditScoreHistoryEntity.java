package com.bcp.ia.asistent.model.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "credit_score_history")
public class CreditScoreHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "score_id", nullable = false, columnDefinition = "INT")
    private Long scoreId;

    @JsonBackReference("customer-creditScoreHistory")
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_customer_id", value = ConstraintMode.NO_CONSTRAINT),
            columnDefinition = "INT")
    private CustomersEntity customers;

    @JsonDeserialize(using = LocalDateDeserializer.class)
    @Column(name = "score_date", nullable = false)
    private LocalDate scoreDate;

    @Column(name = "credit_score", nullable = false, columnDefinition = "INT")
    private Integer creditScore;
}
