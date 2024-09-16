package org.openmrs.module.appointments.model;

public enum AppointmentStatus {
    Requested("Requested", 0), Scheduled("Scheduled", 1), CheckedIn("CheckedIn", 2), Completed("Completed", 3), Cancelled("Cancelled", 4), Missed("Missed", 5), Rescheduled("Rescheduled", 6), CameEarly("CameEarly", 7), Pending("Pending", 8), Honored("Honored", 9);

    private final String value;
    private final int sequence;

    AppointmentStatus(String value, int sequence) {
        this.value = value;
        this.sequence = sequence;
    }

    public int getSequence() {
        return sequence;
    }
}
