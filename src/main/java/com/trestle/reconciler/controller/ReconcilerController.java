package com.trestle.reconciler.controller;

import com.trestle.reconciler.dto.ReconcileResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/reconcile")
public class ReconcilerController {

    @PostMapping("/upload")
    public ResponseEntity<ReconcileResponse> upload(
            @RequestParam("sourceA") MultipartFile sourceA,
            @RequestParam("sourceB") MultipartFile sourceB) {
        // TODO: wire pipeline — parse → normalize → score → result
        return ResponseEntity.status(501).build();
    }
}
