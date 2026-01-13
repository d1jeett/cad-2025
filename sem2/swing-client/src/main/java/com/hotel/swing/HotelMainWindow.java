package com.hotel.swing;

import com.hotel.swing.client.RoomApiClient;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class HotelMainWindow extends JFrame {
    
    private JTable roomsTable;
    private DefaultTableModel roomsTableModel;
    private JLabel statusLabel;
    private JLabel serverStatusLabel;
    private boolean isConnected = false;
    
    // Поля формы бронирования
    private JTextField roomField;
    private JTextField checkInField;
    private JTextField checkOutField;
    private JTextField guestNameField;
    private JTextField guestEmailField;
    private JTextField phoneField;
    private JTextArea specialRequestsArea;
    private JButton clearBtn;
    
    // Текущий пользователь
    private Long currentUserId = 1L;
    
    public HotelMainWindow() {
        setTitle("Система бронирования отелей - Клиент");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 750);
        setLocationRelativeTo(null);
        
        // Меню
        JMenuBar menuBar = new JMenuBar();
        
        // Меню Файл
        JMenu fileMenu = new JMenu("Файл");
        JMenuItem connectItem = new JMenuItem("Подключиться к серверу");
        JMenuItem disconnectItem = new JMenuItem("Отключиться");
        JMenuItem settingsItem = new JMenuItem("Настройки сервера");
        JMenuItem exitItem = new JMenuItem("Выход");
        
        connectItem.addActionListener(e -> connectToServer());
        disconnectItem.addActionListener(e -> disconnectFromServer());
        exitItem.addActionListener(e -> System.exit(0));
        
        settingsItem.addActionListener(e -> {
            String currentUrl = RoomApiClient.getBaseUrl();
            String newUrl = JOptionPane.showInputDialog(this,
                    "Введите адрес сервера:",
                    "Настройки сервера",
                    JOptionPane.QUESTION_MESSAGE,
                    null, null, currentUrl).toString();
            
            if (newUrl != null && !newUrl.isEmpty()) {
                RoomApiClient.setBaseUrl(newUrl);
                statusLabel.setText("URL сервера изменен на: " + newUrl);
            }
        });
        
        fileMenu.add(connectItem);
        fileMenu.add(disconnectItem);
        fileMenu.addSeparator();
        fileMenu.add(settingsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        // Меню Отель
        JMenu hotelMenu = new JMenu("Отель");
        JMenuItem refreshRoomsItem = new JMenuItem("Обновить список номеров");
        JMenuItem autoConnectItem = new JMenuItem("Автоподключение");
        
        refreshRoomsItem.addActionListener(e -> refreshRoomsList());
        autoConnectItem.addActionListener(e -> {
            if (!isConnected) {
                connectToServer();
            }
        });
        
        hotelMenu.add(refreshRoomsItem);
        hotelMenu.add(autoConnectItem);
        
        // Меню Помощь
        JMenu helpMenu = new JMenu("Помощь");
        JMenuItem aboutItem = new JMenuItem("О программе");
        JMenuItem helpItem = new JMenuItem("Справка");
        JMenuItem apiTestItem = new JMenuItem("Тест API");
        
        aboutItem.addActionListener(e -> showAbout());
        helpItem.addActionListener(e -> showHelp());
        apiTestItem.addActionListener(e -> testApiConnection());
        
        helpMenu.add(helpItem);
        helpMenu.add(apiTestItem);
        helpMenu.addSeparator();
        helpMenu.add(aboutItem);
        
        menuBar.add(fileMenu);
        menuBar.add(hotelMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
        
        // Панель с вкладками
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Вкладка 1: ДОСТУПНЫЕ НОМЕРА
        JPanel roomsPanel = createRoomsPanel();
        
        // Вкладка 2: БРОНИРОВАНИЕ
        JPanel bookingPanel = createBookingPanel();
        
        // Вкладка 3: МОИ БРОНИРОВАНИЯ
        JPanel myBookingsPanel = createMyBookingsPanel();
        
        // Вкладка 4: ПОИСК
        JPanel searchPanel = createSearchPanel();
        
        // Добавляем вкладки
        tabbedPane.addTab("Доступные номера", roomsPanel);
        tabbedPane.addTab("Бронирование", bookingPanel);
        tabbedPane.addTab("Мои бронирования", myBookingsPanel);
        tabbedPane.addTab("Поиск", searchPanel);
        
        add(tabbedPane);
        
        // Статус бар
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Готов к работе. Нажмите 'Подключиться к серверу'.");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        serverStatusLabel = new JLabel("Сервер: отключен");
        serverStatusLabel.setForeground(Color.RED);
        serverStatusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(serverStatusLabel, BorderLayout.EAST);
        statusPanel.setBorder(BorderFactory.createEtchedBorder());
        add(statusPanel, BorderLayout.SOUTH);
        
        // Автоподключение при запуске
        SwingUtilities.invokeLater(() -> {
            if (RoomApiClient.testConnection()) {
                isConnected = true;
                statusLabel.setText("Автоподключение успешно! Сервер работает.");
                serverStatusLabel.setText("Сервер: онлайн");
                serverStatusLabel.setForeground(new Color(0, 150, 0));
                refreshRoomsList();
            }
        });
    }
    
    /**
     * Подключение к серверу
     */
    private void connectToServer() {
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                statusLabel.setText("Проверка подключения к серверу...");
                serverStatusLabel.setText("Сервер: проверка...");
                serverStatusLabel.setForeground(Color.ORANGE);
                
                return RoomApiClient.testConnection();
            }
            
            @Override
            protected void done() {
                try {
                    boolean connected = get();
                    if (connected) {
                        isConnected = true;
                        statusLabel.setText("Успешно подключено к " + RoomApiClient.getBaseUrl());
                        serverStatusLabel.setText("Сервер: онлайн");
                        serverStatusLabel.setForeground(new Color(0, 150, 0));
                        
                        // Загружаем номера после подключения
                        refreshRoomsList();
                    } else {
                        showConnectionError();
                    }
                } catch (Exception e) {
                    showConnectionError();
                }
            }
        };
        worker.execute();
    }
    
    /**
     * Отключение от сервера
     */
    private void disconnectFromServer() {
        isConnected = false;
        statusLabel.setText("Отключено от сервера");
        serverStatusLabel.setText("Сервер: отключен");
        serverStatusLabel.setForeground(Color.RED);
        
        // Очищаем таблицу
        if (roomsTableModel != null) {
            roomsTableModel.setRowCount(0);
        }
    }
    
    /**
     * Показать ошибку подключения
     */
    private void showConnectionError() {
        statusLabel.setText("Не удалось подключиться к серверу!");
        serverStatusLabel.setText("Сервер: недоступен");
        serverStatusLabel.setForeground(Color.RED);
        
        JOptionPane.showMessageDialog(this,
            "Ошибка подключения к серверу!\n" +
            "Убедитесь, что:\n" +
            "1. Сервер Spring Boot запущен\n" +
            "2. Адрес сервера: " + RoomApiClient.getBaseUrl() + "\n" +
            "3. Нет блокировки брандмауэром",
            "Ошибка подключения",
            JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Обновление списка номеров
     */
    private void refreshRoomsList() {
        if (!isConnected) {
            JOptionPane.showMessageDialog(this,
                "Сначала подключитесь к серверу!",
                "Ошибка",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        SwingWorker<JSONArray, Void> worker = new SwingWorker<JSONArray, Void>() {
            @Override
            protected JSONArray doInBackground() throws Exception {
                return RoomApiClient.getAllRooms();
            }
            
            @Override
            protected void done() {
                try {
                    JSONArray rooms = get();
                    updateRoomsTable(rooms);
                    
                    JOptionPane.showMessageDialog(HotelMainWindow.this,
                        "Список номеров успешно обновлен!\n" +
                        "Загружено: " + rooms.length() + " номеров",
                        "Успех",
                        JOptionPane.INFORMATION_MESSAGE);
                        
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(HotelMainWindow.this,
                        "Ошибка при загрузке данных:\n" + e.getMessage(),
                        "Ошибка",
                        JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }
    
    /**
     * Обновление таблицы номеров
     */
    private void updateRoomsTable(JSONArray rooms) {
        if (roomsTableModel == null) return;
        
        roomsTableModel.setRowCount(0);
        
        for (int i = 0; i < rooms.length(); i++) {
            JSONObject room = rooms.getJSONObject(i);
            
            String type = room.optString("displayType", room.getString("type"));
            String status = room.getBoolean("available") ? "Доступен" : "Занят";
            int price = room.getInt("price");
            
            roomsTableModel.addRow(new Object[]{
                room.getString("number"),
                type,
                room.getInt("capacity") + " человека",
                String.format("%,d", price) + " руб.",
                status,
                room.getLong("id")
            });
        }
    }
    
    private JPanel createRoomsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Заголовок
        JLabel titleLabel = new JLabel("Список доступных номеров отеля (реальные данные)");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Таблица с номерами
        String[] columnNames = {"№", "Тип", "Вместимость", "Цена за ночь", "Статус", "ID"};
        roomsTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 5) return Long.class;
                return String.class;
            }
        };
        
        roomsTable = new JTable(roomsTableModel);
        roomsTable.setRowHeight(30);
        roomsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomsTable.getTableHeader().setReorderingAllowed(false);
        
        // Скрываем ID колонку
        roomsTable.removeColumn(roomsTable.getColumnModel().getColumn(5));
        
        // Центрируем текст
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < roomsTable.getColumnCount(); i++) {
            roomsTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        
        // Раскрашиваем строки по статусу
        roomsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, 
                        isSelected, hasFocus, row, column);
                
                String status = (String) table.getModel().getValueAt(row, 4);
                
                if (isSelected) {
                    c.setBackground(new Color(200, 220, 255));
                    c.setForeground(Color.BLACK);
                } else {
                    if ("Доступен".equals(status)) {
                        c.setBackground(new Color(220, 255, 220));
                        c.setForeground(Color.BLACK);
                    } else if ("Занят".equals(status)) {
                        c.setBackground(new Color(255, 220, 220));
                        c.setForeground(Color.BLACK);
                    } else {
                        c.setBackground(Color.WHITE);
                        c.setForeground(Color.BLACK);
                    }
                }
                
                return c;
            }
        });
        
        JScrollPane tableScrollPane = new JScrollPane(roomsTable);
        
        // Панель кнопок
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JButton refreshBtn = new JButton("Обновить список");
        JButton bookSelectedBtn = new JButton("Забронировать выбранный");
        JButton detailsBtn = new JButton("Подробнее о номере");
        
        // Стили кнопок
        bookSelectedBtn.setBackground(new Color(76, 175, 80));
        bookSelectedBtn.setForeground(Color.WHITE);
        detailsBtn.setBackground(new Color(33, 150, 243));
        detailsBtn.setForeground(Color.WHITE);
        
        refreshBtn.addActionListener(e -> refreshRoomsList());
        
        bookSelectedBtn.addActionListener(e -> {
            int selectedRow = roomsTable.getSelectedRow();
            if (selectedRow >= 0) {
                String roomNumber = (String) roomsTable.getValueAt(selectedRow, 0);
                String roomType = (String) roomsTable.getValueAt(selectedRow, 1);
                String price = (String) roomsTable.getValueAt(selectedRow, 3);
                String status = (String) roomsTable.getValueAt(selectedRow, 4);
                
                // Получаем ID комнаты
                Long roomId = (Long) roomsTableModel.getValueAt(selectedRow, 5);
                
                if ("Занят".equals(status)) {
                    JOptionPane.showMessageDialog(this,
                        "Номер " + roomNumber + " занят!\nВыберите другой номер.",
                        "Ошибка",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Автозаполняем форму бронирования
                roomField.setText(roomNumber);
                roomField.putClientProperty("roomId", roomId);
                
                // Устанавливаем текущие даты по умолчанию
                LocalDate tomorrow = LocalDate.now().plusDays(1);
                LocalDate checkOut = tomorrow.plusDays(3);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                
                checkInField.setText(tomorrow.format(formatter));
                checkOutField.setText(checkOut.format(formatter));
                
                // Переключаемся на вкладку бронирования
                ((JTabbedPane)getContentPane().getComponent(0)).setSelectedIndex(1);
                
                JOptionPane.showMessageDialog(this,
                    "Номер " + roomNumber + " (" + roomType + ") выбран.\n" +
                    "Цена: " + price + " за ночь\n" +
                    "Даты автоматически заполнены.\n" +
                    "Заполните остальные поля формы.",
                    "Информация",
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Выберите номер из таблицы",
                    "Внимание",
                    JOptionPane.WARNING_MESSAGE);
            }
        });
        
        detailsBtn.addActionListener(e -> {
            int selectedRow = roomsTable.getSelectedRow();
            if (selectedRow >= 0) {
                String roomNumber = (String) roomsTable.getValueAt(selectedRow, 0);
                String roomType = (String) roomsTable.getValueAt(selectedRow, 1);
                String capacity = (String) roomsTable.getValueAt(selectedRow, 2);
                String price = (String) roomsTable.getValueAt(selectedRow, 3);
                String status = (String) roomsTable.getValueAt(selectedRow, 4);
                
                JOptionPane.showMessageDialog(this,
                    "Детали номера " + roomNumber + ":\n\n" +
                    "Тип: " + roomType + "\n" +
                    "Вместимость: " + capacity + "\n" +
                    "Цена за ночь: " + price + "\n" +
                    "Статус: " + status + "\n\n" +
                    ( "Доступен".equals(status) ? 
                      "Номер свободен для бронирования" : 
                      "Номер занят, выберите другой" ),
                    "Информация о номере",
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Выберите номер из таблицы",
                    "Внимание",
                    JOptionPane.WARNING_MESSAGE);
            }
        });
        
        buttonPanel.add(refreshBtn);
        buttonPanel.add(bookSelectedBtn);
        buttonPanel.add(detailsBtn);
        
        panel.add(tableScrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createBookingPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Заголовок
        JLabel titleLabel = new JLabel("Форма бронирования номера (реальное API)");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(titleLabel, gbc);
        
        // Поля формы
        gbc.gridwidth = 1;
        
        // Номер комнаты
        gbc.gridy = 1;
        gbc.gridx = 0;
        panel.add(new JLabel("Номер комнаты:"), gbc);
        gbc.gridx = 1;
        roomField = new JTextField(15);
        panel.add(roomField, gbc);
        
        // Даты
        gbc.gridy = 2;
        gbc.gridx = 0;
        panel.add(new JLabel("Дата заезда (ГГГГ-ММ-ДД):"), gbc);
        gbc.gridx = 1;
        checkInField = new JTextField(15);
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        checkInField.setText(tomorrow.toString());
        panel.add(checkInField, gbc);
        
        gbc.gridy = 3;
        gbc.gridx = 0;
        panel.add(new JLabel("Дата выезда (ГГГГ-ММ-ДД):"), gbc);
        gbc.gridx = 1;
        checkOutField = new JTextField(15);
        checkOutField.setText(tomorrow.plusDays(3).toString());
        panel.add(checkOutField, gbc);
        
        // Данные гостя
        gbc.gridy = 4;
        gbc.gridx = 0;
        panel.add(new JLabel("ФИО гостя:*"), gbc);
        gbc.gridx = 1;
        guestNameField = new JTextField(15);
        panel.add(guestNameField, gbc);
        
        gbc.gridy = 5;
        gbc.gridx = 0;
        panel.add(new JLabel("Email гостя:*"), gbc);
        gbc.gridx = 1;
        guestEmailField = new JTextField(15);
        panel.add(guestEmailField, gbc);
        
        gbc.gridy = 6;
        gbc.gridx = 0;
        panel.add(new JLabel("Телефон:"), gbc);
        gbc.gridx = 1;
        phoneField = new JTextField(15);
        panel.add(phoneField, gbc);
        
        // Особые пожелания
        gbc.gridy = 7;
        gbc.gridx = 0;
        panel.add(new JLabel("Особые пожелания:"), gbc);
        gbc.gridx = 1;
        specialRequestsArea = new JTextArea(3, 15);
        JScrollPane scrollPane = new JScrollPane(specialRequestsArea);
        panel.add(scrollPane, gbc);
        
        // Кнопки
        gbc.gridy = 8;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        JButton checkAvailabilityBtn = new JButton("Проверить доступность");
        JButton calculatePriceBtn = new JButton("Рассчитать стоимость");
        JButton bookBtn = new JButton("Забронировать");
        clearBtn = new JButton("Очистить форму");
        
        bookBtn.setBackground(new Color(76, 175, 80));
        bookBtn.setForeground(Color.WHITE);
        bookBtn.setFont(new Font("Arial", Font.BOLD, 14));
        
        // Обработчики кнопок
        checkAvailabilityBtn.addActionListener(e -> checkAvailability());
        calculatePriceBtn.addActionListener(e -> calculatePrice());
        bookBtn.addActionListener(e -> createBooking());
        
        clearBtn.addActionListener(e -> {
            roomField.setText("");
            roomField.putClientProperty("roomId", null);
            
            LocalDate tomorrowDate = LocalDate.now().plusDays(1);
            checkInField.setText(tomorrowDate.toString());
            checkOutField.setText(tomorrowDate.plusDays(3).toString());
            
            guestNameField.setText("");
            guestEmailField.setText("");
            phoneField.setText("");
            specialRequestsArea.setText("");
        });
        
        buttonPanel.add(checkAvailabilityBtn);
        buttonPanel.add(calculatePriceBtn);
        buttonPanel.add(bookBtn);
        buttonPanel.add(clearBtn);
        
        panel.add(buttonPanel, gbc);
        
        return panel;
    }
    
    /**
     * Проверка доступности номера
     */
    private void checkAvailability() {
        if (!isConnected) {
            JOptionPane.showMessageDialog(this,
                "Сначала подключитесь к серверу!",
                "Ошибка",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String roomNumber = roomField.getText().trim();
        String checkIn = checkInField.getText().trim();
        String checkOut = checkOutField.getText().trim();
        
        // Валидация
        if (roomNumber.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Введите номер комнаты",
                "Ошибка",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (!isValidDate(checkIn) || !isValidDate(checkOut)) {
            JOptionPane.showMessageDialog(this,
                "Неверный формат даты! Используйте ГГГГ-ММ-ДД",
                "Ошибка",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            Long roomId = getRoomIdFromField();
            if (roomId == null) {
                roomId = Long.parseLong(roomNumber);
            }
            
            JSONObject result = RoomApiClient.checkRoomAvailability(roomId, checkIn, checkOut);
            boolean available = result.getBoolean("available");
            
            if (available) {
                double price = result.getDouble("price");
                JOptionPane.showMessageDialog(this,
                    "Номер " + roomNumber + " ДОСТУПЕН на даты:\n" +
                    "Заезд: " + checkIn + "\n" +
                    "Выезд: " + checkOut + "\n" +
                    "Общая стоимость: " + String.format("%,.0f", price) + " руб.",
                    "Проверка доступности",
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Номер " + roomNumber + " ЗАНЯТ на выбранные даты\n" +
                    "Попробуйте другие даты или выберите другой номер",
                    "Недоступен",
                    JOptionPane.WARNING_MESSAGE);
            }
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                "Номер комнаты должен быть числом",
                "Ошибка",
                JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Ошибка проверки доступности:\n" + e.getMessage(),
                "Ошибка",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Расчет стоимости
     */
    private void calculatePrice() {
        if (!isConnected) {
            JOptionPane.showMessageDialog(this,
                "Сначала подключитесь к серверу!",
                "Ошибка",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String roomNumber = roomField.getText().trim();
        String checkIn = checkInField.getText().trim();
        String checkOut = checkOutField.getText().trim();
        
        if (roomNumber.isEmpty() || !isValidDate(checkIn) || !isValidDate(checkOut)) {
            JOptionPane.showMessageDialog(this,
                "Заполните номер комнаты и даты (формат ГГГГ-ММ-ДД)",
                "Ошибка",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            Long roomId = getRoomIdFromField();
            if (roomId == null) {
                roomId = Long.parseLong(roomNumber);
            }
            
            JSONObject result = RoomApiClient.calculatePrice(roomId, checkIn, checkOut);
            
            if (result.getBoolean("available")) {
                double price = result.getDouble("price");
                long days = java.time.temporal.ChronoUnit.DAYS.between(
                    LocalDate.parse(checkIn), LocalDate.parse(checkOut));
                
                JOptionPane.showMessageDialog(this,
                    "Расчет стоимости:\n\n" +
                    "Номер: " + roomNumber + "\n" +
                    "Период: " + days + " ночей\n" +
                    "С " + checkIn + " по " + checkOut + "\n" +
                    "Общая стоимость: " + String.format("%,.0f", price) + " руб.\n" +
                    "Средняя цена за ночь: " + String.format("%,.0f", price / days) + " руб.",
                    "Расчет стоимости",
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Номер недоступен на выбранные даты",
                    "Ошибка",
                    JOptionPane.WARNING_MESSAGE);
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Ошибка расчета стоимости:\n" + e.getMessage(),
                "Ошибка",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Создание бронирования
     */
    private void createBooking() {
        if (!isConnected) {
            JOptionPane.showMessageDialog(this,
                "Сначала подключитесь к серверу!",
                "Ошибка",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Сбор данных
        String roomNumber = roomField.getText().trim();
        String guestName = guestNameField.getText().trim();
        String guestEmail = guestEmailField.getText().trim();
        String checkIn = checkInField.getText().trim();
        String checkOut = checkOutField.getText().trim();
        String phone = phoneField.getText().trim();
        String requests = specialRequestsArea.getText().trim();
        
        // Валидация
        StringBuilder errors = new StringBuilder();
        if (roomNumber.isEmpty()) errors.append("• Номер комнаты\n");
        if (guestName.isEmpty()) errors.append("• ФИО гостя\n");
        if (guestEmail.isEmpty()) errors.append("• Email гостя\n");
        if (!isValidDate(checkIn)) errors.append("• Дата заезда (формат ГГГГ-ММ-ДД)\n");
        if (!isValidDate(checkOut)) errors.append("• Дата выезда (формат ГГГГ-ММ-ДД)\n");
        
        if (errors.length() > 0) {
            JOptionPane.showMessageDialog(this,
                "Заполните обязательные поля:\n" + errors.toString(),
                "Ошибка",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Подтверждение
        int confirm = JOptionPane.showConfirmDialog(this,
            "Подтвердите бронирование:\n\n" +
            "Номер: " + roomNumber + "\n" +
            "Гость: " + guestName + "\n" +
            "Email: " + guestEmail + "\n" +
            "Даты: " + checkIn + " - " + checkOut + "\n\n" +
            "Создать бронирование?",
            "Подтверждение бронирования",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        // Создание бронирования в фоновом потоке
        SwingWorker<JSONObject, Void> worker = new SwingWorker<JSONObject, Void>() {
            @Override
            protected JSONObject doInBackground() throws Exception {
                Long roomId = getRoomIdFromField();
                if (roomId == null) {
                    roomId = Long.parseLong(roomNumber);
                }
                
                // Добавляем телефон к особым пожеланиям
                String fullRequests = requests;
                if (!phone.isEmpty()) {
                    fullRequests = (requests.isEmpty() ? "" : requests + "\n") + 
                                  "Телефон: " + phone;
                }
                
                return RoomApiClient.createBooking(
                    roomId, currentUserId, checkIn, checkOut, 
                    guestName, guestEmail, fullRequests
                );
            }
            
            @Override
            protected void done() {
                try {
                    JSONObject booking = get();
                    
                    // Успешное создание
                    JOptionPane.showMessageDialog(HotelMainWindow.this,
                        "Бронирование создано успешно!\n\n" +
                        "Номер бронирования: #" + booking.getLong("id") + "\n" +
                        "Статус: " + booking.getString("status") + "\n" +
                        "Номер комнаты: " + roomNumber + "\n" +
                        "Гость: " + guestName + "\n" +
                        "Даты: " + checkIn + " - " + checkOut + "\n\n" +
                        "Подробности во вкладке 'Мои бронирования'",
                        "Успех",
                        JOptionPane.INFORMATION_MESSAGE);
                    
                    // Очищаем форму
                    clearBtn.doClick();
                    
                    // Обновляем список номеров
                    refreshRoomsList();
                    
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(HotelMainWindow.this,
                        "Ошибка при создании бронирования:\n" + e.getMessage(),
                        "Ошибка",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    /**
     * Получение ID комнаты из поля
     */
    private Long getRoomIdFromField() {
        Object roomIdObj = roomField.getClientProperty("roomId");
        if (roomIdObj instanceof Long) {
            return (Long) roomIdObj;
        }
        return null;
    }
    
    /**
     * Проверка формата даты
     */
    private boolean isValidDate(String dateStr) {
        try {
            LocalDate.parse(dateStr);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
    
    private JPanel createMyBookingsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Заголовок
        JLabel titleLabel = new JLabel("Мои бронирования (реальные данные)");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Список бронирований
        DefaultListModel<String> bookingsModel = new DefaultListModel<String>();
        JList<String> bookingsList = new JList<>(bookingsModel);
        bookingsList.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane listScrollPane = new JScrollPane(bookingsList);
        
        // Панель кнопок
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshBtn = new JButton("Обновить мои бронирования");
        
        refreshBtn.addActionListener(e -> {
            if (!isConnected) {
                JOptionPane.showMessageDialog(this,
                    "Сначала подключитесь к серверу!",
                    "Ошибка",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            SwingWorker<JSONArray, Void> worker = new SwingWorker<JSONArray, Void>() {
                @Override
                protected JSONArray doInBackground() throws Exception {
                    return RoomApiClient.getUserBookings(currentUserId);
                }
                
                @Override
                protected void done() {
                    try {
                        JSONArray bookings = get();
                        bookingsModel.clear();
                        
                        if (bookings.length() == 0) {
                            bookingsModel.addElement("У вас пока нет бронирований");
                        } else {
                            for (int i = 0; i < bookings.length(); i++) {
                                JSONObject booking = bookings.getJSONObject(i);
                                JSONObject room = booking.getJSONObject("room");
                                
                                bookingsModel.addElement(String.format(
                                    "#%d - Номер %s - %s - %s до %s",
                                    booking.getLong("id"),
                                    room.getString("number"),
                                    booking.getString("status"),
                                    booking.getString("checkInDate"),
                                    booking.getString("checkOutDate")
                                ));
                            }
                        }
                        
                    } catch (Exception ex) {
                        bookingsModel.clear();
                        bookingsModel.addElement("Ошибка загрузки: " + ex.getMessage());
                    }
                }
            };
            worker.execute();
        });
        
        buttonPanel.add(refreshBtn);
        
        panel.add(listScrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Панель поиска
        JPanel searchPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Критерии поиска"));
        
        searchPanel.add(new JLabel("Тип номера:"));
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Любой", "STANDARD", "VIP", "DELUXE", "SUITE", "FAMILY", "EXECUTIVE"});
        searchPanel.add(typeCombo);
        
        searchPanel.add(new JLabel("Минимальная вместимость:"));
        JSpinner capacitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        searchPanel.add(capacitySpinner);
        
        searchPanel.add(new JLabel("Макс. цена за ночь:"));
        JTextField maxPriceField = new JTextField("10000");
        searchPanel.add(maxPriceField);
        
        // Кнопки поиска
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton searchBtn = new JButton("Найти номера");
        JButton clearSearchBtn = new JButton("Очистить");
        
        // Результаты поиска
        JTextArea resultsArea = new JTextArea();
        resultsArea.setEditable(false);
        resultsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane resultsScrollPane = new JScrollPane(resultsArea);
        
        searchBtn.addActionListener(e -> {
            if (!isConnected) {
                JOptionPane.showMessageDialog(this,
                    "Сначала подключитесь к серверу!",
                    "Ошибка",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            try {
                String type = (String) typeCombo.getSelectedItem();
                if ("Любой".equals(type)) type = null;
                
                Integer minCapacity = (Integer) capacitySpinner.getValue();
                Double maxPrice = null;
                
                try {
                    maxPrice = Double.parseDouble(maxPriceField.getText());
                } catch (NumberFormatException ex) {
                    // Оставляем null
                }
                
                JSONArray results = RoomApiClient.searchRooms(type, minCapacity, maxPrice);
                
                StringBuilder output = new StringBuilder();
                output.append("Найдено номеров: ").append(results.length()).append("\n\n");
                
                for (int i = 0; i < results.length(); i++) {
                    JSONObject room = results.getJSONObject(i);
                    output.append(String.format("№%s - %s\n", 
                        room.getString("number"), 
                        room.optString("displayType", room.getString("type"))));
                    output.append(String.format("  Вместимость: %d чел.\n", room.getInt("capacity")));
                    output.append(String.format("  Цена: %,d руб./ночь\n", room.getInt("price")));
                    output.append(String.format("  Статус: %s\n\n", 
                        room.getBoolean("available") ? "Доступен" : "Занят"));
                }
                
                resultsArea.setText(output.toString());
                
            } catch (Exception ex) {
                resultsArea.setText("Ошибка поиска: " + ex.getMessage());
            }
        });
        
        clearSearchBtn.addActionListener(e -> {
            typeCombo.setSelectedIndex(0);
            capacitySpinner.setValue(1);
            maxPriceField.setText("10000");
            resultsArea.setText("");
        });
        
        buttonPanel.add(searchBtn);
        buttonPanel.add(clearSearchBtn);
        
        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(resultsScrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Тестирование API
     */
    private void testApiConnection() {
        if (RoomApiClient.testConnection()) {
            JOptionPane.showMessageDialog(this,
                "API сервера работает нормально!\n" +
                "Адрес: " + RoomApiClient.getBaseUrl() + "\n" +
                "Все функции доступны.",
                "Тест API",
                JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                "Не удалось подключиться к API сервера!\n" +
                "Проверьте:\n" +
                "1. Запущен ли сервер Spring Boot\n" +
                "2. Адрес: " + RoomApiClient.getBaseUrl() + "\n" +
                "3. Нет ли ошибок в консоли сервера",
                "Ошибка теста",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void showAbout() {
        JOptionPane.showMessageDialog(this,
            "Система бронирования отелей - Клиент v1.0\n\n" +
            "Функции:\n" +
            "• Просмотр доступных номеров с сервера\n" +
            "• Проверка доступности в реальном времени\n" +
            "• Онлайн бронирование через API\n" +
            "• Просмотр истории бронирований\n" +
            "• Поиск номеров по критериям\n\n" +
            "Требует запущенный сервер Spring Boot\n" +
            "По умолчанию: http://localhost:8080",
            "О программе",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showHelp() {
        JOptionPane.showMessageDialog(this,
            "Инструкция по использованию:\n\n" +
            "1. Запустите сервер Spring Boot:\n" +
            "   mvn spring-boot:run\n\n" +
            "2. Подключитесь к серверу:\n" +
            "   Файл → Подключиться к серверу\n\n" +
            "3. Работа с системой:\n" +
            "   • Вкладка 'Доступные номера' - просмотр и выбор\n" +
            "   • Вкладка 'Бронирование' - создание брони\n" +
            "   • Вкладка 'Мои бронирования' - история\n" +
            "   • Вкладка 'Поиск' - поиск по параметрам\n\n" +
            "4. При возникновении проблем:\n" +
            "   • Проверьте консоль сервера на ошибки\n" +
            "   • Убедитесь что сервер доступен по адресу\n" +
            "   • Используйте 'Тест API' в меню Помощь",
            "Справка",
            JOptionPane.INFORMATION_MESSAGE);
    }
}