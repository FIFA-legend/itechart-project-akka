CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT,
    login VARCHAR(64) NOT NULL,
    password_hash VARCHAR(256) NOT NULL,
    email VARCHAR(256) NOT NULL,
    role ENUM ('ADMIN', 'USER') DEFAULT 'USER',
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS countries (
	id BIGINT AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    continent VARCHAR(15) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS leagues (
	id BIGINT AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    country_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (country_id) REFERENCES countries(id)
);

CREATE TABLE IF NOT EXISTS seasons (
	id BIGINT AUTO_INCREMENT,
    name VARCHAR(5) NOT NULL,
    is_current BOOLEAN NOT NULL,
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS teams (
	id BIGINT AUTO_INCREMENT,
    name VARCHAR(20) NOT NULL,
    short_code VARCHAR(3) NOT NULL,
    logo VARCHAR(20) NOT NULL,
    country_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (country_id) REFERENCES countries(id)
);

CREATE TABLE IF NOT EXISTS formations (
	id BIGINT AUTO_INCREMENT,
    name VARCHAR(9) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS players (
	id BIGINT AUTO_INCREMENT,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    birthday DATE NOT NULL,
    age SMALLINT NOT NULL,
    weight SMALLINT,
    height SMALLINT,
    image VARCHAR(20),
    country_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (country_id) REFERENCES countries(id)
);

CREATE TABLE IF NOT EXISTS referees (
	id BIGINT AUTO_INCREMENT,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    image VARCHAR(20),
    country_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (country_id) REFERENCES countries(id)
);

CREATE TABLE IF NOT EXISTS venues (
	id BIGINT AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    capacity INT NOT NULL,
    city VARCHAR(50) NOT NULL,
    country_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (country_id) REFERENCES countries(id)
);

CREATE TABLE IF NOT EXISTS stats (
	id BIGINT AUTO_INCREMENT,
    ht_score VARCHAR(5),
    ft_score VARCHAR(5),
    et_score VARCHAR(5),
    pt_score VARCHAR(5),
    attencdance INT,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS matches (
	id BIGINT AUTO_INCREMENT,
    league_id BIGINT NOT NULL,
    season_id BIGINT NOT NULL,
    status_code SMALLINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    match_start DATETIME NOT NULL,
    home_team_id BIGINT NOT NULL,
    away_team_id BIGINT NOT NULL,
    venue_id BIGINT NOT NULL,
    referee_id BIGINT NOT NULL,
    stats_id BIGINT NOT NULL,
    home_team_formation_id BIGINT NOT NULL,
    away_team_formation_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (league_id) REFERENCES leagues(id),
    FOREIGN KEY (season_id) REFERENCES seasons(id),
    FOREIGN KEY (home_team_id) REFERENCES teams(id),
    FOREIGN KEY (away_team_id) REFERENCES teams(id),
    FOREIGN KEY (venue_id) REFERENCES venues(id),
    FOREIGN KEY (referee_id) REFERENCES referees(id),
    FOREIGN KEY (home_team_formation_id) REFERENCES formations(id),
    FOREIGN KEY (away_team_formation_id) REFERENCES formations(id)
);

CREATE TABLE IF NOT EXISTS players_in_matches (
	match_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    is_home_team BOOLEAN NOT NULL,
    PRIMARY KEY (match_id, player_id),
    FOREIGN KEY (match_id) REFERENCES matches (id),
    FOREIGN KEY (player_id) REFERENCES players (id)
);