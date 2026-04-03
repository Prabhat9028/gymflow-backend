package com.gymflow.service;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class ExcelExportService {
    public byte[] export(String sheet, List<String> headers, List<List<String>> rows) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet s = wb.createSheet(sheet);
            CellStyle hs = wb.createCellStyle(); Font hf = wb.createFont(); hf.setBold(true); hs.setFont(hf);
            hs.setFillForegroundColor(IndexedColors.ORANGE.getIndex()); hs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Row hr = s.createRow(0);
            for (int i = 0; i < headers.size(); i++) { Cell c = hr.createCell(i); c.setCellValue(headers.get(i)); c.setCellStyle(hs); }
            for (int r = 0; r < rows.size(); r++) { Row row = s.createRow(r+1); List<String> rd = rows.get(r); for (int c = 0; c < rd.size(); c++) row.createCell(c).setCellValue(rd.get(c) != null ? rd.get(c) : ""); }
            for (int i = 0; i < headers.size(); i++) s.autoSizeColumn(i);
            ByteArrayOutputStream bos = new ByteArrayOutputStream(); wb.write(bos); return bos.toByteArray();
        } catch (Exception e) { throw new RuntimeException("Export failed", e); }
    }
}
