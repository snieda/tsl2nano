CREATE TABLE `party` (
  `id` INTEGER PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL,
  `description` VARCHAR(255) NOT NULL,
  `birthday` VARCHAR(255) NOT NULL
);

CREATE TABLE `address` (
  `id` INTEGER PRIMARY KEY AUTO_INCREMENT,
  `party` INTEGER NOT NULL,
  `code` VARCHAR(255) NOT NULL,
  `city` VARCHAR(255) NOT NULL,
  `country` VARCHAR(255) NOT NULL
);

CREATE INDEX `idx_address__party` ON `address` (`party`);

ALTER TABLE `address` ADD CONSTRAINT `fk_address__party` FOREIGN KEY (`party`) REFERENCES `party` (`id`);

CREATE TABLE `project` (
  `id` INTEGER PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `description` VARCHAR(255) NOT NULL
);

CREATE TABLE `party_project` (
  `party` INTEGER NOT NULL,
  `project` INTEGER NOT NULL,
  PRIMARY KEY (`party`, `project`)
);

CREATE INDEX `idx_party_project` ON `party_project` (`project`);

ALTER TABLE `party_project` ADD CONSTRAINT `fk_party_project__party` FOREIGN KEY (`party`) REFERENCES `party` (`id`);

ALTER TABLE `party_project` ADD CONSTRAINT `fk_party_project__project` FOREIGN KEY (`project`) REFERENCES `project` (`id`);

CREATE TABLE `request` (
  `id` INTEGER PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(255) UNIQUE NOT NULL,
  `description` VARCHAR(255) NOT NULL,
  `location` VARCHAR(255) NOT NULL,
  `start` VARCHAR(255) NOT NULL,
  `end` VARCHAR(255) NOT NULL
);

CREATE TABLE `skill` (
  `id` INTEGER PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `description` VARCHAR(255) NOT NULL
);

CREATE TABLE `priority` (
  `id` INTEGER PRIMARY KEY AUTO_INCREMENT,
  `value` VARCHAR(255) NOT NULL,
  `request` INTEGER NOT NULL,
  `skill` INTEGER NOT NULL
);

CREATE INDEX `idx_priority__request` ON `priority` (`request`);

CREATE INDEX `idx_priority__skill` ON `priority` (`skill`);

ALTER TABLE `priority` ADD CONSTRAINT `fk_priority__request` FOREIGN KEY (`request`) REFERENCES `request` (`id`);

ALTER TABLE `priority` ADD CONSTRAINT `fk_priority__skill` FOREIGN KEY (`skill`) REFERENCES `skill` (`id`);

CREATE TABLE `project_skill` (
  `project` INTEGER NOT NULL,
  `skill` INTEGER NOT NULL,
  PRIMARY KEY (`project`, `skill`)
);

CREATE INDEX `idx_project_skill` ON `project_skill` (`skill`);

ALTER TABLE `project_skill` ADD CONSTRAINT `fk_project_skill__project` FOREIGN KEY (`project`) REFERENCES `project` (`id`);

ALTER TABLE `project_skill` ADD CONSTRAINT `fk_project_skill__skill` FOREIGN KEY (`skill`) REFERENCES `skill` (`id`);

CREATE TABLE `rating` (
  `id` INTEGER PRIMARY KEY AUTO_INCREMENT,
  `duration` VARCHAR(255) NOT NULL,
  `party` INTEGER NOT NULL,
  `skill` INTEGER NOT NULL
);

CREATE INDEX `idx_rating__party` ON `rating` (`party`);

CREATE INDEX `idx_rating__skill` ON `rating` (`skill`);

ALTER TABLE `rating` ADD CONSTRAINT `fk_rating__party` FOREIGN KEY (`party`) REFERENCES `party` (`id`);

ALTER TABLE `rating` ADD CONSTRAINT `fk_rating__skill` FOREIGN KEY (`skill`) REFERENCES `skill` (`id`);

commit;