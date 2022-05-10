package com.fiats.arrangement.constant;

import com.fiats.exception.NeoFiatsException;
import com.fiats.exception.ValidationException;
import com.fiats.exception.payload.ErrorResponseMessage;
import com.fiats.tmgcoreutils.common.ErrorCode;
import com.fiats.tmgcoreutils.utils.CommonUtils;
import com.neo.exception.LoggingUtils;
import org.slf4j.Logger;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class ArrConstant {

    public final static Integer EXCEPTION = 1;

    public final static String PDF_EXTENSION = ".pdf";

    public final static Long HDBS_CUSTOMER_ID = 144L;
    public final static String HDBS_ACCOUNT = "HDBS";

    public interface ARR_STATUS {
        int ACTIVE = 1;
        int INACTIVE = 0;
    }

    public final static int LOCK_STATUS = 1;

    public interface ATTRIBUTE {
        String CONTRACT_VARIABLE_CODE = "contract";
    }

    public interface OPERATIONS {
        String customerStatus = "CustomerStatus";
        String contractStatus = "ContractStatus";
        String paymentStatus = "PaymentStatus";
        String deliveryStatus = "DeliveryStatus";
        String collateralStatus = "CollateralStatus";
        String releaseStatus = "ReleaseStatus";
        String arrangementStatus = "ArrangementStatus";
    }

    public enum MakeCheckStatus {

        DRAFT("DRAFT"), WAITING("WAITING"), REJECTED("REJECTED"), APPROVED("APPROVED"), OUTDATED("OUTDATED");
        private String content;

        MakeCheckStatus(String content) {
            this.content = content;
        }

        public String content() {
            return content;
        }

        @Override
        public String toString() {
            return content;
        }
    }

    public enum AssetFilterCondition {

        ALL("ALL"), AVAILABLE("AVAILABLE"), HOLD("HOLD"), BLOCK("BLOCK");
        private String content;

        AssetFilterCondition(String content) {
            this.content = content;
        }

        public String content() {
            return content;
        }

        @Override
        public String toString() {
            return content;
        }
    }

    public final static String EOL = "\n";
    public final static String DOLLAR_SIGN = "$";

    public static String NON_NUMMERIC_REGEX = "[^0-9]";

    public final static String XLS = "xls";
    public final static String XLSX = "xlsx";

    public final static int T24_SHEET = 0;

    public final static String REGEX_TO_DETECT_ARRANGEMENT_CODE = "(?s).*?(?i:H\\s*D\\s*B\\s*S.*?\\d+.*?H\\s*D.*?\\s*)([A-Za-z0-9\\s]+\\s*\\.\\s*[A-Za-z0-9-_\\s]+\\s*\\.\\s*[0-9\\s]+).*?";

    // $SNAPSHOT_PATH/arrangement_contract/{account}/{arrangementId}/{template-name}
    public final static String CONTRACT_PATH_FORMAT = "{0}/arrangement_contract/{1}/{2}/{3}";

    // arrangmentId:action
    public final static String USER_KEY_FORMAT = "{0}:{1}";

    public final static String PARAM_TYPE_STRING = "String";
    public final static String PARAM_TYPE_NUMBER = "Number";
    public final static String PARAM_TYPE_DATE_TIME = "Datetime";

}
