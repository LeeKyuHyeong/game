"""
Song Integrity MCP - Configuration
운영 DB 접속 정보 및 공통 설정
"""
import os

# Database Configuration
DB_CONFIG = {
    "host": os.getenv("MARIADB_HOST", "203.245.28.199"),
    "port": int(os.getenv("MARIADB_PORT", "3308")),
    "user": os.getenv("MARIADB_USER", "root"),
    "password": os.getenv("MARIADB_PASSWORD", "Olympus2426!"),
    "database": os.getenv("MARIADB_DATABASE", "song"),
    "charset": "utf8mb4"
}

# YouTube API (optional, for enhanced search)
YOUTUBE_API_KEY = os.getenv("YOUTUBE_API_KEY", "")

# Agent Names
AGENT_NAMES = {
    "data_manager": "DataManager",
    "youtube_searcher": "YouTubeSearcher",
    "data_verifier": "DataVerifier",
    "data_comparator": "DataComparator",
    "search_helper": "SearchHelper",
    "decision_advisor": "DecisionAdvisor",
    "db_updater": "DBUpdater"
}

# Logging
LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")
