package com.it.greenfinance.controller;

import com.it.greenfinance.pojo.bo.BillBo;
import com.it.greenfinance.pojo.vo.BillVo;
import com.it.greenfinance.pojo.vo.ReceiptOcrTaskVo;
import com.it.greenfinance.service.BillService;
import com.it.greenfinance.service.impl.ReceiptOcrTaskService;
import com.it.utils.Result;
import com.it.utils.UserContextUtil;
import io.swagger.annotations.Api;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "截图记账-OCR")
@RestController
@Validated
@RequestMapping("/receipt-ocr")
public class ReceiptOcrController {

    private final ReceiptOcrTaskService receiptOcrTaskService;
    private final UserContextUtil userContextUtil;
    private final BillService billService;

    public ReceiptOcrController(ReceiptOcrTaskService receiptOcrTaskService, UserContextUtil userContextUtil, BillService billService) {
        this.receiptOcrTaskService = receiptOcrTaskService;
        this.userContextUtil = userContextUtil;
        this.billService = billService;
    }

    @PostMapping("/tasks")
    public Result createTask(@RequestParam("files") List<MultipartFile> files) {
        Long userId = userContextUtil.getCurrentUserId();
        String taskId = receiptOcrTaskService.createTask(userId, files);
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        return Result.ok(data);
    }

    @GetMapping("/tasks/{taskId}")
    public Result getTask(@PathVariable String taskId) {
        Long userId = userContextUtil.getCurrentUserId();
        ReceiptOcrTaskVo vo = receiptOcrTaskService.getTask(userId, taskId);
        if (vo == null) {
            return Result.error(404, "任务不存在");
        }
        return Result.ok(vo);
    }

    @GetMapping(value = "/tasks/{taskId}/events", produces = "text/event-stream")
    public SseEmitter events(@PathVariable String taskId) {
        Long userId = userContextUtil.getCurrentUserId();
        SseEmitter emitter = receiptOcrTaskService.subscribe(userId, taskId);
        if (emitter == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        return emitter;
    }

    @PostMapping("/tasks/{taskId}/confirm")
    public Result confirm(@PathVariable String taskId, @Valid @RequestBody List<BillBo> bills) {
        Long userId = userContextUtil.getCurrentUserId();
        ReceiptOcrTaskVo vo = receiptOcrTaskService.getTask(userId, taskId);
        if (vo == null) {
            return Result.error(404, "任务不存在");
        }
        List<BillVo> created = billService.createBills(bills);
        return Result.ok("入账成功", created);
    }

    @PostMapping("/manual")
    public Result manual(@Valid @RequestBody List<BillBo> bills) {
        List<BillVo> created = billService.createBills(bills);
        return Result.ok("入账成功", created);
    }
}

