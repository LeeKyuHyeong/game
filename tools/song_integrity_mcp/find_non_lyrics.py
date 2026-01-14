import sys
sys.stdout.reconfigure(encoding='utf-8')
import csv
import re
from agents.data_manager import DataManager
from agents.youtube_searcher import YouTubeSearcher

# Initialize
dm = DataManager()
searcher = YouTubeSearcher()

# Get all songs with YouTube IDs
songs = dm.get_songs_with_youtube_id()
print(f'Total songs with YouTube IDs: {len(songs)}')

# Keywords to search for (case insensitive)
LYRICS_KEYWORDS = ['lyrics', 'lyric', '가사', 'lyri']

def has_lyrics_keyword(title):
    """Check if title contains lyrics-related keywords"""
    if not title:
        return False
    title_lower = title.lower()
    for keyword in LYRICS_KEYWORDS:
        if keyword in title_lower:
            return True
    return False

# Process all songs
non_lyrics_songs = []
lyrics_songs = []
error_songs = []

batch_size = 100
for i in range(0, len(songs), batch_size):
    batch = songs[i:i+batch_size]
    batch_num = i // batch_size + 1
    total_batches = (len(songs) + batch_size - 1) // batch_size
    print(f'Processing batch {batch_num}/{total_batches} (songs {i+1}-{min(i+batch_size, len(songs))})')

    for song in batch:
        youtube_info = searcher.search_by_video_id(song.youtube_video_id)

        if not youtube_info or not youtube_info.title:
            error_songs.append({
                'song_id': song.id,
                'artist': song.artist,
                'title': song.title,
                'youtube_video_id': song.youtube_video_id,
                'youtube_title': '',
                'status': 'error'
            })
            continue

        youtube_title = youtube_info.title

        if has_lyrics_keyword(youtube_title):
            lyrics_songs.append({
                'song_id': song.id,
                'artist': song.artist,
                'title': song.title,
                'youtube_video_id': song.youtube_video_id,
                'youtube_title': youtube_title
            })
        else:
            non_lyrics_songs.append({
                'song_id': song.id,
                'artist': song.artist,
                'title': song.title,
                'youtube_video_id': song.youtube_video_id,
                'youtube_title': youtube_title
            })

# Summary
print()
print('=' * 80)
print('PROCESSING COMPLETE')
print('=' * 80)
print(f'Total processed: {len(songs)}')
print(f'With Lyrics keyword: {len(lyrics_songs)} ({len(lyrics_songs)/len(songs)*100:.1f}%)')
print(f'WITHOUT Lyrics keyword: {len(non_lyrics_songs)} ({len(non_lyrics_songs)/len(songs)*100:.1f}%)')
print(f'Errors: {len(error_songs)} ({len(error_songs)/len(songs)*100:.1f}%)')
print('=' * 80)

# Save non-lyrics songs to CSV
fieldnames = ['song_id', 'artist', 'title', 'youtube_video_id', 'youtube_title']

with open('가사없는영상.csv', 'w', encoding='utf-8-sig', newline='') as f:
    writer = csv.DictWriter(f, fieldnames=fieldnames)
    writer.writeheader()
    writer.writerows(non_lyrics_songs)
print(f'Saved {len(non_lyrics_songs)} non-lyrics songs to 가사없는영상.csv')

# Save error songs
if error_songs:
    with open('가사체크_에러.csv', 'w', encoding='utf-8-sig', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=['song_id', 'artist', 'title', 'youtube_video_id', 'youtube_title', 'status'])
        writer.writeheader()
        writer.writerows(error_songs)
    print(f'Saved {len(error_songs)} error songs to 가사체크_에러.csv')
