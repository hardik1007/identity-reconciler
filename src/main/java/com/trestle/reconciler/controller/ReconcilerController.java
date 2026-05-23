package com.trestle.reconciler.controller;

import com.trestle.reconciler.dto.ReconcileResponse;
import com.trestle.reconciler.service.ReconciliationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/reconcile")
public class ReconcilerController {

    @Autowired
    private ReconciliationService reconciliationService;

    @PostMapping("/upload")
    public ResponseEntity<ReconcileResponse> upload(
            @RequestParam("sourceA") MultipartFile sourceA,
            @RequestParam("sourceB") MultipartFile sourceB) throws Exception {
        ReconcileResponse response = reconciliationService.reconcile(sourceA, sourceB);
        return ResponseEntity.ok(response);
    }
}
