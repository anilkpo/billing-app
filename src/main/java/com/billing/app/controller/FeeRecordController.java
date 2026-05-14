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
import org.springframework.web.bind.annotation.RequestParam;
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
    public List<FeeRecord> getAllFeeRecords(@RequestParam(required = false) String query) {
        return feeRecordService.search(query);
    }

    @GetMapping("/{id:[0-9]+}")
    public FeeRecord getFeeRecordById(@PathVariable String id) {
        return feeRecordService.findById(parseId(id));
    }

    @GetMapping("/{id:[0-9]+}/bill")
    public ResponseEntity<byte[]> generateBill(@PathVariable String id) {
        return buildBillResponse(parseId(id));
    }

    @GetMapping("/bill")
    public ResponseEntity<byte[]> generateBillByQueryParam(@RequestParam String id) {
        return buildBillResponse(parseId(id));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadBill(@PathVariable String id) {
        return buildBillResponse(parseId(id));
    }

    private ResponseEntity<byte[]> buildBillResponse(Long id) {
        FeeRecord feeRecord = feeRecordService.findById(id);
        byte[] pdf = billPdfService.generateBill(feeRecord);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename("bill-" + id + ".pdf").build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }

    private Long parseId(String rawId) {
        if (rawId == null) {
            throw new IllegalArgumentException("Invalid fee record id.");
        }
        String normalized = rawId.trim();
        int semicolonIndex = normalized.indexOf(';');
        if (semicolonIndex >= 0) {
            normalized = normalized.substring(0, semicolonIndex);
        }
        try {
            return Long.parseLong(normalized);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid fee record id: " + rawId);
        }
    }
}
