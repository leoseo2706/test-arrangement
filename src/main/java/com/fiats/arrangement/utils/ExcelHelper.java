package com.fiats.arrangement.utils;

import com.fiats.arrangement.constant.ArrConstant;
import com.fiats.arrangement.constant.ArrangementErrorCode;
import com.fiats.arrangement.constant.T24ColumnEnum;
import com.fiats.arrangement.payload.AccountingEntryDTO;
import com.fiats.exception.NeoFiatsException;
import com.fiats.exception.ValidationException;
import com.fiats.tmgcoreutils.constant.Constant;
import com.fiats.tmgcoreutils.utils.CommonUtils;
import com.fiats.tmgcoreutils.utils.DateHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringJoiner;

@Component
@Slf4j
public class ExcelHelper {

    public String rowToString(Row row) {

        try {
            if (row == null) {
                return Constant.EMPTY;
            }

            StringJoiner sb = new StringJoiner(Constant.COMMA);
            row.forEach(cell -> {
                String cellVal = getCellStringValue(cell);
                if (StringUtils.hasText(cellVal)) {
                    sb.add(cellVal);
                }
            });

            return sb.toString().trim();
        } catch (Exception e) {
            // ignore
            log.error("getting error ", e);
        }

        return Constant.EMPTY;
    }

    public Map<Integer, T24ColumnEnum> findT24Header(Row row) {

        Iterator<Cell> cellIterator = row.cellIterator();
        Map<Integer, T24ColumnEnum> header = new HashMap<>();

        while (cellIterator.hasNext()) {

            Cell cell = cellIterator.next();
            Object cellValue = getCellValue(cell);

            // header must be string value
            if (cellValue instanceof String) {

                // check if this is a recognizable header column
                T24ColumnEnum col = T24ColumnEnum.lookForColNo((String) cellValue);

                if (col != null) {

                    int colIdx = cell.getColumnIndex();
                    if (T24ColumnEnum.ENTRY_NO == col) {
                        header.put(colIdx, T24ColumnEnum.ENTRY_NO);
                    } else if (T24ColumnEnum.TRANSACTION_DATE == col) {
                        header.put(colIdx, T24ColumnEnum.TRANSACTION_DATE);
                    } else if (T24ColumnEnum.EFFECTIVE_DATE == col) {
                        header.put(colIdx, T24ColumnEnum.EFFECTIVE_DATE);
                    } else if (T24ColumnEnum.DESCRIPTION == col) {
                        header.put(colIdx, T24ColumnEnum.DESCRIPTION);
                    } else if (T24ColumnEnum.WITHDRAWING_AMOUNT == col) {
                        header.put(colIdx, T24ColumnEnum.WITHDRAWING_AMOUNT);
                    } else if (T24ColumnEnum.DEPOSITING_AMOUNT == col) {
                        header.put(colIdx, T24ColumnEnum.DEPOSITING_AMOUNT);
                    } else if (T24ColumnEnum.REMAINING_AMOUNT == col) {
                        header.put(colIdx, T24ColumnEnum.REMAINING_AMOUNT);
                    }
                }
            }

        }


        boolean invalidHeader = header.size() == 0
                || T24ColumnEnum.isInvalidMandatory(header);

        if (invalidHeader) {
            return null;
        } else {
            log.info("Detected header {}", header);
            return header;
        }

    }

    public void lookForIndexAndSetT24Data(AccountingEntryDTO model,
                                          T24ColumnEnum col, Object cellValue) {


        if (model != null) {
            if (T24ColumnEnum.ENTRY_NO == col) {
                Integer no = ConverterUtils.parseIntFlexibly(log, cellValue,
                        T24ColumnEnum.ENTRY_NO.isMandatory());
                model.setEntryNo(no);
            } else if (T24ColumnEnum.TRANSACTION_DATE == col) {
                Timestamp td = ConverterUtils.parseTimestampFlexibly(log, cellValue,
                        Constant.DATE_FORMAT, T24ColumnEnum.TRANSACTION_DATE.isMandatory());
                model.setEntryTransactionDate(td);
            } else if (T24ColumnEnum.EFFECTIVE_DATE == col) {
                Timestamp ed = ConverterUtils.parseTimestampFlexibly(log, cellValue,
                        Constant.DATE_FORMAT, T24ColumnEnum.EFFECTIVE_DATE.isMandatory());
                model.setEntryEffectiveDate(ed);
            } else if (T24ColumnEnum.DESCRIPTION == col) {
                String desc = ConverterUtils.parseStringFlexibly(log, cellValue,
                        T24ColumnEnum.DESCRIPTION.isMandatory());
                model.setEntryDescription(desc);
            } else if (T24ColumnEnum.WITHDRAWING_AMOUNT == col) {
                Double wm = ConverterUtils.parseDoubleFlexibly(log, cellValue,
                        T24ColumnEnum.WITHDRAWING_AMOUNT.isMandatory());
                model.setEntryWithdrawingAmount(wm);
            } else if (T24ColumnEnum.DEPOSITING_AMOUNT == col) {
                Double dm = ConverterUtils.parseDoubleFlexibly(log, cellValue,
                        T24ColumnEnum.DEPOSITING_AMOUNT.isMandatory());
                model.setEntryDepositingAmount(dm);
            } else if (T24ColumnEnum.REMAINING_AMOUNT == col) {
                Double ra = ConverterUtils.parseDoubleFlexibly(log, cellValue,
                        T24ColumnEnum.REMAINING_AMOUNT.isMandatory());
                model.setEntryRemainingAmount(ra);
            }
        }
    }


    public Workbook decideXLSXorXLS(String filePath, File file) {
        try {
            if (filePath.toLowerCase().endsWith(ArrConstant.XLSX)) {
                OPCPackage pkg = OPCPackage.open(file);
                return new XSSFWorkbook(pkg);
            } else if (filePath.toLowerCase().endsWith(ArrConstant.XLS)) {
                POIFSFileSystem fs = new POIFSFileSystem(file);
                return new HSSFWorkbook(fs.getRoot(), Constant.ACTIVE);
            }
        } catch (IOException | InvalidFormatException e) {
            log.error("Error creating workbook {} for path {}", e.getMessage(), filePath, e);
            throw new NeoFiatsException(ArrangementErrorCode.T24_INVALID_FILE_TYPE,
                    CommonUtils.format("Invalid file type {0}"));
        }

        throw new ValidationException(ArrangementErrorCode.T24_INVALID_FILE_TYPE,
                CommonUtils.format("Unsupported excel file type {0}", filePath));
    }

    public Object getCellValue(Cell cell) {

        try {

            if (cell == null) {
                return Constant.EMPTY;
            }

            CellType cellType = cell.getCellType();

            switch (cellType) {
                case STRING:

                    // plain text
                    return cell.getRichStringCellValue().getString();
                case NUMERIC:

                    // is date
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue();
                    } else { // number
                        return cell.getNumericCellValue();
                    }
                case BOOLEAN:
                    return cell.getBooleanCellValue();
                case FORMULA:
                    return cell.getCellFormula();
                default:
                    return Constant.EMPTY;
            }
        } catch (Exception e) {

        }

        return Constant.EMPTY;
    }

    public String getCellStringValue(Cell cell) {

        try {

            if (cell == null) {
                return Constant.EMPTY;
            }

            CellType cellType = cell.getCellType();

            switch (cellType) {
                case STRING:

                    // plain text
                    return cell.getRichStringCellValue().getString();
                case NUMERIC:

                    // is date yyyy-mm-dd
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return DateHelper.formatDateSilently(cell.getDateCellValue());
                    } else { // number
                        return String.valueOf(cell.getNumericCellValue());
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    return cell.getCellFormula();
                default:
                    return Constant.EMPTY;
            }
        } catch (Exception e) {

        }

        return Constant.EMPTY;
    }

}
