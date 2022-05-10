package com.fiats.arrangement.mapper;

import com.fiats.arrangement.payload.filter.ReportingFilter;
import com.fiats.tmgcoreutils.payload.ArrangementInfoDTO;
import com.fiats.tmgcoreutils.payload.ReportingMatchedArrangementDTO;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

@Mapper
public interface ReportingMapper {

    ReportingMatchedArrangementDTO retrieveMatchedArrangementCount(@Param("filter") ReportingFilter filter);

    Page<ArrangementInfoDTO> retrieveMatchedArrangement(@Param("filter") ReportingFilter filter);

    Page<ArrangementInfoDTO> retrieveMatchedArrangement(@Param("filter") ReportingFilter filter, RowBounds rowBounds);

    ReportingMatchedArrangementDTO retrievePaymentArrangementCount(@Param("filter") ReportingFilter filter);

    Page<ArrangementInfoDTO> retrievePaymentArrangement(@Param("filter") ReportingFilter filter);

    Page<ArrangementInfoDTO> retrievePaymentArrangement(@Param("filter") ReportingFilter filter, RowBounds rowBounds);

    ReportingMatchedArrangementDTO retrieveSummaryArrangementCount(@Param("filter") ReportingFilter filter);

    Page<ArrangementInfoDTO> retrieveSummaryArrangement(@Param("filter") ReportingFilter filter);

    Page<ArrangementInfoDTO> retrieveSummaryArrangement(@Param("filter") ReportingFilter filter, RowBounds rowBounds);
}
