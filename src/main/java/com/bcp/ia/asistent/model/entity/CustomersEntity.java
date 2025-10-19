package com.bcp.ia.asistent.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "customers")
@JsonIgnoreProperties({"creditScoreHistory", "paymentsHistory"})
public class CustomersEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id", nullable = false, columnDefinition = "INT")
    private Long customerId;

    @Column(name = "full_name", nullable = false, length = 100, columnDefinition = "VARCHAR(100)")
    private String fullName;

    @JsonManagedReference("customer-loans")
    @OneToMany(mappedBy = "customers", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    private Set<LoansEntity> loans;

    @JsonManagedReference("customer-cards")
    @OneToMany(mappedBy = "customers", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    private Set<CardsEntity> cards;

    @JsonManagedReference("customer-paymentsHistory")
    @OneToMany(mappedBy = "customers", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<PaymentsHistoryEntity> paymentsHistory;

    @JsonManagedReference("customer-creditScoreHistory")
    @OneToMany(mappedBy = "customers", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    private Set<CreditScoreHistoryEntity> creditScoreHistory;

    @JsonManagedReference("customer-customerCashflow")
    @OneToMany(mappedBy = "customers", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    private Set<CustomerCashflowEntity> customerCashflow;
}
