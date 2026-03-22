package com.it.greenfinance.pojo.vo;

import lombok.Data;

import java.util.List;

@Data
public class OcrBillParseResultVo {
    private Boolean billScene;
    private Integer sceneConfidence;
    private List<BillVo> bills;
}
