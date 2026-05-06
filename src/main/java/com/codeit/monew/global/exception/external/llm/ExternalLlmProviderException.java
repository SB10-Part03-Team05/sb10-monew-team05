package com.codeit.monew.global.exception.external.llm;

import com.codeit.monew.global.exception.ErrorCode;

public class ExternalLlmProviderException extends ExternalLlmException {

  public ExternalLlmProviderException(String provider, String sourceUrl, Throwable cause) {
    super(ErrorCode.EXTERNAL_LLM_PROVIDER_ERROR, cause);
    addDetail("provider", provider);
    addDetail("sourceUrl", sourceUrl);
  }
}
