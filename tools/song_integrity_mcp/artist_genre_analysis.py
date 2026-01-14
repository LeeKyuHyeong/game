import sys
sys.stdout.reconfigure(encoding='utf-8')
import pymysql
from config import DB_CONFIG

conn = pymysql.connect(**DB_CONFIG)
cursor = conn.cursor()

# 현재 사용 중인 장르 ID 매핑
GENRE_MAP = {
    '아이돌': 32,
    '발라드': 33,
    '힙합/랩': 34,
    'R&B/소울': 35,
    '인디/어쿠스틱': 36,
    '트로트': 37,
    '밴드/록': 38,
    'OST': 39,
    '레트로/가요': 40,
    'EDM/댄스': 22,
}

# 아티스트별 추천 장르 (수동 분류)
# 잘못 분류된 아티스트들을 올바른 장르로 매핑
ARTIST_RECOMMENDED_GENRE = {
    # === 힙합/랩으로 변경해야 할 아티스트 ===
    '다이나믹듀오': '힙합/랩',  # R&B/소울 -> 힙합/랩 (대표적 힙합 듀오)
    '드렁큰타이거': '힙합/랩',  # R&B/소울 -> 힙합/랩
    'MC몽': '힙합/랩',  # R&B/소울 -> 힙합/랩
    '리쌍': '힙합/랩',  # R&B/소울 -> 힙합/랩
    '산이': '힙합/랩',  # R&B/소울 -> 힙합/랩
    '마이티마우스': '힙합/랩',  # R&B/소울 -> 힙합/랩
    '배치기': '힙합/랩',  # R&B/소울 -> 힙합/랩
    '하하': '힙합/랩',  # R&B/소울 -> 힙합/랩

    # === 아이돌로 변경해야 할 아티스트 ===
    'AOA': '아이돌',  # 힙합/랩 -> 아이돌 (걸그룹)
    'EXID': '아이돌',  # 힙합/랩 -> 아이돌 (걸그룹)
    '걸스데이': '아이돌',  # 힙합/랩 -> 아이돌 (걸그룹)
    '레이디스코드': '아이돌',  # 힙합/랩 -> 아이돌 (걸그룹)
    '모모랜드': '아이돌',  # 힙합/랩 -> 아이돌 (걸그룹)
    '미쓰에이': '아이돌',  # 힙합/랩 -> 아이돌 (걸그룹)
    '미쓰에스': '아이돌',  # 힙합/랩 -> 아이돌 (걸그룹)
    '손담비': '아이돌',  # 힙합/랩 -> 아이돌 (솔로 아이돌)
    '아이비': '아이돌',  # 힙합/랩 -> 아이돌 (솔로 아이돌)
    '보아': '아이돌',  # 힙합/랩 -> 아이돌 (솔로 아이돌)
    '비': '아이돌',  # 힙합/랩 -> 아이돌 (솔로 아이돌)
    '포미닛': '아이돌',  # 힙합/랩 -> 아이돌 (걸그룹)
    '크레용팝': '아이돌',  # 힙합/랩 -> 아이돌 (걸그룹)
    '청하': '아이돌',  # 힙합/랩 -> 아이돌 (솔로 아이돌, I.O.I 출신)
    '지나': '아이돌',  # 힙합/랩 -> 아이돌 (솔로 아이돌)
    '쥬얼리': '아이돌',  # 힙합/랩 -> 아이돌 (걸그룹)
    '엄정화': '아이돌',  # 힙합/랩 -> 레트로/가요 또는 아이돌 (댄스가수)
    '이정현': '아이돌',  # 힙합/랩 -> 아이돌 (솔로 아이돌)
    '이효리': '아이돌',  # 힙합/랩 -> 아이돌 (솔로 아이돌, 핑클 출신)
    '박진영': '아이돌',  # 힙합/랩 -> 아이돌 (프로듀서겸 솔로)
    '거북이': '아이돌',  # 힙합/랩 -> 아이돌/레트로 (버스커버스커 스타일)
    'FT Island': '밴드/록',  # 힙합/랩 -> 밴드/록 (밴드)

    # === R&B/소울로 유지 또는 변경 ===
    'G-DRAGON': '아이돌',  # R&B/소울 -> 아이돌 (빅뱅 멤버)
    '태양': '아이돌',  # R&B/소울 -> 아이돌 (빅뱅 멤버, R&B 스타일이지만 아이돌)
    '백현': '아이돌',  # R&B/소울 -> 아이돌 (엑소 멤버)
    '화사': '아이돌',  # R&B/소울 -> 아이돌 (마마무 멤버)
    '비비': '아이돌',  # R&B/소울 -> 아이돌 (솔로 아이돌)
    'WOODZ': '아이돌',  # R&B/소울 -> 아이돌 (솔로 아이돌)
    '소유': '아이돌',  # R&B/소울 -> 아이돌 (씨스타 멤버)
    '이하이': 'R&B/소울',  # 유지 (R&B 보컬리스트)
    '백예린': 'R&B/소울',  # 유지 (R&B/인디)
    '헤이즈': 'R&B/소울',  # 유지 (R&B)
    '나얼': 'R&B/소울',  # 유지 (R&B 보컬리스트)
    '휘성': 'R&B/소울',  # 유지 (R&B 보컬리스트)
    '거미': 'R&B/소울',  # 유지 (R&B 보컬리스트)
    '바비킴': 'R&B/소울',  # 유지 (R&B 보컬리스트)
    '브라운 아이즈': 'R&B/소울',  # 유지 (R&B 그룹)
    '빅마마': 'R&B/소울',  # 유지 (R&B 보컬 그룹)
    '플라이투더스카이': 'R&B/소울',  # 유지 (R&B 듀오)
    '프라이머리': 'R&B/소울',  # 유지 (프로듀서, R&B 기반)

    # === 레트로/가요로 변경 ===
    '엄정화': '레트로/가요',  # 힙합/랩 -> 레트로/가요 (90년대 댄스 가수)
    '거북이': '레트로/가요',  # 힙합/랩 -> 레트로/가요 (비뚤어진 사랑 등)

    # === EDM/댄스 ===
    '싸이': 'EDM/댄스',  # 유지 (EDM/댄스)
    '김현정': 'EDM/댄스',  # 유지 (댄스)
}

# 현재 DB 상태 조회
cursor.execute('''
    SELECT s.artist, g.name as current_genre, COUNT(*) as cnt
    FROM song s
    JOIN genre g ON s.genre_id = g.id
    GROUP BY s.artist, g.name
    ORDER BY s.artist
''')
rows = cursor.fetchall()

# 아티스트별 현재 장르
current_mapping = {}
for artist, genre, cnt in rows:
    if artist not in current_mapping:
        current_mapping[artist] = []
    current_mapping[artist].append({'genre': genre, 'count': cnt})

# 변경이 필요한 아티스트 찾기
changes_needed = []
for artist, recommended in ARTIST_RECOMMENDED_GENRE.items():
    if artist in current_mapping:
        current_genres = current_mapping[artist]
        # 현재 장르 중 추천 장르와 다른 것이 있는지 확인
        for cg in current_genres:
            if cg['genre'] != recommended:
                changes_needed.append({
                    'artist': artist,
                    'current': cg['genre'],
                    'recommended': recommended,
                    'count': cg['count']
                })

print('=' * 80)
print('아티스트 장르 변경 추천 목록')
print('=' * 80)
print()

# 카테고리별로 정리
categories = {
    '힙합/랩으로 변경': [],
    '아이돌로 변경': [],
    'R&B/소울로 변경': [],
    '밴드/록으로 변경': [],
    '레트로/가요로 변경': [],
    'EDM/댄스로 변경': [],
}

for change in changes_needed:
    key = f"{change['recommended']}으로 변경"
    if key in categories:
        categories[key].append(change)
    else:
        key = f"{change['recommended']}로 변경"
        if key in categories:
            categories[key].append(change)

for category, items in categories.items():
    if items:
        print(f'[{category}]')
        for item in items:
            print(f"  {item['artist']}: {item['current']} -> {item['recommended']} ({item['count']}곡)")
        print()

# SQL UPDATE 문 생성
print('=' * 80)
print('SQL UPDATE 문')
print('=' * 80)
print()

for change in changes_needed:
    artist = change['artist']
    current = change['current']
    recommended = change['recommended']
    new_genre_id = GENRE_MAP.get(recommended)
    if new_genre_id:
        print(f"-- {artist}: {current} -> {recommended}")
        print(f"UPDATE song SET genre_id = {new_genre_id} WHERE artist = '{artist}';")
        print()

conn.close()
