--
-- Convert existing tables to UTF-8
--

ALTER TABLE `${prefix}MimeMapping` CONVERT TO CHARACTER SET utf8 COLLATE utf8_bin;
ALTER TABLE `${prefix}ContentRepository` CONVERT TO CHARACTER SET utf8 COLLATE utf8_bin;
ALTER TABLE `${prefix}ContentRepositoryConfig` CONVERT TO CHARACTER SET utf8 COLLATE utf8_bin;
ALTER TABLE `${prefix}ContentFolder` CONVERT TO CHARACTER SET utf8 COLLATE utf8_bin;
ALTER TABLE `${prefix}ContentFile` CONVERT TO CHARACTER SET utf8 COLLATE utf8_bin;