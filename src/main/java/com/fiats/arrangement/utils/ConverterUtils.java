package com.fiats.arrangement.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiats.arrangement.constant.ArrangementErrorCode;
import com.fiats.arrangement.constant.ArrangementRoleEnum;
import com.fiats.arrangement.jpa.entity.Arrangement;
import com.fiats.arrangement.jpa.entity.ArrangementParty;
import com.fiats.arrangement.jpa.entity.ArrangementPricing;
import com.fiats.exception.NeoFiatsException;
import com.fiats.exception.ValidationException;
import com.fiats.tmgcoreutils.constant.Constant;
import com.fiats.tmgcoreutils.payload.*;
import com.fiats.tmgcoreutils.utils.CommonUtils;
import com.fiats.tmgcoreutils.utils.DateHelper;
import com.fiats.tmgcoreutils.utils.ReadNumber;
import com.github.wnameless.json.flattener.JsonFlattener;
import org.slf4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class ConverterUtils {

    public static String castToString(Object input) {
        return input == null ? Constant.EMPTY : input.toString();
    }

    public static Integer parseIntFlexibly(Logger log, Object input, boolean mandatory) {

        if (input == null & !mandatory) {
            return null;
        }

        try {

            if (input instanceof Integer) {
                return (Integer) input;
            }

            // try parsing string
            return Integer.parseInt(String.valueOf(input).replaceAll("(?:\\.\\d+)?", "")
                    .replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            log.debug("ConverterUtils - parseIntFlexibly - Unable to parse {} with err {}", input, e.getMessage());
            if (mandatory) {
                throw new ValidationException(ArrangementErrorCode.T24_INVALID_INPUT,
                        CommonUtils.format("Invalid integer number {0}", input));
            }
        }
        return null;
    }

    public static Double parseDoubleFlexibly(Logger log, Object input, boolean mandatory) {

        if (input == null & !mandatory) {
            return null;
        }

        try {

            if (input instanceof Double) {
                return (Double) input;
            }

            // try parsing String
            return Double.parseDouble(String.valueOf(input).replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            log.debug("ConverterUtils - parseDoubleFlexibly - Unable to parse {} with err {}", input, e.getMessage());
            if (mandatory) {
                throw new ValidationException(ArrangementErrorCode.T24_INVALID_INPUT,
                        CommonUtils.format("Invalid double number {0}", input));
            }
        }
        return null;
    }

    public static Timestamp parseTimestampFlexibly(Logger log, Object input, String format, boolean mandatory) {

        if (input == null & !mandatory) {
            return null;
        }

        try {
            if (input instanceof Timestamp) {
                return (Timestamp) input;
            } else if (input instanceof Date) {
                return DateHelper.dateToTimestamp((Date) input);
            }

            // try parsing String
            return DateHelper.parseTimestamp((String.valueOf(input)), format);
        } catch (Exception e) {
            log.debug("ConverterUtils - parseTimestampFlexibly - Unable to parse {} with err {}", input, e.getMessage());
            if (mandatory) {
                throw new ValidationException(ArrangementErrorCode.T24_INVALID_INPUT,
                        CommonUtils.format("Invalid date format {0}", input));
            }
        }
        return null;
    }

    public static String parseStringFlexibly(Logger log, Object input, boolean mandatory) {

        if (input == null & !mandatory) {
            return null;
        }

        try {

            // try parsing String
            String converted = (String) input;
            if (!StringUtils.hasText(converted) && mandatory) {
                throw new ValidationException(ArrangementErrorCode.T24_INVALID_INPUT,
                        CommonUtils.format("Empty string mandatory field {0}", input));
            }

            return converted.replaceAll("\\s+", " ");

        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.debug("ConverterUtils - parseStringFlexibly - Unable to parse {} with err {}", input, e.getMessage());
            if (mandatory) {
                throw new ValidationException(ArrangementErrorCode.T24_INVALID_INPUT,
                        CommonUtils.format("Invalid string format {0}", input));
            }
        }
        return null;
    }


    public static boolean isValidMoney(Double amount) {
        return amount != null && amount > 0;
    }

    public static String formatDouble(Double d) {

        if (d == null) {
            return Constant.EMPTY;
        }

        NumberFormat formatter = new DecimalFormat("#0.0000",
                DecimalFormatSymbols.getInstance(Locale.CHINA));
        return formatter.format(d);
    }

    public static String formatDecimal(BigDecimal bd) {

        if (bd == null) {
            return Constant.EMPTY;
        }

        NumberFormat f = new DecimalFormat("#0.0000",
                DecimalFormatSymbols.getInstance(Locale.CHINA));
        return f.format(bd);
    }

    public static String formatDecimalWithoutZero(BigDecimal bd) {

        if (bd == null) {
            return Constant.EMPTY;
        }

        NumberFormat f = new DecimalFormat("#0",
                DecimalFormatSymbols.getInstance(Locale.CHINA));
        return f.format(bd);
    }

    public static ArrangementPricingDTO castPricingToDTO(ArrangementPricing pricing) {
        ArrangementPricingDTO pricingDTO = new ArrangementPricingDTO();
        BeanUtils.copyProperties(pricing, pricingDTO);
        pricingDTO.setPrice(CommonUtils.bigDecimalToDoubleSilently(pricing.getPrice()));
        pricingDTO.setFee(CommonUtils.bigDecimalToDoubleSilently(pricing.getFee()));
        pricingDTO.setDiscountedAmount(CommonUtils.bigDecimalToDoubleSilently(pricing.getDiscountedAmount()));
        pricingDTO.setUnitPrice(CommonUtils.bigDecimalToDoubleSilently(pricing.getUnitPrice()));
        pricingDTO.setAgencyFee(CommonUtils.bigDecimalToDoubleSilently(pricing.getAgencyFee()));
        pricingDTO.setTotalInvestAmount(CommonUtils.bigDecimalToDoubleSilently(pricing.getPrincipal()));
        pricingDTO.setTotalMoneyRtm(CommonUtils.bigDecimalToDoubleSilently(pricing.getTotalMoneyRtm()));
        pricingDTO.setTax(CommonUtils.bigDecimalToDoubleSilently(pricing.getTax()));
        return pricingDTO;
    }

    public static OrderPlacementDTO castArrangementToOrder(Arrangement arr,
                                                           CustomerDTO customerDTO) {


        ArrangementDTO arrDTO = new ArrangementDTO();
        BeanUtils.copyProperties(arr, arrDTO);

        ArrangementPricingDTO pricingDTO = castPricingToDTO(arr.getPricing());

        ArrangementOperationDTO operationDTO = new ArrangementOperationDTO();
        BeanUtils.copyProperties(arr.getOperation(), operationDTO);

        ArrangementPartyDTO partyDTO = new ArrangementPartyDTO();
        ArrangementParty party = arr.getParties().stream()
                .filter(p -> ArrangementRoleEnum.OWNER.toString().equals(p.getRole())
                        || ArrangementRoleEnum.ORGANIZATION.toString().equals(p.getRole()))
                .findAny().orElse(null);
        String purchaserAccount = null;

        if (party != null) {
            BeanUtils.copyProperties(party, partyDTO);
        }

        if (customerDTO != null) {
            partyDTO.setAccount(customerDTO.getAccount());
            partyDTO.setCustomerIdCard(customerDTO.getIdCard());
            partyDTO.setCustomer(customerDTO);
            purchaserAccount = customerDTO.getAccount();
        }

        ArrangementPartyDTO brokerPartyDTO = null;
        ArrangementParty brokerParty = arr.getParty(ArrangementRoleEnum.BROKER);
        if (brokerParty != null) {
            brokerPartyDTO = new ArrangementPartyDTO();
            BeanUtils.copyProperties(brokerParty, brokerPartyDTO);
        }

        ProdDerivativeDTO deriDTO = ProdDerivativeDTO.builder()
                .prodAgreement(new ProdAgreementDTO())
                .prodVanilla(new ProdVanillaDTO())
                .build();

        deriDTO.setId(arr.getProductDerivativeId());
        deriDTO.setCode(arr.getProductDerivativeCode());
        deriDTO.getProdVanilla().setId(arr.getProductVanillaId());
        deriDTO.getProdVanilla().setCode(arr.getProductVanillaCode());
        deriDTO.getProdAgreement().setCheckedId(arr.getProductAgreementId());
        deriDTO.getProdAgreement().setCode(arr.getProductAgreementCode());

        OrderPlacementDTO order = OrderPlacementDTO.builder()
                .purchaserAccount(purchaserAccount)
                .arrangement(arrDTO)
                .party(partyDTO)
                .brokerParty(brokerPartyDTO)
                .pricing(pricingDTO)
                .derivative(deriDTO)
                .operation(operationDTO)
                .build();

        return order;
    }

    public static Map<String, Object> flattenMap(Object obj, ObjectMapper mapper) {
        try {
            return JsonFlattener.flattenAsMap(mapper.writeValueAsString(obj));
        } catch (JsonProcessingException e) {
            String err = CommonUtils.format("Failed to flatten the map {0}", obj);
            throw new NeoFiatsException(ArrangementErrorCode.FLATTEN_MAP_ERROR, err);
        }
    }

    public static String formatNumberSilently(Object obj) {
        try {
            return ReadNumber.numberToString(new BigDecimal(obj.toString()), null);
        } catch (Exception e) {
            return Constant.EMPTY;
        }
    }

    public static String formatNumber(Object obj, String format) {
        try {
            if (StringUtils.hasText(format)) {
                DecimalFormat formatter = new DecimalFormat(format, new DecimalFormatSymbols(Locale.US));
                return formatter.format(obj);
            } else {
                return formatNumberSilently(obj);
            }
        } catch (Exception e) {
            return Constant.EMPTY;
        }
    }

    public static Timestamp stringToDateSilently(String input) {

        try {
            return DateHelper.parseTimestamp(input); // yyyy-mm-dd
        } catch (Exception e) {
            return null;
        }
    }

    public static String formatDateFlexibly(Object obj) {

        if (obj instanceof String) { // already date string but in a different format (yyyy-mm-dd)
            Timestamp input = stringToDateSilently((String) obj);
            return input == null ? obj.toString() : DateHelper.formatDateSilently(input, Constant.DATE_FORMAT);
        } else if (obj instanceof Long) {
            return DateHelper.formatDateSilently(new Timestamp((Long) obj), Constant.DATE_FORMAT);
        } else if (obj instanceof Timestamp) {
            return DateHelper.formatDateSilently((Timestamp) obj, Constant.DATE_FORMAT);
        } else if (obj instanceof Date) {
            return DateHelper.formatDateSilently((Date) obj, Constant.DATE_FORMAT);
        }
        return null;
    }


}
