SET FOREIGN_KEY_CHECKS=0;
 
/* Drop Tables */
DROP TABLE IF EXISTS `countries` CASCADE;
DROP TABLE IF EXISTS `events` CASCADE;
DROP TABLE IF EXISTS `formations` CASCADE;
DROP TABLE IF EXISTS `league_stats` CASCADE;
DROP TABLE IF EXISTS `leagues` CASCADE;
DROP TABLE IF EXISTS `match_stats` CASCADE;
DROP TABLE IF EXISTS `matches` CASCADE;
DROP TABLE IF EXISTS `player_stats` CASCADE;
DROP TABLE IF EXISTS `players` CASCADE;
DROP TABLE IF EXISTS `players_in_matches` CASCADE;
DROP TABLE IF EXISTS `referees` CASCADE;
DROP TABLE IF EXISTS `seasons` CASCADE;
DROP TABLE IF EXISTS `stages` CASCADE;
DROP TABLE IF EXISTS `teams` CASCADE;
DROP TABLE IF EXISTS `user_subscriptions_on_players` CASCADE;
DROP TABLE IF EXISTS `user_subscriptions_on_teams` CASCADE;
DROP TABLE IF EXISTS `users` CASCADE;
DROP TABLE IF EXISTS `venues` CASCADE;

/* Create Tables */
CREATE TABLE `countries`
(
	`id` SMALLINT NOT NULL AUTO_INCREMENT,
	`name` NVARCHAR(30) NOT NULL,
	`code` NVARCHAR(2) NOT NULL,
	`continent` NVARCHAR(15) NOT NULL,
	CONSTRAINT `PK_countries` PRIMARY KEY (`id` ASC)
);

CREATE TABLE `events`
(
	`id` BIGINT NOT NULL AUTO_INCREMENT,
	`name` ENUM ('Goal', 'Penalty', 'YellowCard', 'RedCard') NOT NULL,
	`minute` TINYINT NOT NULL,
	`player_id` BIGINT NOT NULL,
	`match_id` BIGINT NOT NULL,
	CONSTRAINT `PK_events` PRIMARY KEY (`id` ASC)
);

CREATE TABLE `formations`
(
	`id` TINYINT NOT NULL AUTO_INCREMENT,
	`name` NVARCHAR(9) NOT NULL,
	CONSTRAINT `PK_formations` PRIMARY KEY (`id` ASC)
);

CREATE TABLE `league_stats`
(
	`season_id` SMALLINT NOT NULL,
	`league_id` SMALLINT NOT NULL,
	`team_id` INT NOT NULL,
	`place` TINYINT NOT NULL,
	`points` SMALLINT NOT NULL,
	`scored_goals` SMALLINT NOT NULL,
	`conceded_goals` SMALLINT NOT NULL,
	`victories` TINYINT NOT NULL,
	`defeats` TINYINT NOT NULL,
	`draws` TINYINT NOT NULL,
	CONSTRAINT `PK_league_stats` PRIMARY KEY (`season_id` ASC, `league_id` ASC, `team_id` ASC)
);

CREATE TABLE `leagues`
(
	`id` SMALLINT NOT NULL AUTO_INCREMENT,
	`name` NVARCHAR(50) NOT NULL,
	`country_id` SMALLINT NULL,
	CONSTRAINT `PK_leagues` PRIMARY KEY (`id` ASC)
);

CREATE TABLE `match_stats`
(
	`id` BIGINT NOT NULL AUTO_INCREMENT,
	`half_time_home_team_score` TINYINT NULL,
	`half_time_away_team_score` TINYINT NULL,
	`full_time_home_team_score` TINYINT NULL,
	`full_time_away_team_score` TINYINT NULL,
	`extra_time_home_team_score` TINYINT NULL,
	`extra_time_away_team_score` TINYINT NULL,
	`penalty_home_team_score` TINYINT NULL,
	`penalty_away_team_score` TINYINT NULL,
	`attendance` INT NULL,
	CONSTRAINT `PK_match_stats` PRIMARY KEY (`id` ASC)
);

CREATE TABLE `matches`
(
	`id` BIGINT NOT NULL AUTO_INCREMENT,
	`season_id` SMALLINT NOT NULL,
	`league_id` SMALLINT NOT NULL,
	`stage_id` TINYINT NOT NULL,
	`status` NVARCHAR(20) NOT NULL,
	`start_date` DATE NOT NULL,
	`start_time` TIME NOT NULL,
	`home_team_id` INT NOT NULL,
	`away_team_id` INT NOT NULL,
	`venue_id` INT NOT NULL,
	`referee_id` INT NOT NULL,
	`match_stats_id` BIGINT NOT NULL,
	`home_team_formation_id` TINYINT NOT NULL,
	`away_team_formation_id` TINYINT NOT NULL,
	CONSTRAINT `PK_matches` PRIMARY KEY (`id` ASC)
);

CREATE TABLE `player_stats`
(
	`id` BIGINT NOT NULL AUTO_INCREMENT,
	`t-shirt_number` TINYINT NOT NULL,
	`position` NVARCHAR(3) NOT NULL,
	`start_minute` SMALLINT NOT NULL,
	`played_minutes` SMALLINT NOT NULL,
	`goals` TINYINT NOT NULL,
	`assists` TINYINT NOT NULL,
	`successful_tackles` TINYINT NOT NULL,
	`total_tackles` TINYINT NOT NULL,
	`successful_passes` TINYINT NOT NULL,
	`total_passes` TINYINT NOT NULL,
	`successful_dribblings` TINYINT NOT NULL,
	`total_dribblings` TINYINT NOT NULL,
	CONSTRAINT `PK_player_stats` PRIMARY KEY (`id` ASC)
);

CREATE TABLE `players`
(
	`id` BIGINT NOT NULL AUTO_INCREMENT,
	`first_name` NVARCHAR(30) NOT NULL,
	`last_name` NVARCHAR(30) NOT NULL,
	`birthday` DATE NOT NULL,
	`age` TINYINT NOT NULL,
	`weight` SMALLINT NULL,
	`height` SMALLINT NULL,
	`image` NVARCHAR(15) NULL,
	`country_id` SMALLINT NOT NULL,
	CONSTRAINT `PK_players` PRIMARY KEY (`id` ASC)
);

CREATE TABLE `players_in_matches`
(
	`match_id` BIGINT NOT NULL,
	`player_id` BIGINT NOT NULL,
	`is_home_team_player` BIT(1) NOT NULL,
	`player_stats_id` BIGINT NOT NULL,
	CONSTRAINT `PK_players_in_matches` PRIMARY KEY (`match_id` ASC, `player_id` ASC)
);

CREATE TABLE `referees`
(
	`id` INT NOT NULL AUTO_INCREMENT,
	`first_name` NVARCHAR(30) NOT NULL,
	`last_name` NVARCHAR(30) NOT NULL,
	`image` NVARCHAR(15) NULL,
	`country_id` SMALLINT NOT NULL,
	CONSTRAINT `PK_referees` PRIMARY KEY (`id` ASC)
);

CREATE TABLE `seasons`
(
	`id` SMALLINT NOT NULL AUTO_INCREMENT,
	`name` NVARCHAR(9) NOT NULL,
	`is_current` BIT(1) NOT NULL,
	`start_date` DATE NOT NULL,
	`end_date` DATE NOT NULL,
	CONSTRAINT `PK_seasons` PRIMARY KEY (`id` ASC)
);

CREATE TABLE `stages`
(
	`id` TINYINT NOT NULL AUTO_INCREMENT,
	`name` NVARCHAR(20) NOT NULL,
	CONSTRAINT `PK_stages` PRIMARY KEY (`id` ASC)
);

CREATE TABLE `teams`
(
	`id` INT NOT NULL AUTO_INCREMENT,
	`full_name` NVARCHAR(30) NOT NULL,
	`short_name` NVARCHAR(3) NOT NULL,
	`logo` NVARCHAR(15) NULL,
	`country_id` SMALLINT NOT NULL,
	CONSTRAINT `PK_teams` PRIMARY KEY (`id` ASC)
);

CREATE TABLE `user_subscriptions_on_players`
(
	`user_id` BIGINT NOT NULL,
	`player_id` BIGINT NOT NULL,
	CONSTRAINT `PK_user_subscriptions_on_players` PRIMARY KEY (`user_id` ASC, `player_id` ASC)
);

CREATE TABLE `user_subscriptions_on_teams`
(
	`user_id` BIGINT NOT NULL,
	`team_id` INT NOT NULL,
	CONSTRAINT `PK_user_subscriptions_on_teams` PRIMARY KEY (`user_id` ASC, `team_id` ASC)
);

CREATE TABLE `users`
(
	`id` BIGINT NOT NULL AUTO_INCREMENT,
	`login` NVARCHAR(64) NOT NULL,
	`password_hash` NVARCHAR(64) NOT NULL,
	`email` NVARCHAR(256) NOT NULL,
	`role` ENUM ('Admin', 'User') DEFAULT 'User' NOT NULL,
	CONSTRAINT `PK_users` PRIMARY KEY (`id` ASC)
);

CREATE TABLE `venues`
(
	`id` INT NOT NULL AUTO_INCREMENT,
	`name` NVARCHAR(50) NOT NULL,
	`capacity` INT NOT NULL,
	`city` NVARCHAR(25) NOT NULL,
	`country_id` SMALLINT NOT NULL,
	CONSTRAINT `PK_venues` PRIMARY KEY (`id` ASC)
);

/* Create Primary Keys, Indexes, Uniques, Checks */

ALTER TABLE `countries` ADD CONSTRAINT `UQ_country_name` UNIQUE (`name` ASC);
ALTER TABLE `countries` ADD CONSTRAINT `UQ_country_code` UNIQUE (`code` ASC);
ALTER TABLE `events` ADD INDEX `IXFK_events_matches` (`match_id` ASC);
ALTER TABLE `events` ADD INDEX `IXFK_events_players` (`player_id` ASC);
ALTER TABLE `formations` ADD CONSTRAINT `UQ_formation_name` UNIQUE (`name` ASC);
ALTER TABLE `league_stats` ADD INDEX `IXFK_league_stats_leagues` (`league_id` ASC);
ALTER TABLE `league_stats` ADD INDEX `IXFK_league_stats_seasons` (`season_id` ASC);
ALTER TABLE `league_stats` ADD INDEX `IXFK_league_stats_teams` (`team_id` ASC);
ALTER TABLE `leagues` ADD CONSTRAINT `UQ_league_name` UNIQUE (`name` ASC);
ALTER TABLE `leagues` ADD INDEX `IXFK_leagues_countries` (`country_id` ASC);
ALTER TABLE `matches` ADD INDEX `IXFK_matches_formations` (`home_team_formation_id` ASC);
ALTER TABLE `matches` ADD INDEX `IXFK_matches_formations_02` (`away_team_formation_id` ASC);
ALTER TABLE `matches` ADD INDEX `IXFK_matches_leagues` (`league_id` ASC);
ALTER TABLE `matches` ADD INDEX `IXFK_matches_match_stats` (`match_stats_id` ASC);
ALTER TABLE `matches` ADD INDEX `IXFK_matches_referees` (`referee_id` ASC);
ALTER TABLE `matches` ADD INDEX `IXFK_matches_seasons` (`season_id` ASC);
ALTER TABLE `matches` ADD INDEX `IXFK_matches_stages` (`stage_id` ASC);
ALTER TABLE `matches` ADD INDEX `IXFK_matches_teams` (`home_team_id` ASC);
ALTER TABLE `matches` ADD INDEX `IXFK_matches_teams_02` (`away_team_id` ASC);
ALTER TABLE `matches` ADD INDEX `IXFK_matches_venues` (`venue_id` ASC);
ALTER TABLE `players` ADD INDEX `IXFK_players_countries` (`country_id` ASC);
ALTER TABLE `players_in_matches` ADD INDEX `IXFK_players_in_matches_matches` (`match_id` ASC);
ALTER TABLE `players_in_matches` ADD INDEX `IXFK_players_in_matches_player_stats` (`player_stats_id` ASC);
ALTER TABLE `players_in_matches` ADD INDEX `IXFK_players_in_matches_players` (`player_id` ASC);
ALTER TABLE `referees` ADD INDEX `IXFK_referees_countries` (`country_id` ASC);
ALTER TABLE `seasons` ADD CONSTRAINT `UQ_season_name` UNIQUE (`name` ASC);
ALTER TABLE `stages` ADD CONSTRAINT `UQ_stage_name` UNIQUE (`name` ASC);
ALTER TABLE `teams` ADD INDEX `IXFK_teams_countries` (`country_id` ASC);
ALTER TABLE `user_subscriptions_on_players` ADD INDEX `IXFK_user_subscriptions_on_players_players` (`player_id` ASC);
ALTER TABLE `user_subscriptions_on_players` ADD INDEX `IXFK_user_subscriptions_on_players_users` (`user_id` ASC);
ALTER TABLE `user_subscriptions_on_teams` ADD INDEX `IXFK_user_subscriptions_on_teams_teams` (`team_id` ASC);
ALTER TABLE `user_subscriptions_on_teams` ADD INDEX `IXFK_user_subscriptions_on_teams_users` (`user_id` ASC);
ALTER TABLE `users` ADD CONSTRAINT `UQ_user_login` UNIQUE (`login` ASC);
ALTER TABLE `venues` ADD CONSTRAINT `UQ_venue_name` UNIQUE (`name` ASC);
ALTER TABLE `venues` ADD INDEX `IXFK_venues_countries` (`country_id` ASC);

/* Create Foreign Key Constraints */

ALTER TABLE `events` ADD CONSTRAINT `FK_events_matches`
	FOREIGN KEY (`match_id`) REFERENCES `matches` (`id`) ON DELETE Restrict ON UPDATE Restrict;

ALTER TABLE `events` ADD CONSTRAINT `FK_events_players`
	FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE Restrict ON UPDATE Restrict;

ALTER TABLE `league_stats` ADD CONSTRAINT `FK_league_stats_leagues`
	FOREIGN KEY (`league_id`) REFERENCES `leagues` (`id`) ON DELETE Restrict ON UPDATE Restrict;

ALTER TABLE `league_stats` ADD CONSTRAINT `FK_league_stats_seasons`
	FOREIGN KEY (`season_id`) REFERENCES `seasons` (`id`) ON DELETE Restrict ON UPDATE Restrict;

ALTER TABLE `league_stats` ADD CONSTRAINT `FK_league_stats_teams`
	FOREIGN KEY (`team_id`) REFERENCES `teams` (`id`) ON DELETE Restrict ON UPDATE Restrict;

ALTER TABLE `leagues` ADD CONSTRAINT `FK_leagues_countries`
	FOREIGN KEY (`country_id`) REFERENCES `countries` (`id`) ON DELETE Restrict ON UPDATE Restrict;

ALTER TABLE `matches` ADD CONSTRAINT `FK_matches_formations`
	FOREIGN KEY (`home_team_formation_id`) REFERENCES `formations` (`id`) ON DELETE Restrict ON UPDATE Restrict;

ALTER TABLE `matches` ADD CONSTRAINT `FK_matches_formations_02`
	FOREIGN KEY (`away_team_formation_id`) REFERENCES `formations` (`id`) ON DELETE Restrict ON UPDATE Restrict;

ALTER TABLE `matches` ADD CONSTRAINT `FK_matches_leagues`
	FOREIGN KEY (`league_id`) REFERENCES `leagues` (`id`) ON DELETE Restrict ON UPDATE Restrict;

ALTER TABLE `matches` ADD CONSTRAINT `FK_matches_match_stats`
	FOREIGN KEY (`match_stats_id`) REFERENCES `match_stats` (`id`) ON DELETE Restrict ON UPDATE Restrict;

ALTER TABLE `matches` ADD CONSTRAINT `FK_matches_referees`
	FOREIGN KEY (`referee_id`) REFERENCES `referees` (`id`) ON DELETE Restrict ON UPDATE Restrict;

ALTER TABLE `matches` ADD CONSTRAINT `FK_matches_seasons`
	FOREIGN KEY (`season_id`) REFERENCES `seasons` (`id`) ON DELETE Restrict ON UPDATE Restrict;

ALTER TABLE `matches` ADD CONSTRAINT `FK_matches_stages`
	FOREIGN KEY (`stage_id`) REFERENCES `stages` (`id`) ON DELETE Restrict ON UPDATE Restrict;

ALTER TABLE `matches` ADD CONSTRAINT `FK_matches_teams`
	FOREIGN KEY (`home_team_id`) REFERENCES `teams` (`id`) ON DELETE Restrict ON UPDATE Restrict;
    
ALTER TABLE `matches` ADD CONSTRAINT `FK_matches_teams_02`
	FOREIGN KEY (`away_team_id`) REFERENCES `teams` (`id`) ON DELETE Restrict ON UPDATE Restrict;

ALTER TABLE `matches` ADD CONSTRAINT `FK_matches_venues`
	FOREIGN KEY (`venue_id`) REFERENCES `venues` (`id`) ON DELETE Restrict ON UPDATE Restrict;

ALTER TABLE `players` ADD CONSTRAINT `FK_players_countries`
	FOREIGN KEY (`country_id`) REFERENCES `countries` (`id`) ON DELETE Restrict ON UPDATE Restrict;

ALTER TABLE `players_in_matches` ADD CONSTRAINT `FK_players_in_matches_matches`
	FOREIGN KEY (`match_id`) REFERENCES `matches` (`id`) ON DELETE Restrict ON UPDATE Restrict;

ALTER TABLE `players_in_matches` ADD CONSTRAINT `FK_players_in_matches_player_stats`
	FOREIGN KEY (`player_stats_id`) REFERENCES `player_stats` (`id`) ON DELETE Restrict ON UPDATE Restrict;

ALTER TABLE `players_in_matches` ADD CONSTRAINT `FK_players_in_matches_players`
	FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE Restrict ON UPDATE Restrict;

ALTER TABLE `referees` ADD CONSTRAINT `FK_referees_countries`
	FOREIGN KEY (`country_id`) REFERENCES `countries` (`id`) ON DELETE Restrict ON UPDATE Restrict;

ALTER TABLE `teams` ADD CONSTRAINT `FK_teams_countries`
	FOREIGN KEY (`country_id`) REFERENCES `countries` (`id`) ON DELETE Restrict ON UPDATE Restrict;

ALTER TABLE `user_subscriptions_on_players` ADD CONSTRAINT `FK_user_subscriptions_on_players_players`
	FOREIGN KEY (`player_id`) REFERENCES `players` (`id`) ON DELETE Restrict ON UPDATE Restrict;

ALTER TABLE `user_subscriptions_on_players` ADD CONSTRAINT `FK_user_subscriptions_on_players_users`
	FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE Restrict ON UPDATE Restrict;

ALTER TABLE `user_subscriptions_on_teams` ADD CONSTRAINT `FK_user_subscriptions_on_teams_teams`
	FOREIGN KEY (`team_id`) REFERENCES `teams` (`id`) ON DELETE Restrict ON UPDATE Restrict;

ALTER TABLE `user_subscriptions_on_teams` ADD CONSTRAINT `FK_user_subscriptions_on_teams_users`
	FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE Restrict ON UPDATE Restrict;

ALTER TABLE `venues` ADD CONSTRAINT `FK_venues_countries`
	FOREIGN KEY (`country_id`) REFERENCES `countries` (`id`) ON DELETE Restrict ON UPDATE Restrict;

SET FOREIGN_KEY_CHECKS=1; 