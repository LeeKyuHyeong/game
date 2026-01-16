package com.kh.game.service;

import com.kh.game.entity.GameRoom;
import com.kh.game.entity.GameRoomParticipant;
import com.kh.game.entity.Member;
import com.kh.game.entity.MemberLoginHistory;
import com.kh.game.repository.GameRoomParticipantRepository;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberLoginHistoryRepository loginHistoryRepository;
    private final GameRoomParticipantRepository participantRepository;
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

    public Member save(Member member) {
        return memberRepository.save(member);
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

    public Page<Member> findByStatus(Member.MemberStatus status, Pageable pageable) {
        return memberRepository.findByStatus(status, pageable);
    }

    public Page<Member> findByRole(Member.MemberRole role, Pageable pageable) {
        return memberRepository.findByRole(role, pageable);
    }

    public long count() {
        return memberRepository.count();
    }

    public long countByStatus(Member.MemberStatus status) {
        return memberRepository.countByStatus(status);
    }

    public long countByRole(Member.MemberRole role) {
        return memberRepository.countByRole(role);
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

    @Transactional
    public void updateRole(Long memberId, Member.MemberRole role) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        member.setRole(role);
    }

    @Transactional
    public void resetWeeklyStats(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        member.resetWeeklyStats();
    }

    @Transactional
    public String resetPasswordToDefault(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        String tempPassword = "temp" + System.currentTimeMillis() % 10000;
        member.setPassword(passwordEncoder.encode(tempPassword));
        return tempPassword;
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

    // Solo Guess (내가맞추기) 게임 결과 반영
    // isEligibleForBestScore: 최고기록 랭킹 대상 여부 (전체랜덤 + 필터없음)
    @Transactional
    public void addGuessGameResult(Long memberId, int score, int correct, int rounds, int skip, boolean isEligibleForBestScore) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        member.addGuessGameResult(score, correct, rounds, skip, isEligibleForBestScore);
        memberRepository.save(member);
    }

    // Multiplayer (멀티게임) 게임 결과 반영
    @Transactional
    public void addMultiGameResult(Long memberId, int score, int correct, int rounds) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        member.addMultiGameResult(score, correct, rounds);
        memberRepository.save(member);
    }

    // ========== 랭킹 (전체 - 기존 호환성 유지) ==========

    public List<Member> getTopRankingByScore(int limit) {
        return memberRepository.findTopByTotalScore(PageRequest.of(0, limit));
    }

    public List<Member> getTopRankingByAccuracy(int limit) {
        return memberRepository.findTopByAccuracy(PageRequest.of(0, limit));
    }

    public List<Member> getTopRankingByGames(int limit) {
        return memberRepository.findTopByTotalGames(PageRequest.of(0, limit));
    }

    // ========== Solo Guess (내가맞추기) 랭킹 ==========

    // 1. 누적 총점
    public List<Member> getGuessRankingByScore(int limit) {
        return memberRepository.findTopGuessRankingByScore(PageRequest.of(0, limit));
    }

    // 2. 평균 정답률
    public List<Member> getGuessRankingByAccuracy(int limit) {
        return memberRepository.findTopGuessRankingByAccuracy(PageRequest.of(0, limit));
    }

    // 3. 평균 점수
    public List<Member> getGuessRankingByAvgScore(int limit) {
        return memberRepository.findTopGuessRankingByAvgScore(PageRequest.of(0, limit));
    }

    // 4. 최다 정답
    public List<Member> getGuessRankingByCorrect(int limit) {
        return memberRepository.findTopGuessRankingByCorrect(PageRequest.of(0, limit));
    }

    // 5. 플레이왕 (게임 수)
    public List<Member> getGuessRankingByGames(int limit) {
        return memberRepository.findTopGuessRankingByGames(PageRequest.of(0, limit));
    }

    // 6. 도전왕 (라운드 수)
    public List<Member> getGuessRankingByRounds(int limit) {
        return memberRepository.findTopGuessRankingByRounds(PageRequest.of(0, limit));
    }

    // 7. 라운드별 평균점수 (10게임 이상)
    public List<Member> getGuessRankingByAvgScorePerRound(int limit) {
        return memberRepository.findTopGuessRankingByAvgScorePerRound(PageRequest.of(0, limit));
    }

    // 8. 정답률 (10게임 이상)
    public List<Member> getGuessRankingByAccuracyMin10(int limit) {
        return memberRepository.findTopGuessRankingByAccuracyMin10(PageRequest.of(0, limit));
    }

    // 내 순위 조회
    public long getMyGuessRank(int score) {
        return memberRepository.countMembersWithHigherGuessScore(score) + 1;
    }

    public long getGuessParticipantCount() {
        return memberRepository.countGuessParticipants();
    }

    // ========== Multiplayer (멀티게임) 랭킹 ==========

    public List<Member> getMultiRankingByScore(int limit) {
        return memberRepository.findTopMultiRankingByScore(PageRequest.of(0, limit));
    }

    public List<Member> getMultiRankingByAccuracy(int limit) {
        return memberRepository.findTopMultiRankingByAccuracy(PageRequest.of(0, limit));
    }

    public List<Member> getMultiRankingByGames(int limit) {
        return memberRepository.findTopMultiRankingByGames(PageRequest.of(0, limit));
    }

    // ========== 주간 랭킹 (Weekly) ==========

    public List<Member> getWeeklyGuessRankingByScore(int limit) {
        return memberRepository.findTopWeeklyGuessRankingByScore(PageRequest.of(0, limit));
    }

    public List<Member> getWeeklyMultiRankingByScore(int limit) {
        return memberRepository.findTopWeeklyMultiRankingByScore(PageRequest.of(0, limit));
    }

    // ========== 최고 기록 랭킹 (Best Score) ==========

    public List<Member> getGuessBestScoreRanking(int limit) {
        return memberRepository.findTopGuessBestScore(PageRequest.of(0, limit));
    }

    public List<Member> getMultiBestScoreRanking(int limit) {
        return memberRepository.findTopMultiBestScore(PageRequest.of(0, limit));
    }

    // ========== 30곡 최고점 랭킹 ==========

    // 주간 30곡 랭킹
    public List<Member> getWeeklyBest30Ranking(int limit) {
        return memberRepository.findWeeklyBest30Ranking(PageRequest.of(0, limit));
    }

    // 월간 30곡 랭킹
    public List<Member> getMonthlyBest30Ranking(int limit) {
        return memberRepository.findMonthlyBest30Ranking(PageRequest.of(0, limit));
    }

    // 역대 30곡 랭킹 (명예의 전당)
    public List<Member> getAllTimeBest30Ranking(int limit) {
        return memberRepository.findAllTimeBest30Ranking(PageRequest.of(0, limit));
    }

    // 내 주간 30곡 순위
    public long getMyWeeklyBest30Rank(int score) {
        return memberRepository.countMembersWithHigherWeeklyBest30Score(score) + 1;
    }

    // 내 월간 30곡 순위
    public long getMyMonthlyBest30Rank(int score) {
        return memberRepository.countMembersWithHigherMonthlyBest30Score(score) + 1;
    }

    // 내 역대 30곡 순위
    public long getMyAllTimeBest30Rank(int score) {
        return memberRepository.countMembersWithHigherAllTimeBest30Score(score) + 1;
    }

    // 30곡 참여자 수
    public long getWeeklyBest30ParticipantCount() {
        return memberRepository.countWeeklyBest30Participants();
    }

    public long getMonthlyBest30ParticipantCount() {
        return memberRepository.countMonthlyBest30Participants();
    }

    public long getAllTimeBest30ParticipantCount() {
        return memberRepository.countAllTimeBest30Participants();
    }

    // 30곡 게임 완료 시 최고점 갱신
    @Transactional
    public boolean update30SongBestScore(Long memberId, int score) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        boolean updated = member.update30SongBestScore(score);
        if (updated) {
            memberRepository.save(member);
        }
        return updated;
    }

    // 월간 통계 리셋
    @Transactional
    public void resetMonthlyStats(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        member.resetMonthlyStats();
    }

    // ========== Retro Game (레트로) 랭킹 ==========

    // 레트로 게임 결과 반영
    @Transactional
    public void addRetroGameResult(Long memberId, int score, int correct, int rounds, int skip, boolean isEligibleForBestScore) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        member.addRetroGameResult(score, correct, rounds, skip, isEligibleForBestScore);
        memberRepository.save(member);
    }

    // 레트로 30곡 최고점 갱신
    @Transactional
    public boolean updateRetro30SongBestScore(Long memberId, int score) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        boolean updated = member.updateRetro30SongBestScore(score);
        if (updated) {
            memberRepository.save(member);
        }
        return updated;
    }

    // 1. 누적 총점
    public List<Member> getRetroRankingByScore(int limit) {
        return memberRepository.findTopRetroRankingByScore(PageRequest.of(0, limit));
    }

    // 2. 정답률
    public List<Member> getRetroRankingByAccuracy(int limit) {
        return memberRepository.findTopRetroRankingByAccuracy(PageRequest.of(0, limit));
    }

    // 3. 게임 수
    public List<Member> getRetroRankingByGames(int limit) {
        return memberRepository.findTopRetroRankingByGames(PageRequest.of(0, limit));
    }

    // 4. 주간 레트로 총점
    public List<Member> getWeeklyRetroRankingByScore(int limit) {
        return memberRepository.findTopWeeklyRetroRankingByScore(PageRequest.of(0, limit));
    }

    // 5. 레트로 30곡 최고점 (역대)
    public List<Member> getRetroBest30Ranking(int limit) {
        return memberRepository.findRetroBest30Ranking(PageRequest.of(0, limit));
    }

    // 6. 레트로 30곡 주간 최고점
    public List<Member> getWeeklyRetroBest30Ranking(int limit) {
        return memberRepository.findWeeklyRetroBest30Ranking(PageRequest.of(0, limit));
    }

    // 내 레트로 순위 조회
    public long getMyRetroRank(int score) {
        return memberRepository.countMembersWithHigherRetroScore(score) + 1;
    }

    public long getRetroParticipantCount() {
        return memberRepository.countRetroParticipants();
    }

    // 내 레트로 30곡 순위
    public long getMyRetroBest30Rank(int score) {
        return memberRepository.countMembersWithHigherRetroBest30Score(score) + 1;
    }

    public long getRetroBest30ParticipantCount() {
        return memberRepository.countRetroBest30Participants();
    }

    // ========== 멀티게임 LP 티어 랭킹 ==========

    public List<Member> getMultiTierRanking(int limit) {
        return memberRepository.findTopMultiTierRanking(PageRequest.of(0, limit));
    }

    public List<Member> getMultiWinsRanking(int limit) {
        return memberRepository.findTopMultiWins(PageRequest.of(0, limit));
    }

    public List<Member> getMultiTop3Ranking(int limit) {
        return memberRepository.findTopMultiTop3(PageRequest.of(0, limit));
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

    // ========== 세션 관리 (중복 로그인 방지) ==========

    /**
     * 회원이 현재 게임 중인지 확인
     */
    public boolean isInGame(Long memberId) {
        Optional<Member> memberOpt = memberRepository.findById(memberId);
        if (memberOpt.isEmpty()) {
            return false;
        }

        Optional<GameRoomParticipant> participation = participantRepository.findActiveParticipation(memberOpt.get());
        if (participation.isEmpty()) {
            return false;
        }

        // 방이 게임 중인 상태인지 확인
        GameRoom room = participation.get().getGameRoom();
        return room.getStatus() == GameRoom.RoomStatus.PLAYING;
    }

    /**
     * 기존 세션 존재 여부 확인
     */
    public boolean hasActiveSession(Long memberId) {
        Optional<Member> memberOpt = memberRepository.findById(memberId);
        if (memberOpt.isEmpty()) {
            return false;
        }
        Member member = memberOpt.get();
        return member.getSessionToken() != null && member.getSessionCreatedAt() != null;
    }

    /**
     * 새 세션 토큰 생성 및 저장
     */
    @Transactional
    public String createSessionToken(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        String token = UUID.randomUUID().toString().replace("-", "");
        member.setSessionToken(token);
        member.setSessionCreatedAt(LocalDateTime.now());
        memberRepository.save(member);

        return token;
    }

    /**
     * 세션 토큰 유효성 검증
     */
    public boolean validateSessionToken(Long memberId, String token) {
        if (token == null || memberId == null) {
            return false;
        }

        Optional<Member> memberOpt = memberRepository.findById(memberId);
        if (memberOpt.isEmpty()) {
            return false;
        }

        Member member = memberOpt.get();
        return token.equals(member.getSessionToken());
    }

    /**
     * 세션 토큰 무효화 (로그아웃)
     */
    @Transactional
    public void invalidateSessionToken(Long memberId) {
        memberRepository.findById(memberId).ifPresent(member -> {
            member.setSessionToken(null);
            member.setSessionCreatedAt(null);
            memberRepository.save(member);
        });
    }

    /**
     * 로그인 시도 상태 확인 (중복 로그인 체크)
     * @return LoginAttemptResult
     */
    public LoginAttemptResult checkLoginAttempt(String email) {
        Optional<Member> memberOpt = memberRepository.findByEmail(email);
        if (memberOpt.isEmpty()) {
            return new LoginAttemptResult(LoginAttemptStatus.NO_EXISTING_SESSION, false);
        }

        Member member = memberOpt.get();

        // 게임 중인지 먼저 확인 (세션 토큰 유무와 관계없이)
        boolean inGame = isInGame(member.getId());
        if (inGame) {
            return new LoginAttemptResult(LoginAttemptStatus.IN_GAME, true);
        }

        // 기존 세션이 없으면 바로 로그인 가능
        if (member.getSessionToken() == null) {
            return new LoginAttemptResult(LoginAttemptStatus.NO_EXISTING_SESSION, false);
        }

        // 기존 세션이 있지만 게임 중 아님
        return new LoginAttemptResult(LoginAttemptStatus.EXISTING_SESSION, true);
    }

    public enum LoginAttemptStatus {
        NO_EXISTING_SESSION,  // 기존 세션 없음 → 바로 로그인
        EXISTING_SESSION,     // 기존 세션 있음 (게임 중 아님) → 바로 로그인 (기존 세션 종료)
        IN_GAME              // 게임 중 → 확인 필요
    }

    public record LoginAttemptResult(LoginAttemptStatus status, boolean hasExistingSession) {}
}
