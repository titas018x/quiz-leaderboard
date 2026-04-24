# Quiz Leaderboard — SRM Internship Assignment

A small Java program that polls a quiz API 10 times, deduplicates the events, builds a leaderboard, and submits the final result.

---

## How it works

The API dishes out quiz round scores across 10 polls. The catch — same round data can repeat across polls. If you just add everything up blindly, your totals will be wrong.

The fix is straightforward: track every `roundId + participant` combo you've already seen. When the same one shows up again, skip it.

Once all 10 polls are done, sort participants by total score descending and POST to the submit endpoint.

---

## Setup

**Requirements:** Java 11 or above (uses `java.net.http.HttpClient` which came in Java 11)

**Clone and run:**

```bash
git clone https://github.com/your-username/quiz-leaderboard.git
cd quiz-leaderboard
javac src/QuizLeaderboard.java -d out
java -cp out QuizLeaderboard
```

---

## Before running

Open `src/QuizLeaderboard.java` and update line 7:

```java
static final String REG = "RA2311003010438"; // your reg number
```

---

## What the output looks like

```
Poll 0 → {"regNo":"...","setId":"SET_1","pollIndex":0,"events":[...]}
Poll 1 → ...
  [dup] skipping R1|Alice
...
Poll 9 → ...

Submit response → {"isCorrect":true,"isIdempotent":true,"submittedTotal":220,"expectedTotal":220,"message":"Correct!"}
```

---

## Project structure

```
quiz-leaderboard/
├── src/
│   └── QuizLeaderboard.java
└── README.md
```

No external dependencies. Pure Java stdlib only.

---

## Logic summary

1. Loop poll index 0–9, hit `GET /quiz/messages?regNo=...&poll=N`
2. Wait 5 seconds between each call (API requirement)
3. For each event in the response, check if `roundId|participant` was already processed
4. If yes → skip. If no → add score to that participant's total
5. Sort by score descending
6. POST the leaderboard to `/quiz/submit` exactly once
