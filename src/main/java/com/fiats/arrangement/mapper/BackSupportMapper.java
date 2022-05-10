package com.fiats.arrangement.mapper;

import com.fiats.arrangement.payload.filter.BackSupportFilter;
import com.fiats.tmgcoreutils.payload.ArrangementInfoDTO;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

@Mapper
public interface BackSupportMapper {

    Long retrieveArrangementsCount(@Param("filter") BackSupportFilter filter);

    Page<ArrangementInfoDTO> retrieveArrangements(@Param("filter") BackSupportFilter filter);

    Page<ArrangementInfoDTO> retrieveArrangements(@Param("filter") BackSupportFilter filter, RowBounds rowBounds);
}
