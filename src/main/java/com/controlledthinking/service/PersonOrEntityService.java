package com.controlledthinking.service;

import com.controlledthinking.dao.PersonOrEntityDAO;
import com.controlledthinking.db.PersonOrEntity;
import com.controlledthinking.dto.UploadResult;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersonOrEntityService {

    private static final Logger logger = LoggerFactory.getLogger(PersonOrEntityService.class);
    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{6,14}$");

    private final PersonOrEntityDAO peDao;

    public PersonOrEntityService(PersonOrEntityDAO peDao) {
        this.peDao = peDao;
    }

    public UploadResult parseAndUpsert(InputStream inputStream, UUID customerId) throws IOException {
        List<PersonOrEntity> result = new ArrayList<>();
        List<PersonOrEntity> candidates = parseSpreadsheet(inputStream);
        var customer = peDao.getCustomerReference(customerId);
        int skipped = 0;

        for (PersonOrEntity pe : candidates) {
            String normalized = normalizePhone(pe.getPhoneNumber());
            if (!isValidE164(normalized)) {
                logger.warn("Invalid phone number for {} {}: '{}'", pe.getFirstName(), pe.getLastName(), pe.getPhoneNumber());
                skipped++;
                continue;
            }
            pe.setPhoneNumber(normalized);
            result.add(peDao.findByNameAndPhone(pe.getFirstName(), pe.getLastName(), normalized).orElseGet(() -> {
                pe.setCustomer(customer);
                return peDao.create(pe);
            }));
        }
        return new UploadResult(result, result.size(), skipped);
    }

    public String importFromSpreadsheet(InputStream inputStream, UUID customerId) throws IOException {
        int inserted = 0;
        int skipped = 0;
        int errors = 0;

        List<PersonOrEntity> candidates = parseSpreadsheet(inputStream);
        var customer = peDao.getCustomerReference(customerId);

        for (PersonOrEntity pe : candidates) {
            String normalized = normalizePhone(pe.getPhoneNumber());
            if (!isValidE164(normalized)) {
                logger.warn("Invalid phone number for {} {}: '{}'", pe.getFirstName(), pe.getLastName(), pe.getPhoneNumber());
                errors++;
                continue;
            }
            pe.setPhoneNumber(normalized);
            if (exists(pe)) {
                logger.debug("Skipping duplicate: {} {} {}", pe.getFirstName(), pe.getLastName(), pe.getPhoneNumber());
                skipped++;
            } else {
                pe.setCustomer(customer);
                peDao.create(pe);
                inserted++;
            }
        }

        return String.format("Import complete. Inserted: %d, Skipped (duplicates): %d, Errors (invalid phone): %d", inserted, skipped, errors);
    }

    private List<PersonOrEntity> parseSpreadsheet(InputStream inputStream) throws IOException {
        List<PersonOrEntity> result = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("Spreadsheet has no header row");
            }

            int firstNameIdx = -1, lastNameIdx = -1, phoneIdx = -1;
            for (Cell cell : headerRow) {
                String header = cell.getStringCellValue().trim().toLowerCase();
                switch (header) {
                    case "first name"   -> firstNameIdx = cell.getColumnIndex();
                    case "last name"    -> lastNameIdx  = cell.getColumnIndex();
                    case "phone number" -> phoneIdx     = cell.getColumnIndex();
                }
            }

            if (firstNameIdx == -1 || lastNameIdx == -1 || phoneIdx == -1) {
                throw new IllegalArgumentException("Spreadsheet must have headers: 'First Name', 'Last Name', 'Phone Number'");
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String firstName   = getCellValue(row, firstNameIdx);
                String lastName    = getCellValue(row, lastNameIdx);
                String phoneNumber = getCellValue(row, phoneIdx);

                if (firstName.isBlank() && lastName.isBlank() && phoneNumber.isBlank()) continue;

                result.add(new PersonOrEntity(firstName, lastName, phoneNumber));
            }
        }

        return result;
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        // Strip everything except digits and a leading +
        String digits = phone.replaceAll("[^\\d+]", "");
        if (!digits.startsWith("+")) {
            digits = "+" + digits;
        }
        // Bare 10-digit number after +: assume US country code
        if (digits.matches("\\+\\d{10}")) {
            digits = "+1" + digits.substring(1);
        }
        return digits;
    }

    private boolean isValidE164(String phone) {
        return phone != null && E164_PATTERN.matcher(phone).matches();
    }

    private boolean exists(PersonOrEntity pe) {
        return peDao.existsByNameAndPhone(pe.getFirstName(), pe.getLastName(), pe.getPhoneNumber());
    }

    private String getCellValue(Row row, int colIdx) {
        Cell cell = row.getCell(colIdx);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default      -> "";
        };
    }
}
