package com.edusync.repository;

import com.edusync.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(User.Role role);

    List<User> findByDepartmentAndRole(String department, User.Role role);

    List<User> findByDepartmentAndSemester(String department, String semester);

    List<User> findByRoleAndDepartment(User.Role role, String department);

    List<User> findByAdvisor(User advisor);

    List<User> findByAdvisorAndActiveTrue(User advisor);
}
