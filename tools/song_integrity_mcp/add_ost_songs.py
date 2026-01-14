import sys
sys.stdout.reconfigure(encoding='utf-8')
import pymysql
import time
import urllib.request
import urllib.parse
import re
import json
from config import DB_CONFIG

conn = pymysql.connect(**DB_CONFIG)
cursor = conn.cursor()

# OST 장르 ID
OST_GENRE_ID = 39

def search_youtube_lyrics(artist, title):
    """YouTube에서 가사 영상 검색"""
    queries = [
        f"{artist} {title} 가사",
        f"{artist} {title} lyrics",
        f"{title} {artist} 가사",
    ]

    for query in queries:
        try:
            encoded_query = urllib.parse.quote(query)
            url = f"https://www.youtube.com/results?search_query={encoded_query}"

            req = urllib.request.Request(url, headers={
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
            })

            with urllib.request.urlopen(req, timeout=10) as response:
                html = response.read().decode('utf-8')

            # video ID 추출
            video_ids = re.findall(r'"videoId":"([a-zA-Z0-9_-]{11})"', html)

            if video_ids:
                # 첫 번째 결과 반환
                return video_ids[0]

            time.sleep(1)
        except Exception as e:
            print(f"  검색 오류: {e}")
            continue

    return None

# 새로 추가할 OST 목록 (50곡)
NEW_OST_SONGS = [
    # 도깨비 OST
    ("크러쉬", "Beautiful", "도깨비"),
    ("에일리", "첫눈처럼 너에게 가겠다", "도깨비"),
    ("소유", "I Miss You", "도깨비"),
    ("헤이즈", "Round And Round", "도깨비"),

    # 태양의 후예 OST
    ("거미", "You Are My Everything", "태양의 후예"),
    ("첸, 백현, 시우민", "Everytime", "태양의 후예"),
    ("SG워너비", "무뚝뚝하게", "태양의 후예"),

    # 사랑의 불시착 OST
    ("백예린", "다시 난, 여기", "사랑의 불시착"),
    ("아이유", "I Give You My Heart", "사랑의 불시착"),

    # 이태원 클라쓰 OST
    ("가호", "시작", "이태원 클라쓰"),
    ("윤미래", "Say", "이태원 클라쓰"),
    ("하현우", "Diamond", "이태원 클라쓰"),

    # 호텔 델루나 OST
    ("태연", "All About You", "호텔 델루나"),
    ("벤", "Can You Hear Me", "호텔 델루나"),
    ("헤이즈", "Can You See My Heart", "호텔 델루나"),
    ("청하", "At The End", "호텔 델루나"),

    # 눈물의 여왕 OST
    ("김수현", "청혼", "눈물의 여왕"),
    ("헤이즈", "눈물의 여왕", "눈물의 여왕"),

    # 응답하라 시리즈 OST
    ("이적", "걱정말아요 그대", "응답하라 1988"),
    ("박보람", "혜화동", "응답하라 1988"),

    # 겨울연가 OST
    ("류", "처음부터 지금까지", "겨울연가"),

    # 미안하다 사랑한다 OST
    ("임재범", "사랑한다는 말 못해", "미안하다 사랑한다"),

    # 괜찮아 사랑이야 OST
    ("첸", "최고의 행운", "괜찮아 사랑이야"),

    # 스물다섯 스물하나 OST
    ("백현", "너를 사랑하고 있어", "스물다섯 스물하나"),
    ("자우림", "스물다섯 스물하나", "스물다섯 스물하나"),

    # 슬기로운 의사생활 OST
    ("미도와 파라솔", "너에게 난, 나에게 넌", "슬기로운 의사생활"),
    ("전미도", "사랑하게 될 줄 알았어", "슬기로운 의사생활"),
    ("규현", "화려하지 않은 고백", "슬기로운 의사생활"),

    # 빈센조 OST
    ("이하이", "비가 내리면", "빈센조"),

    # 더 글로리 OST
    ("웬디", "Like Water", "더 글로리"),

    # 선재 업고 튀어 OST
    ("이클립스", "선재 업고 튀어", "선재 업고 튀어"),
    ("변우석", "소나기", "선재 업고 튀어"),

    # 기타 인기 OST
    ("폴킴", "모든 날 모든 순간", "키스 먼저 할까요"),
    ("효린", "안녕", "별에서 온 그대"),
    ("성시경", "너에게", "응답하라 1997"),
    ("옥주현", "잊을게", "풀하우스"),
    ("거미", "기억상실", "내 이름은 김삼순"),
    ("린", "시간을 거슬러", "해를 품은 달"),
    ("윤미래", "Touch Love", "마스터의 태양"),
    ("케이시", "잠이 오질 않네요", "나의 아저씨"),
    ("정준일", "안아줘", "시크릿가든"),
    ("박정현", "꿈에", "클래식"),
    ("태연", "그대라는 시", "호텔 델루나"),
    ("10cm", "스토커", "킬미힐미"),
    ("아이유", "마음", "달의 연인"),
    ("백지영", "총 맞은 것처럼", "아이리스"),
    ("나윤권", "기다리다", "선덕여왕"),
    ("김연지", "흩어진다", "슬기로운 의사생활"),
    ("조이", "나를 바라봐", "슬기로운 의사생활"),
]

print("OST 곡 YouTube 검색 및 DB 추가")
print("=" * 80)

added_count = 0
failed_list = []
processed = 0

for artist, title, drama in NEW_OST_SONGS:
    if processed >= 50:
        break

    # 중복 체크
    cursor.execute('SELECT id FROM song WHERE artist = %s AND title = %s', (artist, title))
    if cursor.fetchone():
        print(f"[SKIP] 이미 존재: {artist} - {title}")
        continue

    # YouTube 검색
    print(f"검색 중: {artist} - {title} ({drama})")
    video_id = search_youtube_lyrics(artist, title)

    if video_id:
        try:
            cursor.execute('''
                INSERT INTO song (artist, title, genre_id, youtube_video_id, use_yn)
                VALUES (%s, %s, %s, %s, 'Y')
            ''', (artist, title, OST_GENRE_ID, video_id))
            conn.commit()

            song_id = cursor.lastrowid

            # song_answer에 기본 정답 추가
            cursor.execute('''
                INSERT INTO song_answer (song_id, answer, is_primary)
                VALUES (%s, %s, 1)
            ''', (song_id, title))
            conn.commit()

            added_count += 1
            processed += 1
            print(f"  [ADD] -> https://youtu.be/{video_id}")
        except Exception as e:
            print(f"  [ERROR] {e}")
            failed_list.append((artist, title, drama, str(e)))
    else:
        print(f"  [FAIL] YouTube 검색 실패")
        failed_list.append((artist, title, drama, "YouTube 검색 실패"))

    time.sleep(1)  # API 제한 방지

print()
print("=" * 80)
print(f"완료! {added_count}곡 추가됨")

if failed_list:
    print()
    print(f"실패 목록 ({len(failed_list)}개):")
    for artist, title, drama, reason in failed_list:
        print(f"  - {artist} - {title} ({drama}): {reason}")

# 현재 OST 장르 곡 수 확인
cursor.execute('SELECT COUNT(*) FROM song WHERE genre_id = %s', (OST_GENRE_ID,))
total_ost = cursor.fetchone()[0]
print(f"\n현재 OST 장르 총 곡 수: {total_ost}곡")

conn.close()
