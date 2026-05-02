package com.jobassistant.repository;

import com.jobassistant.entity.JobApplication;
import com.jobassistant.entity.Users;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    boolean existsByEmailId(String emailId);

    // ✅ Dashboard Stats
    long countByUser(Users user);

    long countByUserAndStatus(Users user, String status);

    long countByUserAndStatusNot(Users user, String status);

    // ✅ Recent Applications
    List<JobApplication> findTop10ByUserOrderByAppliedDateDesc(Users user);

    // ✅ Source Pie Chart
    @Query("SELECT j.source, COUNT(j) FROM JobApplication j WHERE j.user = :user GROUP BY j.source")
    List<Object[]> getSourceStats(@Param("user") Users user);

    // ✅ Applications Over Time
    @Query("SELECT MONTH(j.appliedDate), COUNT(j) FROM JobApplication j WHERE j.user = :user GROUP BY MONTH(j.appliedDate)")
    List<Object[]> getApplicationsPerMonth(@Param("user") Users user);
}