import sys
sys.stdout.reconfigure(encoding='utf-8')
import csv
from agents.data_manager import DataManager
from agents.answer_matcher import AnswerMatcher
from agents.youtube_searcher import YouTubeSearcher

# Initialize
dm = DataManager()
matcher = AnswerMatcher()
searcher = YouTubeSearcher()

# Get all songs with YouTube IDs
songs = dm.get_songs_with_youtube_id()
print(f'Total songs with YouTube IDs: {len(songs)}')

# Get all song IDs
song_ids = [s.id for s in songs]

# Process in batches of 100
batch_size = 100
all_results = []

for i in range(0, len(song_ids), batch_size):
    batch_ids = song_ids[i:i+batch_size]
    batch_num = i // batch_size + 1
    total_batches = (len(song_ids) + batch_size - 1) // batch_size
    print(f'Processing batch {batch_num}/{total_batches} (songs {i+1}-{min(i+batch_size, len(song_ids))})')

    # Get answers for this batch
    answers_batch = dm.get_songs_answers_batch(batch_ids)

    for song_id in batch_ids:
        song = dm.get_song_by_id(song_id)
        if not song:
            continue

        # Get YouTube info using search_by_video_id
        youtube_info = searcher.search_by_video_id(song.youtube_video_id)
        youtube_title = youtube_info.title if youtube_info else None

        if not youtube_title:
            all_results.append({
                'song_id': song_id,
                'artist': song.artist,
                'title': song.title,
                'youtube_title': '',
                'extracted_title': '',
                'primary_answer': answers_batch.get(song_id, {}).get('primary_answer', ''),
                'match_status': 'error',
                'similarity_score': 0,
                'method': 'no_youtube'
            })
            continue

        # Get answers
        answers = answers_batch.get(song_id, {'primary_answer': '', 'all_answers': []})

        # Analyze with AnswerMatcher
        result = matcher.analyze(
            song_id=song_id,
            youtube_title=youtube_title,
            primary_answer=answers.get('primary_answer', ''),
            all_answers=answers.get('all_answers', [])
        )

        all_results.append({
            'song_id': song_id,
            'artist': song.artist,
            'title': song.title,
            'youtube_title': youtube_title,
            'extracted_title': result.extracted_song_title,
            'primary_answer': answers.get('primary_answer', ''),
            'match_status': result.match_status,
            'similarity_score': result.similarity_score,
            'method': result.extraction_method
        })

# Summary
matched = sum(1 for r in all_results if r['match_status'] == 'matched')
partial = sum(1 for r in all_results if r['match_status'] == 'partial')
no_match = sum(1 for r in all_results if r['match_status'] == 'no_match')
error = sum(1 for r in all_results if r['match_status'] == 'error')

print()
print('=' * 80)
print('BATCH PROCESSING COMPLETE')
print('=' * 80)
print(f'Total: {len(all_results)}')
print(f'Matched: {matched} ({matched/len(all_results)*100:.1f}%)')
print(f'Partial: {partial} ({partial/len(all_results)*100:.1f}%)')
print(f'No Match: {no_match} ({no_match/len(all_results)*100:.1f}%)')
print(f'Error: {error} ({error/len(all_results)*100:.1f}%)')
print('=' * 80)

# Save to CSV
fieldnames = ['song_id', 'artist', 'title', 'youtube_title', 'extracted_title', 'primary_answer', 'match_status', 'similarity_score', 'method']

# Save no_match results
no_match_results = [r for r in all_results if r['match_status'] == 'no_match']
with open('정답매칭_불일치_v3.csv', 'w', encoding='utf-8-sig', newline='') as f:
    writer = csv.DictWriter(f, fieldnames=fieldnames)
    writer.writeheader()
    writer.writerows(no_match_results)
print(f'Saved {len(no_match_results)} no_match results to 정답매칭_불일치_v3.csv')

# Save partial results
partial_results = [r for r in all_results if r['match_status'] == 'partial']
with open('정답매칭_부분일치_v3.csv', 'w', encoding='utf-8-sig', newline='') as f:
    writer = csv.DictWriter(f, fieldnames=fieldnames)
    writer.writeheader()
    writer.writerows(partial_results)
print(f'Saved {len(partial_results)} partial results to 정답매칭_부분일치_v3.csv')

# Save all results
with open('정답매칭_전체_v3.csv', 'w', encoding='utf-8-sig', newline='') as f:
    writer = csv.DictWriter(f, fieldnames=fieldnames)
    writer.writeheader()
    writer.writerows(all_results)
print(f'Saved {len(all_results)} total results to 정답매칭_전체_v3.csv')
