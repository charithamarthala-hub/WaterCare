package com.watercare;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class WaterCareServer {

    private static final int PORT = 8080;

    private static final Path FRONTEND_FOLDER =
            Paths.get(System.getProperty("user.dir"))
                    .resolve("frontend")
                    .toAbsolutePath()
                    .normalize();


    // ============================================================
    // MYSQL DATABASE CONNECTION
    // ============================================================

    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/watercare";

    private static final String DB_USER =
            "root";

    private static final String DB_PASSWORD =
            "Charitha@2005";


    // ============================================================
    // TEMPORARY LOGIN STORAGE
    // ============================================================

    private static final Map<String, User> users =
            new HashMap<>();


    // ============================================================
    // LATEST WATER INFORMATION
    // ============================================================

    private static String latestWaterSource = "";
    private static String latestAppearance = "";
    private static String latestOdor = "";
    private static String latestTreatment = "";
    private static double latestPh = 0;
    private static String latestTestDate = "";


    // ============================================================
    // LATEST RISK RESULT
    // ============================================================

    private static String latestRiskLevel = "";
    private static String latestRiskMessage = "";
    private static int latestRiskScore = 0;


    // ============================================================
    // MAIN
    // ============================================================

    public static void main(String[] args) throws Exception {

        System.out.println("=================================");
        System.out.println("       WaterCare Backend");
        System.out.println("=================================");

        System.out.println();

        System.out.println("Frontend folder:");
        System.out.println(FRONTEND_FOLDER);


        if (!Files.exists(FRONTEND_FOLDER)) {

            System.out.println();
            System.out.println(
                    "ERROR: Frontend folder was not found."
            );

            return;
        }


        // Test MySQL connection
        if (!testDatabaseConnection()) {

            System.out.println();
            System.out.println(
                    "WARNING: MySQL connection failed."
            );

            System.out.println(
                    "The server will not be started."
            );

            return;
        }


        HttpServer server =
                HttpServer.create(
                        new InetSocketAddress(PORT),
                        0
                );


        server.createContext(
                "/",
                WaterCareServer::serveFrontend
        );


        server.createContext(
                "/api/test",
                WaterCareServer::testApi
        );


        server.createContext(
                "/api/signup",
                WaterCareServer::signupApi
        );


        server.createContext(
                "/api/login",
                WaterCareServer::loginApi
        );


        server.createContext(
                "/api/profile",
                WaterCareServer::profileApi
        );


        server.createContext(
                "/api/symptoms",
                WaterCareServer::symptomsApi
        );


        server.createContext(
                "/api/water-source",
                WaterCareServer::waterSourceApi
        );


        server.createContext(
                "/api/water-source/latest",
                WaterCareServer::latestWaterSourceApi
        );


        server.createContext(
                "/api/assess",
                WaterCareServer::assessApi
        );


        server.createContext(
                "/api/risk/latest",
                WaterCareServer::latestRiskApi
        );


        server.createContext(
                "/api/history",
                WaterCareServer::historyApi
        );


        server.setExecutor(null);

        server.start();


        System.out.println();
        System.out.println(
                "WaterCare server started successfully."
        );

        System.out.println();

        System.out.println(
                "Open: http://localhost:8080"
        );

        System.out.println();

        System.out.println(
                "Signup API: POST /api/signup"
        );

        System.out.println(
                "Login API: POST /api/login"
        );

        System.out.println(
                "Profile API: POST /api/profile"
        );

        System.out.println(
                "Symptoms API: POST /api/symptoms"
        );

        System.out.println(
                "Water Source API: POST /api/water-source"
        );

        System.out.println(
                "Saved Water API: GET /api/water-source/latest"
        );

        System.out.println(
                "AI Risk API: POST /api/assess"
        );

        System.out.println(
                "Risk Result API: GET /api/risk/latest"
        );

        System.out.println(
                "History API: GET /api/history"
        );

        System.out.println();

        System.out.println(
                "Press Ctrl+C to stop the server."
        );
    }


    // ============================================================
    // DATABASE CONNECTION TEST
    // ============================================================

    private static boolean testDatabaseConnection() {

        System.out.println();
        System.out.println(
                "Testing MySQL connection..."
        );

        try (Connection connection =
                     DriverManager.getConnection(
                             DB_URL,
                             DB_USER,
                             DB_PASSWORD
                     )) {

            System.out.println(
                    "MySQL connection successful."
            );

            System.out.println(
                    "Database: watercare"
            );

            return true;

        } catch (SQLException e) {

            System.out.println(
                    "MySQL connection failed."
            );

            System.out.println(
                    "Error: " + e.getMessage()
            );

            return false;
        }
    }


    // ============================================================
    // FRONTEND
    // ============================================================

    private static void serveFrontend(
            HttpExchange exchange)
            throws IOException {

        String requestPath =
                exchange.getRequestURI().getPath();


        if (requestPath.equals("/")) {

            requestPath = "/index.html";
        }


        Path requestedFile =
                FRONTEND_FOLDER
                        .resolve(
                                requestPath.substring(1)
                        )
                        .normalize();


        if (!requestedFile.startsWith(
                FRONTEND_FOLDER)) {

            sendResponse(
                    exchange,
                    403,
                    "Forbidden"
            );

            return;
        }


        if (!Files.exists(requestedFile)
                || Files.isDirectory(requestedFile)) {

            sendResponse(
                    exchange,
                    404,
                    "<h1>404 - Page Not Found</h1>"
            );

            return;
        }


        byte[] fileData =
                Files.readAllBytes(
                        requestedFile
                );


        String contentType =
                getContentType(
                        requestedFile
                );


        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        contentType
                );


        exchange.sendResponseHeaders(
                200,
                fileData.length
        );


        try (OutputStream output =
                     exchange.getResponseBody()) {

            output.write(fileData);
        }
    }


    // ============================================================
    // TEST API
    // ============================================================

    private static void testApi(
            HttpExchange exchange)
            throws IOException {

        sendJsonResponse(
                exchange,
                200,
                "{"
                + "\"status\":\"success\","
                + "\"message\":"
                + "\"WaterCare backend is working\","
                + "\"database\":\"connected\""
                + "}"
        );
    }


    // ============================================================
    // SIGNUP API
    // ============================================================

    private static void signupApi(
            HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("POST")) {

            sendResponse(
                    exchange,
                    405,
                    "Method Not Allowed"
            );

            return;
        }


        String requestBody =
                new String(
                        exchange.getRequestBody()
                                .readAllBytes(),
                        StandardCharsets.UTF_8
                );


        System.out.println();
        System.out.println(
                "Signup request received:"
        );

        System.out.println(
                requestBody
        );


        String name =
                getJsonValue(
                        requestBody,
                        "name"
                );


        String email =
                getJsonValue(
                        requestBody,
                        "email"
                );


        String password =
                getJsonValue(
                        requestBody,
                        "password"
                );


        if (name.isEmpty()
                || email.isEmpty()
                || password.isEmpty()) {

            sendJsonResponse(
                    exchange,
                    400,
                    "{"
                    + "\"status\":\"error\","
                    + "\"message\":"
                    + "\"All fields are required\""
                    + "}"
            );

            return;
        }


        // Check existing user
        String checkSql =
                "SELECT id FROM users WHERE email = ?";


        try (Connection connection =
                     DriverManager.getConnection(
                             DB_URL,
                             DB_USER,
                             DB_PASSWORD
                     );

             PreparedStatement statement =
                     connection.prepareStatement(
                             checkSql
                     )) {

            statement.setString(
                    1,
                    email
            );


            try (ResultSet result =
                         statement.executeQuery()) {

                if (result.next()) {

                    sendJsonResponse(
                            exchange,
                            409,
                            "{"
                            + "\"status\":\"error\","
                            + "\"message\":"
                            + "\"Account already exists\""
                            + "}"
                    );

                    return;
                }
            }


        } catch (SQLException e) {

            System.out.println(
                    "Signup check error: "
                    + e.getMessage()
            );

            sendJsonResponse(
                    exchange,
                    500,
                    "{"
                    + "\"status\":\"error\","
                    + "\"message\":"
                    + "\"Database error while checking account\""
                    + "}"
            );

            return;
        }


        // Insert user
        String insertSql =
                "INSERT INTO users "
                + "(name, email, password) "
                + "VALUES (?, ?, ?)";


        try (Connection connection =
                     DriverManager.getConnection(
                             DB_URL,
                             DB_USER,
                             DB_PASSWORD
                     );

             PreparedStatement statement =
                     connection.prepareStatement(
                             insertSql
                     )) {

            statement.setString(
                    1,
                    name
            );

            statement.setString(
                    2,
                    email
            );

            statement.setString(
                    3,
                    password
            );


            statement.executeUpdate();


            users.put(
                    email,
                    new User(
                            name,
                            email,
                            password
                    )
            );


            sendJsonResponse(
                    exchange,
                    200,
                    "{"
                    + "\"status\":\"success\","
                    + "\"message\":"
                    + "\"Signup successful\""
                    + "}"
            );


        } catch (SQLException e) {

            System.out.println(
                    "Signup database error: "
                    + e.getMessage()
            );

            sendJsonResponse(
                    exchange,
                    500,
                    "{"
                    + "\"status\":\"error\","
                    + "\"message\":"
                    + "\"Unable to create account\""
                    + "}"
            );
        }
    }


    // ============================================================
    // LOGIN API
    // ============================================================

    private static void loginApi(
            HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("POST")) {

            sendResponse(
                    exchange,
                    405,
                    "Method Not Allowed"
            );

            return;
        }


        String requestBody =
                new String(
                        exchange.getRequestBody()
                                .readAllBytes(),
                        StandardCharsets.UTF_8
                );


        String email =
                getJsonValue(
                        requestBody,
                        "email"
                );


        String password =
                getJsonValue(
                        requestBody,
                        "password"
                );


        if (email.isEmpty()
                || password.isEmpty()) {

            sendJsonResponse(
                    exchange,
                    400,
                    "{"
                    + "\"status\":\"error\","
                    + "\"message\":"
                    + "\"Email and password are required\""
                    + "}"
            );

            return;
        }


        String sql =
                "SELECT name, email, password "
                + "FROM users "
                + "WHERE email = ?";


        try (Connection connection =
                     DriverManager.getConnection(
                             DB_URL,
                             DB_USER,
                             DB_PASSWORD
                     );

             PreparedStatement statement =
                     connection.prepareStatement(
                             sql
                     )) {

            statement.setString(
                    1,
                    email
            );


            try (ResultSet result =
                         statement.executeQuery()) {

                if (!result.next()) {

                    sendJsonResponse(
                            exchange,
                            401,
                            "{"
                            + "\"status\":\"error\","
                            + "\"message\":"
                            + "\"Account not found\""
                            + "}"
                    );

                    return;
                }


                String storedPassword =
                        result.getString(
                                "password"
                        );


                if (!storedPassword.equals(password)) {

                    sendJsonResponse(
                            exchange,
                            401,
                            "{"
                            + "\"status\":\"error\","
                            + "\"message\":"
                            + "\"Incorrect password\""
                            + "}"
                    );

                    return;
                }


                String name =
                        result.getString(
                                "name"
                        );


                sendJsonResponse(
                        exchange,
                        200,
                        "{"
                        + "\"status\":\"success\","
                        + "\"message\":"
                        + "\"Login successful\","
                        + "\"name\":\""
                        + escapeJson(name)
                        + "\","
                        + "\"email\":\""
                        + escapeJson(email)
                        + "\""
                        + "}"
                );
            }


        } catch (SQLException e) {

            System.out.println(
                    "Login database error: "
                    + e.getMessage()
            );

            sendJsonResponse(
                    exchange,
                    500,
                    "{"
                    + "\"status\":\"error\","
                    + "\"message\":"
                    + "\"Database error during login\""
                    + "}"
            );
        }
    }


    // ============================================================
    // PROFILE API
    // ============================================================

    private static void profileApi(
            HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("POST")) {

            sendResponse(
                    exchange,
                    405,
                    "Method Not Allowed"
            );

            return;
        }


        String requestBody =
                new String(
                        exchange.getRequestBody()
                                .readAllBytes(),
                        StandardCharsets.UTF_8
                );


        String email =
                getJsonValue(
                        requestBody,
                        "email"
                );


        String name =
                getJsonValue(
                        requestBody,
                        "name"
                );


        String phone =
                getJsonValue(
                        requestBody,
                        "phone"
                );


        String location =
                getJsonValue(
                        requestBody,
                        "location"
                );


        if (email.isEmpty()) {

            sendJsonResponse(
                    exchange,
                    400,
                    "{"
                    + "\"status\":\"error\","
                    + "\"message\":"
                    + "\"Email is required\""
                    + "}"
            );

            return;
        }


        String checkSql =
                "SELECT id FROM profiles WHERE email = ?";


        try (Connection connection =
                     DriverManager.getConnection(
                             DB_URL,
                             DB_USER,
                             DB_PASSWORD
                     );

             PreparedStatement checkStatement =
                     connection.prepareStatement(
                             checkSql
                     )) {

            checkStatement.setString(
                    1,
                    email
            );


            try (ResultSet result =
                         checkStatement.executeQuery()) {

                if (result.next()) {

                    String updateSql =
                            "UPDATE profiles "
                            + "SET name = ?, "
                            + "phone = ?, "
                            + "location = ? "
                            + "WHERE email = ?";


                    try (PreparedStatement update =
                                 connection.prepareStatement(
                                         updateSql
                                 )) {

                        update.setString(
                                1,
                                name
                        );

                        update.setString(
                                2,
                                phone
                        );

                        update.setString(
                                3,
                                location
                        );

                        update.setString(
                                4,
                                email
                        );

                        update.executeUpdate();
                    }


                } else {

                    String insertSql =
                            "INSERT INTO profiles "
                            + "(email, name, phone, location) "
                            + "VALUES (?, ?, ?, ?)";


                    try (PreparedStatement insert =
                                 connection.prepareStatement(
                                         insertSql
                                 )) {

                        insert.setString(
                                1,
                                email
                        );

                        insert.setString(
                                2,
                                name
                        );

                        insert.setString(
                                3,
                                phone
                        );

                        insert.setString(
                                4,
                                location
                        );

                        insert.executeUpdate();
                    }
                }
            }


            sendJsonResponse(
                    exchange,
                    200,
                    "{"
                    + "\"status\":\"success\","
                    + "\"message\":"
                    + "\"Profile saved successfully\""
                    + "}"
            );


        } catch (SQLException e) {

            System.out.println(
                    "Profile database error: "
                    + e.getMessage()
            );

            sendJsonResponse(
                    exchange,
                    500,
                    "{"
                    + "\"status\":\"error\","
                    + "\"message\":"
                    + "\"Unable to save profile\""
                    + "}"
            );
        }
    }


    // ============================================================
    // SYMPTOMS API
    // ============================================================

    private static void symptomsApi(
            HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("POST")) {

            sendResponse(
                    exchange,
                    405,
                    "Method Not Allowed"
            );

            return;
        }


        String requestBody =
                new String(
                        exchange.getRequestBody()
                                .readAllBytes(),
                        StandardCharsets.UTF_8
                );


        System.out.println();
        System.out.println(
                "Symptoms request received:"
        );

        System.out.println(
                requestBody
        );


        int stomach =
                getJsonNumber(
                        requestBody,
                        "stomach"
                );


        int diarrhea =
                getJsonNumber(
                        requestBody,
                        "diarrhea"
                );


        int vomiting =
                getJsonNumber(
                        requestBody,
                        "vomiting"
                );


        int fever =
                getJsonNumber(
                        requestBody,
                        "fever"
                );


        int score =
                stomach
                + diarrhea
                + vomiting
                + fever;


        String level;

        String message;


        if (score >= 4) {

            level =
                    "Several symptoms selected";

            message =
                    "Several symptoms were selected. "
                    + "Please tell a parent, guardian, "
                    + "or health professional, especially "
                    + "if symptoms are severe, persistent, "
                    + "or getting worse.";

        } else if (score >= 2) {

            level =
                    "Some symptoms selected";

            message =
                    "Some symptoms were selected. "
                    + "Monitor how you feel and stay hydrated. "
                    + "If symptoms continue or become worse, "
                    + "tell a parent, guardian, or health professional.";

        } else {

            level =
                    "Few symptoms selected";

            message =
                    "Few symptoms were selected. "
                    + "Continue to monitor how you feel "
                    + "and seek help if symptoms develop "
                    + "or become worse.";
        }


        sendJsonResponse(
                exchange,
                200,
                "{"
                + "\"status\":\"success\","
                + "\"level\":\""
                + escapeJson(level)
                + "\","
                + "\"message\":\""
                + escapeJson(message)
                + "\","
                + "\"score\":"
                + score
                + "}"
        );
    }


    // ============================================================
    // WATER SOURCE API
    // ============================================================

    private static void waterSourceApi(
            HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("POST")) {

            sendResponse(
                    exchange,
                    405,
                    "Method Not Allowed"
            );

            return;
        }


        String requestBody =
                new String(
                        exchange.getRequestBody()
                                .readAllBytes(),
                        StandardCharsets.UTF_8
                );


        System.out.println();
        System.out.println(
                "Water Source request received:"
        );

        System.out.println(
                requestBody
        );


        String source =
                getJsonValue(
                        requestBody,
                        "source"
                );


        String appearance =
                getJsonValue(
                        requestBody,
                        "appearance"
                );


        String odor =
                getJsonValue(
                        requestBody,
                        "odor"
                );


        String treatment =
                getJsonValue(
                        requestBody,
                        "treatment"
                );


        String phText =
                getJsonValue(
                        requestBody,
                        "ph"
                );


        String testDate =
                getJsonValue(
                        requestBody,
                        "testDate"
                );


        if (source.isEmpty()
                || appearance.isEmpty()
                || odor.isEmpty()
                || treatment.isEmpty()
                || phText.isEmpty()) {

            sendJsonResponse(
                    exchange,
                    400,
                    "{"
                    + "\"status\":\"error\","
                    + "\"message\":"
                    + "\"Please complete all required water information\""
                    + "}"
            );

            return;
        }


        double ph;


        try {

            ph =
                    Double.parseDouble(
                            phText
                    );

        } catch (NumberFormatException e) {

            sendJsonResponse(
                    exchange,
                    400,
                    "{"
                    + "\"status\":\"error\","
                    + "\"message\":"
                    + "\"Invalid pH value\""
                    + "}"
            );

            return;
        }


        if (ph < 0 || ph > 14) {

            sendJsonResponse(
                    exchange,
                    400,
                    "{"
                    + "\"status\":\"error\","
                    + "\"message\":"
                    + "\"pH must be between 0 and 14\""
                    + "}"
            );

            return;
        }


        latestWaterSource = source;
        latestAppearance = appearance;
        latestOdor = odor;
        latestTreatment = treatment;
        latestPh = ph;
        latestTestDate = testDate;


        String level;

        String message;


        if (ph < 6.5 || ph > 8.5) {

            level =
                    "Water Quality Alert";

            message =
                    "The entered pH value is outside "
                    + "the commonly recommended range. "
                    + "Consider checking the water quality "
                    + "with a reliable test.";

        } else if (
                appearance.equals("Cloudy")
                || appearance.equals("Discolored")
                || odor.equals("Strong unusual odor")
                || treatment.equals("No Treatment")) {

            level =
                    "Water Quality Needs Attention";

            message =
                    "Some entered water characteristics "
                    + "may need attention. Consider appropriate "
                    + "water treatment and further testing.";

        } else {

            level =
                    "Water Information Saved";

            message =
                    "The water information was received "
                    + "successfully. Continue to the AI Risk "
                    + "Assessment for further analysis.";
        }


        sendJsonResponse(
                exchange,
                200,
                "{"
                + "\"status\":\"success\","
                + "\"level\":\""
                + escapeJson(level)
                + "\","
                + "\"message\":\""
                + escapeJson(message)
                + "\","
                + "\"source\":\""
                + escapeJson(source)
                + "\","
                + "\"appearance\":\""
                + escapeJson(appearance)
                + "\","
                + "\"odor\":\""
                + escapeJson(odor)
                + "\","
                + "\"treatment\":\""
                + escapeJson(treatment)
                + "\","
                + "\"ph\":"
                + ph
                + ","
                + "\"testDate\":\""
                + escapeJson(testDate)
                + "\""
                + "}"
        );
    }


    // ============================================================
    // LATEST WATER SOURCE API
    // ============================================================

    private static void latestWaterSourceApi(
            HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("GET")) {

            sendResponse(
                    exchange,
                    405,
                    "Method Not Allowed"
            );

            return;
        }


        if (latestWaterSource.isEmpty()) {

            sendJsonResponse(
                    exchange,
                    404,
                    "{"
                    + "\"status\":\"error\","
                    + "\"message\":"
                    + "\"Water information not stored\""
                    + "}"
            );

            return;
        }


        String response =
                "{"
                + "\"status\":\"success\","
                + "\"source\":\""
                + escapeJson(latestWaterSource)
                + "\","
                + "\"appearance\":\""
                + escapeJson(latestAppearance)
                + "\","
                + "\"odor\":\""
                + escapeJson(latestOdor)
                + "\","
                + "\"treatment\":\""
                + escapeJson(latestTreatment)
                + "\","
                + "\"ph\":"
                + latestPh
                + ","
                + "\"testDate\":\""
                + escapeJson(latestTestDate)
                + "\""
                + "}";


        sendJsonResponse(
                exchange,
                200,
                response
        );
    }


    // ============================================================
    // AI RISK ASSESSMENT API
    // ============================================================

    private static void assessApi(
            HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("POST")) {

            sendResponse(
                    exchange,
                    405,
                    "Method Not Allowed"
            );

            return;
        }


        String requestBody =
                new String(
                        exchange.getRequestBody()
                                .readAllBytes(),
                        StandardCharsets.UTF_8
                );


        System.out.println();
        System.out.println(
                "AI Risk Assessment request received:"
        );

        System.out.println(
                requestBody
        );


        if (latestWaterSource.isEmpty()) {

            sendJsonResponse(
                    exchange,
                    400,
                    "{"
                    + "\"status\":\"error\","
                    + "\"message\":"
                    + "\"Water information not stored. "
                    + "Please save water information first.\""
                    + "}"
            );

            return;
        }


        int symptoms =
                getJsonNumber(
                        requestBody,
                        "symptoms"
                );


        int duration =
                getJsonNumber(
                        requestBody,
                        "duration"
                );


        int worse =
                getJsonNumber(
                        requestBody,
                        "worse"
                );


        int score =
                symptoms
                + duration
                + worse;


        String riskLevel;

        String message;


        if (score >= 5) {

            riskLevel =
                    "High Risk";

            message =
                    "The entered information indicates "
                    + "several factors that may need attention. "
                    + "Please discuss concerning or worsening "
                    + "symptoms with a parent, guardian, "
                    + "or health professional.";

        } else if (score >= 3) {

            riskLevel =
                    "Moderate Risk";

            message =
                    "The entered information indicates "
                    + "some factors that should be monitored. "
                    + "Continue monitoring your symptoms "
                    + "and consider speaking with a parent, "
                    + "guardian, or health professional "
                    + "if they continue.";

        } else {

            riskLevel =
                    "Low Risk";

            message =
                    "The entered information does not show "
                    + "many risk factors. Continue monitoring "
                    + "your symptoms and water information.";
        }


        latestRiskLevel = riskLevel;
        latestRiskMessage = message;
        latestRiskScore = score;


        // Save risk assessment to MySQL
        String sql =
                "INSERT INTO risk_assessments "
                + "(symptoms, duration, worse, score, risk_level, message) "
                + "VALUES (?, ?, ?, ?, ?, ?)";


        try (Connection connection =
                     DriverManager.getConnection(
                             DB_URL,
                             DB_USER,
                             DB_PASSWORD
                     );

             PreparedStatement statement =
                     connection.prepareStatement(
                             sql
                     )) {

            statement.setInt(
                    1,
                    symptoms
            );

            statement.setInt(
                    2,
                    duration
            );

            statement.setInt(
                    3,
                    worse
            );

            statement.setInt(
                    4,
                    score
            );

            statement.setString(
                    5,
                    riskLevel
            );

            statement.setString(
                    6,
                    message
            );


            statement.executeUpdate();


            String response =
                    "{"
                    + "\"status\":\"success\","
                    + "\"riskLevel\":\""
                    + escapeJson(riskLevel)
                    + "\","
                    + "\"message\":\""
                    + escapeJson(message)
                    + "\","
                    + "\"score\":"
                    + score
                    + "}";


            sendJsonResponse(
                    exchange,
                    200,
                    response
            );


        } catch (SQLException e) {

            System.out.println(
                    "Risk assessment database error: "
                    + e.getMessage()
            );

            sendJsonResponse(
                    exchange,
                    500,
                    "{"
                    + "\"status\":\"error\","
                    + "\"message\":"
                    + "\"Unable to save risk assessment\""
                    + "}"
            );
        }
    }


    // ============================================================
    // LATEST RISK API
    // ============================================================

    private static void latestRiskApi(
            HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("GET")) {

            sendResponse(
                    exchange,
                    405,
                    "Method Not Allowed"
            );

            return;
        }


        if (latestRiskLevel.isEmpty()) {

            sendJsonResponse(
                    exchange,
                    404,
                    "{"
                    + "\"status\":\"error\","
                    + "\"message\":"
                    + "\"No risk result available\""
                    + "}"
            );

            return;
        }


        String response =
                "{"
                + "\"status\":\"success\","
                + "\"riskLevel\":\""
                + escapeJson(latestRiskLevel)
                + "\","
                + "\"message\":\""
                + escapeJson(latestRiskMessage)
                + "\","
                + "\"score\":"
                + latestRiskScore
                + "}";


        sendJsonResponse(
                exchange,
                200,
                response
        );
    }


    // ============================================================
    // HISTORY API
    // ============================================================

    private static void historyApi(
            HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("GET")) {

            sendResponse(
                    exchange,
                    405,
                    "Method Not Allowed"
            );

            return;
        }


        String sql =
                "SELECT id, symptoms, duration, worse, "
                + "score, risk_level, message, created_at "
                + "FROM risk_assessments "
                + "ORDER BY created_at DESC";


        StringBuilder history =
                new StringBuilder();


        history.append(
                "{"
                + "\"status\":\"success\","
                + "\"history\":["
        );


        boolean first =
                true;


        try (Connection connection =
                     DriverManager.getConnection(
                             DB_URL,
                             DB_USER,
                             DB_PASSWORD
                     );

             PreparedStatement statement =
                     connection.prepareStatement(
                             sql
                     );

             ResultSet result =
                     statement.executeQuery()) {


            while (result.next()) {

                if (!first) {

                    history.append(",");
                }


                first = false;


                history.append("{");


                history.append(
                        "\"id\":"
                );

                history.append(
                        result.getInt("id")
                );


                history.append(
                        ",\"symptoms\":"
                );

                history.append(
                        result.getInt("symptoms")
                );


                history.append(
                        ",\"duration\":"
                );

                history.append(
                        result.getInt("duration")
                );


                history.append(
                        ",\"worse\":"
                );

                history.append(
                        result.getInt("worse")
                );


                history.append(
                        ",\"score\":"
                );

                history.append(
                        result.getInt("score")
                );


                history.append(
                        ",\"riskLevel\":\""
                );

                history.append(
                        escapeJson(
                                result.getString(
                                        "risk_level"
                                )
                        )
                );

                history.append("\"");


                history.append(
                        ",\"message\":\""
                );

                history.append(
                        escapeJson(
                                result.getString(
                                        "message"
                                )
                        )
                );

                history.append("\"");


                history.append(
                        ",\"createdAt\":\""
                );

                history.append(
                        escapeJson(
                                result.getString(
                                        "created_at"
                                )
                        )
                );

                history.append("\"");


                history.append("}");
            }


            history.append(
                    "]}"
            );


            sendJsonResponse(
                    exchange,
                    200,
                    history.toString()
            );


        } catch (SQLException e) {

            System.out.println(
                    "History database error: "
                    + e.getMessage()
            );

            sendJsonResponse(
                    exchange,
                    500,
                    "{"
                    + "\"status\":\"error\","
                    + "\"message\":"
                    + "\"Unable to load history\""
                    + "}"
            );
        }
    }


    // ============================================================
    // JSON NUMBER
    // ============================================================

    private static int getJsonNumber(
            String json,
            String key) {

        String search =
                "\"" + key + "\"";


        int keyPosition =
                json.indexOf(search);


        if (keyPosition == -1) {

            return 0;
        }


        int colonPosition =
                json.indexOf(
                        ":",
                        keyPosition
                );


        if (colonPosition == -1) {

            return 0;
        }


        int start =
                colonPosition + 1;


        while (start < json.length()
                && Character.isWhitespace(
                        json.charAt(start))) {

            start++;
        }


        int end =
                start;


        if (end < json.length()
                && json.charAt(end) == '"') {

            end++;

            start =
                    end;


            while (end < json.length()
                    && json.charAt(end) != '"') {

                end++;
            }

        } else {

            while (end < json.length()
                    && (
                    Character.isDigit(
                            json.charAt(end))
                    || json.charAt(end) == '-'
                    )) {

                end++;
            }
        }


        if (start == end) {

            return 0;
        }


        try {

            return Integer.parseInt(
                    json.substring(
                            start,
                            end
                    )
            );

        } catch (NumberFormatException e) {

            return 0;
        }
    }


    // ============================================================
    // JSON STRING
    // ============================================================

    private static String getJsonValue(
            String json,
            String key) {

        String search =
                "\"" + key + "\"";


        int keyPosition =
                json.indexOf(search);


        if (keyPosition == -1) {

            return "";
        }


        int colonPosition =
                json.indexOf(
                        ":",
                        keyPosition
                );


        if (colonPosition == -1) {

            return "";
        }


        int firstQuote =
                json.indexOf(
                        "\"",
                        colonPosition
                );


        if (firstQuote == -1) {

            return "";
        }


        int secondQuote =
                json.indexOf(
                        "\"",
                        firstQuote + 1
                );


        if (secondQuote == -1) {

            return "";
        }


        return json.substring(
                firstQuote + 1,
                secondQuote
        );
    }


    // ============================================================
    // ESCAPE JSON
    // ============================================================

    private static String escapeJson(
            String value) {

        if (value == null) {

            return "";
        }


        return value
                .replace(
                        "\\",
                        "\\\\"
                )
                .replace(
                        "\"",
                        "\\\""
                )
                .replace(
                        "\n",
                        "\\n"
                )
                .replace(
                        "\r",
                        "\\r"
                );
    }


    // ============================================================
    // JSON RESPONSE
    // ============================================================

    private static void sendJsonResponse(
            HttpExchange exchange,
            int statusCode,
            String response)
            throws IOException {

        byte[] data =
                response.getBytes(
                        StandardCharsets.UTF_8
                );


        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );


        exchange.sendResponseHeaders(
                statusCode,
                data.length
        );


        try (OutputStream output =
                     exchange.getResponseBody()) {

            output.write(data);
        }
    }


    // ============================================================
    // NORMAL RESPONSE
    // ============================================================

    private static void sendResponse(
            HttpExchange exchange,
            int statusCode,
            String response)
            throws IOException {

        byte[] data =
                response.getBytes(
                        StandardCharsets.UTF_8
                );


        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        "text/html; charset=UTF-8"
                );


        exchange.sendResponseHeaders(
                statusCode,
                data.length
        );


        try (OutputStream output =
                     exchange.getResponseBody()) {

            output.write(data);
        }
    }


    // ============================================================
    // CONTENT TYPE
    // ============================================================

    private static String getContentType(
            Path file) {

        String fileName =
                file.getFileName()
                        .toString()
                        .toLowerCase();


        if (fileName.endsWith(".html")) {

            return "text/html; charset=UTF-8";
        }


        if (fileName.endsWith(".css")) {

            return "text/css; charset=UTF-8";
        }


        if (fileName.endsWith(".js")) {

            return "application/javascript; charset=UTF-8";
        }


        if (fileName.endsWith(".json")) {

            return "application/json; charset=UTF-8";
        }


        if (fileName.endsWith(".png")) {

            return "image/png";
        }


        if (fileName.endsWith(".jpg")
                || fileName.endsWith(".jpeg")) {

            return "image/jpeg";
        }


        if (fileName.endsWith(".svg")) {

            return "image/svg+xml";
        }


        return "application/octet-stream";
    }


    // ============================================================
    // USER CLASS
    // ============================================================

    private static class User {

        String name;

        String email;

        String password;


        User(
                String name,
                String email,
                String password) {

            this.name =
                    name;

            this.email =
                    email;

            this.password =
                    password;
        }
    }
}