package com.lums.eventhub;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for FormBuilderActivity.FormQuestion
 *
 * Covers: Org US-18 (Build Registration Form),
 *         US-19 (Question Types),
 *         US-22 (Duplicate Question)
 *
 * FormQuestion is a static inner class — accessible as
 * FormBuilderActivity.FormQuestion without launching the Activity.
 * All tests are pure-Java, no Android dependencies.
 */
public class FormQuestionTest {

    // ═══════════════════════════════════════════════
    // US-18 — Build Registration Form
    // Constructor & default field values
    // ═══════════════════════════════════════════════

    /**
     * US-18: Constructor sets the type field correctly.
     */
    @Test
    public void testConstructorSetsType() {
        FormBuilderActivity.FormQuestion q = new FormBuilderActivity.FormQuestion("Short Text");
        assertEquals("Short Text", q.type);
    }

    /**
     * US-18: Default label is empty string (not null).
     */
    @Test
    public void testDefaultLabelIsEmptyString() {
        FormBuilderActivity.FormQuestion q = new FormBuilderActivity.FormQuestion("Short Text");
        assertNotNull(q.label);
        assertEquals("", q.label);
    }

    /**
     * US-18: Default required flag is false.
     */
    @Test
    public void testDefaultRequiredIsFalse() {
        FormBuilderActivity.FormQuestion q = new FormBuilderActivity.FormQuestion("Short Text");
        assertFalse(q.required);
    }

    /**
     * US-18: Default options list is empty (not null).
     */
    @Test
    public void testDefaultOptionsListIsEmpty() {
        FormBuilderActivity.FormQuestion q = new FormBuilderActivity.FormQuestion("Short Text");
        assertNotNull(q.options);
        assertEquals(0, q.options.size());
    }

    /**
     * US-18: Default docId is null (not yet saved to Firestore).
     */
    @Test
    public void testDefaultDocIdIsNull() {
        FormBuilderActivity.FormQuestion q = new FormBuilderActivity.FormQuestion("Short Text");
        assertNull(q.docId);
    }

    /**
     * US-18: label field can be set and retrieved.
     */
    @Test
    public void testLabelCanBeSet() {
        FormBuilderActivity.FormQuestion q = new FormBuilderActivity.FormQuestion("Short Text");
        q.label = "What is your full name?";
        assertEquals("What is your full name?", q.label);
    }

    /**
     * US-18: required field can be set to true.
     */
    @Test
    public void testRequiredCanBeSetToTrue() {
        FormBuilderActivity.FormQuestion q = new FormBuilderActivity.FormQuestion("Short Text");
        q.required = true;
        assertTrue(q.required);
    }

    /**
     * US-18: required field can be toggled back to false.
     */
    @Test
    public void testRequiredCanBeToggledBackToFalse() {
        FormBuilderActivity.FormQuestion q = new FormBuilderActivity.FormQuestion("Short Text");
        q.required = true;
        q.required = false;
        assertFalse(q.required);
    }

    /**
     * US-18: Options can be added to the list.
     */
    @Test
    public void testOptionsCanBeAdded() {
        FormBuilderActivity.FormQuestion q = new FormBuilderActivity.FormQuestion("Multiple Choice");
        q.options.add("Option A");
        q.options.add("Option B");
        assertEquals(2, q.options.size());
        assertEquals("Option A", q.options.get(0));
        assertEquals("Option B", q.options.get(1));
    }

    /**
     * US-18: Options can be removed from the list.
     */
    @Test
    public void testOptionsCanBeRemoved() {
        FormBuilderActivity.FormQuestion q = new FormBuilderActivity.FormQuestion("Multiple Choice");
        q.options.add("Option A");
        q.options.add("Option B");
        q.options.remove(0);
        assertEquals(1, q.options.size());
        assertEquals("Option B", q.options.get(0));
    }

    /**
     * US-18: docId can be set when loaded from Firestore.
     */
    @Test
    public void testDocIdCanBeSet() {
        FormBuilderActivity.FormQuestion q = new FormBuilderActivity.FormQuestion("Short Text");
        q.docId = "firestore-doc-id-123";
        assertEquals("firestore-doc-id-123", q.docId);
    }

    // ═══════════════════════════════════════════════
    // US-19 — Question Types (all 7)
    // hasOptions() correct for each type
    // ═══════════════════════════════════════════════

    /**
     * US-19: "Short Text" — hasOptions() returns false.
     */
    @Test
    public void testHasOptionsShortTextReturnsFalse() {
        FormBuilderActivity.FormQuestion q = new FormBuilderActivity.FormQuestion("Short Text");
        assertFalse(q.hasOptions());
    }

    /**
     * US-19: "Paragraph" — hasOptions() returns false.
     */
    @Test
    public void testHasOptionsParagraphReturnsFalse() {
        FormBuilderActivity.FormQuestion q = new FormBuilderActivity.FormQuestion("Paragraph");
        assertFalse(q.hasOptions());
    }

    /**
     * US-19: "Multiple Choice" — hasOptions() returns true.
     */
    @Test
    public void testHasOptionsMultipleChoiceReturnsTrue() {
        FormBuilderActivity.FormQuestion q = new FormBuilderActivity.FormQuestion("Multiple Choice");
        assertTrue(q.hasOptions());
    }

    /**
     * US-19: "Checkboxes" — hasOptions() returns true.
     */
    @Test
    public void testHasOptionsCheckboxesReturnsTrue() {
        FormBuilderActivity.FormQuestion q = new FormBuilderActivity.FormQuestion("Checkboxes");
        assertTrue(q.hasOptions());
    }

    /**
     * US-19: "Dropdown" — hasOptions() returns true.
     */
    @Test
    public void testHasOptionsDropdownReturnsTrue() {
        FormBuilderActivity.FormQuestion q = new FormBuilderActivity.FormQuestion("Dropdown");
        assertTrue(q.hasOptions());
    }

    /**
     * US-19: "Date" — hasOptions() returns false.
     */
    @Test
    public void testHasOptionsDateReturnsFalse() {
        FormBuilderActivity.FormQuestion q = new FormBuilderActivity.FormQuestion("Date");
        assertFalse(q.hasOptions());
    }

    /**
     * US-19: "File Upload" — hasOptions() returns false.
     */
    @Test
    public void testHasOptionsFileUploadReturnsFalse() {
        FormBuilderActivity.FormQuestion q = new FormBuilderActivity.FormQuestion("File Upload");
        assertFalse(q.hasOptions());
    }

    /**
     * US-19: Exactly 3 of the 7 types use options (Multiple Choice, Checkboxes, Dropdown).
     */
    @Test
    public void testExactlyThreeTypesHaveOptions() {
        String[] allTypes = {
                "Short Text", "Paragraph", "Multiple Choice",
                "Checkboxes", "Dropdown", "Date", "File Upload"
        };
        int count = 0;
        for (String type : allTypes) {
            FormBuilderActivity.FormQuestion q = new FormBuilderActivity.FormQuestion(type);
            if (q.hasOptions()) count++;
        }
        assertEquals(3, count);
    }

    /**
     * US-19: Exactly 4 of the 7 types do NOT use options.
     */
    @Test
    public void testExactlyFourTypesHaveNoOptions() {
        String[] allTypes = {
                "Short Text", "Paragraph", "Multiple Choice",
                "Checkboxes", "Dropdown", "Date", "File Upload"
        };
        int count = 0;
        for (String type : allTypes) {
            FormBuilderActivity.FormQuestion q = new FormBuilderActivity.FormQuestion(type);
            if (!q.hasOptions()) count++;
        }
        assertEquals(4, count);
    }

    /**
     * US-19: Multiple Choice question created with one blank option (as wired in activity).
     */
    @Test
    public void testMultipleChoiceCreatedWithOneBlankOption() {
        FormBuilderActivity.FormQuestion q = new FormBuilderActivity.FormQuestion("Multiple Choice");
        q.options.add(""); // mirrors wireTypeButtons() in FormBuilderActivity
        assertEquals(1, q.options.size());
        assertEquals("", q.options.get(0));
    }

    /**
     * US-19: Dropdown question created with one blank option (as wired in activity).
     */
    @Test
    public void testDropdownCreatedWithOneBlankOption() {
        FormBuilderActivity.FormQuestion q = new FormBuilderActivity.FormQuestion("Dropdown");
        q.options.add(""); // mirrors wireTypeButtons() in FormBuilderActivity
        assertEquals(1, q.options.size());
    }

    // ═══════════════════════════════════════════════
    // US-22 — Duplicate Question
    // copy() deep copy behaviour
    // ═══════════════════════════════════════════════

    /**
     * US-22: copy() produces a new object (not the same reference).
     */
    @Test
    public void testCopyProducesNewObject() {
        FormBuilderActivity.FormQuestion original = new FormBuilderActivity.FormQuestion("Short Text");
        FormBuilderActivity.FormQuestion copy = original.copy();
        assertNotSame(original, copy);
    }

    /**
     * US-22: copy() preserves the type field.
     */
    @Test
    public void testCopyPreservesType() {
        FormBuilderActivity.FormQuestion original = new FormBuilderActivity.FormQuestion("Paragraph");
        FormBuilderActivity.FormQuestion copy = original.copy();
        assertEquals("Paragraph", copy.type);
    }

    /**
     * US-22: copy() preserves the label field.
     */
    @Test
    public void testCopyPreservesLabel() {
        FormBuilderActivity.FormQuestion original = new FormBuilderActivity.FormQuestion("Short Text");
        original.label = "What is your student ID?";
        FormBuilderActivity.FormQuestion copy = original.copy();
        assertEquals("What is your student ID?", copy.label);
    }

    /**
     * US-22: copy() preserves the required flag when true.
     */
    @Test
    public void testCopyPreservesRequiredTrue() {
        FormBuilderActivity.FormQuestion original = new FormBuilderActivity.FormQuestion("Short Text");
        original.required = true;
        FormBuilderActivity.FormQuestion copy = original.copy();
        assertTrue(copy.required);
    }

    /**
     * US-22: copy() preserves the required flag when false.
     */
    @Test
    public void testCopyPreservesRequiredFalse() {
        FormBuilderActivity.FormQuestion original = new FormBuilderActivity.FormQuestion("Short Text");
        original.required = false;
        FormBuilderActivity.FormQuestion copy = original.copy();
        assertFalse(copy.required);
    }

    /**
     * US-22: copy() produces a deep copy of options — same values.
     */
    @Test
    public void testCopyPreservesOptionsValues() {
        FormBuilderActivity.FormQuestion original = new FormBuilderActivity.FormQuestion("Multiple Choice");
        original.options.add("Yes");
        original.options.add("No");
        original.options.add("Maybe");
        FormBuilderActivity.FormQuestion copy = original.copy();
        assertEquals(3, copy.options.size());
        assertEquals("Yes",   copy.options.get(0));
        assertEquals("No",    copy.options.get(1));
        assertEquals("Maybe", copy.options.get(2));
    }

    /**
     * US-22: copy() is a DEEP copy — mutating copy's options does not affect original.
     */
    @Test
    public void testCopyOptionsListIsIndependentFromOriginal() {
        FormBuilderActivity.FormQuestion original = new FormBuilderActivity.FormQuestion("Multiple Choice");
        original.options.add("Option A");
        FormBuilderActivity.FormQuestion copy = original.copy();

        copy.options.add("Option B"); // mutate the copy

        assertEquals(1, original.options.size()); // original unchanged
        assertEquals(2, copy.options.size());
    }

    /**
     * US-22: copy() is a DEEP copy — mutating original's options after copy does not affect copy.
     */
    @Test
    public void testOriginalMutationDoesNotAffectCopy() {
        FormBuilderActivity.FormQuestion original = new FormBuilderActivity.FormQuestion("Dropdown");
        original.options.add("Red");
        FormBuilderActivity.FormQuestion copy = original.copy();

        original.options.add("Blue"); // mutate the original after copy

        assertEquals(1, copy.options.size()); // copy unchanged
    }

    /**
     * US-22: copy() sets docId to null (copy is a new unsaved question).
     */
    @Test
    public void testCopyDocIdIsNull() {
        FormBuilderActivity.FormQuestion original = new FormBuilderActivity.FormQuestion("Short Text");
        original.docId = "existing-firestore-id";
        FormBuilderActivity.FormQuestion copy = original.copy();
        assertNull(copy.docId);
    }

    /**
     * US-22: copy() of a question with empty options list produces empty options list.
     */
    @Test
    public void testCopyEmptyOptionsListStaysEmpty() {
        FormBuilderActivity.FormQuestion original = new FormBuilderActivity.FormQuestion("Short Text");
        FormBuilderActivity.FormQuestion copy = original.copy();
        assertNotNull(copy.options);
        assertEquals(0, copy.options.size());
    }

    /**
     * US-22: Mutating copy's label does not affect original's label.
     */
    @Test
    public void testCopyLabelIsIndependentFromOriginal() {
        FormBuilderActivity.FormQuestion original = new FormBuilderActivity.FormQuestion("Short Text");
        original.label = "Original Label";
        FormBuilderActivity.FormQuestion copy = original.copy();

        copy.label = "Changed Label";

        assertEquals("Original Label", original.label); // original unchanged
        assertEquals("Changed Label",  copy.label);
    }
}