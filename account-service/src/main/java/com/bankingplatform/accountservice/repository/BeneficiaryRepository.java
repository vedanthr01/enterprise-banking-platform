package com.bankingplatform.accountservice.repository;

import com.bankingplatform.accountservice.entity.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, String> {
    List<Beneficiary> findByOwnerUserId(String ownerUserId);
    Optional<Beneficiary> findByIdAndOwnerUserId(String id, String ownerUserId);
}
