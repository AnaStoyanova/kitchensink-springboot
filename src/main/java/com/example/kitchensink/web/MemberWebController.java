package com.example.kitchensink.web;

import com.example.kitchensink.domain.Member;
import com.example.kitchensink.service.EmailAlreadyExistsException;
import com.example.kitchensink.service.MemberRegistrationService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/")
public class MemberWebController {

    private final MemberRegistrationService service;

    public MemberWebController(MemberRegistrationService service) {
        this.service = service;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("members", service.findAllOrderedByName());
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new MemberForm());
        }
        return "index";
    }

    @PostMapping
    public String register(@Valid @ModelAttribute("form") MemberForm form,
                           BindingResult result, Model model,
                           RedirectAttributes redirectAttrs) {
        if (result.hasErrors()) {
            model.addAttribute("members", service.findAllOrderedByName());
            return "index";
        }
        try {
            service.register(new Member(null, form.getName(), form.getEmail(), form.getPhoneNumber()));
            redirectAttrs.addFlashAttribute("successMessage",
                form.getName() + " was successfully registered.");
        } catch (EmailAlreadyExistsException e) {
            result.rejectValue("email", "duplicate", e.getMessage());
            model.addAttribute("members", service.findAllOrderedByName());
            return "index";
        }
        return "redirect:/";
    }
}
