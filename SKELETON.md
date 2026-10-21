Walking Skeleton Plan - Flight TrackerOverview
A minimal implementation to verify end-to-end integration between frontend, backend, and database components of the flight tracker application.Skeleton Feature: "Search Flight by Number"
Goal: User enters a flight number, backend queries database, returns flight status to display on frontend.Tech StackFrontend

Framework: Vue.js
Key Libraries: Axios (HTTP requests)
Functionality: Single search input + results display
Backend

Framework: Express.js (Node.js)
Key Libraries:

mysql2 (MySQL driver)
express (web framework)
cors (enable cross-origin requests)


Functionality: REST API endpoint to handle flight queries
Database

System: MySQL
Schema: Single flights table with basic flight information