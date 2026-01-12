package com.example.demo.controller;

import com.example.demo.model.Room;
import com.example.demo.service.RoomService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {
    
    private final RoomService roomService;
    
    public MainController(RoomService roomService) {
        this.roomService = roomService;
    }
    
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("rooms", roomService.getAvailableRooms());
        return "index";
    }
    
    @GetMapping("/login")
    public String login() {
        return "login";
    }
    
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}