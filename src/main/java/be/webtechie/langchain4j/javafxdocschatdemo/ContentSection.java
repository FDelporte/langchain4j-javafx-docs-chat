package be.webtechie.langchain4j.javafxdocschatdemo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Content section on a documentation page. Each page gets split into sections to be able to link directly to anchors.
 */
public record ContentSection(
        @JsonProperty("objectID") UUID objectID,
        @JsonProperty("groupId") String groupId,
        @JsonProperty("groupLabel") String groupLabel,
        @JsonProperty("version") String version,
        @JsonProperty("title") String title,
        @JsonProperty("section") String section,
        @JsonProperty("url") String url,
        @JsonProperty("link") String link,
        @JsonProperty("content") String content) {
}
