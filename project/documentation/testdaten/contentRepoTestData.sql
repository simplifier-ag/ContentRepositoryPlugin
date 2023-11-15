-- MySQL dump 10.13  Distrib 5.6.24, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: contentrepo
-- ------------------------------------------------------
-- Server version	5.6.26-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `contentfile`
--

DROP TABLE IF EXISTS `contentfile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `contentfile` (
  `file_name` varchar(128) COLLATE latin1_bin NOT NULL,
  `ext_file_id` varchar(128) COLLATE latin1_bin DEFAULT NULL,
  `permission_object_type` varchar(128) COLLATE latin1_bin NOT NULL,
  `folder_id` int(11) NOT NULL,
  `status_schema_id` varchar(128) COLLATE latin1_bin NOT NULL,
  `file_id` int(11) NOT NULL AUTO_INCREMENT,
  `security_schema_id` varchar(128) COLLATE latin1_bin NOT NULL,
  `status_id` varchar(128) COLLATE latin1_bin NOT NULL,
  `permission_object_id` varchar(128) COLLATE latin1_bin NOT NULL,
  PRIMARY KEY (`file_id`),
  KEY `ContentFileFK2` (`folder_id`),
  CONSTRAINT `ContentFileFK2` FOREIGN KEY (`folder_id`) REFERENCES `contentfolder` (`folder_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=latin1 COLLATE=latin1_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `contentfile`
--

LOCK TABLES `contentfile` WRITE;
/*!40000 ALTER TABLE `contentfile` DISABLE KEYS */;
INSERT INTO `contentfile` VALUES ('hello.txt',NULL,'App',1,'Default',1,'private','Default','123'),('private.html',NULL,'App',4,'Default',2,'private','Default','123'),('public.html',NULL,'App',4,'Default',3,'public','Default','123'),('test.txt',NULL,'Session',3,'Default',4,'private','Default','abc');
/*!40000 ALTER TABLE `contentfile` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `contentfolder`
--

DROP TABLE IF EXISTS `contentfolder`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `contentfolder` (
  `folder_description` varchar(128) COLLATE latin1_bin NOT NULL,
  `permission_object_type` varchar(128) COLLATE latin1_bin NOT NULL,
  `status_schema_id` varchar(128) COLLATE latin1_bin NOT NULL,
  `folder_name` varchar(128) COLLATE latin1_bin NOT NULL,
  `folder_id` int(11) NOT NULL AUTO_INCREMENT,
  `security_schema_id` varchar(128) COLLATE latin1_bin NOT NULL,
  `status_id` varchar(128) COLLATE latin1_bin NOT NULL,
  `content_id` int(11) NOT NULL,
  `permission_object_id` varchar(128) COLLATE latin1_bin NOT NULL,
  `parent_folder` int(11) DEFAULT NULL,
  `current_status` varchar(128) COLLATE latin1_bin NOT NULL,
  PRIMARY KEY (`folder_id`),
  KEY `ContentFolderFK1` (`content_id`),
  KEY `ContentFolderFK3` (`parent_folder`),
  CONSTRAINT `ContentFolderFK1` FOREIGN KEY (`content_id`) REFERENCES `contentrepository` (`content_id`),
  CONSTRAINT `ContentFolderFK3` FOREIGN KEY (`parent_folder`) REFERENCES `contentfolder` (`folder_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=latin1 COLLATE=latin1_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `contentfolder`
--

LOCK TABLES `contentfolder` WRITE;
/*!40000 ALTER TABLE `contentfolder` DISABLE KEYS */;
INSERT INTO `contentfolder` VALUES ('This is a test folder','App','Default','TestFolder',1,'private','Default',1,'123',NULL,'Default'),('This is a parent folder','App','Default','TestParent',2,'private','Default',1,'123',NULL,'Default'),('This is a subfolder','App','Default','TestSubfolder',3,'private','Default',1,'123',1,'Default'),('This is a public folder','App','Default','TestPublic',4,'public','Default',1,'123',NULL,'Default');
/*!40000 ALTER TABLE `contentfolder` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `contentrepository`
--

DROP TABLE IF EXISTS `contentrepository`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `contentrepository` (
  `name` varchar(128) COLLATE latin1_bin NOT NULL,
  `permission_object_type` varchar(128) COLLATE latin1_bin NOT NULL,
  `provider` varchar(128) COLLATE latin1_bin NOT NULL,
  `content_id` int(11) NOT NULL AUTO_INCREMENT,
  `permission_object_id` varchar(128) COLLATE latin1_bin NOT NULL,
  PRIMARY KEY (`content_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=latin1 COLLATE=latin1_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `contentrepository`
--

LOCK TABLES `contentrepository` WRITE;
/*!40000 ALTER TABLE `contentrepository` DISABLE KEYS */;
INSERT INTO `contentrepository` VALUES ('TestRepo','App','FileSystem',1,'123');
/*!40000 ALTER TABLE `contentrepository` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `contentrepositoryconfiguration`
--

DROP TABLE IF EXISTS `contentrepositoryconfiguration`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `contentrepositoryconfiguration` (
  `configkey` varchar(128) COLLATE latin1_bin NOT NULL,
  `content_configuation_id` int(11) NOT NULL AUTO_INCREMENT,
  `content_id` int(11) NOT NULL,
  `value` varchar(128) COLLATE latin1_bin NOT NULL,
  PRIMARY KEY (`content_configuation_id`),
  KEY `ContentRepositoryConfigurationFK4` (`content_id`),
  CONSTRAINT `ContentRepositoryConfigurationFK4` FOREIGN KEY (`content_id`) REFERENCES `contentrepository` (`content_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=latin1 COLLATE=latin1_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `contentrepositoryconfiguration`
--

LOCK TABLES `contentrepositoryconfiguration` WRITE;
/*!40000 ALTER TABLE `contentrepositoryconfiguration` DISABLE KEYS */;
INSERT INTO `contentrepositoryconfiguration` VALUES ('basedir',1,1,'target/testrepo');
/*!40000 ALTER TABLE `contentrepositoryconfiguration` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mimemapping`
--

DROP TABLE IF EXISTS `mimemapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mimemapping` (
  `mime_type` varchar(128) COLLATE latin1_bin NOT NULL,
  `mime_mapping_id` int(11) NOT NULL AUTO_INCREMENT,
  `ext` varchar(128) COLLATE latin1_bin NOT NULL,
  PRIMARY KEY (`mime_mapping_id`),
  UNIQUE KEY `idx2ddb05d3` (`ext`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=latin1 COLLATE=latin1_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mimemapping`
--

LOCK TABLES `mimemapping` WRITE;
/*!40000 ALTER TABLE `mimemapping` DISABLE KEYS */;
INSERT INTO `mimemapping` VALUES ('application/pdf',1,'pdf'),('text/plain',2,'txt'),('text/html',3,'html');
/*!40000 ALTER TABLE `mimemapping` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2015-11-12 12:34:13
