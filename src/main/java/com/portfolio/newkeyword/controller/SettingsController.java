package com.portfolio.newkeyword.controller;

import com.portfolio.newkeyword.config.CurrentUser;
import com.portfolio.newkeyword.entity.User;
import com.portfolio.newkeyword.entity.UserKeyword;
import com.portfolio.newkeyword.entity.UserNewsSource;
import com.portfolio.newkeyword.service.KeywordService;
import com.portfolio.newkeyword.service.NewsSourceService;
import com.portfolio.newkeyword.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final KeywordService keywordService;
    private final NewsSourceService newsSourceService;
    private final UserService userService;

    @GetMapping
    public String settings(Model model, @CurrentUser User user) {

        List<UserKeyword> userKeywords = keywordService.getUserKeywords(user);
        List<UserNewsSource> userNewsSources = newsSourceService.getUserNewsSources(user);
        List<UserNewsSource.NewsSourceType> availableSourceTypes = newsSourceService.getAllAvailableSourceTypes();

        model.addAttribute("userKeywords", userKeywords);
        model.addAttribute("userNewsSources", userNewsSources);
        model.addAttribute("availableSourceTypes", availableSourceTypes);
        model.addAttribute("user", user);

        return "settings/index";
    }

    @PostMapping("/keywords")
    public String addKeyword(@RequestParam String keyword,
                             @CurrentUser User user,
                             RedirectAttributes redirectAttributes) {
        try {
            keywordService.addKeyword(user, keyword.trim());
            redirectAttributes.addFlashAttribute("success", "키워드가 추가되었습니다. 뉴스 수집을 시작합니다.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settings";
    }

    @PostMapping("/keywords/{keywordId}/toggle")
    public String toggleKeyword(@PathVariable Long keywordId,
                                @CurrentUser User user,
                                RedirectAttributes redirectAttributes) {
        try {
            keywordService.toggleKeywordStatus(user, keywordId);
            redirectAttributes.addFlashAttribute("success", "키워드 상태가 변경되었습니다.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settings";
    }

    @PostMapping("/keywords/{keywordId}/delete")
    public String deleteKeyword(@PathVariable Long keywordId,
                                @CurrentUser User user,
                                RedirectAttributes redirectAttributes) {
        try {
            keywordService.removeKeyword(user, keywordId);
            redirectAttributes.addFlashAttribute("success", "키워드가 삭제되었습니다.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settings";
    }

    @PostMapping("/sources")
    public String addNewsSource(@RequestParam UserNewsSource.NewsSourceType sourceType,
                                @CurrentUser User user,
                                RedirectAttributes redirectAttributes) {
        try {
            newsSourceService.addNewsSource(user, sourceType);
            redirectAttributes.addFlashAttribute("success", "뉴스 소스가 추가되었습니다.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settings";
    }

    @PostMapping("/sources/{sourceId}/toggle")
    public String toggleNewsSource(@PathVariable Long sourceId,
                                   @CurrentUser User user,
                                   RedirectAttributes redirectAttributes) {
        try {
            newsSourceService.toggleNewsSourceStatus(user, sourceId);
            redirectAttributes.addFlashAttribute("success", "뉴스 소스 상태가 변경되었습니다.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settings";
    }

    @PostMapping("/sources/{sourceId}/delete")
    public String deleteNewsSource(@PathVariable Long sourceId,
                                   @CurrentUser User user,
                                   RedirectAttributes redirectAttributes) {
        try {
            newsSourceService.removeNewsSource(user, sourceId);
            redirectAttributes.addFlashAttribute("success", "뉴스 소스가 삭제되었습니다.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settings";
    }
}