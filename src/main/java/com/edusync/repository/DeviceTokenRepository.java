package com.edusync.repository;

import com.edusync.model.DeviceToken;
import com.edusync.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    List<DeviceToken> findByUserAndActiveTrue(User user);

    List<DeviceToken> findByUserIdAndActiveTrue(Long userId);

    Optional<DeviceToken> findByUserAndToken(User user, String token);

    Optional<DeviceToken> findByToken(String token);

    @Query("SELECT d FROM DeviceToken d WHERE d.user.department = :department AND d.active = true")
    List<DeviceToken> findByDepartmentAndActiveTrue(@Param("department") String department);

    @Query("SELECT d FROM DeviceToken d WHERE d.user.role = :role AND d.active = true")
    List<DeviceToken> findByRoleAndActiveTrue(@Param("role") User.Role role);

    @Query("SELECT d FROM DeviceToken d WHERE d.user.role = :role AND d.user.department = :department AND d.active = true")
    List<DeviceToken> findByRoleAndDepartmentAndActiveTrue(@Param("role") User.Role role, @Param("department") String department);

    @Query("SELECT d FROM DeviceToken d WHERE d.active = true")
    List<DeviceToken> findAllActive();

    @Modifying
    @Query("UPDATE DeviceToken d SET d.active = false WHERE d.user.id = :userId AND d.token = :token")
    void deactivateToken(@Param("userId") Long userId, @Param("token") String token);

    @Modifying
    @Query("UPDATE DeviceToken d SET d.active = false WHERE d.user.id = :userId")
    void deactivateAllUserTokens(@Param("userId") Long userId);
}
