package com.arogyalens.model;

/** Non-diagnostic status of a lab value relative to its reference range. */
public enum ParameterStatus {
    WITHIN_RANGE,
    OUTSIDE_RANGE,
    REQUIRES_DISCUSSION,
    IMPORTANT_ATTENTION,
    UNKNOWN,
    LOW_CONFIDENCE
}
