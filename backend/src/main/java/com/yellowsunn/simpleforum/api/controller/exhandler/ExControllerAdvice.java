package com.yellowsunn.simpleforum.api.controller.exhandler;

import com.yellowsunn.simpleforum.exception.*;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import static javax.servlet.http.HttpServletResponse.*;

@RestControllerAdvice
public class ExControllerAdvice {

    @ExceptionHandler
    public void unauthorized(UnauthorizedException e, HttpServletResponse response) throws IOException {
        response.sendError(SC_UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public void notFoundException(HttpServletRequest request, HttpServletResponse response,
                                      NotFoundException e) throws IOException {
        response.sendError(SC_NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(value = {PasswordMismatchException.class, ForbiddenException.class})
    public void forbidden(HttpServletResponse response, Exception e) throws IOException {
        response.sendError(SC_FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(value = {IllegalArgumentException.class, InvalidReferException.class})
    public void badRequest(HttpServletResponse response, Exception e) throws IOException {
        response.sendError(SC_BAD_REQUEST, e.getMessage());
    }

    private void invalidateLoginSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
    }
}
