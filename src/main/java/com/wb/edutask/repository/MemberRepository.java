package com.wb.edutask.repository;

import com.wb.edutask.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 회원 데이터 접근을 위한 Repository 인터페이스
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    
    /**
     * 이메일로 회원을 조회합니다
     * 
     * @param email 조회할 이메일
     * @return 회원 정보 (Optional)
     */
    Optional<Member> findByEmail(String email);
    
    /**
     * 휴대폰 번호로 회원을 조회합니다
     * 
     * @param phoneNumber 조회할 휴대폰 번호
     * @return 회원 정보 (Optional)
     */
    Optional<Member> findByPhoneNumber(String phoneNumber);
    
    /**
     * 이메일이 이미 존재하는지 확인합니다
     * 
     * @param email 확인할 이메일
     * @return 존재 여부
     */
    boolean existsByEmail(String email);
    
    /**
     * 휴대폰 번호가 이미 존재하는지 확인합니다
     * 
     * @param phoneNumber 확인할 휴대폰 번호
     * @return 존재 여부
     */
    boolean existsByPhoneNumber(String phoneNumber);
}
