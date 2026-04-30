package com.jobassistant.repository;

import com.jobassistant.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    boolean existsByEmailId(String emailId);

}