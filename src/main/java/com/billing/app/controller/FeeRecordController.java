package com.billing.app.controller;

import com.billing.app.domain.FeeRecord;
import com.billing.app.dto.CreateFeeRecordRequest;
import com.billing.app.service.BillPdfService;
import com.billing.app.service.FeeRecordService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fees")
public class FeeRecordController {

    private final FeeRecordService feeRecordService;
    private final BillPdfService billPdfService;

    public FeeRecordController(FeeRecordService feeRecordService, BillPdfService billPdfService) {
        this.feeRecordService = feeRecordService;
        this.billPdfService = billPdfService;
    }

    @PostMapping
    public FeeRecord createFeeRecord(@Valid @RequestBody CreateFeeRecordRequest request) {
        return feeRecordService.create(request);
    }

    @GetMapping
    public List<FeeRecord> getAllFeeRecords() {
        return feeRecordService.findAll();
    }

    @GetMapping("/{id}")
    public FeeRecord getFeeRecordById(@PathVariable Long id) {
        return feeRecordService.findById(id);
    }

    @GetMapping("/{id}/bill")
    public ResponseEntity<byte[]> generateBill(@PathVariable Long id) {
        FeeRecord feeRecord = feeRecordService.findById(id);
        byte[] pdf = billPdfService.generateBill(feeRecord);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename("bill-" + id + ".pdf").build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }
}
