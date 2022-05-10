package com.fiats.arrangement.payload;

import com.fiats.tmgcoreutils.constant.PortfolioRetailAction;
import lombok.Data;

@Data
public class PortfolioResponseWrapper {

    private boolean isOrganizationRow;
    private Long customerId;
    private PortfolioRetailAction action;

}
