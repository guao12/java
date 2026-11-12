mysqldump: [Warning] Using a password on the command line interface can be insecure.
-- MySQL dump 10.13  Distrib 8.0.39, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: dragon_raja
-- ------------------------------------------------------
-- Server version	8.0.39

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

--
-- Dumping data for table `dr_user`
--
-- WHERE:  id in (1,2,3,5,6,7,8)

LOCK TABLES `dr_user` WRITE;
/*!40000 ALTER TABLE `dr_user` DISABLE KEYS */;
INSERT INTO `dr_user` (`id`, `account`, `codename`, `password`, `identity`, `spirit`, `status`, `deleted`, `login_count`, `last_login_at`, `create_time`, `update_time`) VALUES (1,'CC20260001','路明非','$2a$10$6Hzup9QGKCA9Hx8S4ouXKuGOVV27/2z4cb1.zpPYHgVP0hagU.SZS','freshman','junyan',1,0,90,'2026-11-09 09:40:52','2026-10-23 14:45:24','2026-11-09 09:40:51');
INSERT INTO `dr_user` (`id`, `account`, `codename`, `password`, `identity`, `spirit`, `status`, `deleted`, `login_count`, `last_login_at`, `create_time`, `update_time`) VALUES (2,'CC20260002','楚子航','$2b$10$EhhQtz0J7vukNhH3zCoBiew.9/FOuG9I/pE3uKWRR.7JcZxq150ou','executor','shiling',1,0,10,'2026-11-09 09:32:34','2026-10-23 14:45:24','2026-11-09 09:32:34');
INSERT INTO `dr_user` (`id`, `account`, `codename`, `password`, `identity`, `spirit`, `status`, `deleted`, `login_count`, `last_login_at`, `create_time`, `update_time`) VALUES (3,'CC20260003','芬格尔','$2b$10$aamVUE83rOCVisNGuH2TbOJHUrEfbXNcNzvwSOsmnASalROlxKlAC','student','fengwang',1,0,2,'2026-11-09 08:58:52','2026-10-23 14:45:24','2026-11-09 08:58:52');
INSERT INTO `dr_user` (`id`, `account`, `codename`, `password`, `identity`, `spirit`, `status`, `deleted`, `login_count`, `last_login_at`, `create_time`, `update_time`) VALUES (5,'CC202600005','端到端己','$2a$10$3Uke4FV.FcQ7sgxj2VPI6uuQols2FLe49jhzKU1.f1nZJ44uIebZ2','freshman','none',1,0,1,'2026-11-02 09:33:46','2026-11-02 09:33:40','2026-11-02 09:33:45');
INSERT INTO `dr_user` (`id`, `account`, `codename`, `password`, `identity`, `spirit`, `status`, `deleted`, `login_count`, `last_login_at`, `create_time`, `update_time`) VALUES (6,'CC202600006','回归测试甲','$2a$10$03YqCwqxkhmo3ijDWV0./OdNH.agVvF2P5Y7K3FQgsVP6YNsgdmi6','freshman','none',1,0,0,NULL,'2026-11-02 09:39:03','2026-11-02 09:39:03');
INSERT INTO `dr_user` (`id`, `account`, `codename`, `password`, `identity`, `spirit`, `status`, `deleted`, `login_count`, `last_login_at`, `create_time`, `update_time`) VALUES (7,'CC202600007','冒烟测试丙','$2a$10$AooGQ4P4q1lNmnypjruzJe7YreFCISu20l1luqzZflOyAe75FptSS','freshman','junyan',1,0,1,'2026-11-04 16:56:01','2026-11-04 16:55:37','2026-11-04 16:56:00');
INSERT INTO `dr_user` (`id`, `account`, `codename`, `password`, `identity`, `spirit`, `status`, `deleted`, `login_count`, `last_login_at`, `create_time`, `update_time`) VALUES (8,'CC202600008','测试读者','$2a$10$wlQuNi8KtIfqhkHphKdwFucZcO8Pb8JwmJeK.yrzI0GMsLt3fnKW.','freshman','none',1,0,1,'2026-11-06 09:57:55','2026-11-06 09:57:43','2026-11-06 09:57:55');
/*!40000 ALTER TABLE `dr_user` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-12 14:55:08
mysqldump: [Warning] Using a password on the command line interface can be insecure.
-- MySQL dump 10.13  Distrib 8.0.39, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: dragon_raja
-- ------------------------------------------------------
-- Server version	8.0.39

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

--
-- Dumping data for table `dr_search_log`
--
-- WHERE:  user_id in (1,2,3,5,6,7,8)

LOCK TABLES `dr_search_log` WRITE;
/*!40000 ALTER TABLE `dr_search_log` DISABLE KEYS */;
INSERT INTO `dr_search_log` (`id`, `user_id`, `keyword`, `top_category`, `category_id`, `level`, `result_count`, `client_ip`, `create_time`) VALUES (15,1,'',NULL,NULL,NULL,18,'127.0.0.1','2026-11-02 09:00:16');
INSERT INTO `dr_search_log` (`id`, `user_id`, `keyword`, `top_category`, `category_id`, `level`, `result_count`, `client_ip`, `create_time`) VALUES (16,1,'',NULL,NULL,NULL,18,'127.0.0.1','2026-11-02 09:00:33');
INSERT INTO `dr_search_log` (`id`, `user_id`, `keyword`, `top_category`, `category_id`, `level`, `result_count`, `client_ip`, `create_time`) VALUES (17,1,'',NULL,NULL,NULL,18,'127.0.0.1','2026-11-02 09:00:51');
INSERT INTO `dr_search_log` (`id`, `user_id`, `keyword`, `top_category`, `category_id`, `level`, `result_count`, `client_ip`, `create_time`) VALUES (18,1,'',NULL,NULL,NULL,18,'127.0.0.1','2026-11-02 09:00:51');
INSERT INTO `dr_search_log` (`id`, `user_id`, `keyword`, `top_category`, `category_id`, `level`, `result_count`, `client_ip`, `create_time`) VALUES (19,1,'',NULL,NULL,NULL,18,'127.0.0.1','2026-11-02 09:03:15');
INSERT INTO `dr_search_log` (`id`, `user_id`, `keyword`, `top_category`, `category_id`, `level`, `result_count`, `client_ip`, `create_time`) VALUES (20,1,'',NULL,NULL,NULL,18,'127.0.0.1','2026-11-02 09:03:21');
INSERT INTO `dr_search_log` (`id`, `user_id`, `keyword`, `top_category`, `category_id`, `level`, `result_count`, `client_ip`, `create_time`) VALUES (21,1,'',NULL,NULL,NULL,18,'127.0.0.1','2026-11-02 09:03:32');
INSERT INTO `dr_search_log` (`id`, `user_id`, `keyword`, `top_category`, `category_id`, `level`, `result_count`, `client_ip`, `create_time`) VALUES (22,1,'',NULL,NULL,NULL,18,'127.0.0.1','2026-11-02 09:03:53');
INSERT INTO `dr_search_log` (`id`, `user_id`, `keyword`, `top_category`, `category_id`, `level`, `result_count`, `client_ip`, `create_time`) VALUES (23,1,'',NULL,NULL,NULL,18,'127.0.0.1','2026-11-02 09:04:10');
INSERT INTO `dr_search_log` (`id`, `user_id`, `keyword`, `top_category`, `category_id`, `level`, `result_count`, `client_ip`, `create_time`) VALUES (24,1,'',NULL,NULL,NULL,18,'127.0.0.1','2026-11-02 09:04:33');
INSERT INTO `dr_search_log` (`id`, `user_id`, `keyword`, `top_category`, `category_id`, `level`, `result_count`, `client_ip`, `create_time`) VALUES (25,1,'',NULL,NULL,NULL,18,'127.0.0.1','2026-11-02 09:04:40');
INSERT INTO `dr_search_log` (`id`, `user_id`, `keyword`, `top_category`, `category_id`, `level`, `result_count`, `client_ip`, `create_time`) VALUES (26,1,'',NULL,NULL,NULL,18,'127.0.0.1','2026-11-02 09:04:54');
INSERT INTO `dr_search_log` (`id`, `user_id`, `keyword`, `top_category`, `category_id`, `level`, `result_count`, `client_ip`, `create_time`) VALUES (27,1,'',NULL,NULL,NULL,18,'127.0.0.1','2026-11-02 09:05:19');
INSERT INTO `dr_search_log` (`id`, `user_id`, `keyword`, `top_category`, `category_id`, `level`, `result_count`, `client_ip`, `create_time`) VALUES (53,2,'龙',NULL,NULL,NULL,10,'127.0.0.1','2026-11-02 09:46:16');
/*!40000 ALTER TABLE `dr_search_log` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-12 14:55:08
