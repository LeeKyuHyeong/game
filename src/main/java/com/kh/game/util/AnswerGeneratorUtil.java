package com.kh.game.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 노래 제목 정답 변형 생성 유틸리티
 */
public class AnswerGeneratorUtil {

    // 영어 → 한글 발음 매핑 (자주 사용되는 패턴)
    private static final Map<String, String> PHONETIC_MAP = new LinkedHashMap<>();

    static {
        // 복합 패턴 (먼저 처리)
        PHONETIC_MAP.put("tion", "션");
        PHONETIC_MAP.put("sion", "션");
        PHONETIC_MAP.put("ight", "아이트");
        PHONETIC_MAP.put("ough", "오");
        PHONETIC_MAP.put("ould", "울드");
        PHONETIC_MAP.put("tion", "션");
        PHONETIC_MAP.put("ness", "니스");
        PHONETIC_MAP.put("ment", "먼트");
        PHONETIC_MAP.put("ful", "풀");
        PHONETIC_MAP.put("less", "리스");
        PHONETIC_MAP.put("ing", "잉");
        PHONETIC_MAP.put("ck", "크");
        PHONETIC_MAP.put("ch", "치");
        PHONETIC_MAP.put("sh", "쉬");
        PHONETIC_MAP.put("th", "스");
        PHONETIC_MAP.put("ph", "프");
        PHONETIC_MAP.put("wh", "와");
        PHONETIC_MAP.put("qu", "쿠");
        PHONETIC_MAP.put("oo", "우");
        PHONETIC_MAP.put("ee", "이");
        PHONETIC_MAP.put("ea", "이");
        PHONETIC_MAP.put("ou", "아우");
        PHONETIC_MAP.put("ow", "오우");
        PHONETIC_MAP.put("ai", "에이");
        PHONETIC_MAP.put("ay", "에이");
        PHONETIC_MAP.put("ey", "이");
        PHONETIC_MAP.put("ie", "이");
        PHONETIC_MAP.put("ue", "유");
        PHONETIC_MAP.put("oa", "오");

        // 단일 자음
        PHONETIC_MAP.put("b", "브");
        PHONETIC_MAP.put("c", "크");
        PHONETIC_MAP.put("d", "드");
        PHONETIC_MAP.put("f", "프");
        PHONETIC_MAP.put("g", "그");
        PHONETIC_MAP.put("h", "흐");
        PHONETIC_MAP.put("j", "제이");
        PHONETIC_MAP.put("k", "크");
        PHONETIC_MAP.put("l", "ㄹ");
        PHONETIC_MAP.put("m", "므");
        PHONETIC_MAP.put("n", "느");
        PHONETIC_MAP.put("p", "프");
        PHONETIC_MAP.put("r", "르");
        PHONETIC_MAP.put("s", "스");
        PHONETIC_MAP.put("t", "트");
        PHONETIC_MAP.put("v", "브");
        PHONETIC_MAP.put("w", "우");
        PHONETIC_MAP.put("x", "크스");
        PHONETIC_MAP.put("z", "즈");

        // 단일 모음
        PHONETIC_MAP.put("a", "아");
        PHONETIC_MAP.put("e", "에");
        PHONETIC_MAP.put("i", "이");
        PHONETIC_MAP.put("o", "오");
        PHONETIC_MAP.put("u", "유");
        PHONETIC_MAP.put("y", "이");
    }

    // 자주 쓰이는 영어 단어 → 한글 발음 (정확한 발음)
    private static final Map<String, String> WORD_MAP = new LinkedHashMap<>();

    static {
        WORD_MAP.put("love", "러브");
        WORD_MAP.put("you", "유");
        WORD_MAP.put("your", "유어");
        WORD_MAP.put("my", "마이");
        WORD_MAP.put("me", "미");
        WORD_MAP.put("the", "더");
        WORD_MAP.put("a", "어");
        WORD_MAP.put("i", "아이");
        WORD_MAP.put("we", "위");
        WORD_MAP.put("us", "어스");
        WORD_MAP.put("be", "비");
        WORD_MAP.put("is", "이즈");
        WORD_MAP.put("are", "아");
        WORD_MAP.put("was", "워즈");
        WORD_MAP.put("were", "워");
        WORD_MAP.put("have", "해브");
        WORD_MAP.put("has", "해즈");
        WORD_MAP.put("had", "해드");
        WORD_MAP.put("do", "두");
        WORD_MAP.put("does", "더즈");
        WORD_MAP.put("did", "디드");
        WORD_MAP.put("will", "윌");
        WORD_MAP.put("would", "우드");
        WORD_MAP.put("can", "캔");
        WORD_MAP.put("could", "쿠드");
        WORD_MAP.put("may", "메이");
        WORD_MAP.put("might", "마이트");
        WORD_MAP.put("must", "머스트");
        WORD_MAP.put("shall", "쉘");
        WORD_MAP.put("should", "슈드");
        WORD_MAP.put("need", "니드");
        WORD_MAP.put("want", "원트");
        WORD_MAP.put("like", "라이크");
        WORD_MAP.put("know", "노우");
        WORD_MAP.put("think", "씽크");
        WORD_MAP.put("feel", "필");
        WORD_MAP.put("see", "씨");
        WORD_MAP.put("look", "룩");
        WORD_MAP.put("come", "컴");
        WORD_MAP.put("go", "고");
        WORD_MAP.put("get", "겟");
        WORD_MAP.put("take", "테이크");
        WORD_MAP.put("make", "메이크");
        WORD_MAP.put("give", "기브");
        WORD_MAP.put("say", "세이");
        WORD_MAP.put("tell", "텔");
        WORD_MAP.put("call", "콜");
        WORD_MAP.put("try", "트라이");
        WORD_MAP.put("ask", "애스크");
        WORD_MAP.put("use", "유즈");
        WORD_MAP.put("find", "파인드");
        WORD_MAP.put("put", "풋");
        WORD_MAP.put("keep", "킵");
        WORD_MAP.put("let", "렛");
        WORD_MAP.put("begin", "비긴");
        WORD_MAP.put("start", "스타트");
        WORD_MAP.put("end", "엔드");
        WORD_MAP.put("stop", "스탑");
        WORD_MAP.put("run", "런");
        WORD_MAP.put("walk", "워크");
        WORD_MAP.put("stand", "스탠드");
        WORD_MAP.put("sit", "싯");
        WORD_MAP.put("live", "리브");
        WORD_MAP.put("die", "다이");
        WORD_MAP.put("work", "워크");
        WORD_MAP.put("play", "플레이");
        WORD_MAP.put("day", "데이");
        WORD_MAP.put("night", "나이트");
        WORD_MAP.put("time", "타임");
        WORD_MAP.put("world", "월드");
        WORD_MAP.put("life", "라이프");
        WORD_MAP.put("heart", "하트");
        WORD_MAP.put("dream", "드림");
        WORD_MAP.put("star", "스타");
        WORD_MAP.put("sun", "썬");
        WORD_MAP.put("moon", "문");
        WORD_MAP.put("sky", "스카이");
        WORD_MAP.put("fire", "파이어");
        WORD_MAP.put("water", "워터");
        WORD_MAP.put("rain", "레인");
        WORD_MAP.put("snow", "스노우");
        WORD_MAP.put("wind", "윈드");
        WORD_MAP.put("blue", "블루");
        WORD_MAP.put("red", "레드");
        WORD_MAP.put("green", "그린");
        WORD_MAP.put("black", "블랙");
        WORD_MAP.put("white", "화이트");
        WORD_MAP.put("gold", "골드");
        WORD_MAP.put("silver", "실버");
        WORD_MAP.put("pink", "핑크");
        WORD_MAP.put("baby", "베이비");
        WORD_MAP.put("boy", "보이");
        WORD_MAP.put("girl", "걸");
        WORD_MAP.put("man", "맨");
        WORD_MAP.put("woman", "우먼");
        WORD_MAP.put("friend", "프렌드");
        WORD_MAP.put("music", "뮤직");
        WORD_MAP.put("song", "송");
        WORD_MAP.put("dance", "댄스");
        WORD_MAP.put("party", "파티");
        WORD_MAP.put("happy", "해피");
        WORD_MAP.put("sad", "새드");
        WORD_MAP.put("good", "굿");
        WORD_MAP.put("bad", "배드");
        WORD_MAP.put("beautiful", "뷰티풀");
        WORD_MAP.put("pretty", "프리티");
        WORD_MAP.put("crazy", "크레이지");
        WORD_MAP.put("sweet", "스윗");
        WORD_MAP.put("forever", "포에버");
        WORD_MAP.put("never", "네버");
        WORD_MAP.put("always", "올웨이즈");
        WORD_MAP.put("again", "어게인");
        WORD_MAP.put("only", "온리");
        WORD_MAP.put("just", "저스트");
        WORD_MAP.put("more", "모어");
        WORD_MAP.put("all", "올");
        WORD_MAP.put("one", "원");
        WORD_MAP.put("two", "투");
        WORD_MAP.put("first", "퍼스트");
        WORD_MAP.put("last", "라스트");
        WORD_MAP.put("new", "뉴");
        WORD_MAP.put("old", "올드");
        WORD_MAP.put("young", "영");
        WORD_MAP.put("true", "트루");
        WORD_MAP.put("real", "리얼");
        WORD_MAP.put("free", "프리");
        WORD_MAP.put("high", "하이");
        WORD_MAP.put("low", "로우");
        WORD_MAP.put("big", "빅");
        WORD_MAP.put("small", "스몰");
        WORD_MAP.put("long", "롱");
        WORD_MAP.put("short", "숏");
        WORD_MAP.put("hot", "핫");
        WORD_MAP.put("cold", "콜드");
        WORD_MAP.put("kiss", "키스");
        WORD_MAP.put("touch", "터치");
        WORD_MAP.put("hold", "홀드");
        WORD_MAP.put("wait", "웨이트");
        WORD_MAP.put("fall", "폴");
        WORD_MAP.put("fly", "플라이");
        WORD_MAP.put("shine", "샤인");
        WORD_MAP.put("smile", "스마일");
        WORD_MAP.put("cry", "크라이");
        WORD_MAP.put("miss", "미스");
        WORD_MAP.put("remember", "리멤버");
        WORD_MAP.put("forget", "포겟");
        WORD_MAP.put("sorry", "쏘리");
        WORD_MAP.put("thank", "땡크");
        WORD_MAP.put("please", "플리즈");
        WORD_MAP.put("hello", "헬로");
        WORD_MAP.put("bye", "바이");
        WORD_MAP.put("yes", "예스");
        WORD_MAP.put("no", "노");
        WORD_MAP.put("ok", "오케이");
        WORD_MAP.put("okay", "오케이");
        WORD_MAP.put("so", "소");
        WORD_MAP.put("very", "베리");
        WORD_MAP.put("too", "투");
        WORD_MAP.put("up", "업");
        WORD_MAP.put("down", "다운");
        WORD_MAP.put("in", "인");
        WORD_MAP.put("out", "아웃");
        WORD_MAP.put("on", "온");
        WORD_MAP.put("off", "오프");
        WORD_MAP.put("here", "히어");
        WORD_MAP.put("there", "데어");
        WORD_MAP.put("where", "웨어");
        WORD_MAP.put("when", "웬");
        WORD_MAP.put("what", "왓");
        WORD_MAP.put("who", "후");
        WORD_MAP.put("why", "와이");
        WORD_MAP.put("how", "하우");
        WORD_MAP.put("with", "위드");
        WORD_MAP.put("without", "위드아웃");
        WORD_MAP.put("for", "포");
        WORD_MAP.put("from", "프롬");
        WORD_MAP.put("to", "투");
        WORD_MAP.put("into", "인투");
        WORD_MAP.put("by", "바이");
        WORD_MAP.put("about", "어바웃");
        WORD_MAP.put("after", "애프터");
        WORD_MAP.put("before", "비포");
        WORD_MAP.put("over", "오버");
        WORD_MAP.put("under", "언더");
        WORD_MAP.put("between", "비트윈");
        WORD_MAP.put("through", "스루");
        WORD_MAP.put("during", "듀링");
        WORD_MAP.put("while", "와일");
        WORD_MAP.put("until", "언틸");
        WORD_MAP.put("till", "틸");
        WORD_MAP.put("since", "신스");
        WORD_MAP.put("if", "이프");
        WORD_MAP.put("but", "벗");
        WORD_MAP.put("and", "앤드");
        WORD_MAP.put("or", "오어");
        WORD_MAP.put("not", "낫");
        WORD_MAP.put("dont", "돈트");
        WORD_MAP.put("don't", "돈트");
        WORD_MAP.put("wont", "원트");
        WORD_MAP.put("won't", "원트");
        WORD_MAP.put("cant", "캔트");
        WORD_MAP.put("can't", "캔트");
        WORD_MAP.put("its", "잇츠");
        WORD_MAP.put("it's", "잇츠");
        WORD_MAP.put("im", "아임");
        WORD_MAP.put("i'm", "아임");
        WORD_MAP.put("youre", "유어");
        WORD_MAP.put("you're", "유어");
        WORD_MAP.put("hes", "히즈");
        WORD_MAP.put("he's", "히즈");
        WORD_MAP.put("shes", "쉬즈");
        WORD_MAP.put("she's", "쉬즈");
        WORD_MAP.put("were", "위어");
        WORD_MAP.put("we're", "위어");
        WORD_MAP.put("theyre", "데어");
        WORD_MAP.put("they're", "데어");
        WORD_MAP.put("thats", "댓츠");
        WORD_MAP.put("that's", "댓츠");
        WORD_MAP.put("whats", "왓츠");
        WORD_MAP.put("what's", "왓츠");
        WORD_MAP.put("heres", "히얼즈");
        WORD_MAP.put("here's", "히얼즈");
        WORD_MAP.put("theres", "데얼즈");
        WORD_MAP.put("there's", "데얼즈");
        WORD_MAP.put("lets", "렛츠");
        WORD_MAP.put("let's", "렛츠");
        WORD_MAP.put("gonna", "고나");
        WORD_MAP.put("wanna", "워나");
        WORD_MAP.put("gotta", "가타");
        WORD_MAP.put("gimme", "기미");
        WORD_MAP.put("lemme", "레미");
        WORD_MAP.put("outta", "아우타");
        WORD_MAP.put("kinda", "카인다");
        WORD_MAP.put("sorta", "소르타");
        WORD_MAP.put("yeah", "예");
        WORD_MAP.put("yea", "예");
        WORD_MAP.put("yeh", "예");
        WORD_MAP.put("nah", "나");
        WORD_MAP.put("oh", "오");
        WORD_MAP.put("ah", "아");
        WORD_MAP.put("uh", "어");
        WORD_MAP.put("hmm", "흠");
        WORD_MAP.put("wow", "와우");
        WORD_MAP.put("ooh", "우");
        WORD_MAP.put("la", "라");
        WORD_MAP.put("na", "나");
        WORD_MAP.put("boom", "붐");
        WORD_MAP.put("bang", "뱅");
        WORD_MAP.put("pow", "파우");
        WORD_MAP.put("ring", "링");
        WORD_MAP.put("bling", "블링");
        WORD_MAP.put("swing", "스윙");
        WORD_MAP.put("thing", "띵");
        WORD_MAP.put("spring", "스프링");
        WORD_MAP.put("bring", "브링");
        WORD_MAP.put("king", "킹");
        WORD_MAP.put("queen", "퀸");
        WORD_MAP.put("prince", "프린스");
        WORD_MAP.put("princess", "프린세스");
        WORD_MAP.put("angel", "엔젤");
        WORD_MAP.put("devil", "데빌");
        WORD_MAP.put("magic", "매직");
        WORD_MAP.put("miracle", "미라클");
        WORD_MAP.put("fantasy", "판타지");
        WORD_MAP.put("mystery", "미스터리");
        WORD_MAP.put("secret", "시크릿");
        WORD_MAP.put("power", "파워");
        WORD_MAP.put("super", "슈퍼");
        WORD_MAP.put("wonder", "원더");
        WORD_MAP.put("special", "스페셜");
        WORD_MAP.put("perfect", "퍼펙트");
        WORD_MAP.put("fantastic", "판타스틱");
        WORD_MAP.put("amazing", "어메이징");
        WORD_MAP.put("awesome", "어썸");
        WORD_MAP.put("cool", "쿨");
        WORD_MAP.put("nice", "나이스");
        WORD_MAP.put("great", "그레이트");
        WORD_MAP.put("best", "베스트");
        WORD_MAP.put("top", "탑");
        WORD_MAP.put("rock", "락");
        WORD_MAP.put("roll", "롤");
        WORD_MAP.put("pop", "팝");
        WORD_MAP.put("hip", "힙");
        WORD_MAP.put("hop", "합");
        WORD_MAP.put("jazz", "재즈");
        WORD_MAP.put("blues", "블루스");
        WORD_MAP.put("funk", "펑크");
        WORD_MAP.put("soul", "소울");
        WORD_MAP.put("beat", "비트");
        WORD_MAP.put("rhythm", "리듬");
        WORD_MAP.put("melody", "멜로디");
        WORD_MAP.put("harmony", "하모니");
        WORD_MAP.put("voice", "보이스");
        WORD_MAP.put("sound", "사운드");
        WORD_MAP.put("loud", "라우드");
        WORD_MAP.put("quiet", "콰이엇");
        WORD_MAP.put("silence", "사일런스");
        WORD_MAP.put("tonight", "투나잇");
        WORD_MAP.put("today", "투데이");
        WORD_MAP.put("tomorrow", "투모로우");
        WORD_MAP.put("yesterday", "예스터데이");
        WORD_MAP.put("morning", "모닝");
        WORD_MAP.put("evening", "이브닝");
        WORD_MAP.put("summer", "썸머");
        WORD_MAP.put("winter", "윈터");
        WORD_MAP.put("spring", "스프링");
        WORD_MAP.put("autumn", "오텀");
        WORD_MAP.put("fall", "폴");
        WORD_MAP.put("light", "라이트");
        WORD_MAP.put("dark", "다크");
        WORD_MAP.put("bright", "브라이트");
        WORD_MAP.put("shadow", "쉐도우");
        WORD_MAP.put("color", "컬러");
        WORD_MAP.put("colour", "컬러");
        WORD_MAP.put("rainbow", "레인보우");
        WORD_MAP.put("diamond", "다이아몬드");
        WORD_MAP.put("crystal", "크리스탈");
        WORD_MAP.put("electric", "일렉트릭");
        WORD_MAP.put("energy", "에너지");
        WORD_MAP.put("dynamite", "다이나마이트");
        WORD_MAP.put("butter", "버터");
        WORD_MAP.put("ice", "아이스");
        WORD_MAP.put("cream", "크림");
        WORD_MAP.put("sugar", "슈가");
        WORD_MAP.put("honey", "허니");
        WORD_MAP.put("chocolate", "초콜릿");
        WORD_MAP.put("coffee", "커피");
        WORD_MAP.put("wine", "와인");
        WORD_MAP.put("blood", "블러드");
        WORD_MAP.put("tears", "티어스");
        WORD_MAP.put("smile", "스마일");
        WORD_MAP.put("face", "페이스");
        WORD_MAP.put("eyes", "아이즈");
        WORD_MAP.put("lips", "립스");
        WORD_MAP.put("hands", "핸즈");
        WORD_MAP.put("body", "바디");
        WORD_MAP.put("mind", "마인드");
        WORD_MAP.put("soul", "소울");
        WORD_MAP.put("spirit", "스피릿");
        WORD_MAP.put("feeling", "필링");
        WORD_MAP.put("emotion", "이모션");
        WORD_MAP.put("passion", "패션");
        WORD_MAP.put("desire", "디자이어");
        WORD_MAP.put("memory", "메모리");
        WORD_MAP.put("promise", "프로미스");
        WORD_MAP.put("destiny", "데스티니");
        WORD_MAP.put("fate", "페이트");
        WORD_MAP.put("story", "스토리");
        WORD_MAP.put("history", "히스토리");
        WORD_MAP.put("future", "퓨처");
        WORD_MAP.put("past", "패스트");
        WORD_MAP.put("present", "프레젠트");
        WORD_MAP.put("moment", "모먼트");
        WORD_MAP.put("second", "세컨드");
        WORD_MAP.put("minute", "미닛");
        WORD_MAP.put("hour", "아워");
        WORD_MAP.put("week", "위크");
        WORD_MAP.put("month", "먼스");
        WORD_MAP.put("year", "이어");
        WORD_MAP.put("century", "센츄리");
        WORD_MAP.put("eternity", "이터니티");
        WORD_MAP.put("infinity", "인피니티");
        WORD_MAP.put("universe", "유니버스");
        WORD_MAP.put("galaxy", "갤럭시");
        WORD_MAP.put("planet", "플래닛");
        WORD_MAP.put("earth", "어스");
        WORD_MAP.put("heaven", "헤븐");
        WORD_MAP.put("hell", "헬");
        WORD_MAP.put("paradise", "파라다이스");
        WORD_MAP.put("home", "홈");
        WORD_MAP.put("house", "하우스");
        WORD_MAP.put("room", "룸");
        WORD_MAP.put("door", "도어");
        WORD_MAP.put("window", "윈도우");
        WORD_MAP.put("wall", "월");
        WORD_MAP.put("floor", "플로어");
        WORD_MAP.put("road", "로드");
        WORD_MAP.put("street", "스트릿");
        WORD_MAP.put("way", "웨이");
        WORD_MAP.put("path", "패스");
        WORD_MAP.put("bridge", "브릿지");
        WORD_MAP.put("city", "시티");
        WORD_MAP.put("town", "타운");
        WORD_MAP.put("village", "빌리지");
        WORD_MAP.put("country", "컨트리");
        WORD_MAP.put("nation", "네이션");
        WORD_MAP.put("mountain", "마운틴");
        WORD_MAP.put("river", "리버");
        WORD_MAP.put("ocean", "오션");
        WORD_MAP.put("sea", "씨");
        WORD_MAP.put("beach", "비치");
        WORD_MAP.put("island", "아일랜드");
        WORD_MAP.put("forest", "포레스트");
        WORD_MAP.put("garden", "가든");
        WORD_MAP.put("flower", "플라워");
        WORD_MAP.put("rose", "로즈");
        WORD_MAP.put("tree", "트리");
        WORD_MAP.put("leaf", "리프");
        WORD_MAP.put("bird", "버드");
        WORD_MAP.put("butterfly", "버터플라이");
        WORD_MAP.put("cat", "캣");
        WORD_MAP.put("dog", "독");
        WORD_MAP.put("wolf", "울프");
        WORD_MAP.put("lion", "라이온");
        WORD_MAP.put("tiger", "타이거");
        WORD_MAP.put("dragon", "드래곤");
        WORD_MAP.put("monster", "몬스터");
        WORD_MAP.put("hero", "히어로");
        WORD_MAP.put("villain", "빌런");
        WORD_MAP.put("warrior", "워리어");
        WORD_MAP.put("soldier", "솔져");
        WORD_MAP.put("fighter", "파이터");
        WORD_MAP.put("champion", "챔피언");
        WORD_MAP.put("winner", "위너");
        WORD_MAP.put("loser", "루저");
        WORD_MAP.put("game", "게임");
        WORD_MAP.put("fight", "파이트");
        WORD_MAP.put("battle", "배틀");
        WORD_MAP.put("war", "워");
        WORD_MAP.put("peace", "피스");
        WORD_MAP.put("freedom", "프리덤");
        WORD_MAP.put("justice", "저스티스");
        WORD_MAP.put("truth", "트루스");
        WORD_MAP.put("lie", "라이");
        WORD_MAP.put("fake", "페이크");
        WORD_MAP.put("fear", "피어");
        WORD_MAP.put("hope", "호프");
        WORD_MAP.put("wish", "위시");
        WORD_MAP.put("prayer", "프레이어");
        WORD_MAP.put("blessing", "블레싱");
        WORD_MAP.put("curse", "커스");
        WORD_MAP.put("luck", "럭");
        WORD_MAP.put("chance", "찬스");
        WORD_MAP.put("choice", "초이스");
        WORD_MAP.put("change", "체인지");
        WORD_MAP.put("turn", "턴");
        WORD_MAP.put("move", "무브");
        WORD_MAP.put("step", "스텝");
        WORD_MAP.put("jump", "점프");
        WORD_MAP.put("spin", "스핀");
        WORD_MAP.put("shake", "쉐이크");
        WORD_MAP.put("break", "브레이크");
        WORD_MAP.put("hurt", "허트");
        WORD_MAP.put("pain", "페인");
        WORD_MAP.put("heal", "힐");
        WORD_MAP.put("save", "세이브");
        WORD_MAP.put("rescue", "레스큐");
        WORD_MAP.put("help", "헬프");
        WORD_MAP.put("need", "니드");
        WORD_MAP.put("lonely", "론리");
        WORD_MAP.put("alone", "얼론");
        WORD_MAP.put("together", "투게더");
        WORD_MAP.put("apart", "어파트");
        WORD_MAP.put("close", "클로즈");
        WORD_MAP.put("far", "파");
        WORD_MAP.put("near", "니어");
        WORD_MAP.put("away", "어웨이");
        WORD_MAP.put("gone", "곤");
        WORD_MAP.put("back", "백");
        WORD_MAP.put("return", "리턴");
        WORD_MAP.put("leave", "리브");
        WORD_MAP.put("stay", "스테이");
        WORD_MAP.put("ride", "라이드");
        WORD_MAP.put("drive", "드라이브");
        WORD_MAP.put("sail", "세일");
        WORD_MAP.put("catch", "캐치");
        WORD_MAP.put("throw", "쓰로우");
        WORD_MAP.put("kick", "킥");
        WORD_MAP.put("hit", "힛");
        WORD_MAP.put("push", "푸시");
        WORD_MAP.put("pull", "풀");
        WORD_MAP.put("open", "오픈");
        WORD_MAP.put("shut", "셧");
        WORD_MAP.put("lock", "락");
        WORD_MAP.put("key", "키");
        WORD_MAP.put("door", "도어");
        WORD_MAP.put("window", "윈도우");
        WORD_MAP.put("glass", "글라스");
        WORD_MAP.put("mirror", "미러");
        WORD_MAP.put("picture", "픽처");
        WORD_MAP.put("photo", "포토");
        WORD_MAP.put("movie", "무비");
        WORD_MAP.put("film", "필름");
        WORD_MAP.put("show", "쇼");
        WORD_MAP.put("stage", "스테이지");
        WORD_MAP.put("scene", "씬");
        WORD_MAP.put("act", "액트");
        WORD_MAP.put("role", "롤");
        WORD_MAP.put("character", "캐릭터");
        WORD_MAP.put("style", "스타일");
        WORD_MAP.put("fashion", "패션");
        WORD_MAP.put("dress", "드레스");
        WORD_MAP.put("suit", "수트");
        WORD_MAP.put("hat", "햇");
        WORD_MAP.put("shoe", "슈");
        WORD_MAP.put("shoes", "슈즈");
        WORD_MAP.put("bag", "백");
        WORD_MAP.put("ring", "링");
        WORD_MAP.put("chain", "체인");
        WORD_MAP.put("crown", "크라운");
        WORD_MAP.put("mask", "마스크");
        WORD_MAP.put("gun", "건");
        WORD_MAP.put("knife", "나이프");
        WORD_MAP.put("sword", "소드");
        WORD_MAP.put("shield", "쉴드");
        WORD_MAP.put("armor", "아머");
        WORD_MAP.put("letter", "레터");
        WORD_MAP.put("word", "워드");
        WORD_MAP.put("name", "네임");
        WORD_MAP.put("number", "넘버");
        WORD_MAP.put("sign", "사인");
        WORD_MAP.put("signal", "시그널");
        WORD_MAP.put("code", "코드");
        WORD_MAP.put("message", "메시지");
        WORD_MAP.put("call", "콜");
        WORD_MAP.put("answer", "앤서");
        WORD_MAP.put("question", "퀘스천");
        WORD_MAP.put("problem", "프라블럼");
        WORD_MAP.put("solution", "솔루션");
        WORD_MAP.put("reason", "리즌");
        WORD_MAP.put("cause", "코즈");
        WORD_MAP.put("effect", "이펙트");
        WORD_MAP.put("result", "리절트");
        WORD_MAP.put("success", "석세스");
        WORD_MAP.put("failure", "페일류어");
        WORD_MAP.put("victory", "빅토리");
        WORD_MAP.put("defeat", "디피트");
        WORD_MAP.put("goal", "골");
        WORD_MAP.put("target", "타겟");
        WORD_MAP.put("aim", "에임");
        WORD_MAP.put("point", "포인트");
        WORD_MAP.put("focus", "포커스");
        WORD_MAP.put("center", "센터");
        WORD_MAP.put("middle", "미들");
        WORD_MAP.put("edge", "엣지");
        WORD_MAP.put("side", "사이드");
        WORD_MAP.put("corner", "코너");
        WORD_MAP.put("line", "라인");
        WORD_MAP.put("circle", "서클");
        WORD_MAP.put("square", "스퀘어");
        WORD_MAP.put("shape", "쉐입");
        WORD_MAP.put("form", "폼");
        WORD_MAP.put("figure", "피규어");
        WORD_MAP.put("size", "사이즈");
        WORD_MAP.put("weight", "웨이트");
        WORD_MAP.put("speed", "스피드");
        WORD_MAP.put("fast", "패스트");
        WORD_MAP.put("slow", "슬로우");
        WORD_MAP.put("quick", "퀵");
        WORD_MAP.put("rush", "러쉬");
        WORD_MAP.put("crazy", "크레이지");
        WORD_MAP.put("mad", "매드");
        WORD_MAP.put("wild", "와일드");
        WORD_MAP.put("calm", "캄");
        WORD_MAP.put("cool", "쿨");
        WORD_MAP.put("warm", "웜");
        WORD_MAP.put("soft", "소프트");
        WORD_MAP.put("hard", "하드");
        WORD_MAP.put("rough", "러프");
        WORD_MAP.put("smooth", "스무스");
        WORD_MAP.put("sharp", "샤프");
        WORD_MAP.put("dull", "덜");
        WORD_MAP.put("thick", "띡");
        WORD_MAP.put("thin", "띤");
        WORD_MAP.put("heavy", "헤비");
        WORD_MAP.put("empty", "엠티");
        WORD_MAP.put("full", "풀");
        WORD_MAP.put("rich", "리치");
        WORD_MAP.put("poor", "푸어");
        WORD_MAP.put("cheap", "칩");
        WORD_MAP.put("expensive", "익스펜시브");
        WORD_MAP.put("worth", "워스");
        WORD_MAP.put("value", "밸류");
        WORD_MAP.put("price", "프라이스");
        WORD_MAP.put("cost", "코스트");
        WORD_MAP.put("pay", "페이");
        WORD_MAP.put("buy", "바이");
        WORD_MAP.put("sell", "셀");
        WORD_MAP.put("trade", "트레이드");
        WORD_MAP.put("deal", "딜");
        WORD_MAP.put("business", "비즈니스");
        WORD_MAP.put("job", "잡");
        WORD_MAP.put("career", "커리어");
        WORD_MAP.put("office", "오피스");
        WORD_MAP.put("company", "컴퍼니");
        WORD_MAP.put("team", "팀");
        WORD_MAP.put("group", "그룹");
        WORD_MAP.put("club", "클럽");
        WORD_MAP.put("band", "밴드");
        WORD_MAP.put("crew", "크루");
        WORD_MAP.put("squad", "스쿼드");
        WORD_MAP.put("army", "아미");
        WORD_MAP.put("family", "패밀리");
        WORD_MAP.put("father", "파더");
        WORD_MAP.put("mother", "마더");
        WORD_MAP.put("son", "선");
        WORD_MAP.put("daughter", "도터");
        WORD_MAP.put("brother", "브라더");
        WORD_MAP.put("sister", "시스터");
        WORD_MAP.put("uncle", "엉클");
        WORD_MAP.put("aunt", "앤트");
        WORD_MAP.put("grandma", "그랜마");
        WORD_MAP.put("grandpa", "그랜파");
        WORD_MAP.put("husband", "허즈밴드");
        WORD_MAP.put("wife", "와이프");
        WORD_MAP.put("lover", "러버");
        WORD_MAP.put("partner", "파트너");
        WORD_MAP.put("couple", "커플");
        WORD_MAP.put("wedding", "웨딩");
        WORD_MAP.put("marriage", "매리지");
        WORD_MAP.put("divorce", "디보스");
        WORD_MAP.put("relationship", "릴레이션십");
        WORD_MAP.put("romance", "로맨스");
        WORD_MAP.put("date", "데이트");
        WORD_MAP.put("crush", "크러쉬");
        WORD_MAP.put("fever", "피버");
        WORD_MAP.put("sick", "식");
        WORD_MAP.put("ill", "일");
        WORD_MAP.put("health", "헬스");
        WORD_MAP.put("hospital", "하스피탈");
        WORD_MAP.put("doctor", "닥터");
        WORD_MAP.put("nurse", "너스");
        WORD_MAP.put("medicine", "메디슨");
        WORD_MAP.put("drug", "드럭");
        WORD_MAP.put("poison", "포이즌");
        WORD_MAP.put("danger", "데인저");
        WORD_MAP.put("safe", "세이프");
        WORD_MAP.put("risk", "리스크");
        WORD_MAP.put("adventure", "어드벤처");
        WORD_MAP.put("journey", "저니");
        WORD_MAP.put("trip", "트립");
        WORD_MAP.put("travel", "트래블");
        WORD_MAP.put("vacation", "베케이션");
        WORD_MAP.put("holiday", "홀리데이");
        WORD_MAP.put("weekend", "위켄드");
        WORD_MAP.put("birthday", "버스데이");
        WORD_MAP.put("christmas", "크리스마스");
        WORD_MAP.put("valentine", "발렌타인");
        WORD_MAP.put("halloween", "할로윈");
        WORD_MAP.put("festival", "페스티벌");
        WORD_MAP.put("carnival", "카니발");
        WORD_MAP.put("celebration", "셀레브레이션");
        WORD_MAP.put("gift", "기프트");
        WORD_MAP.put("present", "프레젠트");
        WORD_MAP.put("surprise", "서프라이즈");
        WORD_MAP.put("reward", "리워드");
        WORD_MAP.put("trophy", "트로피");
        WORD_MAP.put("medal", "메달");
        WORD_MAP.put("prize", "프라이즈");
        WORD_MAP.put("treasure", "트레져");
        WORD_MAP.put("jewel", "쥬얼");
        WORD_MAP.put("gem", "젬");
        WORD_MAP.put("pearl", "펄");
        WORD_MAP.put("ruby", "루비");
        WORD_MAP.put("emerald", "에메랄드");
        WORD_MAP.put("sapphire", "사파이어");
        WORD_MAP.put("velvet", "벨벳");
        WORD_MAP.put("silk", "실크");
        WORD_MAP.put("leather", "레더");
        WORD_MAP.put("denim", "데님");
        WORD_MAP.put("cotton", "코튼");
        WORD_MAP.put("wool", "울");
        WORD_MAP.put("fur", "퍼");
        WORD_MAP.put("neon", "네온");
        WORD_MAP.put("laser", "레이저");
        WORD_MAP.put("digital", "디지털");
        WORD_MAP.put("cyber", "사이버");
        WORD_MAP.put("virtual", "버추얼");
        WORD_MAP.put("online", "온라인");
        WORD_MAP.put("offline", "오프라인");
        WORD_MAP.put("system", "시스템");
        WORD_MAP.put("network", "네트워크");
        WORD_MAP.put("program", "프로그램");
        WORD_MAP.put("computer", "컴퓨터");
        WORD_MAP.put("phone", "폰");
        WORD_MAP.put("telephone", "텔레폰");
        WORD_MAP.put("mobile", "모바일");
        WORD_MAP.put("camera", "카메라");
        WORD_MAP.put("video", "비디오");
        WORD_MAP.put("audio", "오디오");
        WORD_MAP.put("radio", "라디오");
        WORD_MAP.put("television", "텔레비전");
        WORD_MAP.put("channel", "채널");
        WORD_MAP.put("station", "스테이션");
        WORD_MAP.put("broadcast", "브로드캐스트");
        WORD_MAP.put("live", "라이브");
        WORD_MAP.put("record", "레코드");
        WORD_MAP.put("album", "앨범");
        WORD_MAP.put("single", "싱글");
        WORD_MAP.put("track", "트랙");
        WORD_MAP.put("mix", "믹스");
        WORD_MAP.put("remix", "리믹스");
        WORD_MAP.put("version", "버전");
        WORD_MAP.put("original", "오리지널");
        WORD_MAP.put("cover", "커버");
        WORD_MAP.put("classic", "클래식");
        WORD_MAP.put("modern", "모던");
        WORD_MAP.put("retro", "레트로");
        WORD_MAP.put("vintage", "빈티지");
        WORD_MAP.put("fresh", "프레쉬");
        WORD_MAP.put("brand", "브랜드");
    }

    /**
     * 제목에서 정답 후보들 생성
     */
    public static Set<String> generateAnswerVariants(String title) {
        Set<String> variants = new LinkedHashSet<>();

        if (title == null || title.trim().isEmpty()) {
            return variants;
        }

        String originalTitle = title.trim();
        variants.add(originalTitle);

        // 1. 괄호 제거 버전
        String withoutBrackets = originalTitle
                .replaceAll("\\(.*?\\)", "")
                .replaceAll("\\[.*?\\]", "")
                .replaceAll("\\{.*?\\}", "")
                .replaceAll("「.*?」", "")
                .replaceAll("『.*?』", "")
                .trim();
        if (!withoutBrackets.isEmpty() && !withoutBrackets.equals(originalTitle)) {
            variants.add(withoutBrackets);
        }

        // 2. 하이픈/특수문자 제거 버전
        String withoutSpecial = originalTitle
                .replaceAll("[\\-_~·]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (!withoutSpecial.equals(originalTitle)) {
            variants.add(withoutSpecial);
        }

        // 3. 영어 제목이면 한글 발음 추가
        if (containsEnglish(originalTitle)) {
            String koreanPronunciation = convertToKorean(originalTitle);
            if (koreanPronunciation != null && !koreanPronunciation.isEmpty()) {
                variants.add(koreanPronunciation);
            }

            // 괄호 제거 버전도 한글로
            if (!withoutBrackets.isEmpty() && containsEnglish(withoutBrackets)) {
                String koreanWithoutBrackets = convertToKorean(withoutBrackets);
                if (koreanWithoutBrackets != null && !koreanWithoutBrackets.isEmpty()) {
                    variants.add(koreanWithoutBrackets);
                }
            }
        }

        // 4. feat. / ft. 제거
        String withoutFeat = originalTitle
                .replaceAll("(?i)\\s*(feat\\.?|ft\\.?)\\s*.*$", "")
                .replaceAll("(?i)\\s*(featuring)\\s*.*$", "")
                .trim();
        if (!withoutFeat.isEmpty() && !withoutFeat.equals(originalTitle)) {
            variants.add(withoutFeat);
        }

        return variants;
    }

    /**
     * 영어가 포함되어 있는지 확인
     */
    private static boolean containsEnglish(String text) {
        return Pattern.compile("[a-zA-Z]").matcher(text).find();
    }

    /**
     * 영어를 한글 발음으로 변환
     */
    public static String convertToKorean(String english) {
        if (english == null || english.trim().isEmpty()) {
            return "";
        }

        String text = english.toLowerCase().trim();
        StringBuilder result = new StringBuilder();

        // 단어 단위로 분리
        String[] words = text.split("\\s+");

        for (int i = 0; i < words.length; i++) {
            String word = words[i].replaceAll("[^a-z0-9']", "");

            if (word.isEmpty()) {
                continue;
            }

            // 숫자는 그대로
            if (word.matches("\\d+")) {
                result.append(word);
            }
            // 단어 매핑에 있으면 사용
            else if (WORD_MAP.containsKey(word)) {
                result.append(WORD_MAP.get(word));
            }
            // 없으면 음소 변환
            else {
                result.append(phoneticConvert(word));
            }

            if (i < words.length - 1) {
                result.append(" ");
            }
        }

        return result.toString().trim();
    }

    /**
     * 음소 단위 변환
     */
    private static String phoneticConvert(String word) {
        StringBuilder result = new StringBuilder();
        String remaining = word.toLowerCase();

        while (!remaining.isEmpty()) {
            boolean matched = false;

            // 긴 패턴부터 매칭 시도
            for (int len = Math.min(remaining.length(), 5); len >= 1; len--) {
                String sub = remaining.substring(0, len);
                if (PHONETIC_MAP.containsKey(sub)) {
                    result.append(PHONETIC_MAP.get(sub));
                    remaining = remaining.substring(len);
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                // 매칭 안 되면 한 글자 건너뛰기
                remaining = remaining.substring(1);
            }
        }

        return result.toString();
    }
}