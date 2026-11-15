package com.arequiparescue.shared.presentation;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Structural HTML assertions for rendered Thymeleaf views to prevent duplicate shell,
 * main content, page headers, or HTML id attributes.
 */
public final class HtmlStructureAssertions {

    private static final Pattern ID_ATTR_PATTERN = Pattern.compile("\\bid=\"([^\"]+)\"");

    private HtmlStructureAssertions() {
    }

    public static void assertSingleAppShellStructure(String html, String expectedHeadingId) {
        assertThat(countOccurrences(html, "class=\"are-shell\""))
                .as("Exactamente un contenedor .are-shell")
                .isEqualTo(1);
        assertThat(countOccurrences(html, "<main "))
                .as("Exactamente un elemento <main>")
                .isEqualTo(1);
        assertThat(countOccurrences(html, "<header class=\"are-topbar\">"))
                .as("Exactamente un encabezado .are-topbar")
                .isEqualTo(1);
        assertThat(countOccurrences(html, "<h1 class=\"are-topbar-title\""))
                .as("Exactamente un <h1> de página")
                .isEqualTo(1);
        assertThat(countOccurrences(html, "<footer class=\"are-footer\">"))
                .as("Exactamente un pie de página .are-footer")
                .isEqualTo(1);

        if (expectedHeadingId != null) {
            assertThat(countOccurrences(html, "id=\"" + expectedHeadingId + "\""))
                    .as("Exactamente una instancia del título principal " + expectedHeadingId)
                    .isEqualTo(1);
        }

        assertNoDuplicateHtmlIds(html);
    }

    public static void assertNoDuplicateHtmlIds(String html) {
        Matcher matcher = ID_ATTR_PATTERN.matcher(html);
        Set<String> seen = new HashSet<>();
        while (matcher.find()) {
            String id = matcher.group(1);
            assertThat(seen.add(id))
                    .as("Atributo HTML id duplicado: '%s'", id)
                    .isTrue();
        }
    }

    public static int countOccurrences(String text, String token) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(token, index)) != -1) {
            count++;
            index += token.length();
        }
        return count;
    }
}
