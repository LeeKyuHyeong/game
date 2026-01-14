import sys
sys.stdout.reconfigure(encoding='utf-8')
from agents.answer_matcher import AnswerMatcher

matcher = AnswerMatcher()

test_cases = [
    ('BTS (방탄소년단) - Boy With Luv (작은 것들을 위한 시) feat. Halsey', 'Boy With Luv'),
    ('BTS (방탄소년단) - FAKE LOVE', 'FAKE LOVE'),
    ('BTS - FIRE (방탄소년단 - 불타오르네)', 'FIRE'),
    ('BTS (방탄소년단) - DNA', 'DNA'),
    ('BTS (방탄소년단) - ON', 'ON'),
    ('IU(아이유) - Blueming(블루밍)', 'Blueming'),
    ('BLACKPINK - DDU-DU DDU-DU (뚜두뚜두)', 'DDU-DU DDU-DU'),
    ('Fine Thank You And You? - 10CM', 'Fine Thank You And You?'),
    ('aespa 에스파 - Supernova', 'Supernova'),
    ('NewJeans (뉴진스) - Ditto', 'Ditto'),
    ('(G)I-DLE - TOMBOY', 'TOMBOY'),
    ('PSY - GANGNAM STYLE (강남스타일)', 'GANGNAM STYLE'),
    ('BIGBANG - BANG BANG BANG', 'BANG BANG BANG'),
    ('BIGBANG - BLUE', 'BLUE'),
    ('Red Velvet - Red Flavor (빨간 맛)', 'Red Flavor'),
]

print('=' * 80)
print('Title Extraction Test Results')
print('=' * 80)
passed = 0
failed = 0
for youtube_title, expected in test_cases:
    extracted, method = matcher.extract_song_title(youtube_title)
    status = 'PASS' if extracted == expected else 'FAIL'
    if extracted != expected:
        failed += 1
    else:
        passed += 1
    print('[{}] {}...'.format(status, youtube_title[:55]))
    print('    Expected: {} | Got: {} ({})'.format(expected, extracted, method))
    print()

print('=' * 80)
print('Results: {} passed, {} failed'.format(passed, failed))
print('=' * 80)
