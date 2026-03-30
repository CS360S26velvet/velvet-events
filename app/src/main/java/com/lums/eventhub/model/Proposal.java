package com.lums.eventhub.model;

public class Proposal {
    private String id, title, organizerUsername, status, eventDate, venue, description;

    public Proposal() {} // Required for Firebase

    public Proposal(String title, String organizerUsername, String eventDate, String venue, String description) {
        this.title = title;
        this.organizerUsername = organizerUsername;
        this.status = "pending";
        this.eventDate = eventDate;
        this.venue = venue;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOrganizerUsername() {
        return organizerUsername;
    }

    public void setOrganizerUsername(String organizerUsername) {
        this.organizerUsername = organizerUsername;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEventDate() {
        return eventDate;
    }

    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
