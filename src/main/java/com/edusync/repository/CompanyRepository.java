package com.edusync.repository;

import com.edusync.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByNameIgnoreCase(String name);

    List<Company> findByNameContainingIgnoreCase(String name);

    List<Company> findByPartnershipStage(Company.PartnershipStage stage);

    List<Company> findByPlacementReadyTrue();

    List<Company> findAllByOrderByNameAsc();
}
