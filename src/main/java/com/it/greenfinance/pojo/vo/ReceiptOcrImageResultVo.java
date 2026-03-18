package com.it.greenfinance.pojo.vo;

import lombok.Data;

import java.util.List;

@Data
public class ReceiptOcrImageResultVo {
    private Integer index;
    private String originalFilename;
    private String imageKey;
    private String imageUrl;
    private String status;
    private String errorMessage;
    private List<String> ocrTexts;
    private List<AnalyzedTransactionVo> drafts;
}

