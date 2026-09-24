package com.vadimsjjs.qualitycontrollapp.controller;

import org.springframework.security.access.prepost.PreAuthorize;
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
                        @RequestParam(required = false) String blocked,
                        @RequestParam(required = false) String minutes,
                        Model model) {
        if (error != null) model.addAttribute("error", "РќРµРІРµСЂРЅС‹Р№ С‚Р°Р±РµР»СЊРЅС‹Р№ РЅРѕРјРµСЂ РёР»Рё РїР°СЂРѕР»СЊ");
        if (logout != null) model.addAttribute("message", "Р’С‹ СѓСЃРїРµС€РЅРѕ РІС‹С€Р»Рё РёР· СЃРёСЃС‚РµРјС‹");
        if (expired != null) model.addAttribute("error", "РЎРµСЃСЃРёСЏ РёСЃС‚РµРєР»Р°, РІРѕР№РґРёС‚Рµ Р·Р°РЅРѕРІРѕ");
        if (blocked != null && minutes != null) {
            model.addAttribute("blocked", true);
            model.addAttribute("remainingMinutes", minutes);
        }
        return "login";
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("currentPage", "index");
        model.addAttribute("pageTitle", "Р“Р»Р°РІРЅР°СЏ");
        return "index";
    }

    @GetMapping("/defects")
    @PreAuthorize("@roleChecker.canEdit()")
    public String defectsList(Model model) {
        model.addAttribute("currentPage", "defects");
        model.addAttribute("pageTitle", "Р–СѓСЂРЅР°Р» РЅРµСЃРѕРѕС‚РІРµС‚СЃС‚РІРёР№");
        return "defects/list";
    }

    @GetMapping("/defects/add")
    @PreAuthorize("@roleChecker.canEdit()")
    public String addDefect(Model model) {
        model.addAttribute("currentPage", "add-defect");
        model.addAttribute("pageTitle", "Р”РѕР±Р°РІР»РµРЅРёРµ Р·Р°РїРёСЃРё");
        model.addAttribute("isEdit", false);
        model.addAttribute("defectId", null);
        return "defects/add";
    }

    @GetMapping("/defects/edit/{id}")
    @PreAuthorize("@roleChecker.canEdit()")
    public String editDefect(@PathVariable Long id, Model model) {
        model.addAttribute("defectId", id);
        model.addAttribute("currentPage", "add-defect");
        model.addAttribute("pageTitle", "Р РµРґР°РєС‚РёСЂРѕРІР°РЅРёРµ Р·Р°РїРёСЃРё");
        model.addAttribute("isEdit", true);
        return "defects/add";
    }

    @GetMapping("/reports")
    @PreAuthorize("@roleChecker.canView()")
    public String reports(Model model) {
        model.addAttribute("currentPage", "reports");
        model.addAttribute("pageTitle", "РћС‚С‡С‘С‚С‹");
        return "reports/index";
    }

    @GetMapping("/reports/production")
    @PreAuthorize("@roleChecker.canView()")
    public String productionReports(Model model) {
        model.addAttribute("currentPage", "production-reports");
        model.addAttribute("pageTitle", "РћС‚С‡С‘С‚С‹ РїСЂРѕРёР·РІРѕРґСЃС‚РІР°");
        return "reports/production";
    }

    // analetics
    @GetMapping("/charts")
    @PreAuthorize("@roleChecker.canView()")
    public String charts(Model model) {
        model.addAttribute("currentPage", "charts");
        model.addAttribute("pageTitle", "РђРЅР°Р»РёС‚РёРєР°");
        return "charts/index";
    }

    // admin
    @GetMapping("/directories")
    @PreAuthorize("@roleChecker.isAdmin()")
    public String directories(Model model) {
        model.addAttribute("currentPage", "directories");
        model.addAttribute("pageTitle", "РЎРїСЂР°РІРѕС‡РЅРёРєРё");
        return "directories/index";
    }

    @GetMapping("/access-denied")
    public String accessDenied(Model model) {
        model.addAttribute("currentPage", "index");
        model.addAttribute("pageTitle", "Р”РѕСЃС‚СѓРї Р·Р°РїСЂРµС‰С‘РЅ");
        return "access-denied";
    }
}