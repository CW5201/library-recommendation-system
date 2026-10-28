package com.library.recommendation.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.library.recommendation.common.Result;
import com.library.recommendation.dto.ChatRequest;
import com.library.recommendation.dto.ReadingPlanRequest;
import com.library.recommendation.dto.ReasonRequest;
import com.library.recommendation.dto.SemanticSearchRequest;
import com.library.recommendation.service.AiService;
import com.library.recommendation.vo.AiStatusVO;
import com.library.recommendation.vo.BookVO;
import com.library.recommendation.vo.ChatMessageVO;
import com.library.recommendation.vo.ChatVO;
import com.library.recommendation.vo.ReadingPlanVO;
import com.library.recommendation.vo.ReasonVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @GetMapping("/status")
    public Result<AiStatusVO> status() {
        return Result.ok(aiService.status());
    }

    @PostMapping("/chat")
    public Result<ChatVO> chat(@Valid @RequestBody ChatRequest request) {
        return Result.ok(aiService.chat(StpUtil.getLoginIdAsLong(), request));
    }

    @PostMapping("/semantic-search")
    public Result<List<BookVO>> semanticSearch(@Valid @RequestBody SemanticSearchRequest request) {
        return Result.ok(aiService.semanticSearch(StpUtil.getLoginIdAsLong(), request));
    }

    @PostMapping("/reading-plan")
    public Result<ReadingPlanVO> readingPlan(@Valid @RequestBody ReadingPlanRequest request) {
        return Result.ok(aiService.readingPlan(StpUtil.getLoginIdAsLong(), request));
    }

    @PostMapping("/recommend-reason")
    public Result<ReasonVO> recommendReason(@Valid @RequestBody ReasonRequest request) {
        return Result.ok(aiService.recommendReason(StpUtil.getLoginIdAsLong(), request.bookId()));
    }

    @GetMapping("/history")
    public Result<List<ChatMessageVO>> history(@RequestParam(required = false) String sessionId) {
        return Result.ok(aiService.history(StpUtil.getLoginIdAsLong(), sessionId));
    }
}
