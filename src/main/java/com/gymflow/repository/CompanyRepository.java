package com.gymflow.repository;
import com.gymflow.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface CompanyRepository extends JpaRepository<Company, UUID> {
    Optional<Company> findByCode(String code);
}
