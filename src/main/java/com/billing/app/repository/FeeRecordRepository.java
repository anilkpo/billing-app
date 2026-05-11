package com.billing.app.repository;

import com.billing.app.domain.FeeRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeeRecordRepository extends JpaRepository<FeeRecord, Long> {
}
