package com.fiats.arrangement.constant;

import com.neo.exception.INeoErrorCode;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletResponse;

public enum ArrangementErrorCode implements INeoErrorCode {
    ASSET_UNAVAILABLE("100", "arrgmt.asset.unavailable", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    CUSTOMER_NOT_FOUND("101", "arrgmt.customer.notfound", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    ASSET_PORTFOLIO_UNBALANCE("102", "arrgmt.asset.portfolio.unbalance", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    ASSET_NOT_ENOUGH("103", "arrgmt.asset.notenough", HttpStatus.BAD_REQUEST.value()),
    INFORMATION_REQUIRED("104", "arrgmt.info.required", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    ARRANGEMENT_TYPE_NOT_SUPPORT("105", "arrgmt.type.notsupport", HttpStatus.BAD_REQUEST.value()),
    APPROVE_COLLATERAL_ERROR("106", "arrgmt.collateral.error", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    PORTFOLIO_TRANSACTION_ERROR("107", "arrgmt.portfolio.transaction.error", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    COLLATERAL_REQUEST_INVALID_STATUS("108", "arrgmt.collateral.status.invalid", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    EXCEPTION_REQUEST_INVALID_STATUS("109", "arrgmt.exception.status.invalid", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    ARRANGEMENT_STATUS_INVALID("110", "arrgmt.status.invalid", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    BROKER_NOT_FOUND("111", "arrgmt.broker.notfound", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    PRODUCT_UNAVAILABLE("112", "arrgmt.product.unavailable", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    ASSET_PORTFOLIO_UNAVAILABLE("113", "arrgmt.asset.portfolio.unavailable", HttpStatus.INTERNAL_SERVER_ERROR.value()),

    // attribute violations
    VIOLATED_DEADLINE_PAYMENT("114", "arrgmt.violated.deadline.payment", HttpStatus.BAD_REQUEST.value()),
    VIOLATED_LIMITED_DAY("115", "arrgmt.violated.limited.day", HttpStatus.BAD_REQUEST.value()),
    VIOLATED_BOND_STATUS("116", "arrgmt.violated.bond.status", HttpStatus.BAD_REQUEST.value()),
    VIOLATED_LOCK_STATUS("117", "arrgmt.violated.lock.status", HttpStatus.BAD_REQUEST.value()),
    VIOLATED_MIN_TRADING_AMOUNT("118", "arrgmt.violated.min.trading.amount", HttpStatus.BAD_REQUEST.value()),
    UNSUPPORTED_TIME("119", "unsupported.time", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    STOCK_ACCOUNT_STATUS_INVALID("120", "arrgmt.account.stock.status.invalid", HttpStatus.BAD_REQUEST.value()),
    UNSUPPORTED_HTTP_STATUS("121", "arrgmt.http.status", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    FLATTEN_MAP_ERROR("122", "arrgmt.flatten.map.error", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    NOTIFICATION_PAYLOAD_EMPTY_ERROR("123", "arrgmt.notification.payload.empty", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    BROKER_CUSTOMER_ID_EMPTY("124", "arrgmt.broker.customer.id.empty", HttpStatus.BAD_REQUEST.value()),
    ARRANGEMENT_RECORD_NOT_FOUND("125", "arrgmt.record.not.found", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    ARRANGEMENT_SELL_ARRANGEMENT_CODE_NOT_FOUND("126", "arrgmt.sell.arrangement.code.not.found",
            HttpStatus.BAD_REQUEST.value()),
    UNSUPPORTED_MATCHING_MECHANISM("127", "arrgmt.unsupported.matching.mechanism",
            HttpStatus.INTERNAL_SERVER_ERROR.value()),
    T24_INVALID_INPUT("128", "arrgmt.t24.invalid.input", HttpStatus.BAD_REQUEST.value()),

    PRICING_SERVICE_ERROR("129", "arrgmt.pricing.service.error", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    EMPTY_ASSET_PAYLOAD("130", "empty.asset.input", HttpStatus.BAD_REQUEST.value()),
    ARRANGEMENT_PRICING_NOT_FOUND("131", "arrgmt.pricing.not.found", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    T24_INVALID_HEADER("132", "arrgmt.t24.invalid.header", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    T24_INVALID_FILE_TYPE("133", "arrgmt.t24.invalid.file.type", HttpStatus.BAD_REQUEST.value()),
    T24_UPLOADING_FILE_ERROR("134", "arrgmt.t24.uploading.file.error", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    INFORMATION_SERVICE_ERROR("135", "arrgmt.info.service.error", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    T24_SERVER_ERROR("136", "arrgmt.t24.server.error", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    T24_ENTRY_NOT_AVAILABLE("137", "arrgmt.t24.entry.not.available", HttpStatus.BAD_REQUEST.value()),
    T24_ARRANGEMENT_NOT_AVAILABLE("138", "arrgmt.t24.arrangement.not.available", HttpStatus.BAD_REQUEST.value()),
    T24_INVALID_MAPPING_STATUS("139", "arrgmt.t24.invalid.mapping.status", HttpStatus.BAD_REQUEST.value()),

    CUSTOMER_NOT_UNIQUE("140", "arrgmt.customer.not.unique", HttpStatus.BAD_REQUEST.value()),
    ARRANGEMENT_PAYMENT_ALREADY("141", "arrgmt.payment.already", HttpStatus.BAD_REQUEST.value()),
    INTRADAY_LIMIT_EMPTY_PAYLOAD("142", "arrgmt.intraday.limit.empty.payload", HttpStatus.BAD_REQUEST.value()),
    INTRADAY_LIMIT_EXCEEDED("143", "arrgmt.intraday.limit.exceeded", HttpStatus.BAD_REQUEST.value()),
    CONTRACT_INFO_EMPTY("144", "arrgmt.contract.info.empty", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    ATTRIBUTE_NOT_FOUND("145", "arrgmt.attribute.not.found", HttpStatus.NOT_FOUND.value()),
    TEMPLATE_NOT_FOUND("146", "arrgmt.template.not.found", HttpStatus.NOT_FOUND.value()),
    SNAPSHOT_MODEL_FAILED("147", "arrgmt.snapshot.model.failed", HttpStatus.INTERNAL_SERVER_ERROR.value()),

    PROFESSIONAL_CUSTOMER_REQUIRED("148", "arrgmt.professional.customer.required", HttpStatus.BAD_REQUEST.value()),

    CONTRACT_SERVICE_ERROR("149", "arrgmt.contract.service.error", HttpStatus.INTERNAL_SERVER_ERROR.value()),

    PERMISSION_VIOLATED("150", "arrgmt.contract.permission.violated", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    SELF_REFER_VIOLATED("151", "arrgmt.self.refer.violated", HttpStatus.BAD_REQUEST.value()),

    WSO2_ERROR("998", "invalid.wso2.error", HttpStatus.INTERNAL_SERVER_ERROR.value())
    ;

    private String code;

    private String messageCode;
    private Integer httpStatus;

    ArrangementErrorCode(String errorCode, String messageCode) {
        this.code = errorCode;
        this.messageCode = messageCode;
        this.httpStatus = HttpServletResponse.SC_BAD_REQUEST;
    }

    ArrangementErrorCode(String errorCode, String messageCode, Integer httpStatus) {
        this.code = errorCode;
        this.messageCode = messageCode;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return this.code;
    }

    public String getMessageCode() {
        return messageCode;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }
}
