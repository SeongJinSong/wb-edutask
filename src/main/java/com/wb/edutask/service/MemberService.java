package com.wb.edutask.service;

import com.wb.edutask.dto.MemberRequestDto;
import com.wb.edutask.dto.MemberResponseDto;
import com.wb.edutask.entity.Member;
import com.wb.edutask.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 회원 관리를 위한 서비스 클래스
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@Service
@Transactional
public class MemberService {
    
    private final MemberRepository memberRepository;
    
    @Autowired
    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }
    
    /**
     * 회원을 등록합니다
     * 
     * @param memberRequestDto 회원 가입 요청 정보
     * @return 등록된 회원 정보
     * @throws IllegalArgumentException 중복된 이메일이나 휴대폰 번호가 있는 경우
     */
    public MemberResponseDto registerMember(MemberRequestDto memberRequestDto) {
        // 이메일 중복 확인
        if (memberRepository.existsByEmail(memberRequestDto.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다: " + memberRequestDto.getEmail());
        }
        
        // 휴대폰 번호 중복 확인
        if (memberRepository.existsByPhoneNumber(memberRequestDto.getPhoneNumber())) {
            throw new IllegalArgumentException("이미 사용 중인 휴대폰 번호입니다: " + memberRequestDto.getPhoneNumber());
        }
        
        // 회원 엔티티 생성
        Member member = new Member(
            memberRequestDto.getName(),
            memberRequestDto.getEmail(),
            memberRequestDto.getPhoneNumber(),
            memberRequestDto.getPassword(), // 실제 프로덕션에서는 암호화 필요
            memberRequestDto.getMemberType()
        );
        
        // 회원 저장
        Member savedMember = memberRepository.save(member);
        
        // 응답 DTO 생성 및 반환
        return convertToResponseDto(savedMember);
    }
    
    /**
     * 이메일로 회원을 조회합니다
     * 
     * @param email 조회할 이메일
     * @return 회원 정보 (Optional)
     */
    @Transactional(readOnly = true)
    public Optional<MemberResponseDto> findMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .map(this::convertToResponseDto);
    }
    
    /**
     * ID로 회원을 조회합니다
     * 
     * @param id 조회할 회원 ID
     * @return 회원 정보 (Optional)
     */
    @Transactional(readOnly = true)
    public Optional<MemberResponseDto> findMemberById(Long id) {
        return memberRepository.findById(id)
                .map(this::convertToResponseDto);
    }
    
    /**
     * 회원 엔티티를 응답 DTO로 변환합니다
     * 
     * @param member 변환할 회원 엔티티
     * @return 회원 응답 DTO
     */
    private MemberResponseDto convertToResponseDto(Member member) {
        return new MemberResponseDto(
            member.getId(),
            member.getName(),
            member.getEmail(),
            member.getPhoneNumber(),
            member.getMemberType(),
            member.getCreatedAt(),
            member.getUpdatedAt()
        );
    }
}
