package com.vadimsjjs.qualitycontrollapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PageController {

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

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("currentPage", "index");
        model.addAttribute("pageTitle", "Главная");
        return "index";
    }

    @GetMapping("/defects")
    public String defectsList(Model model) {
        model.addAttribute("currentPage", "defects");
        model.addAttribute("pageTitle", "Журнал несоответствий");
        return "defects/list";
    }

    @GetMapping("/defects/add")
    public String addDefect(Model model) {
        model.addAttribute("currentPage", "add-defect");
        model.addAttribute("pageTitle", "Добавление записи");
        model.addAttribute("isEdit", false);
        return "defects/add";
    }

    @GetMapping("/defects/edit/{id}")
    public String editDefect(@PathVariable Long id, Model model) {
        model.addAttribute("defectId", id);
        model.addAttribute("currentPage", "add-defect");
        model.addAttribute("pageTitle", "Редактирование записи");
        model.addAttribute("isEdit", true);
        return "defects/add";
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAttribute("currentPage", "reports");
        model.addAttribute("pageTitle", "Отчёты");
        return "reports/index";
    }

    @GetMapping("/charts")
    public String charts(Model model) {
        model.addAttribute("currentPage", "charts");
        model.addAttribute("pageTitle", "Аналитика");
        return "charts/index";
    }

    @GetMapping("/directories")
    public String directories(Model model) {
        model.addAttribute("currentPage", "directories");
        model.addAttribute("pageTitle", "Справочники");
        return "directories/index";
    }

    @GetMapping("/access-denied")
    public String accessDenied(Model model) {
        model.addAttribute("currentPage", "index");
        model.addAttribute("pageTitle", "Доступ запрещён");
        return "access-denied";
    }
}