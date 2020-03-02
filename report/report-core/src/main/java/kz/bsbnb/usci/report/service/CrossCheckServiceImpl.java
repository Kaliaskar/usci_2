package kz.bsbnb.usci.report.service;

import jxl.CellView;
import jxl.WorkbookSettings;
import jxl.format.Border;
import jxl.format.BorderLineStyle;
import jxl.format.Colour;
import jxl.write.*;
import kz.bsbnb.usci.report.crosscheck.CrossCheck;
import kz.bsbnb.usci.report.crosscheck.CrossCheckMessageDisplayWrapper;
import kz.bsbnb.usci.report.crosscheck.Message;
import kz.bsbnb.usci.report.dao.CrossCheckDao;
import kz.bsbnb.usci.util.SqlJdbcConverter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.List;

@Service
public class CrossCheckServiceImpl implements  CrossCheckService {
    private  final CrossCheckDao crossCheckDao;
    private WritableFont times12Font;
    private WritableCellFormat headerFormat;
    private WritableCellFormat times12FormatBold;
    private WritableCellFormat times12FormatRed;
    private WritableCellFormat times12FormatGreen;
    private WritableCellFormat groupedCellFormatGreen;
    private WritableCellFormat groupedCellFormatRed;
    private CellView autoSizeCellView;

    public  CrossCheckServiceImpl(CrossCheckDao crossCheckDao) {
        this.crossCheckDao = crossCheckDao;
    }

    @Override
    public List<CrossCheck> getCrossChecks(List<Long> creditorIds, LocalDate reportDate, Long productId) {
        return crossCheckDao.getCrossChecks(creditorIds, reportDate, productId);
    }

    @Override
    public void crossCheck(Long userId, Long creditorId, LocalDate reportDate, long productId, String executeClause) {
        crossCheckDao.crossCheck(userId, creditorId, reportDate, productId, executeClause);
    }

    @Override
    public void crossCheckAll(Long userId, LocalDate reportDate, long productId, String executeClause) {
        crossCheckDao.crossCheckAll(userId, reportDate, productId, executeClause);
    }

    @Override
    public List<CrossCheckMessageDisplayWrapper> getCrossCheckMessages(Long crossCheckId) {
        return crossCheckDao.getCrossCheckMessages(crossCheckId);
    }

    @Override
    public LocalDate getFirstNotApprovedDate(Long creditorId) {
        return crossCheckDao.getFirstNotApprovedDate(creditorId);
    }

    @Override
    public LocalDate getLastApprovedDate(Long creditorId) {
        return crossCheckDao.getLastApprovedDate(creditorId);
    }

    @Override
    public CrossCheck getCrossCheck(Long crossCheckId) {
        return crossCheckDao.getCrossCheck(crossCheckId);
    }

    @Override
    public Message getMessage(Long messageId) {
        return crossCheckDao.getMessage(messageId);
    }

    @Override
    public byte[] exportToExcel(CrossCheck crossCheck) {
        try {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            WorkbookSettings settings = new WorkbookSettings();
            times12Font = new jxl.write.WritableFont(jxl.write.WritableFont.TIMES, 12);
            WritableFont times12Bold = new jxl.write.WritableFont(jxl.write.WritableFont.TIMES, 12,
                    jxl.write.WritableFont.BOLD);

            headerFormat = new WritableCellFormat(times12Bold);
            times12FormatBold = new WritableCellFormat(times12Bold);
            times12FormatBold.setAlignment(jxl.format.Alignment.CENTRE);
            times12FormatBold.setBorder(Border.ALL, BorderLineStyle.THIN);
            times12FormatGreen = new jxl.write.WritableCellFormat(times12Font);
            times12FormatGreen.setBackground(Colour.LIGHT_GREEN);
            times12FormatGreen.setBorder(Border.ALL, BorderLineStyle.THIN);
            times12FormatRed = new WritableCellFormat(times12Font);
            times12FormatRed.setBackground(Colour.RED);
            times12FormatRed.setBorder(Border.ALL, BorderLineStyle.THIN);

            NumberFormat groupedNumberFormat = new NumberFormat("### ### ### ##0");

            groupedCellFormatGreen = new WritableCellFormat(times12Font, groupedNumberFormat);
            groupedCellFormatGreen.setBackground(Colour.LIGHT_GREEN);
            groupedCellFormatGreen.setBorder(Border.ALL, BorderLineStyle.THIN);
            groupedCellFormatRed = new WritableCellFormat(times12Font, groupedNumberFormat);
            groupedCellFormatRed.setBackground(Colour.RED);
            groupedCellFormatRed.setBorder(Border.ALL, BorderLineStyle.THIN);
            autoSizeCellView = new jxl.CellView();
            autoSizeCellView.setAutosize(true);

            WritableWorkbook workbook = jxl.Workbook.createWorkbook(baos, settings);
            writeCrossCheckOnSheet(crossCheck, workbook);

            workbook.write();
            workbook.close();
            return baos.toByteArray();
        } catch (IOException ioe) {

        } catch (WriteException we) {

        }
        return  null;
    }

    private WritableSheet writeCrossCheckOnSheet(CrossCheck crossCheck, WritableWorkbook workbook)
            throws WriteException {

        WritableSheet sheet = workbook.createSheet(crossCheck.getCreditorName(), 0);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

        int rowCounter = 1;
        sheet.addCell(new jxl.write.Label(2, rowCounter, String.format(
                crossCheck.getCreditorName()), headerFormat));
        sheet.addCell(new jxl.write.Label(1, rowCounter++, String.format(
                "Банк"), headerFormat));

        sheet.addCell(new jxl.write.Label(2, rowCounter, String.format(
                sdf.format(SqlJdbcConverter.convertToDate(crossCheck.getReportDate()))), headerFormat));
        sheet.addCell(new jxl.write.Label(1, rowCounter++, String.format(
                "Отчетная дата"), headerFormat));

        String[] columnNames = new String[] {"Показатель", "Значение в АИС ЕССП", "Значение во внешнем источнике", "Расхождение"};

        for (int columnIndex = 0; columnIndex < columnNames.length; columnIndex++)
            sheet.addCell(new jxl.write.Label(columnIndex, rowCounter,
                    columnNames[columnIndex], times12FormatBold));

        rowCounter++;

        for (CrossCheckMessageDisplayWrapper message : getCrossCheckMessages(crossCheck.getId())) {
            WritableCellFormat usedGroupedCellFormat = message.getIsError() == 0 ?
                    groupedCellFormatGreen : groupedCellFormatRed;

            WritableCellFormat usedCellFormat = message.getIsError() == 0 ?
                    times12FormatGreen : times12FormatRed;

            String description = message.getDescription();

            if (description.length() == 7)
                description = description.length() == 7 ? description.substring(0, 4) + " "
                        + description.substring(4) : description;

            sheet.addCell(new jxl.write.Label(0, rowCounter, description, usedCellFormat));
            addNumberCell(sheet, 1, rowCounter, message.getInnerValue(), usedGroupedCellFormat, usedCellFormat);
            addNumberCell(sheet, 2, rowCounter, message.getOuterValue(), usedGroupedCellFormat, usedCellFormat);
            addNumberCell(sheet, 3, rowCounter, message.getDifference(), usedGroupedCellFormat, usedCellFormat);

            rowCounter++;

            for (int i = 0; i < 4; i++)
                sheet.setColumnView(i, autoSizeCellView);
        }

        return sheet;
    }

    private void addNumberCell(WritableSheet sheet, int columnNumber, int rowNumber, String value,
                               WritableCellFormat numberFormat, WritableCellFormat textFormat) throws WriteException {
        Long longValue = null;

        try {
            longValue = Long.parseLong(value);
        } catch (NumberFormatException nfe) {
        }

        if (longValue != null)
            sheet.addCell(new jxl.write.Number(columnNumber, rowNumber, longValue, numberFormat));
        else
            sheet.addCell(new jxl.write.Label(columnNumber, rowNumber, value, textFormat));

    }

}
