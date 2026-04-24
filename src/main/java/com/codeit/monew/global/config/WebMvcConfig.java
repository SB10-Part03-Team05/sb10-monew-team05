package com.codeit.monew.global.config;

import com.codeit.monew.global.logging.ClientIpResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

  private final ObjectProvider<ClientIpResolver> clientIpResolverProvider;

  @Bean
  public MDCLoggingInterceptor mdcLoggingInterceptor() {
    ClientIpResolver clientIpResolver =
        clientIpResolverProvider.getIfAvailable(ClientIpResolver::new);

    return new MDCLoggingInterceptor(clientIpResolver);
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(mdcLoggingInterceptor())
        .addPathPatterns("/**"); // 모든 경로에 적용
  }
}
