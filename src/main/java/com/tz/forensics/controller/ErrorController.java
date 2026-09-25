package com.tz.forensics.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    private static final Logger log = LoggerFactory.getLogger(ErrorController.class);

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        log.error("Error occurred: status={}, message={}", status, message);
        if (exception != null) {
            log.error("Exception: ", (Throwable) exception);
        }

        int statusCode = 500;
        if (status != null) {
            try { statusCode = Integer.parseInt(status.toString()); }
            catch (Exception e) { /* ignore */ }
        }

        model.addAttribute("statusCode", statusCode);
        model.addAttribute("errorMessage", message != null ? message : "An unexpected error occurred");
        model.addAttribute("path", request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));

        return "error-page";
    }
}
