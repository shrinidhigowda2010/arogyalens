package com.arogyalens.model;

/** A category of personal information detected and masked by the Privacy Shield. */
public record PiiFinding(String type, String maskedValue, boolean redacted) {}
