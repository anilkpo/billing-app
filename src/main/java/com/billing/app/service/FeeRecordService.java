package com.billing.app.service;

import com.billing.app.domain.FeeRecord;
import com.billing.app.dto.CreateFeeRecordRequest;
import com.billing.app.repository.FeeRecordRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FeeRecordService {

    private final FeeRecordRepository repository;

    public FeeRecordService(FeeRecordRepository repository) {
        this.repository = repository;
    }

    public FeeRecord create(CreateFeeRecordRequest request) {
        FeeRecord feeRecord = new FeeRecord();
        feeRecord.setStudentName(request.getStudentName());
        feeRecord.setStudentEmail(request.getStudentEmail());
        feeRecord.setCourseName(request.getCourseName());
        feeRecord.setAmount(request.getAmount());
        feeRecord.setPaymentDate(request.getPaymentDate() == null ? LocalDate.now() : request.getPaymentDate());
        feeRecord.setStatus(request.getStatus());
        return repository.save(feeRecord);
    }

    public List<FeeRecord> findAll() {
        return repository.findAll();
    }

    public FeeRecord findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fee record not found for id: " + id));
    }
}
