package com.wb.edutask.controller;

import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.wb.edutask.dto.MemberRequestDto;
import com.wb.edutask.dto.MemberResponseDto;
import com.wb.edutask.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 회원 관리를 위한 REST API 컨트롤러
 * API 전용 컨트롤러로 JSON 응답만 처리
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@RestController
@RequestMapping("/api/v1/members")
@CrossOrigin(origins = "http://localhost:8080") // CORS 설정 (개발용)
@RequiredArgsConstructor
public class MemberApiController {
    
    private final MemberService memberService;
    
    /**
     * 모든 회원 목록 조회 API (페이징 + 검색)
     * 
     * @param pageable 페이징 정보 (기본 20개씩)
     * @param search 검색어 (이름, 이메일 검색)
     * @param memberType 회원 유형 필터
     * @return 회원 목록 (페이징)
     */
    @GetMapping
    public ResponseEntity<?> getAllMembers(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String memberType) {
        try {
            if (StringUtils.hasText(search) || StringUtils.hasText(memberType)) {
                return ResponseEntity.ok(memberService.searchMembers(search, memberType, pageable));
            } else {
                return ResponseEntity.ok(memberService.getAllMembers(pageable));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("서버 오류", "회원 목록 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 회원 가입 API
     * 
     * @param memberRequestDto 회원 가입 요청 정보
     * @return 등록된 회원 정보
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerMember(@Valid @RequestBody MemberRequestDto memberRequestDto) {
        try {
            MemberResponseDto responseDto = memberService.registerMember(memberRequestDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("회원 가입 실패", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("서버 오류", "회원 가입 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 이메일로 회원 조회 API
     * 
     * @param email 조회할 이메일
     * @return 회원 정보
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<?> getMemberByEmail(@PathVariable String email) {
        try {
            Optional<MemberResponseDto> member = memberService.findMemberByEmail(email);
            if (member.isPresent()) {
                return ResponseEntity.ok(member.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("회원을 찾을 수 없습니다", "해당 이메일의 회원이 존재하지 않습니다."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("서버 오류", "회원 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * ID로 회원 조회 API
     * 
     * @param id 조회할 회원 ID
     * @return 회원 정보
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getMemberById(@PathVariable("id") Long id) {
        try {
            Optional<MemberResponseDto> member = memberService.findMemberById(id);
            if (member.isPresent()) {
                return ResponseEntity.ok(member.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("회원을 찾을 수 없습니다", "해당 ID의 회원이 존재하지 않습니다."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("서버 오류", "회원 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 에러 응답을 위한 내부 클래스
     */
    public static class ErrorResponse {
        private String error;
        private String message;
        
        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
        
        public String getError() {
            return error;
        }
        
        public void setError(String error) {
            this.error = error;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
}
