package com.it.greenfinance.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReceiptOcrTaskVo {
    private String taskId;
    private String status;
    private Integer total;
    private Integer completed;
    private Integer failed;
    private Integer progress;
    private LocalDateTime createTime;
    private LocalDateTime finishTime;
    private List<ReceiptOcrImageResultVo> images;
}

