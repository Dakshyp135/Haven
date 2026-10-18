# Haven Hotel Management System

Haven includes a Java 17/MySQL console application, a Spring Boot REST API, and a static room and reservation dashboard that can be hosted on GitHub Pages.

GitHub Pages hosts only the frontend. The API and MySQL database must run on a server that supports Java and MySQL. The backend Dockerfile is in backend-api/demo/Dockerfile.

## Project layout

- frontend/ — static GitHub Pages website
- backend-api/demo/ — Spring Boot API
- src/ — original Java console application
- sql/HotelManagementSystem.sql — database schema and demo data
- sql/Optional_Advanced_SQL.sql — optional SQL examples

## Keep database credentials private

The uploaded project contained a real-looking MySQL password in two files. Those values have been removed. If the password was used anywhere else, change it before deploying.

Do not commit .env or config.properties. Both are ignored by Git. For the console app, set DB_URL, DB_USER, and DB_PASSWORD in the shell. For the Spring API, set the same variables on its host. The frontend's API URL is public configuration, not a secret.

## Run the backend locally

1. Create a MySQL 8 database and import sql/HotelManagementSystem.sql.
2. Create a database account for the application with access to hotel_management.
3. Copy .env.example to .env, then set the local DB_USER, DB_PASSWORD, and DB_URL. Do not commit .env.
4. Start the API from backend-api/demo with ./mvnw spring-boot:run (or mvn spring-boot:run).
5. Set frontend/config.js to window.HAVEN_API_BASE_URL = "http://localhost:8080"; and serve frontend/ locally. For example, python3 -m http.server 5173 --directory frontend.
6. If the frontend runs at another origin, set HAVEN_CORS_ORIGINS on the API to that exact origin.

The API provides GET /api/rooms, GET /api/guests, POST /api/guests, and POST /api/reservations. The reservation API uses staff ID 3 as in the original demo schema.

## Publish the website with GitHub Pages

1. Create a GitHub repository and push this project to its main branch.
2. In the repository, open Settings → Pages and set the build source to GitHub Actions.
3. GitHub Actions will publish the contents of frontend/ whenever frontend files or the Pages workflow change.
4. Deploy the Spring Boot API and MySQL database to a Java-capable hosting provider. Use a managed MySQL database for production, import sql/HotelManagementSystem.sql, and configure the API host's DB_URL, DB_USER, and DB_PASSWORD.
5. Set HAVEN_CORS_ORIGINS on the API host to the exact HTTPS origin shown by GitHub Pages (for example, https://YOUR-USER.github.io for a project site).
6. Put the API's public HTTPS base URL in frontend/config.js, commit that change, and push. Wait for the Pages action to finish.

The Pages URL will be https://YOUR-USER.github.io/YOUR-REPOSITORY/ for a project repository. The API host's URL is separate. Do not put database credentials in frontend/config.js or GitHub Pages.

## Deploy the API with Docker

The backend Dockerfile builds a runnable Java 17 image. Build from backend-api/demo and provide DB_URL, DB_USER, DB_PASSWORD, PORT (usually supplied by the host), and HAVEN_CORS_ORIGINS as runtime environment variables. The container does not include MySQL; point it to a managed MySQL server reachable from the API host.

## Run the original console application

Install Java 17 and Maven, import the SQL schema, then set DB_URL, DB_USER, and DB_PASSWORD in the environment. From the repository root run mvn package and mvn exec:java. The optional ignored config.properties file remains supported for local-only settings.

## Current limitations

This is an academic demo, not a production booking system. The API has no user login or authorization, and its public guest and reservation endpoints should not be exposed to real customer data until authentication and operational safeguards are added. The reservation form creates a guest before attempting the reservation, so a rejected booking can leave that guest in the database.
