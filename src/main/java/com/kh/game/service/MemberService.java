package com.kh.game.service;

import com.kh.game.entity.Member;
import com.kh.game.entity.MemberLoginHistory;
import com.kh.game.repository.MemberLoginHistoryRepository;
import com.kh.game.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberLoginHistoryRepository loginHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    // ========== 회원 관리 ==========

    @Transactional
    public Member register(String email, String password, String nickname, String username) {
        // 이메일 중복 체크
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 닉네임 중복 시 번호 자동 추가
        String finalNickname = generateUniqueNickname(nickname);

        Member member = new Member();
        member.setEmail(email);
        member.setPassword(passwordEncoder.encode(password));
        member.setNickname(finalNickname);
        member.setUsername(username);
        member.setRole(Member.MemberRole.USER);
        member.setStatus(Member.MemberStatus.ACTIVE);

        return memberRepository.save(member);
    }

    /**
     * 고유 닉네임 생성 (중복 시 번호 추가)
     * 예: 홍길동 → 홍길동, 홍길동2, 홍길동3 ...
     */
    private String generateUniqueNickname(String nickname) {
        // 원본 닉네임이 사용 가능하면 그대로 반환
        if (!memberRepository.existsByNickname(nickname)) {
            return nickname;
        }

        // 같은 닉네임으로 시작하는 모든 닉네임 조회
        List<String> existingNicknames = memberRepository.findNicknamesStartingWith(nickname);

        // 가장 큰 번호 찾기
        int maxNumber = 1;
        for (String existing : existingNicknames) {
            if (existing.equals(nickname)) {
                continue;  // 원본 닉네임은 스킵
            }

            // 닉네임 뒤의 숫자 추출 (예: "홍길동3" → 3)
            String suffix = existing.substring(nickname.length());
            if (!suffix.isEmpty()) {
                try {
                    int num = Integer.parseInt(suffix);
                    maxNumber = Math.max(maxNumber, num);
                } catch (NumberFormatException ignored) {
                    // 숫자가 아닌 경우 무시
                }
            }
        }

        return nickname + (maxNumber + 1);
    }

    public Optional<Member> findById(Long id) {
        return memberRepository.findById(id);
    }

    public Optional<Member> findByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    public Page<Member> findAll(Pageable pageable) {
        return memberRepository.findAll(pageable);
    }

    public Page<Member> search(String keyword, Pageable pageable) {
        return memberRepository.findByEmailContainingOrNicknameContaining(keyword, keyword, pageable);
    }

    @Transactional
    public void updateNickname(Long memberId, String nickname) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        if (memberRepository.existsByNickname(nickname) && !member.getNickname().equals(nickname)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }
        member.setNickname(nickname);
    }

    @Transactional
    public void updatePassword(Long memberId, String currentPassword, String newPassword) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
        member.setPassword(passwordEncoder.encode(newPassword));
    }

    @Transactional
    public void updateStatus(Long memberId, Member.MemberStatus status) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        member.setStatus(status);
    }

    // ========== 로그인 ==========

    @Transactional
    public Member login(String email, String password, String ipAddress, String userAgent) {
        Optional<Member> memberOpt = memberRepository.findByEmail(email);

        if (memberOpt.isEmpty()) {
            // 회원 없음
            loginHistoryRepository.save(MemberLoginHistory.fail(
                    email, MemberLoginHistory.LoginResult.FAIL_NOT_FOUND,
                    "존재하지 않는 이메일", ipAddress, userAgent));
            throw new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        Member member = memberOpt.get();

        // 상태 체크
        if (member.getStatus() == Member.MemberStatus.BANNED) {
            loginHistoryRepository.save(MemberLoginHistory.fail(
                    email, MemberLoginHistory.LoginResult.FAIL_BANNED,
                    "정지된 계정", ipAddress, userAgent));
            throw new IllegalArgumentException("정지된 계정입니다.");
        }

        if (member.getStatus() == Member.MemberStatus.INACTIVE) {
            loginHistoryRepository.save(MemberLoginHistory.fail(
                    email, MemberLoginHistory.LoginResult.FAIL_INACTIVE,
                    "비활성 계정", ipAddress, userAgent));
            throw new IllegalArgumentException("비활성 계정입니다.");
        }

        // 비밀번호 체크
        if (!passwordEncoder.matches(password, member.getPassword())) {
            loginHistoryRepository.save(MemberLoginHistory.fail(
                    email, MemberLoginHistory.LoginResult.FAIL_PASSWORD,
                    "비밀번호 불일치", ipAddress, userAgent));
            throw new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        // 로그인 성공
        member.setLastLoginAt(LocalDateTime.now());
        memberRepository.save(member);

        loginHistoryRepository.save(MemberLoginHistory.success(member, ipAddress, userAgent));

        return member;
    }

    // ========== 게임 결과 ==========

    @Transactional
    public void addGameResult(Long memberId, int score, int correct, int rounds, int skip) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        member.addGameResult(score, correct, rounds, skip);
        memberRepository.save(member);
    }

    // ========== 랭킹 ==========

    public List<Member> getTopRankingByScore(int limit) {
        return memberRepository.findTopByTotalScore(PageRequest.of(0, limit));
    }

    public List<Member> getTopRankingByAccuracy(int limit) {
        return memberRepository.findTopByAccuracy(PageRequest.of(0, limit));
    }

    public List<Member> getTopRankingByGames(int limit) {
        return memberRepository.findTopByTotalGames(PageRequest.of(0, limit));
    }

    // ========== 로그인 이력 ==========

    public Page<MemberLoginHistory> getLoginHistory(Pageable pageable) {
        return loginHistoryRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Page<MemberLoginHistory> getLoginHistoryByEmail(String email, Pageable pageable) {
        return loginHistoryRepository.findByEmailContainingOrderByCreatedAtDesc(email, pageable);
    }

    public Page<MemberLoginHistory> getLoginHistoryByResult(MemberLoginHistory.LoginResult result, Pageable pageable) {
        return loginHistoryRepository.findByResultOrderByCreatedAtDesc(result, pageable);
    }

    public Page<MemberLoginHistory> getFailedLogins(Pageable pageable) {
        return loginHistoryRepository.findFailedLogins(pageable);
    }

    public Page<MemberLoginHistory> getLoginHistoryByPeriod(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return loginHistoryRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end, pageable);
    }
}