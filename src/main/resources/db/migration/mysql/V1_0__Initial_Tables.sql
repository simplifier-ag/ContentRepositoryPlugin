--
-- Data Model for Content Repository
--

--
-- Rename Existing tables (from pre-Flyway installations), so the data is kept
--

SELECT DATABASE() INTO @db_name FROM DUAL;

SELECT Count(*)
INTO @exists
FROM information_schema.tables
WHERE table_schema = @db_name
    AND table_type = 'BASE TABLE'
    AND table_name = 'ContentRepositoryConfiguration';

SET @query = If(@exists>0,
    'RENAME TABLE ContentRepositoryConfiguration TO ContentRepositoryConfig',
    'SELECT 1 from DUAL');

PREPARE stmt FROM @query;

EXECUTE stmt;


--
-- Table structure for table `MimeMapping`
--

CREATE TABLE IF NOT EXISTS `${prefix}MimeMapping` (
  `mime_mapping_id` int(11) NOT NULL AUTO_INCREMENT,
  `mime_type` varchar(128) NOT NULL,
  `ext` varchar(128) NOT NULL,
  PRIMARY KEY (`mime_mapping_id`),
  UNIQUE KEY `idx2ddb05d3` (`ext`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;


--
-- Table structure for table `ContentRepository`
--

CREATE TABLE IF NOT EXISTS `${prefix}ContentRepository` (
  `name` varchar(128) NOT NULL,
  `permission_object_type` varchar(128) NOT NULL,
  `provider` varchar(128) NOT NULL,
  `content_id` int(11) NOT NULL AUTO_INCREMENT,
  `permission_object_id` varchar(128) NOT NULL,
  PRIMARY KEY (`content_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;


--
-- Table structure for table `ContentRepositoryConfiguration`
--

CREATE TABLE IF NOT EXISTS `${prefix}ContentRepositoryConfig` (
  `configkey` varchar(128) NOT NULL,
  `content_configuation_id` int(11) NOT NULL AUTO_INCREMENT,
  `content_id` int(11) NOT NULL,
  `value` varchar(128) NOT NULL,
  PRIMARY KEY (`content_configuation_id`),
  KEY `ContentRepositoryConfigFK4` (`content_id`),
  CONSTRAINT `${prefix}ContentRepositoryConfigFK4` FOREIGN KEY (`content_id`) REFERENCES `${prefix}ContentRepository` (`content_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;


--
-- Table structure for table `ContentFolder`
--

CREATE TABLE IF NOT EXISTS `${prefix}ContentFolder` (
  `folder_description` varchar(128) NOT NULL,
  `permission_object_type` varchar(128) NOT NULL,
  `status_schema_id` varchar(128) NOT NULL,
  `folder_name` varchar(128) NOT NULL,
  `folder_id` int(11) NOT NULL AUTO_INCREMENT,
  `security_schema_id` varchar(128) NOT NULL,
  `status_id` varchar(128) NOT NULL,
  `content_id` int(11) NOT NULL,
  `permission_object_id` varchar(128) NOT NULL,
  `parent_folder` int(11) DEFAULT NULL,
  `current_status` varchar(128) NOT NULL,
  PRIMARY KEY (`folder_id`),
  KEY `ContentFolderFK1` (`content_id`),
  KEY `ContentFolderFK3` (`parent_folder`),
  CONSTRAINT `${prefix}ContentFolderFK1` FOREIGN KEY (`content_id`) REFERENCES `${prefix}ContentRepository` (`content_id`),
  CONSTRAINT `${prefix}ContentFolderFK3` FOREIGN KEY (`parent_folder`) REFERENCES `${prefix}ContentFolder` (`folder_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

--
-- Table structure for table `ContentFile`
--


CREATE TABLE IF NOT EXISTS `${prefix}ContentFile` (
  `file_name` varchar(128) NOT NULL,
  `ext_file_id` varchar(128) DEFAULT NULL,
  `permission_object_type` varchar(128) NOT NULL,
  `folder_id` int(11) NOT NULL,
  `status_schema_id` varchar(128) NOT NULL,
  `file_id` int(11) NOT NULL AUTO_INCREMENT,
  `security_schema_id` varchar(128) NOT NULL,
  `status_id` varchar(128) NOT NULL,
  `permission_object_id` varchar(128) NOT NULL,
  PRIMARY KEY (`file_id`),
  KEY `ContentFileFK2` (`folder_id`),
  CONSTRAINT `${prefix}ContentFileFK2` FOREIGN KEY (`folder_id`) REFERENCES `${prefix}ContentFolder` (`folder_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
