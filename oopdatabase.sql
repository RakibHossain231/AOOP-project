-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Oct 20, 2025 at 03:07 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `oopdatabase`
--

-- --------------------------------------------------------

--
-- Table structure for table `conversations`
--

CREATE TABLE `conversations` (
  `id` int(11) NOT NULL,
  `room_key` varchar(64) NOT NULL,
  `title` varchar(128) NOT NULL,
  `is_group` tinyint(1) NOT NULL DEFAULT 1,
  `created_by` int(11) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `coursemembers`
--

CREATE TABLE `coursemembers` (
  `CourseID` int(11) NOT NULL,
  `UserID` int(11) NOT NULL,
  `Role` enum('student','instructor') DEFAULT 'student',
  `JoinedAt` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `coursemembers`
--

INSERT INTO `coursemembers` (`CourseID`, `UserID`, `Role`, `JoinedAt`) VALUES
(1, 1, 'student', '2025-10-12 01:47:26'),
(1, 4, 'student', '2025-10-20 08:18:40'),
(1, 10, 'student', '2025-10-20 08:18:34'),
(3, 1, 'student', '2025-10-12 01:13:53'),
(3, 4, 'student', '2025-10-12 13:00:14'),
(3, 9, 'student', '2025-10-14 15:56:47'),
(3, 10, 'student', '2025-10-20 12:36:04'),
(4, 1, 'student', '2025-10-12 01:14:35'),
(4, 4, 'student', '2025-10-20 19:05:58'),
(5, 1, 'student', '2025-10-19 23:24:20');

-- --------------------------------------------------------

--
-- Table structure for table `coursemessages`
--

CREATE TABLE `coursemessages` (
  `MessageID` int(11) NOT NULL,
  `CourseID` int(11) NOT NULL,
  `SenderID` int(11) NOT NULL,
  `MessageType` enum('text','pdf','image','sticker') DEFAULT 'text',
  `Content` text DEFAULT NULL,
  `FilePath` varchar(255) DEFAULT NULL,
  `SentAt` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `coursemessages`
--

INSERT INTO `coursemessages` (`MessageID`, `CourseID`, `SenderID`, `MessageType`, `Content`, `FilePath`, `SentAt`) VALUES
(1, 3, 1, 'text', 'Hi', NULL, '2025-10-12 01:14:02'),
(2, 4, 1, 'text', 'YEs', NULL, '2025-10-12 01:14:38'),
(3, 4, 1, 'text', 'No', NULL, '2025-10-12 01:14:51'),
(4, 3, 1, 'text', 'hi', NULL, '2025-10-12 11:29:34'),
(5, 3, 1, 'text', 'hi', NULL, '2025-10-12 13:00:04'),
(6, 3, 4, 'text', 'YEs', NULL, '2025-10-12 13:00:30'),
(7, 3, 1, 'text', 'Rakib Test', NULL, '2025-10-12 13:01:27'),
(8, 3, 4, 'text', 'Tamim Test', NULL, '2025-10-12 13:01:33'),
(9, 3, 1, 'text', 'whats the formula for bells curve', NULL, '2025-10-12 13:02:43'),
(10, 3, 4, 'text', 'janina', NULL, '2025-10-12 13:02:47'),
(11, 3, 4, 'text', 'Okay', NULL, '2025-10-12 13:28:18'),
(12, 3, 1, 'text', 'Pore ditachi', NULL, '2025-10-12 13:28:36'),
(13, 3, 4, 'text', 'Ajke sir ki poraiche?', NULL, '2025-10-14 11:22:23'),
(14, 3, 1, 'text', 'NPV math', NULL, '2025-10-14 11:22:36'),
(15, 3, 9, 'text', 'hi', NULL, '2025-10-14 15:57:10'),
(16, 3, 1, 'text', 'helo', NULL, '2025-10-14 15:57:18'),
(17, 3, 1, 'text', 'hi', NULL, '2025-10-15 13:46:09'),
(18, 3, 4, 'text', 'hello', NULL, '2025-10-15 13:46:17'),
(19, 3, 4, 'text', 'hei tamim what\'s up?', NULL, '2025-10-15 21:53:06'),
(20, 3, 1, 'text', 'Yeah fine, what about you?', NULL, '2025-10-15 21:53:31'),
(21, 3, 4, 'text', 'could you please give me the today\'s lecture note?', NULL, '2025-10-15 21:54:01'),
(22, 3, 1, 'text', 'I didn\'t complete yet!!! after finished I will send you.', NULL, '2025-10-15 21:54:44'),
(23, 3, 4, 'text', 'Hi', NULL, '2025-10-17 12:29:30'),
(24, 3, 1, 'text', 'hello', NULL, '2025-10-17 12:29:40'),
(25, 3, 1, 'text', 'Yes', NULL, '2025-10-17 14:19:07'),
(26, 3, 4, 'text', 'No', NULL, '2025-10-17 14:19:12'),
(27, 1, 10, 'text', 'Hi', NULL, '2025-10-20 08:18:47'),
(28, 1, 4, 'text', 'Hello', NULL, '2025-10-20 08:18:54'),
(29, 3, 10, 'text', 'fgg', NULL, '2025-10-20 12:36:32'),
(30, 3, 4, 'text', 'tytytyty', NULL, '2025-10-20 12:36:41'),
(31, 4, 1, 'text', 'yes', NULL, '2025-10-20 19:05:16');

-- --------------------------------------------------------

--
-- Table structure for table `courses`
--

CREATE TABLE `courses` (
  `CourseID` int(11) NOT NULL,
  `CourseCode` varchar(20) NOT NULL,
  `CourseName` varchar(100) NOT NULL,
  `Semester` int(11) NOT NULL,
  `InstructorID` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `courses`
--

INSERT INTO `courses` (`CourseID`, `CourseCode`, `CourseName`, `Semester`, `InstructorID`) VALUES
(1, 'CSE101', 'Introduction to Programming', 1, NULL),
(2, 'CSE201', 'Data Structures', 2, NULL),
(3, 'MAT101', 'Calculus', 1, NULL),
(4, 'PHY101', 'Physics', 1, NULL),
(5, 'ENG101', 'English Composition', 1, NULL),
(6, 'CSE301', 'Algorithms', 3, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `expenses`
--

CREATE TABLE `expenses` (
  `ExpenseID` int(11) NOT NULL,
  `UserID` int(11) DEFAULT NULL,
  `Category` varchar(100) DEFAULT NULL,
  `Type` enum('income','expense') NOT NULL,
  `Amount` decimal(10,2) NOT NULL,
  `Description` text DEFAULT NULL,
  `Notes` text DEFAULT NULL,
  `ExpenseDate` date NOT NULL,
  `CreatedAt` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `expenses`
--

INSERT INTO `expenses` (`ExpenseID`, `UserID`, `Category`, `Type`, `Amount`, `Description`, `Notes`, `ExpenseDate`, `CreatedAt`) VALUES
(1, 4, 'General', 'expense', 20.00, 'Bus rent', NULL, '2025-10-01', '2025-10-04 00:16:27'),
(3, 4, 'Transport', 'expense', 300.00, 'Bus rent', 'Manik nagar to UIU', '2025-10-05', '2025-10-07 10:02:54'),
(4, 4, 'General', 'expense', 300.00, 'Food', 'Dinner', '2025-10-05', '2025-10-07 15:17:38'),
(5, 4, 'General', 'expense', 10000.00, 'Flat rent', '', '2025-10-08', '2025-10-08 08:03:09'),
(6, 1, 'Food', 'expense', 50.00, 'Breakfast', '', '2025-10-08', '2025-10-08 08:32:04'),
(7, 1, 'Study', 'expense', 2000.00, 'table', '', '2025-10-10', '2025-10-10 21:40:07'),
(8, 4, 'Food', 'expense', 40.00, 'Food', '', '2025-10-14', '2025-10-14 14:46:30'),
(9, 9, 'Transport', 'expense', 23.00, 'bus', '', '2025-10-14', '2025-10-14 15:55:23'),
(10, 1, 'Food', 'expense', 25.00, 'coffee', '', '2025-10-15', '2025-10-15 13:44:36'),
(11, 1, 'Transport', 'expense', 50.00, 'Bus rent', 'From Notun bazar', '2025-10-17', '2025-10-17 12:25:30'),
(13, 4, 'Other', 'expense', 3.00, 'Janina', '', '2025-10-17', '2025-10-17 14:26:55'),
(14, 1, 'Other', 'expense', 10.00, 'rrer', '', '2025-10-17', '2025-10-17 16:45:48'),
(15, 1, 'General', 'expense', 34.00, 'Food', '', '2025-10-20', '2025-10-20 19:04:39');

-- --------------------------------------------------------

--
-- Table structure for table `pomodorocycles`
--

CREATE TABLE `pomodorocycles` (
  `CycleID` int(11) NOT NULL,
  `SessionID` int(11) DEFAULT NULL,
  `CycleNumber` int(11) NOT NULL,
  `CycleType` enum('focus','short_break','long_break') NOT NULL,
  `PlannedDuration` int(11) NOT NULL,
  `ActualDuration` int(11) DEFAULT NULL,
  `StartTime` datetime DEFAULT NULL,
  `EndTime` datetime DEFAULT NULL,
  `TaskID` int(11) DEFAULT NULL,
  `Status` enum('completed','skipped','interrupted') DEFAULT 'completed',
  `GroupID` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `pomodorocycles`
--

INSERT INTO `pomodorocycles` (`CycleID`, `SessionID`, `CycleNumber`, `CycleType`, `PlannedDuration`, `ActualDuration`, `StartTime`, `EndTime`, `TaskID`, `Status`, `GroupID`) VALUES
(24, 11, 1, 'focus', 1, 1, '2025-10-20 12:32:47', '2025-10-20 12:33:01', 5, 'skipped', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `pomodorogroups`
--

CREATE TABLE `pomodorogroups` (
  `GroupID` int(11) NOT NULL,
  `UserID` int(11) NOT NULL,
  `Title` varchar(128) NOT NULL,
  `TotalMinutes` int(11) NOT NULL,
  `ElapsedSeconds` int(11) NOT NULL DEFAULT 0,
  `Status` enum('active','paused','completed') NOT NULL DEFAULT 'paused',
  `CreatedAt` timestamp NOT NULL DEFAULT current_timestamp(),
  `UpdatedAt` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `pomodorogrouptasks`
--

CREATE TABLE `pomodorogrouptasks` (
  `GroupID` int(11) NOT NULL,
  `TaskID` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `pomodorosessions`
--

CREATE TABLE `pomodorosessions` (
  `SessionID` int(11) NOT NULL,
  `UserID` int(11) DEFAULT NULL,
  `StartTime` datetime NOT NULL,
  `EndTime` datetime DEFAULT NULL,
  `Duration` int(11) NOT NULL,
  `BreakDuration` int(11) DEFAULT NULL,
  `Status` enum('active','completed','skipped','interrupted') NOT NULL DEFAULT 'active',
  `CompletedCycles` int(11) DEFAULT 0,
  `GroupID` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `pomodorosessions`
--

INSERT INTO `pomodorosessions` (`SessionID`, `UserID`, `StartTime`, `EndTime`, `Duration`, `BreakDuration`, `Status`, `CompletedCycles`, `GroupID`) VALUES
(1, 4, '2025-10-08 08:12:37', NULL, 0, NULL, 'completed', 0, NULL),
(2, 1, '2025-10-10 21:12:37', NULL, 0, NULL, 'completed', 0, NULL),
(5, 1, '2025-10-14 08:50:54', NULL, 0, NULL, 'completed', 7, NULL),
(6, 4, '2025-10-14 14:44:27', NULL, 0, NULL, 'completed', 0, NULL),
(7, 1, '2025-10-17 12:18:23', NULL, 0, NULL, 'completed', 7, NULL),
(8, 4, '2025-10-17 14:29:16', NULL, 0, NULL, 'completed', 0, NULL),
(9, 1, '2025-10-17 16:06:04', NULL, 0, NULL, 'completed', 0, NULL),
(11, 4, '2025-10-20 12:32:47', NULL, 0, NULL, 'completed', 0, NULL),
(12, 1, '2025-10-20 19:03:59', NULL, 0, NULL, 'completed', 0, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `pomodorosettings`
--

CREATE TABLE `pomodorosettings` (
  `UserID` int(11) NOT NULL,
  `FocusMinutes` int(11) NOT NULL DEFAULT 25,
  `ShortBreakMinutes` int(11) NOT NULL DEFAULT 5,
  `LongBreakMinutes` int(11) NOT NULL DEFAULT 15,
  `AutoStart` tinyint(1) NOT NULL DEFAULT 0,
  `UpdatedAt` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `tasks`
--

CREATE TABLE `tasks` (
  `TaskID` int(11) NOT NULL,
  `UserID` int(11) DEFAULT NULL,
  `Title` varchar(200) NOT NULL,
  `Description` text DEFAULT NULL,
  `Priority` enum('low','medium','high') DEFAULT 'medium',
  `Status` enum('pending','in-progress','completed') DEFAULT 'pending',
  `DueDate` date DEFAULT NULL,
  `CreatedAt` datetime DEFAULT current_timestamp(),
  `UpdatedAt` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tasks`
--

INSERT INTO `tasks` (`TaskID`, `UserID`, `Title`, `Description`, `Priority`, `Status`, `DueDate`, `CreatedAt`, `UpdatedAt`) VALUES
(1, 4, 'AOOP Project', NULL, 'medium', 'completed', '2025-10-09', '2025-10-08 08:03:58', '2025-10-08 08:04:56'),
(2, 4, 'Work on Remember Button', NULL, 'medium', 'in-progress', '2025-10-09', '2025-10-08 08:06:11', '2025-10-08 08:17:44'),
(3, 4, 'Work On forgot Password', NULL, 'medium', 'pending', '2025-10-10', '2025-10-08 08:06:49', '2025-10-08 08:17:37'),
(4, 4, 'CN Note', NULL, 'medium', 'in-progress', '2025-10-08', '2025-10-08 08:07:31', '2025-10-08 08:12:37'),
(5, 4, 'IPE note', NULL, 'medium', 'in-progress', '2025-10-08', '2025-10-08 08:07:55', '2025-10-20 12:33:01'),
(9, 1, 'aoop', NULL, 'medium', 'completed', '2025-10-07', '2025-10-08 13:59:28', '2025-10-11 14:44:11'),
(10, 1, 'SAD CT', NULL, 'medium', 'completed', NULL, '2025-10-10 22:56:42', '2025-10-11 14:44:49'),
(11, 1, 'SAD CT', NULL, 'medium', 'completed', '2025-10-10', '2025-10-10 22:56:52', '2025-10-14 08:50:54'),
(13, 1, 'Project update', NULL, 'medium', 'in-progress', '2025-10-14', '2025-10-14 08:56:35', '2025-10-19 20:12:31'),
(14, 1, 'CN LAB Assignment', NULL, 'medium', 'in-progress', NULL, '2025-10-14 08:56:53', '2025-10-20 19:03:59'),
(15, 1, 'CN Lab assignment', NULL, 'medium', 'completed', '2025-10-14', '2025-10-14 08:57:18', '2025-10-19 20:04:44'),
(16, 1, 'CN Theory Note', NULL, 'medium', 'pending', '2025-10-14', '2025-10-14 09:21:16', '2025-10-14 09:21:16'),
(17, 1, 'CN Theory Assignment', NULL, 'medium', 'in-progress', NULL, '2025-10-14 09:21:28', '2025-10-19 20:05:03'),
(18, 1, 'CN theory Assignment', NULL, 'medium', 'in-progress', '2025-10-14', '2025-10-14 09:22:16', '2025-10-17 12:24:23'),
(19, 4, 'LAb final update', NULL, 'medium', 'pending', '2025-10-14', '2025-10-14 14:41:12', '2025-10-14 14:41:12'),
(20, 4, 'Final', NULL, 'medium', 'pending', NULL, '2025-10-14 14:41:36', '2025-10-14 14:41:36'),
(21, 4, 'CN final exam', NULL, 'medium', 'completed', '2025-10-31', '2025-10-14 14:42:22', '2025-10-17 14:27:19'),
(22, 9, 'bus', NULL, 'medium', 'pending', '2025-10-01', '2025-10-14 15:54:50', '2025-10-14 15:54:50'),
(23, 9, 'eee', NULL, 'medium', 'pending', '2025-10-23', '2025-10-14 15:54:58', '2025-10-14 15:54:58'),
(24, 9, 'eee', NULL, 'medium', 'pending', '2025-10-14', '2025-10-14 15:55:04', '2025-10-14 15:55:04'),
(25, 9, 'Dummy Task', 'Auto-created by Pomodoro', 'low', 'in-progress', NULL, '2025-10-14 15:56:54', '2025-10-14 15:56:54'),
(26, 1, 'CN theory note', NULL, 'medium', 'in-progress', '2025-10-15', '2025-10-15 13:44:01', '2025-10-19 20:08:21'),
(27, 1, 'Dummy Task', 'Auto-created by Pomodoro', 'low', 'in-progress', NULL, '2025-10-17 13:05:12', '2025-10-17 13:05:12'),
(28, 4, 'Dummy Task', 'Auto-created by Pomodoro', 'low', 'in-progress', NULL, '2025-10-17 14:28:38', '2025-10-17 14:29:46'),
(29, 1, 'pppp', NULL, 'medium', 'in-progress', '2025-10-17', '2025-10-17 15:56:27', '2025-10-19 20:08:14'),
(31, 1, 'Dummy Task', 'Auto-created by Pomodoro', 'low', 'in-progress', NULL, '2025-10-17 16:05:41', '2025-10-17 16:06:04'),
(33, 1, 'rerer', NULL, 'medium', 'completed', '2025-10-17', '2025-10-17 16:45:25', '2025-10-17 16:46:24'),
(34, 1, 'Dummy Task', 'Auto-created by Pomodoro', 'low', 'in-progress', NULL, '2025-10-19 20:12:44', '2025-10-19 20:12:44'),
(35, 1, 'Dummy Task', 'Auto-created by Pomodoro', 'low', 'in-progress', NULL, '2025-10-19 20:12:49', '2025-10-19 20:12:49'),
(37, 1, 'sdsd', NULL, 'medium', 'pending', '2025-10-20', '2025-10-20 19:04:22', '2025-10-20 19:04:22');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `UserID` int(11) NOT NULL,
  `Username` varchar(50) NOT NULL,
  `Email` varchar(100) NOT NULL,
  `PasswordHash` varchar(255) NOT NULL,
  `FullName` varchar(100) DEFAULT NULL,
  `SecurityQuestion` varchar(150) NOT NULL,
  `SecurityAnswerHash` varchar(60) NOT NULL,
  `JoinDate` datetime DEFAULT current_timestamp(),
  `Status` enum('active','inactive','banned') DEFAULT 'active'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`UserID`, `Username`, `Email`, `PasswordHash`, `FullName`, `SecurityQuestion`, `SecurityAnswerHash`, `JoinDate`, `Status`) VALUES
(1, 'rakib231', 'rakibrazcse@gmail.com', '$2a$10$u0xSE9X7W7BRofohFSaq9e7XC6Q.3I/07aeor9StsB12gvXO.Ne6a', 'Rakib Hossain', 'Where Do you live?', '$2a$10$5cNF.zoiWrwrW63ssZIbCeuoBtDUr0Miq8egkOWI/2BB85hj8bK/m', '2025-10-02 20:34:48', 'active'),
(2, 'linkon123', 'linkon123@gmail.com', '$2a$10$CAsQNa3YED02s5UgAgXBJe67f4Omsn/Z9oiZu1SHTkeq4KYhMdYei', 'Linkon Penaru', 'Where Do you live?', '$2a$10$5cNF.zoiWrwrW63ssZIbCeuoBtDUr0Miq8egkOWI/2BB85hj8bK/m', '2025-10-02 22:41:02', 'active'),
(3, 'tamim123', 'tamim@gmail.com', '$2a$10$j0XtaNzqdxhA4XcHmkSdqO3IQer/QjyNdXT7LS8aq.SVHmwV/CxAq', 'Nur Uddin Tamim', 'Where Do you live?', '$2a$10$5cNF.zoiWrwrW63ssZIbCeuoBtDUr0Miq8egkOWI/2BB85hj8bK/m', '2025-10-03 11:22:18', 'active'),
(4, 'tamim111', 'tamim123@gmail.com', '$2a$10$cuB.yLB1stUOD6cBkV9dtOdsEQIBswxPOLjebD9qQaALKb34vCWS6', 'Tamim', 'Where Do you live?', '$2a$10$5cNF.zoiWrwrW63ssZIbCeuoBtDUr0Miq8egkOWI/2BB85hj8bK/m', '2025-10-03 15:16:18', 'active'),
(5, 'Nahid123', 'nahid123@gmail.com', '$2a$10$XcqDyfhnXmPcwKZ2mGPpluYMs/MejWnWscC.KlO/iZmaGUL7GsXQS', 'Nahid Hasan', 'Where Do you live?', '$2a$10$5cNF.zoiWrwrW63ssZIbCeuoBtDUr0Miq8egkOWI/2BB85hj8bK/m', '2025-10-07 11:20:01', 'active'),
(6, 'Opu123', 'opu123@gmail.com', '$2a$10$oTXeeKHnm8mv8VuSQGswaeCYfEwmO.n.txO2bOCLDEK2d8ghC/g0C', 'Opu', 'Where Do you live?', '$2a$10$5cNF.zoiWrwrW63ssZIbCeuoBtDUr0Miq8egkOWI/2BB85hj8bK/m', '2025-10-10 21:49:59', 'active'),
(7, 'raz231', 'raz@gmail.com', '$2a$10$oObxJZg7wYfA372/hnHSc.B8k6.QCqSYaTTr1g6mDES6NMhSv3RXi', 'Rakib Raz', 'What is your favorite teacher\'s name?', '$2a$10$7u4papGrcwIpIwTHxINiv.DebBDvgjD6MZkqgsV9gG/VgIPNmMHvq', '2025-10-12 14:17:23', 'active'),
(8, 'Nahida111', 'nahida@gmail.com', '$2a$10$9Xz5fYI7EO3I2hFlwH2G0.FIhuh8aknRUOXf9yCWnMRxKdhveAi6q', 'Nahida Akter', 'What city were you born in?', '$2a$10$vxlhS.bRlAL8ml2PK9DvZ.eyZglSifAfqUpQZUxwNLDZ//TGWU9PO', '2025-10-14 14:48:19', 'active'),
(9, 'rakib222', 'rakb123@gmail.com', '$2a$10$lqcGJLtHBd/WQu/9qroSuOhk9aNh9LKymDFbzuC51skmsilpr7j6m', 'rakih', 'Where Do you live?', '$2a$10$F7Qpjkodx/Go9WrhWPzIL.hL7OPJtPW5L8jDWlb4F4vlKuQatrxT2', '2025-10-14 15:53:43', 'active'),
(10, 'tamima123', 'tamima123@gmail.com', '$2a$10$QINrQxtXMegjwsq5jquiHeoKnT/GB8FARUr/tGNNZAdyjxRafI4Ey', 'Tamima Akter', 'What is your first pet\'s name?', '$2a$10$R21ex0fTiH4IFl02OZ3aRegAPXWN0.lVfXBe8q3lbVBKMVrxTGu8q', '2025-10-20 08:14:04', 'active');

-- --------------------------------------------------------

--
-- Table structure for table `usersettings`
--

CREATE TABLE `usersettings` (
  `UserID` int(11) NOT NULL,
  `FocusMinutes` int(11) NOT NULL DEFAULT 25,
  `ShortBreakMinutes` int(11) NOT NULL DEFAULT 5,
  `LongBreakMinutes` int(11) NOT NULL DEFAULT 15,
  `AutoStart` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `usersettings`
--

INSERT INTO `usersettings` (`UserID`, `FocusMinutes`, `ShortBreakMinutes`, `LongBreakMinutes`, `AutoStart`) VALUES
(1, 1, 1, 15, 1),
(4, 1, 1, 1, 1),
(9, 1, 5, 15, 1),
(10, 25, 5, 15, 0);

--
-- Indexes for dumped tables
--

--
-- Indexes for table `conversations`
--
ALTER TABLE `conversations`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `room_key` (`room_key`),
  ADD KEY `fk_conversations_created_by` (`created_by`);

--
-- Indexes for table `coursemembers`
--
ALTER TABLE `coursemembers`
  ADD PRIMARY KEY (`CourseID`,`UserID`),
  ADD KEY `UserID` (`UserID`);

--
-- Indexes for table `coursemessages`
--
ALTER TABLE `coursemessages`
  ADD PRIMARY KEY (`MessageID`),
  ADD KEY `CourseID` (`CourseID`),
  ADD KEY `SenderID` (`SenderID`);

--
-- Indexes for table `courses`
--
ALTER TABLE `courses`
  ADD PRIMARY KEY (`CourseID`),
  ADD UNIQUE KEY `CourseCode` (`CourseCode`),
  ADD KEY `InstructorID` (`InstructorID`);

--
-- Indexes for table `expenses`
--
ALTER TABLE `expenses`
  ADD PRIMARY KEY (`ExpenseID`),
  ADD KEY `UserID` (`UserID`);

--
-- Indexes for table `pomodorocycles`
--
ALTER TABLE `pomodorocycles`
  ADD PRIMARY KEY (`CycleID`),
  ADD KEY `SessionID` (`SessionID`),
  ADD KEY `fk_cycles_task` (`TaskID`),
  ADD KEY `fk_cycles_group` (`GroupID`);

--
-- Indexes for table `pomodorogroups`
--
ALTER TABLE `pomodorogroups`
  ADD PRIMARY KEY (`GroupID`),
  ADD KEY `idx_group_status_user` (`UserID`,`Status`);

--
-- Indexes for table `pomodorogrouptasks`
--
ALTER TABLE `pomodorogrouptasks`
  ADD PRIMARY KEY (`GroupID`,`TaskID`),
  ADD KEY `TaskID` (`TaskID`);

--
-- Indexes for table `pomodorosessions`
--
ALTER TABLE `pomodorosessions`
  ADD PRIMARY KEY (`SessionID`),
  ADD KEY `UserID` (`UserID`),
  ADD KEY `fk_sessions_group` (`GroupID`);

--
-- Indexes for table `pomodorosettings`
--
ALTER TABLE `pomodorosettings`
  ADD PRIMARY KEY (`UserID`);

--
-- Indexes for table `tasks`
--
ALTER TABLE `tasks`
  ADD PRIMARY KEY (`TaskID`),
  ADD KEY `UserID` (`UserID`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`UserID`),
  ADD UNIQUE KEY `Username` (`Username`),
  ADD UNIQUE KEY `Email` (`Email`),
  ADD UNIQUE KEY `uq_username` (`Username`),
  ADD UNIQUE KEY `uq_email` (`Email`);

--
-- Indexes for table `usersettings`
--
ALTER TABLE `usersettings`
  ADD PRIMARY KEY (`UserID`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `conversations`
--
ALTER TABLE `conversations`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `coursemessages`
--
ALTER TABLE `coursemessages`
  MODIFY `MessageID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=32;

--
-- AUTO_INCREMENT for table `courses`
--
ALTER TABLE `courses`
  MODIFY `CourseID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT for table `expenses`
--
ALTER TABLE `expenses`
  MODIFY `ExpenseID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;

--
-- AUTO_INCREMENT for table `pomodorocycles`
--
ALTER TABLE `pomodorocycles`
  MODIFY `CycleID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=25;

--
-- AUTO_INCREMENT for table `pomodorogroups`
--
ALTER TABLE `pomodorogroups`
  MODIFY `GroupID` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `pomodorosessions`
--
ALTER TABLE `pomodorosessions`
  MODIFY `SessionID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=13;

--
-- AUTO_INCREMENT for table `tasks`
--
ALTER TABLE `tasks`
  MODIFY `TaskID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=38;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `UserID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `conversations`
--
ALTER TABLE `conversations`
  ADD CONSTRAINT `fk_conversations_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`UserID`) ON DELETE SET NULL;

--
-- Constraints for table `coursemembers`
--
ALTER TABLE `coursemembers`
  ADD CONSTRAINT `coursemembers_ibfk_1` FOREIGN KEY (`CourseID`) REFERENCES `courses` (`CourseID`),
  ADD CONSTRAINT `coursemembers_ibfk_2` FOREIGN KEY (`UserID`) REFERENCES `users` (`UserID`);

--
-- Constraints for table `coursemessages`
--
ALTER TABLE `coursemessages`
  ADD CONSTRAINT `coursemessages_ibfk_1` FOREIGN KEY (`CourseID`) REFERENCES `courses` (`CourseID`),
  ADD CONSTRAINT `coursemessages_ibfk_2` FOREIGN KEY (`SenderID`) REFERENCES `users` (`UserID`);

--
-- Constraints for table `courses`
--
ALTER TABLE `courses`
  ADD CONSTRAINT `courses_ibfk_1` FOREIGN KEY (`InstructorID`) REFERENCES `users` (`UserID`);

--
-- Constraints for table `expenses`
--
ALTER TABLE `expenses`
  ADD CONSTRAINT `expenses_ibfk_1` FOREIGN KEY (`UserID`) REFERENCES `users` (`UserID`);

--
-- Constraints for table `pomodorocycles`
--
ALTER TABLE `pomodorocycles`
  ADD CONSTRAINT `fk_cycles_group` FOREIGN KEY (`GroupID`) REFERENCES `pomodorogroups` (`GroupID`),
  ADD CONSTRAINT `fk_cycles_task` FOREIGN KEY (`TaskID`) REFERENCES `tasks` (`TaskID`),
  ADD CONSTRAINT `fk_pomodoro_task` FOREIGN KEY (`TaskID`) REFERENCES `tasks` (`TaskID`),
  ADD CONSTRAINT `pomodorocycles_ibfk_1` FOREIGN KEY (`SessionID`) REFERENCES `pomodorosessions` (`SessionID`),
  ADD CONSTRAINT `pomodorocycles_ibfk_2` FOREIGN KEY (`TaskID`) REFERENCES `tasks` (`TaskID`),
  ADD CONSTRAINT `pomodorocycles_ibfk_3` FOREIGN KEY (`TaskID`) REFERENCES `tasks` (`TaskID`);

--
-- Constraints for table `pomodorogroups`
--
ALTER TABLE `pomodorogroups`
  ADD CONSTRAINT `pomodorogroups_ibfk_1` FOREIGN KEY (`UserID`) REFERENCES `users` (`UserID`);

--
-- Constraints for table `pomodorogrouptasks`
--
ALTER TABLE `pomodorogrouptasks`
  ADD CONSTRAINT `pomodorogrouptasks_ibfk_1` FOREIGN KEY (`GroupID`) REFERENCES `pomodorogroups` (`GroupID`) ON DELETE CASCADE,
  ADD CONSTRAINT `pomodorogrouptasks_ibfk_2` FOREIGN KEY (`TaskID`) REFERENCES `tasks` (`TaskID`);

--
-- Constraints for table `pomodorosessions`
--
ALTER TABLE `pomodorosessions`
  ADD CONSTRAINT `fk_sessions_group` FOREIGN KEY (`GroupID`) REFERENCES `pomodorogroups` (`GroupID`),
  ADD CONSTRAINT `pomodorosessions_ibfk_1` FOREIGN KEY (`UserID`) REFERENCES `users` (`UserID`);

--
-- Constraints for table `pomodorosettings`
--
ALTER TABLE `pomodorosettings`
  ADD CONSTRAINT `pomodorosettings_ibfk_1` FOREIGN KEY (`UserID`) REFERENCES `users` (`UserID`);

--
-- Constraints for table `tasks`
--
ALTER TABLE `tasks`
  ADD CONSTRAINT `tasks_ibfk_1` FOREIGN KEY (`UserID`) REFERENCES `users` (`UserID`);

--
-- Constraints for table `usersettings`
--
ALTER TABLE `usersettings`
  ADD CONSTRAINT `usersettings_ibfk_1` FOREIGN KEY (`UserID`) REFERENCES `users` (`UserID`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
