package com.it.greenfinance.controller;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.it.utils.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/test-hanlp")
public class TestHanLPController {

    @GetMapping("/segment")
    public Result segment(@RequestParam String text) {
        List<Term> termList = HanLP.segment(text);
        return Result.ok(termList);
    }
}