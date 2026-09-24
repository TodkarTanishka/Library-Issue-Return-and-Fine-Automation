-- MySQL dump 10.13  Distrib 8.0.40, for Win64 (x86_64)
--
-- Host: localhost    Database: mmcoe_library
-- ------------------------------------------------------
-- Server version	26.7.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- GTID state at the beginning of the backup 
--


--
-- Current Database: `mmcoe_library`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `mmcoe_library` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `mmcoe_library`;

--
-- Table structure for table `admins`
--

DROP TABLE IF EXISTS `admins`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admins` (
  `id` varchar(20) NOT NULL,
  `full_name` varchar(100) NOT NULL,
  `email` varchar(150) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` varchar(30) NOT NULL DEFAULT 'admin',
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `admins`
--

LOCK TABLES `admins` WRITE;
/*!40000 ALTER TABLE `admins` DISABLE KEYS */;
INSERT INTO `admins` VALUES ('MMADMIN01','System Administrator','admin@mmcoe.edu.in','admin123','admin');
/*!40000 ALTER TABLE `admins` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `books`
--

DROP TABLE IF EXISTS `books`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `books` (
  `id` int NOT NULL AUTO_INCREMENT,
  `book_id` varchar(20) NOT NULL,
  `title` varchar(255) NOT NULL,
  `author` varchar(255) DEFAULT NULL,
  `isbn` varchar(20) DEFAULT NULL,
  `category` varchar(100) DEFAULT NULL,
  `total_quantity` int NOT NULL,
  `available_quantity` int NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `book_id` (`book_id`),
  UNIQUE KEY `isbn` (`isbn`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `books`
--

LOCK TABLES `books` WRITE;
/*!40000 ALTER TABLE `books` DISABLE KEYS */;
INSERT INTO `books` VALUES (1,'MMLIB101','The C Programming Language','Brian W. Kernighan, Dennis M. Ritchie','9780131103627','C Programming',5,5,'2026-09-19 15:57:21'),(2,'MMLIB102','Introduction to Algorithms, Fourth Edition','Thomas H. Cormen, Charles E. Leiserson, Ronald L. Rivest, Clifford Stein','9780262046305','Algorithms',8,8,'2026-09-19 15:57:21'),(3,'MMLIB103','Artificial Intelligence: A Modern Approach, 4th Edition','Stuart J. Russell, Peter Norvig','9780134610993','Artificial Intelligence',6,6,'2026-09-19 15:57:21'),(4,'MMLIB104','Pattern Recognition and Machine Learning','Christopher M. Bishop','9780387310732','Machine Learning',5,5,'2026-09-19 15:57:21'),(5,'MMLIB105','Data Mining: Concepts and Techniques, 3rd Edition','Jiawei Han, Micheline Kamber, Jian Pei','9780123814791','Data Mining',6,6,'2026-09-19 15:57:21'),(6,'MMLIB106','Computer Networks','Andrew S. Tanenbaum, David J. Wetherall','9780132126953','Computer Networks',8,8,'2026-09-19 15:57:21'),(7,'MMLIB107','Compilers: Principles, Techniques, and Tools','Alfred V. Aho, Monica S. Lam, Ravi Sethi, Jeffrey D. Ullman','9780321486813','Compiler Design',5,5,'2026-09-19 15:57:21'),(8,'MMLIB108','Fundamentals of Database Systems','Ramez Elmasri, Shamkant B. Navathe','9780133970777','DBMS',7,7,'2026-09-19 15:57:21'),(9,'MMLIB109','Modern Operating Systems','Andrew S. Tanenbaum, Herbert Bos','9780133591620','Operating Systems',6,6,'2026-09-19 15:57:21'),(10,'MMLIB110','Computer Organization and Architecture','William Stallings','9780134101613','Computer Architecture',6,6,'2026-09-19 15:57:21'),(11,'MMLIB111','Clean Code','Robert C. Martin','9780132350884','Software Engineering',5,5,'2026-09-19 15:57:21'),(12,'MMLIB112','Computer Architecture: A Quantitative Approach','John L. Hennessy, David A. Patterson','9780128119051','Computer Architecture',4,4,'2026-09-19 15:57:21'),(13,'MMLIB113','Operating System Concepts','Abraham Silberschatz, Peter B. Galvin, Greg Gagne','9781119456339','Operating Systems',6,6,'2026-09-19 15:57:21'),(14,'MMLIB114','Software Engineering','Ian Sommerville','9780133943030','Software Engineering',7,7,'2026-09-19 15:57:21'),(15,'MMLIB115','Deep Learning','Ian Goodfellow, Yoshua Bengio, Aaron Courville','9780262035613','Deep Learning',5,5,'2026-09-19 15:57:21'),(16,'MMLIB116','Python Crash Course','Eric Matthes','9781718502703','Python Programming',6,6,'2026-09-19 15:57:21'),(17,'MMLIB117','Java: The Complete Reference','Herbert Schildt','9781260440232','Java Programming',8,8,'2026-09-19 15:57:21'),(18,'MMLIB118','Web Technologies','Uttam K. Roy','9780199459750','Web Development',4,4,'2026-09-19 15:57:21'),(19,'MMLIB119','Artificial Intelligence','Elaine Rich, Kevin Knight','9780070522633','Artificial Intelligence',3,3,'2026-09-19 15:57:21');
/*!40000 ALTER TABLE `books` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `issued_books`
--

DROP TABLE IF EXISTS `issued_books`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `issued_books` (
  `id` int NOT NULL AUTO_INCREMENT,
  `student_username` varchar(150) NOT NULL,
  `book_id` int NOT NULL,
  `issue_date` date NOT NULL,
  `due_date` date NOT NULL,
  `status` enum('Issued','Returned','Overdue') NOT NULL DEFAULT 'Issued',
  PRIMARY KEY (`id`),
  KEY `book_id` (`book_id`),
  CONSTRAINT `issued_books_ibfk_1` FOREIGN KEY (`book_id`) REFERENCES `books` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `issued_books`
--

LOCK TABLES `issued_books` WRITE;
/*!40000 ALTER TABLE `issued_books` DISABLE KEYS */;
/*!40000 ALTER TABLE `issued_books` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `librarian`
--

DROP TABLE IF EXISTS `librarian`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `librarian` (
  `id` varchar(20) NOT NULL,
  `full_name` varchar(100) NOT NULL,
  `email` varchar(150) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` varchar(30) NOT NULL DEFAULT 'librarian',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `librarian`
--

LOCK TABLES `librarian` WRITE;
/*!40000 ALTER TABLE `librarian` DISABLE KEYS */;
INSERT INTO `librarian` VALUES ('MMLIB01','Librarian01','staff1@mmcoe.edu.in','staff123','librarian'),('MMLIB02','Librarian02','staff2@mmcoe.edu.in','staff124','librarian'),('MMLIB03','Librarian03','staff3@mmcoe.edu.in','staff125','librarian');
/*!40000 ALTER TABLE `librarian` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_code` varchar(20) DEFAULT NULL,
  `full_name` varchar(100) NOT NULL,
  `email` varchar(150) NOT NULL,
  `password` varchar(255) NOT NULL,
  `department` varchar(100) DEFAULT NULL,
  `graduation_year` int DEFAULT NULL,
  `division` varchar(20) DEFAULT NULL,
  `designation` varchar(150) DEFAULT NULL,
  `role` enum('student','faculty') NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`),
  UNIQUE KEY `user_code` (`user_code`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'CES101','Tanishka Todkar','tanishkatodkar2025.comp@mmcoe.edu.in','B25CE2006','Computer',2028,'II',NULL,'student','2026-09-19 15:56:31'),(2,'CES102','Anuja Salunkhe','anujasalunkhe2025.comp@mmcoe.edu.in','B25CE2007','Computer',2028,'II',NULL,'student','2026-09-19 15:56:31'),(3,'CES103','Munajja Dalimbkar','munajjadalimbkar2025.comp@mmcoe.edu.in','B25CE2011','Computer',2028,'II',NULL,'student','2026-09-19 15:56:31'),(4,'CES104','Reva Kulkarni','revakulkarni2024.comp@mmcoe.edu.in','B24CE1134','Computer',2028,'II',NULL,'student','2026-09-19 15:56:31'),(5,'CES105','Manasvi Lunawat','manasvilunavat2024.comp@mmcoe.edu.in','B24CE1136','Computer',2028,'II',NULL,'student','2026-09-19 15:56:31'),(6,'CES106','Anjali Deshmukh','anjalideshmukh2025.comp@mmcoe.edu.in','B25CE2001','Computer',2028,'II',NULL,'student','2026-09-19 15:56:31'),(7,'CES107','Arundhati Nath','arundhatinath2024.comp@mmcoe.edu.in','B24CE1137','Computer',2028,'II',NULL,'student','2026-09-19 15:56:31'),(8,'CES108','Arti Pokalwar','artipokalwar2024.comp@mmcoe.edu.in','B24CE1138','Computer',2028,'II',NULL,'student','2026-09-19 15:56:31'),(9,'CES109','Abitha S','abhithas2025.comp@mmcoe.edu.in','B25CE2015','Computer',2028,'II',NULL,'student','2026-09-19 15:56:31'),(10,'CES110','Kaif Untwale','kaifuntwale2025.comp@mmcoe.edu.in','B25CE2013','Computer',2028,'II',NULL,'student','2026-09-19 15:56:31'),(11,'CES111','Shubham Joshi','shubhamjoshi2021.comp@mmcoe.edu.in','72242371D','Computer',2028,'II',NULL,'student','2026-09-19 15:56:31'),(12,'CES112','Aditya Thore','adityathore2023.comp@mmcoe.edu.in','72318014J','Computer',2028,'II',NULL,'student','2026-09-19 15:56:31'),(13,'CES113','Swaroopa Dhepe','swaroopadhepe2024.comp@mmcoe.edu.in','B24CE1135','Computer',2028,'II',NULL,'student','2026-09-19 15:56:31'),(14,'CEFF101','Dr. Mrs. Sankirti Shiravale','sankirtihiravale@mmcoe.edu.in','sankriti@CEF101','Computer Engineering',NULL,NULL,'HoD(Comp)','faculty','2026-09-19 15:56:37'),(15,'CEFF102','Dr. Mrs. Anita Shinde','anitashinde@mmcoe.edu.in','anita@CEF102','Computer Engineering',NULL,NULL,'Assistant Professor','faculty','2026-09-19 15:56:37'),(16,'CEF103','Mrs. Rupali Dalvi','rupalidalvi@mmcoe.edu.in','rupali@CEF103','Computer Engineering',NULL,NULL,'Assistant Professor','faculty','2026-09-19 15:56:37'),(17,'CEF104','Mrs. Jagruti Wagh','jagrutiwagh@mmcoe.edu.in','jagruti@CEF104','Computer Engineering',NULL,NULL,'Assistant Professor','faculty','2026-09-19 15:56:37'),(18,'CEF105','Dr. Geetha Chilarge','geethachilarge@mmcoe.edu.in','geetha@CEF105','Computer Engineering',NULL,NULL,'Associate Professor','faculty','2026-09-19 15:56:37'),(19,'CEF106','Dr. Smita Chaudhari','smitachaudhari@mmcoe.edu.in','smita@CEF106','Computer Engineering',NULL,NULL,'Associate Professor','faculty','2026-09-19 15:56:37'),(20,'CEF107','Dr. Girija Chiddarwar','girijachiddarwar@mmcoe.edu.in','girija@CEF107','Computer Engineering',NULL,NULL,'Associate Professor','faculty','2026-09-19 15:56:37'),(21,'CEF108','Ms. Vandana Rupnar','vandanarupnar@mmcoe.edu.in','vandana@CEF108','Computer Engineering',NULL,NULL,'Assistant Professor and T&P Coordinator','faculty','2026-09-19 15:56:37'),(22,'CEF109','Mr. Rudragouda Patil','rudragoudapatil@mmcoe.edu.in','rudra@CEF109','Computer Engineering',NULL,NULL,'Assistant Professor and T&P Coordinator','faculty','2026-09-19 15:56:37'),(23,'CEF110','Dr. Neha Jain','nehajain@mmcoe.edu.in','neha@CEF110','Computer Engineering',NULL,NULL,'Assistant Professor','faculty','2026-09-19 15:56:37'),(24,'CEF111','Mrs. Snehal Kuche','snehalkuche@mmcoe.edu.in','sneha@CEF111','Computer Engineering',NULL,NULL,'Assistant Professor','faculty','2026-09-19 15:56:37'),(25,'CEF112','Mrs. Anupama Pandit','anupamapandit@mmcoe.edu.in','anupama@CEF112','Computer Engineering',NULL,NULL,'Assistant Professor','faculty','2026-09-19 15:56:37'),(26,'CEF113','Dr. Sarita Sapkal','saritasapkal@mmcoe.edu.in','sarita@CEF113','Computer Engineering',NULL,NULL,'Assistant Professor','faculty','2026-09-19 15:56:37'),(27,'CEF114','Ms. Vidya Menoki','vidyamenoki@mmcoe.edu.in','vidya@CEF114','Computer Engineering',NULL,NULL,'Assistant Professor','faculty','2026-09-19 15:56:37'),(28,'CEF115','Ms. Suvarna Patil','suvarnapatil@mmcoe.edu.in','suvarna@CEF115','Computer Engineering',NULL,NULL,'Assistant Professor','faculty','2026-09-19 15:56:37'),(29,'CEF116','Ms. Aishwarya Mane','aishwaryamane@mmcoe.edu.in','aishwarya@CEF116','Computer Engineering',NULL,NULL,'Assistant Professor','faculty','2026-09-19 15:56:37'),(30,'CEF117','Ms. Swarupa Deshpane','swarupadeshpane@mmcoe.edu.in','swarupa@CEF117','Computer Engineering',NULL,NULL,'Assistant Professor','faculty','2026-09-19 15:56:37'),(31,'CEF118','Ms. Reshma Kapadi','reshmakapadi@mmcoe.edu.in','reshma@CEF118','Computer Engineering',NULL,NULL,'Assistant Professor','faculty','2026-09-19 15:56:37');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-19 22:39:15
