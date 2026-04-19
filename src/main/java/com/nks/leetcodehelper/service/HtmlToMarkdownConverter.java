package com.nks.leetcodehelper.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Component;

@Component
public class HtmlToMarkdownConverter {

    public String convert(String html) {
        if (html == null || html.isBlank()) return "";
        return convertNode(Jsoup.parseBodyFragment(html).body()).strip();
    }

    private String convertNode(Node node) {
        StringBuilder sb = new StringBuilder();
        for (Node child : node.childNodes()) {
            if (child instanceof TextNode textNode) {
                sb.append(textNode.text());
            } else if (child instanceof Element el) {
                sb.append(convertElement(el));
            }
        }
        return sb.toString();
    }

    private String convertElement(Element el) {
        return switch (el.tagName()) {
            case "p" -> convertNode(el).strip() + "\n\n";
            case "strong", "b" -> "**" + convertNode(el) + "**";
            case "em", "i" -> "_" + convertNode(el) + "_";
            case "code" -> "`" + el.text() + "`";
            case "pre" -> "```\n" + el.text() + "\n```\n\n";
            case "ul" -> convertList(el, false) + "\n";
            case "ol" -> convertList(el, true) + "\n";
            case "li" -> "- " + convertNode(el).strip() + "\n";
            case "br" -> "\n";
            case "sup" -> "^" + el.text();
            case "img" -> "";
            default -> convertNode(el);
        };
    }

    private String convertList(Element list, boolean ordered) {
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (Element li : list.select("> li")) {
            String prefix = ordered ? (i++ + ". ") : "- ";
            sb.append(prefix).append(convertNode(li).strip()).append("\n");
        }
        return sb.toString();
    }
}
