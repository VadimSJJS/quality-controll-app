package com.vadimsjjs.qualitycontrollapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PageController {

    private static final String ERROR = "error";
    private static final String LOGOUT = "logout";
    private static final String EXPIRED = "expired";

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        @RequestParam(required = false) String expired,
                        Model model) {
        if (error != null) model.addAttribute("error", "Неверный табельный номер или пароль");
        if (logout != null) model.addAttribute("message", "Вы успешно вышли из системы");
        if (expired != null) model.addAttribute("error", "Сессия истекла, войдите заново");
        return "login";
    }

    @GetMapping({"/", "/defects", "/defects/add", "/reports", "/charts", "/directories", "/access-denied"})
    public String pages(String page, Model model) {
        return resolvePage(page, model);
    }

    private String resolvePage(String page, Model model) {
        return switch (page != null ? page : "index") {
            case "index" -> setPage(model, "index", "Главная", "index");
            case "defects" -> setPage(model, "defects", "Журнал несоответствий", "defects/list");
            case "defects/add" -> setPage(model, "add-defect", "Добавить запись", "defects/add");
            case "reports" -> setPage(model, "reports", "Отчёты", "reports/index");
            case "charts" -> setPage(model, "charts", "Аналитика", "charts/index");
            case "directories" -> setPage(model, "directories", "Справочники", "directories/index");
            case "access-denied" -> setPage(model, "index", "Доступ запрещён", "access-denied");
            default -> "error/404";
        };
    }

    private String setPage(Model model, String currentPage, String pageTitle, String template) {
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("pageTitle", pageTitle);
        return template;
    }
}