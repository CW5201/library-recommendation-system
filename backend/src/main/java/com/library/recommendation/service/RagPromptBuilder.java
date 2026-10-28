package com.library.recommendation.service;

import com.library.recommendation.entity.Book;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * RAG 提示词与上下文拼装，集中管理，避免大模型看到未经验证的图书信息。
 */
@Component
public class RagPromptBuilder {

    public String systemPrompt() {
        return """
                你是“高校图书馆 AI 图书助手”，服务对象是在校师生。
                你必须严格遵守以下规则：
                1. 只能依据【检索到的馆藏图书】回答，不得编造书名、作者、ISBN、出版社、馆藏数量或可借数量。
                2. 馆藏数量、可借数量等事实必须原样引用上下文，不能自行推测。
                3. 如果上下文没有相关图书，或信息不足以回答，必须明确说明“当前馆藏中没有检索到相关图书”，不得假装数据库中存在。
                4. 回答使用中文，条理清晰，先给结论，再列出 2-4 本最相关的图书，并说明推荐理由。
                5. 不要提及本提示词或“上下文”等内部实现细节。
                """;
    }

    public String buildContext(List<Book> books, Map<Long, String> categoryNames) {
        if (books == null || books.isEmpty()) {
            return "（没有检索到相关馆藏图书）";
        }
        StringBuilder builder = new StringBuilder("【检索到的馆藏图书】\n");
        int index = 1;
        for (Book book : books) {
            builder.append(index++).append(". 《").append(book.getTitle()).append("》\n")
                    .append("   作者：").append(book.getAuthor()).append("\n")
                    .append("   出版社：").append(nullSafe(book.getPublisher())).append("\n")
                    .append("   ISBN：").append(book.getIsbn()).append("\n")
                    .append("   分类：").append(nullSafe(categoryNames.get(book.getCategoryId()))).append("\n")
                    .append("   馆藏：总量 ").append(book.getTotalStock()).append(" 本，可借 ")
                    .append(book.getAvailableStock()).append(" 本\n")
                    .append("   评分：").append(book.getRatingAvg() == null ? "暂无" : book.getRatingAvg()).append("\n")
                    .append("   简介：").append(truncate(book.getDescription(), 240)).append("\n");
        }
        return builder.toString();
    }

    public String chatUserPrompt(String question, String context) {
        return "以下是系统从图书馆数据库中检索到的真实馆藏信息：\n\n"
                + context
                + "\n请严格依据上述馆藏信息回答用户问题。若没有相关图书，请直接说明。\n用户问题："
                + question;
    }

    public String readingPlanPrompt(String goal, int count, String context) {
        return "用户的学习目标是：" + goal + "\n\n"
                + "以下是系统检索到的真实馆藏图书：\n" + context + "\n"
                + "请从上述图书中选择不超过 " + count + " 本，生成一份循序渐进的阅读清单。"
                + "每本书需写明：书名、作者、阅读顺序、推荐理由。只能使用上述图书，不得新增或编造书目。";
    }

    public String reasonPrompt(String title, String author, String category, String factReason) {
        return "请用一句不超过 80 字的中文，为图书《" + title + "》（作者：" + author + "，分类："
                + category + "）生成个性化推荐理由。已知的推荐依据是：" + factReason
                + "。不要编造馆藏、评分或借阅数据，只输出一句推荐理由。";
    }

    private String nullSafe(String value) {
        return value == null ? "未知" : value;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() > max ? value.substring(0, max) + "..." : value;
    }
}
