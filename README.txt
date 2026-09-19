# IMTPS

A desktop application that automatically schedules your daily tasks around your personal preferences, fixed appointments, and productivity windows. Built with JavaFX 21 and SQLite as the CSC3003S Capstone Project.

---

## Team NEN


Nadim Jakoot JKTNAD001 
Nur Baderoon BDRMUH003 
Ebrahim Harnekar HRNEBR001 

CSC3003S Capstone Project  
University of Cape Town  
**Git Repository:** https://gitlab.cs.uct.ac.za/jktnad001/csc3003s_capstone_imtps

---

## Overview

IMTPS is a personal scheduling assistant that takes the guesswork out of planning your day. Instead of manually assigning every task a time slot, you describe what needs to be done its importance, duration, category, and deadline and the app decides when to do it.

The scheduler respects:

- Your **working window** (e.g. 07:00 – 22:00), for scheduling floating tasks
- Your **daily productive-hours limit** (e.g. only 6 hours of Work per day)
- Your **fixed appointments** (pinned tasks)
- Your **current focus mode** (Work, Health, or Personal)

Three different scheduling algorithms are available, each optimised for a different planning style.

---

## Features

### Task Management
- **Pinned tasks** fixed appointments with a set start date and time. They are never moved by the scheduler.
- **Floating tasks** tasks with a start date and due date but no fixed time. The scheduler places them automatically.
- **Add, edit, complete, restore, and delete** tasks through the UI.
- Multi-day floating tasks are split across days if they don't fit in a single work window.
- Automatic **time-overlap detection** prevents double-booking.

### Three Schedulers

| Scheduler              | Best for          | Strategy                                                                                                                                     |
---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
| **Priority Scheduler** | Everyday use      | Scores each task using focus mode, duration preference, importance, and urgency (time until due). Highest-scoring tasks are scheduled first. |
| **Deadline Scheduler** | Deadline crunches | Strictly earliest-deadline-first. Ignores personal preferences so that nothing critical slips.                                               |
| **Knapsack Scheduler** | Maximising output | Finds the optimal subset of tasks that fits today's capacity based on importance, pushing the rest to later days.                            |

### Focus Modes
Toggle between **Work**, **Health**, and **Personal** in the View tab. Tasks in the selected category receive a priority boost in the Priority Scheduler.

### Onboarding Questionnaire
On first sign-up, you answer four quick questions that personalise the scheduler:

1. How many hours a day do you want to be productive? (1–10)
2. Do you prefer doing quick tasks first? (Yes/No)
3. When are you most productive? (Morning / Midday / Night)
4. What working window do you want tasks placed in? (start / end time)

Preferences are stored per user and can be updated at any time.

### Security
- Passwords are hashed with **PBKDF2WithHmacSHA1** using a random per-user salt (65,536 iterations, 128-bit key). Plaintext passwords are never written to the database.
- All SQL queries use **prepared statements**, protecting against SQL injection.
- Input validation on sign-up (email format, password strength).

### Completed Tasks
Mark tasks as complete from anywhere in the app. Completed tasks can be **restored** (with a new due date if overdue) or **permanently cleared** in bulk.

---

## Tech Stack

| Layer     | Technology                            |
-----------------------------------------------------
| Language  | Java 21                               |
| UI        | JavaFX 21.0.2 (FXML + CSS styling)    |
| Database  | SQLite 3 (via `sqlite-jdbc` 3.45.1.0) |
| Build Tool| Apache Maven 3.8+                     |
| Testing   | JUnit 5 (Jupiter)                     |
| Packaging | Maven Shade Plugin, JDK `jpackage`    |

---

## Getting Started

### Prerequisites

JDK 21 (LTS):Temurin, Oracle, or Microsoft OpenJDK. Must be a JDK, not a JRE. 
Apache Maven 3.8+: Needed to build and run the app. 
Internet connection: Only on the first run, to download dependencies. 

Verify your setup:

    java -version    # should show version 21
    mvn -version     # should show Maven 3.8 or higher

### Clone and Run

    git clone https://gitlab.cs.uct.ac.za/jktnad001/csc3003s_capstone_imtps.git
    cd csc3003s_capstone_imtps
    mvn clean javafx:run

The application window opens on the login screen.

### First-Time Use

1. Click **Sign up** and create an account. Passwords must be at least 8 characters long and contain at least one number.
2. Complete the **onboarding questionnaire** to set your productivity preferences.
3. From the main screen, click **+ Add New Task**. Leave *Start Time* blank to make it a floating task, or fill it in to pin it.
4. Switch to the **View Tasks** tab, pick an algorithm and focus mode, and click **Refresh View** to see your schedule.

---

## Testing

Run the full JUnit 5 test suite:

    mvn test

Test reports are written to `target/surefire-reports/`.

The suite covers:

- **Scheduling algorithms**  pinned/floating separation, collision avoidance, multi-day placement, edge cases (no due date, unplaceable tasks).
- **Database layer**  user CRUD, task CRUD, preferences, time-overlap detection, completion lifecycle.
- **Security**  password hashing (verifies plaintext is never stored), SQL injection resistance on login and sign-up.
- **Questionnaire**  default fallbacks and preference parsing.

---

## Database

The application uses a single SQLite file, `imtps_db.db`, created automatically in the working directory on first launch. Three tables:

---

## Known Limitations

- **Emoji rendering on Windows/Linux**: JavaFX does not perform automatic font fallback for colour emoji. Icons render correctly on macOS (which ships *Apple Color Emoji*). On Windows and Linux, emojis are replaced with monochrome glyphs where available.
- **The DatePicker only shows floating tasks for today onwards.** Floating tasks are always scheduled starting from the current day. Pinned tasks appear on any date within their original range.
- **Single-user desktop app**: the database is not shared between installations, updates to DB are local. This was a scope decision for the capstone.

---
