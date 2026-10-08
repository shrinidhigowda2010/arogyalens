package com.arogyalens.controller;

import com.arogyalens.exception.ArogyaLensException;
import com.arogyalens.model.TrustedSource;
import com.arogyalens.source.SourceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sources")
public class SourceController {

    private final SourceService sourceService;

    public SourceController(SourceService sourceService) {
        this.sourceService = sourceService;
    }

    @GetMapping
    public List<TrustedSource> all() {
        return sourceService.all();
    }

    @GetMapping("/{id}")
    public TrustedSource byId(@PathVariable String id) {
        return sourceService.findById(id).orElseThrow(() -> new ArogyaLensException(
                "SOURCE_NOT_FOUND",
                "Source missing",
                "We could not verify this information from a trusted source."
        ));
    }
}
