package com.codeit.monew.domain.interest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.monew.domain.interest.dto.request.InterestRegisterRequest;
import com.codeit.monew.domain.interest.dto.request.InterestUpdateRequest;
import com.codeit.monew.domain.interest.dto.response.CursorPageResponseInterestDto;
import com.codeit.monew.domain.interest.dto.response.InterestDto;
import com.codeit.monew.domain.interest.dto.response.SubscriptionDto;
import com.codeit.monew.domain.interest.entity.Subscription;
import com.codeit.monew.domain.interest.service.InterestService;
import com.codeit.monew.global.exception.ErrorCode;
import com.codeit.monew.global.exception.GlobalExceptionHandler;
import com.codeit.monew.global.exception.Interest.DuplicateInterestException;
import com.codeit.monew.global.exception.Interest.InterestNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.MissingServletRequestParameterException;

@WebMvcTest(InterestController.class)
@Import(GlobalExceptionHandler.class)
public class InterestControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private InterestService interestService;

  @Nested
  @DisplayName("관심사 등록 API 테스트")
  class postInterest {

    @Test
    @DisplayName("유효한 관심사 등록 요청 시 201 상태코드와 등록된 관심사 정보가 반환된다.")
    void should_register_interest_success_and_return_201() throws Exception {
      // given
      UUID interestId = UUID.randomUUID();
      InterestRegisterRequest request = new InterestRegisterRequest(
          "관심사명",
          List.of("키워드1")
      );
      InterestDto interestDto = new InterestDto(interestId,
          "관심사명",
          List.of("키워드1"),
          0L,
          false
      );

      given(interestService.register(any(InterestRegisterRequest.class))).willReturn(interestDto);
      // when, then

      mockMvc.perform(post("/api/interests")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").value(interestId.toString()))
          .andExpect(jsonPath("$.name").value(request.name()))
          .andExpect(jsonPath("$.keywords[0]").value(request.keywords().get(0)));
    }

    @Test
    @DisplayName("유사 관심사 중복 시 409 상태 코드와 DuplicateInterest 예외가 발생한다.")
    void should_fail_register_when_interest_duplicated() throws Exception {
      // given
      InterestRegisterRequest request = new InterestRegisterRequest(
          "관심사명",
          List.of("키워드1")
      );

      given(interestService.register(any(InterestRegisterRequest.class))).willThrow(
          new DuplicateInterestException(request.keywords().get(0)));

      // when, then
      mockMvc.perform(post("/api/interests")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value(ErrorCode.DUPLICATE_INTEREST.toString()))
          .andExpect(jsonPath("$.status").value(409))
          .andExpect(
              jsonPath("$.exceptionType").value(DuplicateInterestException.class.getSimpleName()));
    }

    @Test
    @DisplayName("잘못된 형식으로 관심사 등록 요청 시 400 상태 코드가 반환된다.")
    void should_fail_register_when_keywords_invalid() throws Exception {
      // given
      InterestRegisterRequest request = new InterestRegisterRequest(
          "관심사명",
          List.of()
      );

      // when, then
      mockMvc.perform(post("/api/interests")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400));
    }
  }

  @Nested
  @DisplayName("관심사 정보 수정 API 테스트")
  class updateInterest {

    @Test
    @DisplayName("유효한 수정 요청 시 200 상태코드와 수정된 관심사 정보가 반환된다.")
    void should_update_interest_success_and_return_200() throws Exception {
      // given
      UUID interestId = UUID.randomUUID();
      InterestUpdateRequest request = new InterestUpdateRequest(
          List.of("newKeyword")
      );
      InterestDto interestDto = new InterestDto(interestId,
          "관심사명",
          List.of("newKeyword"),
          0L,
          false
      );
      given(interestService.update(any(UUID.class), any(InterestUpdateRequest.class)))
          .willReturn(interestDto);

      // when, then
      mockMvc.perform(patch("/api/interests/" + interestId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(interestId.toString()))
          .andExpect(jsonPath("$.keywords[0]").value(request.keywords().get(0)));
    }

    @Test
    @DisplayName("잘못된 형식으로 수정 요청 시 400 상태 코드가 반환된다.")
    void should_fail_update_interest_when_keyword_invalid() throws Exception {
      // given
      UUID interestId = UUID.randomUUID();
      InterestUpdateRequest request = new InterestUpdateRequest(
          List.of()
      );

      // when, then
      mockMvc.perform(patch("/api/interests/" + interestId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("수정할 관심사가 존재하지 않으면 404 상태 코드가 반환된다.")
    void should_fail_update_interest_when_interest_not_found() throws Exception {
      // given
      UUID interestId = UUID.randomUUID();
      InterestUpdateRequest request = new InterestUpdateRequest(
          List.of("newKeyword")
      );
      given(interestService.update(any(UUID.class), any(InterestUpdateRequest.class)))
          .willThrow(new InterestNotFoundException(interestId));

      // when, then
      mockMvc.perform(patch("/api/interests/" + interestId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value(ErrorCode.INTEREST_NOT_FOUND.toString()))
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(
              jsonPath("$.exceptionType").value(InterestNotFoundException.class.getSimpleName()));
    }
  }

  @Nested
  @DisplayName("관심사 물리 삭제 API 테스트")
  class deleteInterest {

    @Test
    @DisplayName("유효한 삭제 요청 시 204 상태코드가 반환된다.")
    void should_delete_interest_and_return_204() throws Exception {
      // given
      UUID interestId = UUID.randomUUID();

      willDoNothing().given(interestService).delete(interestId);

      // when, then
      mockMvc.perform(delete("/api/interests/" + interestId))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("물리 삭제할 관심사가 존재하지 않으면 404 상태 코드가 반환된다.")
    void should_fail_delete_interest_when_interest_not_found() throws Exception {
      // given
      UUID interestId = UUID.randomUUID();

      willThrow(new InterestNotFoundException(interestId)).given(interestService)
          .delete(any(UUID.class));

      // when, then
      mockMvc.perform(delete("/api/interests/" + interestId))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value(ErrorCode.INTEREST_NOT_FOUND.toString()))
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(
              jsonPath("$.exceptionType").value(InterestNotFoundException.class.getSimpleName()));
    }
  }

  @Nested
  @DisplayName("관심사 목록 조회 API 테스트")
  class getInterestList {

    @Test
    @DisplayName("관심사 목록 조회 시 200 상태코드와 커서 페이지네이션이 적용된 관심사 목록이 반환된다.")
    void should_search_interest_list_success_and_return__200() throws Exception {
      // given
      UUID interestId1 = UUID.randomUUID();
      UUID interestId2 = UUID.randomUUID();
      UUID requestUserId = UUID.randomUUID();
      InterestDto interestDto1 = new InterestDto(
          interestId1,
          "관심사명",
          List.of("keyword"),
          0L,
          false
      );
      InterestDto interestDto2 = new InterestDto(
          interestId2,
          "관심사명",
          List.of("keyword"),
          0L,
          false
      );

      CursorPageResponseInterestDto response = new CursorPageResponseInterestDto(
          List.of(interestDto1, interestDto2),
          "2026-04-17T09:12:15Z",
          2,
          2,
          false
      );

      given(interestService.getList(
              any(String.class),
              any(String.class),
              any(String.class),
              isNull(),
              anyInt(),
              any(UUID.class)
          )
      ).willReturn(response);

      // when, then
      mockMvc.perform(get("/api/interests")
              .param("keyword", "keyword")
              .param("orderBy", "name")
              .param("direction", "ASC")
              .param("limit", "2")
              .header("Monew-Request-User-ID", requestUserId.toString()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content[0].id").value(interestDto1.id().toString()))
          .andExpect(jsonPath("$.content[1].id").value(interestDto2.id().toString()))
          .andExpect(jsonPath("$.size").value(2));
    }

    @Test
    @DisplayName("필수 파라미터(orderBy/direction/limit)가 누락된 경우 400 상태코드와 MissingServletRequestParameterException 예외 발생")
    void should_fail_search_interest_list_when_required_parameter_is_missing() throws Exception {
      // given
      UUID requestUserId = UUID.randomUUID();

      // when, then
      mockMvc.perform(get("/api/interests")
              .param("keyword", "keyword")
              .param("limit", "2")
              .header("Monew-Request-User-ID", requestUserId.toString()))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("MISSING_REQUEST_PARAMETER"))
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.exceptionType").value(
              MissingServletRequestParameterException.class.getSimpleName()));
      // Article은 DTO를 통한 필드 유효성 검사(@Valid)를 거치므로 MethodArgumentNotValidException이 발생함
      // Interest는 @RequestParam을 통한 개별 파라미터 필수값 검사를 거치므로 MissingServletRequestParameterException이 발생함
    }

    @Test
    @DisplayName("limit이 1미만일 경우 400 상태코드와 ConstraintViolationException 예외 발생")
    void should_fail_search_interest_list_when_limit_is_less_than_1() throws Exception {
      // given
      UUID requestUserId = UUID.randomUUID();

      // when, then
      mockMvc.perform(get("/api/interests")
              .param("keyword", "keyword")
              .param("orderBy", "name")
              .param("direction", "ASC")
              .param("limit", "0")
              .header("Monew-Request-User-ID", requestUserId.toString()))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("CONSTRAINT_VIOLATION_ERROR"))
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.exceptionType").value(
              ConstraintViolationException.class.getSimpleName()));
      // @RequestParam에 대한 제약 조건(@Min 등) 위반 시, 객체 검증 예외가 아닌 ConstraintViolationException이 발생함
    }
  }

  @Nested
  @DisplayName("관심사 구독 API 테스트")
  class subscribeInterest {

    @Test
    @DisplayName("유효한 관심사 구독 요청 시 201 상태코드와 구독 정보가 반환된다.")
    void should_subscribe_interest_success_and_return_201() throws Exception {
      // given
      UUID interestId = UUID.randomUUID();
      UUID requestUserId = UUID.randomUUID();
      UUID subscriptionId = UUID.randomUUID();

      SubscriptionDto subscriptionDto = new SubscriptionDto(
          subscriptionId,
          interestId,
          "관심사명",
          List.of("keyword"),
          1L,
          Instant.now()
      );
      given(interestService.subscribe(any(UUID.class), any(UUID.class)))
          .willReturn(subscriptionDto);

      // when, then
      mockMvc.perform(post("/api/interests/" + interestId + "/subscriptions")
              .contentType(MediaType.APPLICATION_JSON)
              .header("Monew-Request-User-ID", requestUserId.toString()))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").value(subscriptionId.toString()))
          .andExpect(jsonPath("$.interestId").value(interestId.toString()))
          .andExpect(jsonPath("$.interestName").value("관심사명"))
          .andExpect(
              jsonPath("$.interestKeywords[0]").value(subscriptionDto.interestKeywords().get(0)))
          .andExpect(jsonPath("$.interestSubscriberCount").value(1L));
    }

    @Test
    @DisplayName("구독할 관심사가 존재하지 않으면 404 상태 코드가 반환된다.")
    void should_fail_subscribe_interest_when_interest_not_found() throws Exception {
      // given
      UUID interestId = UUID.randomUUID();
      UUID requestUserId = UUID.randomUUID();
      given(interestService.subscribe(any(UUID.class), any(UUID.class)))
          .willThrow(new InterestNotFoundException(interestId));

      // when, then
      mockMvc.perform(post("/api/interests/" + interestId + "/subscriptions")
              .contentType(MediaType.APPLICATION_JSON)
              .header("Monew-Request-User-ID", requestUserId.toString()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value(ErrorCode.INTEREST_NOT_FOUND.toString()))
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(
              jsonPath("$.exceptionType").value(InterestNotFoundException.class.getSimpleName()));
    }
  }

  @Nested
  @DisplayName("관심사 구독 취소 API 테스트")
  class unsubscribeInterest {

    @Test
    @DisplayName("유효한 삭제 요청 시 204 상태코드가 반환된다.")
    void should_unsubscribe_interest_and_return_204() throws Exception {
      // given
      UUID interestId = UUID.randomUUID();
      UUID requestUserId = UUID.randomUUID();

      willDoNothing().given(interestService).unsubscribe(interestId, requestUserId);

      // when, then
      mockMvc.perform(delete("/api/interests/" + interestId + "/subscriptions")
              .header("Monew-Request-User-ID", requestUserId.toString()))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("구독 취소할 관심사가 존재하지 않으면 404 상태 코드가 반환된다.")
    void should_fail_delete_interest_when_interest_not_found() throws Exception {
      // given
      UUID interestId = UUID.randomUUID();
      UUID requestUserId = UUID.randomUUID();

      willThrow(new InterestNotFoundException(interestId)).given(interestService)
          .unsubscribe(any(UUID.class), any(UUID.class));

      // when, then
      mockMvc.perform(delete("/api/interests/" + interestId + "/subscriptions")
              .header("Monew-Request-User-ID", requestUserId.toString()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value(ErrorCode.INTEREST_NOT_FOUND.toString()))
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(
              jsonPath("$.exceptionType").value(InterestNotFoundException.class.getSimpleName()));
    }
  }

}
