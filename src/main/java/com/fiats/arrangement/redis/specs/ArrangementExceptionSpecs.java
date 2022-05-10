package com.fiats.arrangement.redis.specs;

import com.fiats.arrangement.payload.filter.ArrangementExceptionFilter;
import com.fiats.arrangement.redis.entity.ArrangementException;
import com.fiats.tmgcoreutils.utils.CommonUtils;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ArrangementExceptionSpecs {

    public Example<ArrangementException> buildArrangementExceptionSpecs(ArrangementExceptionFilter filter) {

        ArrangementException rd = new ArrangementException();
        ExampleMatcher matcher = ExampleMatcher.matching();

        if (!CommonUtils.isInvalidPK(filter.getId())) {
            rd.setId(String.valueOf(filter.getId()));
            matcher =  matcher.withMatcher("id", match -> match.exact());
        }

        if (filter.getPartyId() != null) {
            rd.setCustomerId(filter.getPartyId());
            matcher = matcher.withMatcher("customerId", match -> match.exact());
        }

//        if (StringUtils.hasText(filter.getArrangementCode())) {
//            rd.setCode(filter.getArrangementCode());
//            matcher = matcher.withMatcher("code", match -> match.contains());
//        }

//        if (filter.getDerivativeId() != null) {
//            rd.setProductDerivativeId(filter.getDerivativeId());
//            matcher = matcher.withMatcher("productDerivativeId", match -> match.exact());
//        }

        Example<ArrangementException> example = Example.of(rd, matcher);
        return example;
    }

}
