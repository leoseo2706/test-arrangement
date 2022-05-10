package com.fiats.arrangement.controller;

import com.fiats.arrangement.service.AssetService;
import com.fiats.exception.payload.ErrorResponseMessage;
import com.fiats.tmgcoreutils.jwt.JWTHelper;
import com.fiats.tmgcoreutils.payload.ArrangementDTO;
import com.fiats.tmgjpa.payload.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotEmpty;

@RestController
@RequestMapping(path = "/asset")
public class AssetController {

    @Autowired
    AssetService assetService;

    @Operation(
            operationId = "retrieveAsset",
            summary = "Lấy thông tin tài sản",
            tags = {"Arrangement"},
            parameters = {
                    @Parameter(in = ParameterIn.QUERY, name = "os", description = "Tài sản còn lại hay không?"),
                    @Parameter(in = ParameterIn.QUERY, name = "vc", description = "Mã sản phẩm gốc (vanilla)",
                            example = "Bond3M-TMG02"),
                    @Parameter(in = ParameterIn.QUERY, name = "dc", description = "Mã sản phẩm thứ cấp (derivative)",
                            example = "BondFlexy-HDBS202601")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lấy thông tin thành công",
                            content = @Content(schema = @Schema(implementation = ArrangementDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Thông tin request không đúng",
                            content = @Content(schema = @Schema(implementation = ErrorResponseMessage.class))),
                    @ApiResponse(responseCode = "404", description = "Không tìm thấy api",
                            content = @Content(schema = @Schema(implementation = ErrorResponseMessage.class))),
                    @ApiResponse(responseCode = "500", description = "Lỗi hệ thống",
                            content = @Content(schema = @Schema(implementation = ErrorResponseMessage.class)))
            }
    )
    @GetMapping("/{customerId}")
    public ResponseMessage retrieveAsset(@RequestHeader(JWTHelper.Authorization) String authHeader,
                                         @PathVariable Long customerId,
                                         @RequestParam(value = "vc", required = false) String vanillaCode,
                                         @RequestParam(value = "dc", required = false) String derivativeCode,
                                         @RequestParam(value = "os", required = false, defaultValue = "false") Boolean outstanding,
                                         @RequestParam(value = "con", required = false, defaultValue = "ALL") String condition) {



        return new ResponseMessage<>(assetService.findAssetByCustomerId(customerId, vanillaCode, derivativeCode, outstanding, condition));
    }
}