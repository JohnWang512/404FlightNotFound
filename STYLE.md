Purpose

This document lays out our coding conventions for the project. We like our code readable, relaxed, and consistent — like your favorite hoodie.

1. Naming

Variables & constants
Use camelCase for variables, UPPER_SNAKE_CASE for constants.

const API_KEY = "12345";
let userName = "Sam";

Functions
Use camelCase, and name functions like you’re describing what they do.

function fetchUserData() { ... }
function calculateScore(points) { ... }

Components (React / UI)
Use PascalCase. Keep names short but descriptive.

function UserCard() { ... }
function ScoreBoard() { ... }
2. File & Folder Structure

Keep things tidy, no spaghetti allowed 🍝

Example:

src/
  components/
    UserCard.jsx
    ScoreBoard.jsx
  utils/
    api.js
    helpers.js
  App.jsx

One component or module per file. Keep it small, keep it chill.

3. Code Style

Indentation: 2 spaces (tabs are a little too rowdy 😜)

Line length: max 100 characters

Semicolons: optional, but be consistent

Quotes: single ' preferred

const greet = name => {
  console.log('Hello, ' + name + '!');
};

Braces & spacing: relaxed, readable

if (isLoggedIn) {
  showDashboard();
} else {
  showLogin();
}
4. Comments

Keep comments helpful, not “I have no idea what this does” style.

Use // for single lines, /* */ for blocks.

Example:

// fetches user info from the API
async function fetchUserData(userId) {
  const response = await fetch(`/api/user/${userId}`);
  return await response.json();
}
5. Git & Commits

Commit messages should be friendly and descriptive:

feat: add user login flow
fix: correct button alignment on mobile
chore: update README

Keep your branches tidy: feature/login or bugfix/button-fix

6. Misc Chill Vibes

DRY but don’t over-engineer. Code is for humans.

Keep your code readable; your future self will thank you.

Test when you can, but no stress.Here you can document your coding standards and conventions. This includes decisions about naming, style guides, etc.