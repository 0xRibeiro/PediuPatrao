package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.service.DashboardService;
import org.springframework.dao.DataAccessException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<DashboardService.Resumo> consultar(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate inicio,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fim,

            @RequestParam(required = false)
            String status) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.consultar(inicio, fim, status));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> filtroInvalido(
            IllegalArgumentException ex) {

        return ResponseEntity.badRequest()
                .body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> dataInvalida() {
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "erro",
                        "Use datas no formato AAAA-MM-DD."
                ));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> bancoIndisponivel() {
        return ResponseEntity.status(503)
                .body(Map.of(
                        "erro",
                        "Não foi possível consultar o banco. Tente novamente."
                ));
    }
}