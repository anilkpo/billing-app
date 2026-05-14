package com.billing.app.repository;

import com.billing.app.domain.FeeRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeeRecordRepository extends JpaRepository<FeeRecord, Long> {
    List<FeeRecord> findByStudentNameContainingIgnoreCaseOrStudentEmailContainingIgnoreCaseOrCourseNameContainingIgnoreCase(
            String studentName, String studentEmail, String courseName);
}
