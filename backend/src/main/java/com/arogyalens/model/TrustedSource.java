package com.arogyalens.model;

/** A curated public health reference shown as evidence. */
public record TrustedSource(
        String id, String name, String title, String description, String url, String category) {}
