package com.edusync.repository;

import com.edusync.model.AdvisoryMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdvisoryMemberRepository extends JpaRepository<AdvisoryMember, Long> {

    List<AdvisoryMember> findAllByOrderByCreatedAtDesc();

    List<AdvisoryMember> findByStatus(AdvisoryMember.MemberStatus status);
}
