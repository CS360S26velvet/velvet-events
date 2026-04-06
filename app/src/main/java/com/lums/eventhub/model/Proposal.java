package com.lums.eventhub.model;

import java.util.List;
import java.util.Map;

/**
 * Proposal.java
 *
 * Model for a proposal document in Firestore proposals/ collection.
 *
 * Field naming:
 *   organizerUsername — canonical field name used everywhere.
 *   This is the same as organizerId in older code — reconciled to organizerUsername.
 *
 * Status values (canonical):
 *   "Draft"              — saved by organizer, not visible to admin
 *   "Submitted"          — submitted to CCA, visible to admin as pending
 *   "Approved"           — admin approved
 *   "Revision Requested" — admin requested changes
 *   "Rejected"           — admin rejected
 */
public class Proposal {

    // Core identity
    private String id;
    private String status;
    private String organizerUsername;  // canonical: same as organizerId. e.g. #ORG_spades
    private String societyName;        // e.g. SPADES Society

    // Section 1 — Basic info
    private String title;
    private String description;
    private String eventType;
    private String date;
    private String venue;

    // Section 2 — Participants & guests
    private long   expectedParticipants;
    private List<Map<String, Object>> guests;

    // Section 3 — Budget
    private long estimatedBudget;

    // Section 4 — Sessions
    private List<Map<String, Object>> sessions;

    // Section 5 — Accommodation
    private boolean requiresAccommodation;
    private long    accommodationCount;
    private String  checkInDate;
    private String  checkOutDate;
    private String  specialRequirements;

    // Timestamps
    private long createdAt;
    private long submittedAt;

    // Legacy field aliases — kept for backward compatibility with existing Firestore docs
    // eventDate maps to date, so we keep both getters
    private String eventDate;

    public Proposal() {} // Required for Firestore

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    /** organizerUsername == organizerId — same concept, canonical name is organizerUsername */
    public String getOrganizerUsername() { return organizerUsername; }
    public void setOrganizerUsername(String organizerUsername) { this.organizerUsername = organizerUsername; }

    public String getSocietyName() { return societyName; }
    public void setSocietyName(String societyName) { this.societyName = societyName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    /** getEventDate: returns date field, falls back to eventDate for legacy docs */
    public String getEventDate() {
        return date != null ? date : eventDate;
    }
    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
        if (this.date == null) this.date = eventDate;
    }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public long getExpectedParticipants() { return expectedParticipants; }
    public void setExpectedParticipants(long expectedParticipants) { this.expectedParticipants = expectedParticipants; }

    public List<Map<String, Object>> getGuests() { return guests; }
    public void setGuests(List<Map<String, Object>> guests) { this.guests = guests; }

    public long getEstimatedBudget() { return estimatedBudget; }
    public void setEstimatedBudget(long estimatedBudget) { this.estimatedBudget = estimatedBudget; }

    public List<Map<String, Object>> getSessions() { return sessions; }
    public void setSessions(List<Map<String, Object>> sessions) { this.sessions = sessions; }

    public boolean isRequiresAccommodation() { return requiresAccommodation; }
    public void setRequiresAccommodation(boolean requiresAccommodation) { this.requiresAccommodation = requiresAccommodation; }

    public long getAccommodationCount() { return accommodationCount; }
    public void setAccommodationCount(long accommodationCount) { this.accommodationCount = accommodationCount; }

    public String getCheckInDate() { return checkInDate; }
    public void setCheckInDate(String checkInDate) { this.checkInDate = checkInDate; }

    public String getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(String checkOutDate) { this.checkOutDate = checkOutDate; }

    public String getSpecialRequirements() { return specialRequirements; }
    public void setSpecialRequirements(String specialRequirements) { this.specialRequirements = specialRequirements; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(long submittedAt) { this.submittedAt = submittedAt; }
}