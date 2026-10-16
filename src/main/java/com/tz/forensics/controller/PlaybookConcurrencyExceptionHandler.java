package com.tz.forensics.controller;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** Return a safe conflict response when concurrent responders update the same playbook record. */
@RestControllerAdvice
public class PlaybookConcurrencyExceptionHandler {
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, String>> handleOptimisticLockConflict(OptimisticLockingFailureException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
            "error", "PLAYBOOK_STATE_CONFLICT",
            "message", "This playbook changed in another request. Refresh the case and retry."
        ));
    }
}
