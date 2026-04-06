package com.example.event_management;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

//main logic to be tested here is the filter logic

public class EventBrowsingTest {
    private List<Event> allEvents;
    private List<Event> filteredEvents;

    private void filter_events(String text, String filter) {
        filteredEvents.clear();
        for (Event event : allEvents) {
            if (!filter.equals("All") && !event.category.equals(filter)) {
                continue;
            }
            if (!text.isEmpty() && !event.title.toLowerCase().contains(text.toLowerCase())) {
                continue;
            }
            filteredEvents.add(event);
        }
    }

    @Before
    public void sample_data() {
        // This runs before every test — sets up fresh sample data
        allEvents = new ArrayList<>();
        filteredEvents = new ArrayList<>();

        allEvents.add(new Event("1", "Basketball Championship Finals", "Sports Society",
                "Mar 15, 2026", "Sports Complex", "Society Events",
                "Annual championship", "Mar 10, 2026", "4:00 PM", "Free", 200, 500));

        allEvents.add(new Event("2", "Physics Seminar", "SBASSE",
                "Mar 6, 2026", "SBASSE 10-204", "Workshops/Seminars",
                "Physics talk", "Mar 1, 2026", "2:30 PM", "Free", 40, 60));

        allEvents.add(new Event("3", "Startup Weekend LUMS", "SPADES",
                "Apr 4, 2026", "SDSB Atrium", "Society Events",
                "Startup event", "Apr 1, 2026", "9:00 AM", "Free", 120, 200));

        allEvents.add(new Event("4", "AI Workshop", "CS Society",
                "Apr 10, 2026", "SBASSE Lab", "Workshops/Seminars",
                "AI talk", "Apr 5, 2026", "3:00 PM", "PKR 500", 30, 50));
    }


    //filter=All
    @Test
    public void testFilterAll_emptySearch() {
        filter_events("", "All");
        assertEquals(4, filteredEvents.size());
    }

    @Test
    public void testFilterAll_withText() {
        filter_events("basketball", "All");
        assertEquals(1, filteredEvents.size());
        assertEquals("Basketball Championship Finals", filteredEvents.get(0).title);
    }

    //filter=Society Events
    @Test
    public void testFilterSocietyEvents_emptySearch() {
        filter_events("", "Society Events");
        assertEquals(2, filteredEvents.size());
    }

    @Test
    public void testFilterSocietyEvents_withText() {
        filter_events("basketball", "Society Events");
        assertEquals(1, filteredEvents.size());
        assertEquals("Basketball Championship Finals", filteredEvents.get(0).title);
    }

    //filter=Workshop/seminars
    @Test
    public void testFilterWorkshops_emptySearch() {
        filter_events("", "Workshops/Seminars");
        assertEquals(2, filteredEvents.size());
    }

    @Test
    public void testFilterWorkshops_withText() {
        filter_events("physics", "Workshops/Seminars");
        assertEquals(1, filteredEvents.size());
        assertEquals("Physics Seminar", filteredEvents.get(0).title);
    }

    //seat availability test
    @Test
    public void testSeatsLeft_calculatesCorrectly() {
        Event e = allEvents.get(0); // Basketball, 200 booked, 500 total
        int seatsLeft = e.seatsTotal - e.seatsbooked;
        assertEquals(300, seatsLeft);
    }


}
