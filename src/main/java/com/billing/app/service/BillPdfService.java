package com.billing.app.service;

import com.billing.app.domain.FeeRecord;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

@Service
public class BillPdfService {

    public byte[] generateBill(FeeRecord feeRecord) {
        String html = buildHtml(feeRecord);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate PDF bill", exception);
        }
    }

    private String buildHtml(FeeRecord feeRecord) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset=\"UTF-8\" />
                    <style>
                        body { font-family: Arial, sans-serif; margin: 36px; color: #1f2937; }
                        h1 { margin-bottom: 8px; }
                        .card { border: 1px solid #e5e7eb; border-radius: 8px; padding: 18px; }
                        table { width: 100%; border-collapse: collapse; margin-top: 12px; }
                        td { padding: 8px; border-bottom: 1px solid #f3f4f6; }
                        .label { width: 35%; color: #4b5563; font-weight: bold; }
                        .amount { font-size: 18px; font-weight: bold; color: #111827; }
                    </style>
                </head>
                <body>
                    <h1>Fee Payment Bill</h1>
                    <div class=\"card\">
                        <table>
                            <tr><td class=\"label\">Student Name</td><td>%s</td></tr>
                            <tr><td class=\"label\">Student Email</td><td>%s</td></tr>
                            <tr><td class=\"label\">Course</td><td>%s</td></tr>
                            <tr><td class=\"label\">Payment Date</td><td>%s</td></tr>
                            <tr><td class=\"label\">Status</td><td>%s</td></tr>
                            <tr><td class=\"label\">Amount</td><td class=\"amount\">%s</td></tr>
                        </table>
                    </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(feeRecord.getStudentName()),
                escapeHtml(feeRecord.getStudentEmail()),
                escapeHtml(feeRecord.getCourseName()),
                feeRecord.getPaymentDate().format(DateTimeFormatter.ISO_DATE),
                escapeHtml(feeRecord.getStatus()),
                feeRecord.getAmount().toPlainString());
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
