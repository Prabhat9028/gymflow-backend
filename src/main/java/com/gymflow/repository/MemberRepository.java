package com.gymflow.repository;

import com.gymflow.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberRepository extends JpaRepository<Member, UUID> {
    Optional<Member> findByMemberCode(String memberCode);
    Optional<Member> findByEmail(String email);
    Page<Member> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT m FROM Member m WHERE " +
           "LOWER(m.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(m.lastName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(m.email) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "m.memberCode LIKE CONCAT('%', :q, '%') OR " +
           "m.phone LIKE CONCAT('%', :q, '%')")
    Page<Member> search(@Param("q") String query, Pageable pageable);

    long countByIsActiveTrue();

    @Query("SELECT m FROM Member m ORDER BY m.createdAt DESC")
    List<Member> findRecentMembers(Pageable pageable);
}
