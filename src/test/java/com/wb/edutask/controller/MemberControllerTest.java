package com.wb.edutask.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wb.edutask.dto.MemberRequestDto;
import com.wb.edutask.entity.MemberType;
import com.wb.edutask.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 회원 관리 API 테스트 클래스
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 데이터베이스 초기화
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("수강생 회원가입 성공 테스트")
    void registerStudentMember_Success() throws Exception {
        // Given
        MemberRequestDto requestDto = new MemberRequestDto(
            "홍길동",
            "hong@weolbu.com",
            "010-1234-5678",
            "Test123",
            MemberType.STUDENT
        );

        // When & Then
        mockMvc.perform(post("/api/members/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.email").value("hong@weolbu.com"))
                .andExpect(jsonPath("$.phoneNumber").value("010-1234-5678"))
                .andExpect(jsonPath("$.memberType").value("STUDENT"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @DisplayName("강사 회원가입 성공 테스트")
    void registerInstructorMember_Success() throws Exception {
        // Given
        MemberRequestDto requestDto = new MemberRequestDto(
            "너나위",
            "neona@weolbu.com",
            "010-9876-5432",
            "Inst123",
            MemberType.INSTRUCTOR
        );

        // When & Then
        mockMvc.perform(post("/api/members/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("너나위"))
                .andExpect(jsonPath("$.email").value("neona@weolbu.com"))
                .andExpect(jsonPath("$.phoneNumber").value("010-9876-5432"))
                .andExpect(jsonPath("$.memberType").value("INSTRUCTOR"));
    }

    @Test
    @DisplayName("중복 이메일 회원가입 실패 테스트")
    void registerMember_DuplicateEmail_Fail() throws Exception {
        // Given - 이미 등록된 회원
        MemberRequestDto existingMember = new MemberRequestDto(
            "기존회원",
            "existing@weolbu.com",
            "010-1111-1111",
            "Test123",
            MemberType.STUDENT
        );
        
        mockMvc.perform(post("/api/members/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(existingMember)))
                .andExpect(status().isCreated());

        // When - 같은 이메일로 새 회원가입 시도
        MemberRequestDto newMember = new MemberRequestDto(
            "새회원",
            "existing@weolbu.com", // 같은 이메일
            "010-2222-2222",
            "Test456",
            MemberType.STUDENT
        );

        // Then
        mockMvc.perform(post("/api/members/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newMember)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("회원 가입 실패"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다: existing@weolbu.com"));
    }

    @Test
    @DisplayName("중복 휴대폰 번호 회원가입 실패 테스트")
    void registerMember_DuplicatePhoneNumber_Fail() throws Exception {
        // Given - 이미 등록된 회원
        MemberRequestDto existingMember = new MemberRequestDto(
            "기존회원",
            "existing@weolbu.com",
            "010-1111-1111",
            "Test123",
            MemberType.STUDENT
        );
        
        mockMvc.perform(post("/api/members/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(existingMember)))
                .andExpect(status().isCreated());

        // When - 같은 휴대폰 번호로 새 회원가입 시도
        MemberRequestDto newMember = new MemberRequestDto(
            "새회원",
            "new@weolbu.com",
            "010-1111-1111", // 같은 휴대폰 번호
            "Test456",
            MemberType.STUDENT
        );

        // Then
        mockMvc.perform(post("/api/members/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newMember)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("회원 가입 실패"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 휴대폰 번호입니다: 010-1111-1111"));
    }

    @Test
    @DisplayName("잘못된 비밀번호 형식 회원가입 실패 테스트")
    void registerMember_InvalidPassword_Fail() throws Exception {
        // Given
        MemberRequestDto requestDto = new MemberRequestDto(
            "홍길동",
            "hong@weolbu.com",
            "010-1234-5678",
            "123", // 너무 짧은 비밀번호
            MemberType.STUDENT
        );

        // When & Then
        mockMvc.perform(post("/api/members/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("유효성 검증 실패"))
                .andExpect(jsonPath("$.message").value("입력값을 확인해주세요"))
                .andExpect(jsonPath("$.details.password").value("비밀번호는 6자 이상 10자 이하여야 합니다"));
    }

    @Test
    @DisplayName("잘못된 이메일 형식 회원가입 실패 테스트")
    void registerMember_InvalidEmail_Fail() throws Exception {
        // Given
        MemberRequestDto requestDto = new MemberRequestDto(
            "홍길동",
            "invalid-email", // 잘못된 이메일 형식
            "010-1234-5678",
            "Test123",
            MemberType.STUDENT
        );

        // When & Then
        mockMvc.perform(post("/api/members/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("유효성 검증 실패"))
                .andExpect(jsonPath("$.message").value("입력값을 확인해주세요"))
                .andExpect(jsonPath("$.details.email").value("올바른 이메일 형식이 아닙니다"));
    }

    @Test
    @DisplayName("잘못된 휴대폰 번호 형식 회원가입 실패 테스트")
    void registerMember_InvalidPhoneNumber_Fail() throws Exception {
        // Given
        MemberRequestDto requestDto = new MemberRequestDto(
            "홍길동",
            "hong@weolbu.com",
            "010-1234-567", // 잘못된 휴대폰 번호 형식
            "Test123",
            MemberType.STUDENT
        );

        // When & Then
        mockMvc.perform(post("/api/members/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("유효성 검증 실패"))
                .andExpect(jsonPath("$.message").value("입력값을 확인해주세요"))
                .andExpect(jsonPath("$.details.phoneNumber").value("휴대폰 번호는 010-XXXX-XXXX 형식이어야 합니다"));
    }

    @Test
    @DisplayName("ID로 회원 조회 성공 테스트")
    void getMemberById_Success() throws Exception {
        // Given - 회원 등록
        MemberRequestDto requestDto = new MemberRequestDto(
            "홍길동",
            "hong@weolbu.com",
            "010-1234-5678",
            "Test123",
            MemberType.STUDENT
        );

        String response = mockMvc.perform(post("/api/members/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 회원 ID 추출
        String memberId = objectMapper.readTree(response).get("id").asText();

        // When & Then - ID로 회원 조회
        mockMvc.perform(get("/api/members/" + memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(memberId))
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.email").value("hong@weolbu.com"))
                .andExpect(jsonPath("$.phoneNumber").value("010-1234-5678"))
                .andExpect(jsonPath("$.memberType").value("STUDENT"));
    }

    @Test
    @DisplayName("이메일로 회원 조회 성공 테스트")
    void getMemberByEmail_Success() throws Exception {
        // Given - 회원 등록
        MemberRequestDto requestDto = new MemberRequestDto(
            "홍길동",
            "hong@weolbu.com",
            "010-1234-5678",
            "Test123",
            MemberType.STUDENT
        );

        mockMvc.perform(post("/api/members/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated());

        // When & Then - 이메일로 회원 조회
        mockMvc.perform(get("/api/members/email/hong@weolbu.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.email").value("hong@weolbu.com"))
                .andExpect(jsonPath("$.phoneNumber").value("010-1234-5678"))
                .andExpect(jsonPath("$.memberType").value("STUDENT"));
    }

    @Test
    @DisplayName("존재하지 않는 회원 조회 실패 테스트")
    void getMemberById_NotFound_Fail() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/members/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("회원을 찾을 수 없습니다"))
                .andExpect(jsonPath("$.message").value("해당 ID의 회원이 존재하지 않습니다."));
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 회원 조회 실패 테스트")
    void getMemberByEmail_NotFound_Fail() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/members/email/notfound@weolbu.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("회원을 찾을 수 없습니다"))
                .andExpect(jsonPath("$.message").value("해당 이메일의 회원이 존재하지 않습니다."));
    }
}
