package com.codeit.monew.infra.external.rss;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

public final class ResponseBodyDecoder {

  private static final Pattern XML_DECLARATION_CHARSET = Pattern.compile(
      "(?i)<\\?xml[^>]*encoding\\s*=\\s*['\"]([A-Za-z0-9._-]+)['\"]");
  private static final Pattern HTML_META_CHARSET = Pattern.compile(
      "(?i)<meta[^>]*charset\\s*=\\s*['\"]?([A-Za-z0-9._-]+)");

  private ResponseBodyDecoder() {
  }

  public static String decode(byte[] body, HttpHeaders headers) {
    Charset charset = resolveCharset(body, headers);
    return new String(body, charset);
  }

  private static Charset resolveCharset(byte[] body, HttpHeaders headers) {
    String probe = new String(body, 0, Math.min(body.length, 4096), StandardCharsets.ISO_8859_1);

    Charset xmlDeclared = parseCharset(probe, XML_DECLARATION_CHARSET);
    if (xmlDeclared != null) {
      return xmlDeclared;
    }

    Charset htmlDeclared = parseCharset(probe, HTML_META_CHARSET);
    if (htmlDeclared != null) {
      return htmlDeclared;
    }

    MediaType contentType = headers == null ? null : headers.getContentType();
    if (contentType != null && contentType.getCharset() != null) {
      return contentType.getCharset();
    }

    return StandardCharsets.UTF_8;
  }

  private static Charset parseCharset(String probe, Pattern pattern) {
    Matcher matcher = pattern.matcher(probe);
    if (!matcher.find()) {
      return null;
    }

    String charsetName = matcher.group(1);
    if (!StringUtils.hasText(charsetName)) {
      return null;
    }

    try {
      return Charset.forName(charsetName.trim());
    } catch (RuntimeException e) {
      return null;
    }
  }
}
