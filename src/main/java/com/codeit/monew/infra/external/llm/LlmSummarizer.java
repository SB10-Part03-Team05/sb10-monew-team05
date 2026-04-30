package com.codeit.monew.infra.external.llm;

public interface LlmSummarizer {

  String summarize(String bodyText);
}
