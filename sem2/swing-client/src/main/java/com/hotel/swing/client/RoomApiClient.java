package com.hotel.swing.client;

import org.json.JSONArray;
import org.json.JSONObject;
import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class RoomApiClient {
    
    private static String baseUrl = "http://localhost:8080/api";
    
    // Установить базовый URL
    public static void setBaseUrl(String url) {
        baseUrl = url;
        System.out.println("API URL установлен: " + baseUrl);
    }
    
    // Получить текущий URL
    public static String getBaseUrl() {
        return baseUrl;
    }
    
    /**
     * Получить список всех номеров
     */
    public static JSONArray getAllRooms() throws Exception {
        URL url = new URL(baseUrl + "/rooms/available");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            return new JSONArray(response.toString());
        } else {
            throw new Exception("Ошибка сервера: " + responseCode);
        }
    }
    
    /**
     * Проверить доступность номера на даты
     */
    public static JSONObject checkRoomAvailability(Long roomId, String checkIn, String checkOut) throws Exception {
        String urlStr = baseUrl + "/rooms/" + roomId + "/availability" +
                       "?checkIn=" + URLEncoder.encode(checkIn, "UTF-8") +
                       "&checkOut=" + URLEncoder.encode(checkOut, "UTF-8");
        
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(3000);
        
        int responseCode = conn.getResponseCode();
        BufferedReader reader;
        
        if (responseCode == 200) {
            reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        } else {
            reader = new BufferedReader(
                new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
        }
        
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        if (responseCode == 200) {
            return new JSONObject(response.toString());
        } else {
            JSONObject error = new JSONObject(response.toString());
            throw new Exception("Ошибка: " + error.optString("error", "Unknown error"));
        }
    }
    
    /**
     * Создать бронирование
     */
    public static JSONObject createBooking(Long roomId, Long userId, String checkIn, 
                                         String checkOut, String guestName, 
                                         String guestEmail, String specialRequests) throws Exception {
        URL url = new URL(baseUrl + "/bookings");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        
        // Создаем JSON данные
        JSONObject bookingData = new JSONObject();
        bookingData.put("roomId", roomId);
        bookingData.put("userId", userId);
        bookingData.put("checkIn", checkIn);
        bookingData.put("checkOut", checkOut);
        bookingData.put("guestName", guestName);
        bookingData.put("guestEmail", guestEmail);
        bookingData.put("specialRequests", specialRequests);
        
        // Отправляем данные
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = bookingData.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        int responseCode = conn.getResponseCode();
        BufferedReader reader;
        
        if (responseCode >= 200 && responseCode < 300) {
            reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        } else {
            reader = new BufferedReader(
                new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
        }
        
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        if (responseCode >= 200 && responseCode < 300) {
            return new JSONObject(response.toString());
        } else {
            JSONObject error = new JSONObject(response.toString());
            throw new Exception("Ошибка: " + error.optString("error", "Unknown error (Code: " + responseCode + ")"));
        }
    }
    
    /**
     * Получить бронирования пользователя
     */
    public static JSONArray getUserBookings(Long userId) throws Exception {
        URL url = new URL(baseUrl + "/users/" + userId + "/bookings");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(3000);
        
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            return new JSONArray(response.toString());
        } else {
            throw new Exception("Ошибка сервера: " + responseCode);
        }
    }
    
    /**
     * Рассчитать стоимость
     */
    public static JSONObject calculatePrice(Long roomId, String checkIn, String checkOut) throws Exception {
        String urlStr = baseUrl + "/rooms/" + roomId + "/availability" +
                       "?checkIn=" + URLEncoder.encode(checkIn, "UTF-8") +
                       "&checkOut=" + URLEncoder.encode(checkOut, "UTF-8");
        
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(3000);
        
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            return new JSONObject(response.toString());
        } else {
            throw new Exception("Ошибка расчета: " + responseCode);
        }
    }
    
    /**
     * Тестирование подключения к серверу
     */
    public static boolean testConnection() {
        try {
            URL url = new URL(baseUrl + "/test");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            
            int responseCode = conn.getResponseCode();
            return responseCode == 200;
        } catch (Exception e) {
            System.err.println("Ошибка подключения: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Поиск номеров по параметрам
     */
    public static JSONArray searchRooms(String type, Integer minCapacity, Double maxPrice) throws Exception {
        StringBuilder urlBuilder = new StringBuilder(baseUrl + "/rooms/search?");
        
        if (type != null && !type.equals("Любой")) {
            urlBuilder.append("type=").append(URLEncoder.encode(type, "UTF-8")).append("&");
        }
        if (minCapacity != null) {
            urlBuilder.append("minCapacity=").append(minCapacity).append("&");
        }
        if (maxPrice != null) {
            urlBuilder.append("maxPrice=").append(maxPrice).append("&");
        }
        
        String urlStr = urlBuilder.toString();
        if (urlStr.endsWith("&")) {
            urlStr = urlStr.substring(0, urlStr.length() - 1);
        }
        
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(3000);
        
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            return new JSONArray(response.toString());
        } else {
            throw new Exception("Ошибка поиска: " + responseCode);
        }
    }
}