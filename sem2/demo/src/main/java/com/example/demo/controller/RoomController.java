package com.example.demo.controller;

import com.example.demo.model.Room;
import com.example.demo.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @GetMapping("/")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR')")
    public String index(Model model) {
        model.addAttribute("rooms", roomService.getAllRooms());
        return "index";
    }

    @PostMapping("/addRoom")
    @PreAuthorize("hasRole('MODERATOR')")
    public String addRoom(Room room) {
        roomService.save(room);
        return "redirect:/";
    }

    @GetMapping("/deleteRoom/{id}")
    @PreAuthorize("hasRole('MODERATOR')")
    public String deleteRoom(@PathVariable Long id) {
        roomService.delete(id);
        return "redirect:/";
    }

    @GetMapping("/api/rooms")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR')")
    public ResponseEntity<List<Room>> getRoomsApi() {
        return ResponseEntity.ok(roomService.getAllRooms());
    }
}