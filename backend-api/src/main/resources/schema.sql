-- ============================================================================
-- TikTok Scraper - PostgreSQL Schema v1.0
-- Complete database schema for social media tracking system
-- ============================================================================

-- ============================================================================
-- CORE TABLES
-- ============================================================================

-- Testers: Users who run the scraper extension
CREATE TABLE IF NOT EXISTS tester (
    id              SERIAL PRIMARY KEY,
    username        VARCHAR(100) NOT NULL UNIQUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_active_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tester_username ON tester(username);

-- Scraping Sessions: Track each scraping session
CREATE TABLE IF NOT EXISTS scraping_session (
    id              SERIAL PRIMARY KEY,
    session_uuid    UUID NOT NULL UNIQUE,
    tester_id       INTEGER NOT NULL REFERENCES tester(id) ON DELETE CASCADE,
    started_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at        TIMESTAMP,
    video_count     INTEGER DEFAULT 0,
    comment_count   INTEGER DEFAULT 0,
    interaction_count INTEGER DEFAULT 0
);

CREATE INDEX idx_session_tester ON scraping_session(tester_id);
CREATE INDEX idx_session_uuid ON scraping_session(session_uuid);

-- ============================================================================
-- CONTENT CREATOR TABLES
-- ============================================================================

-- Profiles: TikTok user profiles (content creators)
CREATE TABLE IF NOT EXISTS profile (
    id              SERIAL PRIMARY KEY,
    platform_handle VARCHAR(100) NOT NULL UNIQUE,
    display_name    VARCHAR(255),
    bio             TEXT,
    profile_link    VARCHAR(500),
    is_verified     BOOLEAN DEFAULT FALSE,
    follower_count  BIGINT,
    following_count BIGINT,
    likes_count     BIGINT,
    first_seen_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_profile_handle ON profile(platform_handle);
CREATE INDEX idx_profile_verified ON profile(is_verified);

-- ============================================================================
-- MEDIA METADATA TABLES
-- ============================================================================

-- Music: Audio tracks used in videos
CREATE TABLE IF NOT EXISTS music (
    id              SERIAL PRIMARY KEY,
    platform_id     VARCHAR(100) UNIQUE,
    name            VARCHAR(500),
    url             VARCHAR(1000),
    first_seen_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_music_platform_id ON music(platform_id);

-- Effect: Visual effects used in videos
CREATE TABLE IF NOT EXISTS effect (
    id              SERIAL PRIMARY KEY,
    platform_id     VARCHAR(100) UNIQUE,
    name            VARCHAR(500),
    url             VARCHAR(1000),
    first_seen_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_effect_platform_id ON effect(platform_id);

-- Hashtag: Tags used in video descriptions
CREATE TABLE IF NOT EXISTS hashtag (
    id              SERIAL PRIMARY KEY,
    tag             VARCHAR(200) NOT NULL UNIQUE,
    usage_count     INTEGER DEFAULT 1,
    first_seen_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_hashtag_tag ON hashtag(tag);

-- Place: Location tags in videos
CREATE TABLE IF NOT EXISTS place (
    id              SERIAL PRIMARY KEY,
    platform_id     VARCHAR(100) UNIQUE,
    name            VARCHAR(500),
    first_seen_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_place_platform_id ON place(platform_id);

-- ============================================================================
-- VIDEO TABLES
-- ============================================================================

-- Video: Main video content table
CREATE TABLE IF NOT EXISTS video (
    id              SERIAL PRIMARY KEY,
    platform_id     VARCHAR(100) NOT NULL UNIQUE,
    profile_id      INTEGER REFERENCES profile(id) ON DELETE SET NULL,
    music_id        INTEGER REFERENCES music(id) ON DELETE SET NULL,
    effect_id       INTEGER REFERENCES effect(id) ON DELETE SET NULL,
    place_id        INTEGER REFERENCES place(id) ON DELETE SET NULL,
    
    -- Content
    description     TEXT,
    video_url       VARCHAR(1000),
    
    -- Flags
    is_ad           BOOLEAN DEFAULT FALSE,
    is_live         BOOLEAN DEFAULT FALSE,
    is_ai           BOOLEAN DEFAULT FALSE,
    
    -- Timestamps
    first_seen_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_video_platform_id ON video(platform_id);
CREATE INDEX idx_video_profile ON video(profile_id);
CREATE INDEX idx_video_music ON video(music_id);
CREATE INDEX idx_video_is_ad ON video(is_ad);
CREATE INDEX idx_video_is_live ON video(is_live);

-- Video-Hashtag junction table (many-to-many)
CREATE TABLE IF NOT EXISTS video_hashtag (
    video_id        INTEGER NOT NULL REFERENCES video(id) ON DELETE CASCADE,
    hashtag_id      INTEGER NOT NULL REFERENCES hashtag(id) ON DELETE CASCADE,
    PRIMARY KEY (video_id, hashtag_id)
);

-- Video Stats: Historicized engagement metrics (always INSERT, never UPDATE)
CREATE TABLE IF NOT EXISTS video_stats (
    id              SERIAL PRIMARY KEY,
    video_id        INTEGER NOT NULL REFERENCES video(id) ON DELETE CASCADE,
    session_id      INTEGER REFERENCES scraping_session(id) ON DELETE SET NULL,
    
    -- Stats
    likes           BIGINT DEFAULT 0,
    comments        BIGINT DEFAULT 0,
    shares          BIGINT DEFAULT 0,
    saves           BIGINT DEFAULT 0,
    
    -- Raw display strings (for debugging)
    likes_raw       VARCHAR(50),
    comments_raw    VARCHAR(50),
    shares_raw      VARCHAR(50),
    saves_raw       VARCHAR(50),
    
    -- Timestamp
    captured_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_video_stats_video ON video_stats(video_id);
CREATE INDEX idx_video_stats_session ON video_stats(session_id);
CREATE INDEX idx_video_stats_captured ON video_stats(captured_at);

-- ============================================================================
-- COMMENT TABLES
-- ============================================================================

-- Comment: Comments on videos
CREATE TABLE IF NOT EXISTS comment (
    id                  SERIAL PRIMARY KEY,
    comment_hash        VARCHAR(100) NOT NULL UNIQUE,
    video_id            INTEGER NOT NULL REFERENCES video(id) ON DELETE CASCADE,
    author_handle       VARCHAR(100),
    author_name         VARCHAR(255),
    
    -- Content
    text_content        TEXT,
    all_text            TEXT,
    image_url           VARCHAR(1000),
    
    -- Engagement
    likes               INTEGER DEFAULT 0,
    liked_by_author     BOOLEAN DEFAULT FALSE,
    
    -- Timestamps
    captured_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_comment_video ON comment(video_id);
CREATE INDEX idx_comment_hash ON comment(comment_hash);
CREATE INDEX idx_comment_author ON comment(author_handle);

-- Comment Mention: Users mentioned in comments
CREATE TABLE IF NOT EXISTS comment_mention (
    id              SERIAL PRIMARY KEY,
    comment_id      INTEGER NOT NULL REFERENCES comment(id) ON DELETE CASCADE,
    display_name    VARCHAR(255),
    encoded_url     VARCHAR(500)
);

CREATE INDEX idx_comment_mention_comment ON comment_mention(comment_id);

-- ============================================================================
-- USER BEHAVIOR TABLES
-- ============================================================================

-- Interaction: Like/Save/Share actions by the tester
CREATE TABLE IF NOT EXISTS interaction (
    id              SERIAL PRIMARY KEY,
    session_id      INTEGER REFERENCES scraping_session(id) ON DELETE SET NULL,
    video_id        INTEGER REFERENCES video(id) ON DELETE CASCADE,
    
    -- Action details
    interaction_type VARCHAR(20) NOT NULL,  -- 'like', 'save', 'share'
    action          VARCHAR(20) NOT NULL,   -- 'initial', 'add', 'remove'
    is_active       BOOLEAN NOT NULL,
    
    -- Context
    context_type    VARCHAR(50),
    
    -- Timestamp
    performed_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_interaction_session ON interaction(session_id);
CREATE INDEX idx_interaction_video ON interaction(video_id);
CREATE INDEX idx_interaction_type ON interaction(interaction_type);

-- Watch Time: How long the tester watched each video
CREATE TABLE IF NOT EXISTS watch_time (
    id              SERIAL PRIMARY KEY,
    session_id      INTEGER REFERENCES scraping_session(id) ON DELETE SET NULL,
    video_id        INTEGER REFERENCES video(id) ON DELETE CASCADE,
    
    -- Duration
    duration_ms     BIGINT NOT NULL,
    
    -- Timestamp
    recorded_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_watch_time_session ON watch_time(session_id);
CREATE INDEX idx_watch_time_video ON watch_time(video_id);

-- Search Event: Search queries made by the tester
CREATE TABLE IF NOT EXISTS search_event (
    id              SERIAL PRIMARY KEY,
    session_id      INTEGER REFERENCES scraping_session(id) ON DELETE SET NULL,
    tester_id       INTEGER REFERENCES tester(id) ON DELETE CASCADE,
    
    -- Search details
    query           VARCHAR(500) NOT NULL,
    url             VARCHAR(1000),
    
    -- Timestamp
    searched_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_search_event_session ON search_event(session_id);
CREATE INDEX idx_search_event_tester ON search_event(tester_id);
CREATE INDEX idx_search_event_query ON search_event(query);

-- Profile Visit: When the tester visits a profile page
CREATE TABLE IF NOT EXISTS profile_visit (
    id              SERIAL PRIMARY KEY,
    session_id      INTEGER REFERENCES scraping_session(id) ON DELETE SET NULL,
    tester_id       INTEGER REFERENCES tester(id) ON DELETE CASCADE,
    profile_id      INTEGER REFERENCES profile(id) ON DELETE CASCADE,
    
    -- Context
    context_type    VARCHAR(50),
    referrer_url    VARCHAR(1000),
    
    -- Timestamp
    visited_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_profile_visit_session ON profile_visit(session_id);
CREATE INDEX idx_profile_visit_profile ON profile_visit(profile_id);
CREATE INDEX idx_profile_visit_tester ON profile_visit(tester_id);

-- ============================================================================
-- ANALYTICS VIEWS (Optional - for dashboard queries)
-- ============================================================================

-- View: Latest stats for each video
CREATE OR REPLACE VIEW v_video_latest_stats AS
SELECT DISTINCT ON (vs.video_id)
    v.id,
    v.platform_id,
    p.platform_handle AS author_handle,
    p.is_verified,
    v.description,
    v.is_ad,
    v.is_live,
    vs.likes,
    vs.comments,
    vs.shares,
    vs.saves,
    vs.captured_at AS stats_captured_at,
    v.first_seen_at
FROM video v
LEFT JOIN profile p ON v.profile_id = p.id
LEFT JOIN video_stats vs ON v.id = vs.video_id
ORDER BY vs.video_id, vs.captured_at DESC;

-- View: Video engagement summary
CREATE OR REPLACE VIEW v_video_engagement AS
SELECT 
    v.id,
    v.platform_id,
    p.platform_handle AS author_handle,
    COUNT(DISTINCT c.id) AS comment_count,
    COUNT(DISTINCT i.id) AS interaction_count,
    SUM(wt.duration_ms) AS total_watch_time_ms,
    COUNT(DISTINCT wt.id) AS watch_count
FROM video v
LEFT JOIN profile p ON v.profile_id = p.id
LEFT JOIN comment c ON v.id = c.video_id
LEFT JOIN interaction i ON v.id = i.video_id
LEFT JOIN watch_time wt ON v.id = wt.video_id
GROUP BY v.id, v.platform_id, p.platform_handle;

-- View: Top authors by video count
CREATE OR REPLACE VIEW v_top_authors AS
SELECT 
    p.id,
    p.platform_handle,
    p.display_name,
    p.is_verified,
    p.follower_count,
    COUNT(v.id) AS video_count,
    SUM(COALESCE(ls.likes, 0)) AS total_likes
FROM profile p
LEFT JOIN video v ON p.id = v.profile_id
LEFT JOIN LATERAL (
    SELECT likes FROM video_stats vs 
    WHERE vs.video_id = v.id 
    ORDER BY vs.captured_at DESC 
    LIMIT 1
) ls ON true
GROUP BY p.id, p.platform_handle, p.display_name, p.is_verified, p.follower_count
ORDER BY video_count DESC;
