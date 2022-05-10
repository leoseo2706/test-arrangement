package com.fiats.arrangement.payload;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fiats.tmgcoreutils.constant.Constant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import java.io.Serializable;
import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArrangementLogDTO implements Serializable {

    private static final long serialVersionUID = -5316010103759471571L;

    private Long id;

    private Long arrangementId;

    private Long userId;

    private Long customerId;

    private Integer type;

    private String arrangementCode;

    private Integer arrangementType;

    private String contents;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constant.FORMAT_SQLSERVER_FULL,
            timezone = Constant.TIMEZONE_ICT)
    private Timestamp createdDate;

    private Integer active;

    private String logType;
}
