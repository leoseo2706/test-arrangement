package com.fiats.arrangement.redis.specs;

import com.fiats.arrangement.payload.filter.ArrangementExceptionFilter;
import com.fiats.arrangement.payload.filter.CollateralFilter;
import com.fiats.arrangement.redis.entity.ArrangementException;
import com.fiats.arrangement.redis.entity.Collateral;
import com.fiats.tmgcoreutils.utils.CommonUtils;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CollateralSpecs {

    public Example<Collateral> buildSpecs(CollateralFilter filter) {

        Collateral rd = new Collateral();
        ExampleMatcher matcher = ExampleMatcher.matching();

        if (!CommonUtils.isInvalidPK(filter.getId())) {
            rd.setId(String.valueOf(filter.getId()));
            matcher =  matcher.withMatcher("id", match -> match.exact());
        }

        if (StringUtils.hasText(filter.getArrangementCode())) {
            rd.setAssetCode(filter.getArrangementCode());
            matcher = matcher.withMatcher("assetCode", match -> match.contains());
        }

        if (filter.getPartyId() != null) {
            rd.setPartyId(filter.getPartyId());
            matcher = matcher.withMatcher("partyId", match -> match.exact());
        }

        Example<Collateral> example = Example.of(rd, matcher);
        return example;
    }

}
