package com.wb.edutask.service;

import com.wb.edutask.dto.MemberRequestDto;
import com.wb.edutask.dto.MemberResponseDto;
import com.wb.edutask.entity.Member;
import com.wb.edutask.entity.MemberType;
import com.wb.edutask.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * 회원 서비스 테스트 클래스
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 데이터베이스 초기화
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("회원 등록 성공 테스트")
    void registerMember_Success() {
        // Given
        MemberRequestDto requestDto = new MemberRequestDto(
            "홍길동",
            "hong@weolbu.com",
            "010-1234-5678",
            "Test123",
            MemberType.STUDENT
        );

        // When
        MemberResponseDto responseDto = memberService.registerMember(requestDto);

        // Then
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getId()).isNotNull();
        assertThat(responseDto.getName()).isEqualTo("홍길동");
        assertThat(responseDto.getEmail()).isEqualTo("hong@weolbu.com");
        assertThat(responseDto.getPhoneNumber()).isEqualTo("010-1234-5678");
        assertThat(responseDto.getMemberType()).isEqualTo(MemberType.STUDENT);
        assertThat(responseDto.getCreatedAt()).isNotNull();
        assertThat(responseDto.getUpdatedAt()).isNotNull();

        // 데이터베이스에 실제로 저장되었는지 확인
        Optional<Member> savedMember = memberRepository.findById(responseDto.getId());
        assertThat(savedMember).isPresent();
        assertThat(savedMember.get().getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("중복 이메일 회원 등록 실패 테스트")
    void registerMember_DuplicateEmail_Fail() {
        // Given - 이미 등록된 회원
        Member existingMember = new Member(
            "기존회원",
            "existing@weolbu.com",
            "010-1111-1111",
            "Test123",
            MemberType.STUDENT
        );
        memberRepository.save(existingMember);

        // When - 같은 이메일로 새 회원 등록 시도
        MemberRequestDto newMemberRequest = new MemberRequestDto(
            "새회원",
            "existing@weolbu.com", // 같은 이메일
            "010-2222-2222",
            "Test456",
            MemberType.STUDENT
        );

        // Then
        assertThatThrownBy(() -> memberService.registerMember(newMemberRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 사용 중인 이메일입니다: existing@weolbu.com");
    }

    @Test
    @DisplayName("중복 휴대폰 번호 회원 등록 실패 테스트")
    void registerMember_DuplicatePhoneNumber_Fail() {
        // Given - 이미 등록된 회원
        Member existingMember = new Member(
            "기존회원",
            "existing@weolbu.com",
            "010-1111-1111",
            "Test123",
            MemberType.STUDENT
        );
        memberRepository.save(existingMember);

        // When - 같은 휴대폰 번호로 새 회원 등록 시도
        MemberRequestDto newMemberRequest = new MemberRequestDto(
            "새회원",
            "new@weolbu.com",
            "010-1111-1111", // 같은 휴대폰 번호
            "Test456",
            MemberType.STUDENT
        );

        // Then
        assertThatThrownBy(() -> memberService.registerMember(newMemberRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 사용 중인 휴대폰 번호입니다: 010-1111-1111");
    }

    @Test
    @DisplayName("이메일로 회원 조회 성공 테스트")
    void findMemberByEmail_Success() {
        // Given - 회원 등록
        Member member = new Member(
            "홍길동",
            "hong@weolbu.com",
            "010-1234-5678",
            "Test123",
            MemberType.STUDENT
        );
        Member savedMember = memberRepository.save(member);

        // When
        Optional<MemberResponseDto> result = memberService.findMemberByEmail("hong@weolbu.com");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(savedMember.getId());
        assertThat(result.get().getName()).isEqualTo("홍길동");
        assertThat(result.get().getEmail()).isEqualTo("hong@weolbu.com");
        assertThat(result.get().getPhoneNumber()).isEqualTo("010-1234-5678");
        assertThat(result.get().getMemberType()).isEqualTo(MemberType.STUDENT);
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 회원 조회 실패 테스트")
    void findMemberByEmail_NotFound_Fail() {
        // When
        Optional<MemberResponseDto> result = memberService.findMemberByEmail("notfound@weolbu.com");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("ID로 회원 조회 성공 테스트")
    void findMemberById_Success() {
        // Given - 회원 등록
        Member member = new Member(
            "홍길동",
            "hong@weolbu.com",
            "010-1234-5678",
            "Test123",
            MemberType.STUDENT
        );
        Member savedMember = memberRepository.save(member);

        // When
        Optional<MemberResponseDto> result = memberService.findMemberById(savedMember.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(savedMember.getId());
        assertThat(result.get().getName()).isEqualTo("홍길동");
        assertThat(result.get().getEmail()).isEqualTo("hong@weolbu.com");
        assertThat(result.get().getPhoneNumber()).isEqualTo("010-1234-5678");
        assertThat(result.get().getMemberType()).isEqualTo(MemberType.STUDENT);
    }

    @Test
    @DisplayName("존재하지 않는 ID로 회원 조회 실패 테스트")
    void findMemberById_NotFound_Fail() {
        // When
        Optional<MemberResponseDto> result = memberService.findMemberById(999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("강사 회원 등록 성공 테스트")
    void registerInstructorMember_Success() {
        // Given
        MemberRequestDto requestDto = new MemberRequestDto(
            "너나위",
            "neona@weolbu.com",
            "010-9876-5432",
            "Inst123",
            MemberType.INSTRUCTOR
        );

        // When
        MemberResponseDto responseDto = memberService.registerMember(requestDto);

        // Then
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getId()).isNotNull();
        assertThat(responseDto.getName()).isEqualTo("너나위");
        assertThat(responseDto.getEmail()).isEqualTo("neona@weolbu.com");
        assertThat(responseDto.getPhoneNumber()).isEqualTo("010-9876-5432");
        assertThat(responseDto.getMemberType()).isEqualTo(MemberType.INSTRUCTOR);
    }
}
