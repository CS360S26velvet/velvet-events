package com.lums.eventhub.attendee.events;

public class Event {
    public String id, title, organizer, date, venue, category, desc, deadline, time, fee;
    public int seatsbooked, seatsTotal;
    public String imageBase64;

    public Event(String id, String title, String org, String date, String venue, String category,String desc,String deadline, String time,String fee, int seatsBooked, int totalSeats){
        this.id = id;
        this.title = title;
        this.organizer=org;
        this.date = date;
        this.venue = venue;
        this.category = category;
        this.seatsbooked = seatsBooked;
        this.seatsTotal = totalSeats;
        this.fee = fee;
        this.deadline = deadline;
        this.desc = desc;
        this.time = time;
        this.imageBase64 = "";
    }
}