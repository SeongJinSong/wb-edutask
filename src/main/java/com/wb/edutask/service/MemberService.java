package com.wb.edutask.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.wb.edutask.dto.MemberRequestDto;
import com.wb.edutask.dto.MemberResponseDto;
import com.wb.edutask.entity.Member;
import com.wb.edutask.enums.MemberType;
import com.wb.edutask.repository.MemberRepository;
import lombok.RequiredArgsConstructor;

/**
 * 회원 관리를 위한 서비스 클래스
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {
    
    private final MemberRepository memberRepository;
    
    /**
     * 모든 회원 목록을 페이징으로 조회합니다
     * 
     * @param pageable 페이징 정보
     * @return 회원 목록 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<MemberResponseDto> getAllMembers(Pageable pageable) {
        return memberRepository.findAll(pageable)
                .map(this::convertToResponseDto);
    }
    
    /**
     * 모든 회원 목록을 조회합니다 (페이징 없음)
     * 
     * @return 회원 목록
     */
    @Transactional(readOnly = true)
    public List<MemberResponseDto> getAllMembers() {
        return memberRepository.findAll()
                .stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 회원을 검색합니다 (이름, 이메일, 회원 유형으로 검색)
     * 
     * @param search 검색어 (이름, 이메일)
     * @param memberType 회원 유형
     * @param pageable 페이징 정보
     * @return 검색된 회원 목록 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<MemberResponseDto> searchMembers(String search, String memberType, Pageable pageable) {
        Specification<Member> spec = Specification.where(null);
        
        // 검색어가 있으면 이름 또는 이메일로 검색
        if (StringUtils.hasText(search)) {
            spec = spec.and((root, query, criteriaBuilder) -> 
                criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + search.toLowerCase() + "%"),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), "%" + search.toLowerCase() + "%")
                )
            );
        }
        
        // 회원 유형 필터
        if (StringUtils.hasText(memberType) && !memberType.equals("all")) {
            try {
                MemberType type = MemberType.valueOf(memberType.toUpperCase());
                spec = spec.and((root, query, criteriaBuilder) -> 
                    criteriaBuilder.equal(root.get("memberType"), type)
                );
            } catch (IllegalArgumentException e) {
                // 잘못된 회원 유형은 무시
            }
        }
        
        return memberRepository.findAll(spec, pageable)
                .map(this::convertToResponseDto);
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
