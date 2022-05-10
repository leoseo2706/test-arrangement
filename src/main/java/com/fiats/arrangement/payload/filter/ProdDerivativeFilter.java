package com.fiats.arrangement.payload.filter;

import com.fiats.tmgjpa.filter.BaseFilter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class ProdDerivativeFilter extends BaseFilter {
    private String name;
    private String code;
    private Long vanillaId;
    private String status;
}
