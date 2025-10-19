package com.bcp.ia.asistent.repository;

import com.bcp.ia.asistent.model.entity.CustomersEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository
    extends CrudRepository<CustomersEntity, Long> {


    @EntityGraph(attributePaths = {"loans", "cards", "customerCashflow"})
    @Query("SELECT cu FROM CustomersEntity cu WHERE cu.customerId =:customerId")
    CustomersEntity findCustomerById(@Param("customerId") Integer customerId);

    @EntityGraph(attributePaths = {"loans", "cards", "paymentsHistory", "creditScoreHistory", "customerCashflow"})
    @Query("SELECT cu FROM CustomersEntity cu WHERE cu.customerId =:customerId")
    CustomersEntity findCustomerDebtById(@Param("customerId") Integer customerId);
}
