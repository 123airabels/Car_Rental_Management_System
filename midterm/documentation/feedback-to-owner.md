# Group 6 – Feedback to Group 2
## COMP 018 Midterm Assessment: Integrated Database Administration Challenge

**Designer Group:** Group 6  
**Owner Group:** Group 2  
**Project Reviewed:** High School Event Management System (HSEMS)

## 1. Introduction

This document presents the feedback and recommendations of Group 6 regarding the review of Group 2's High School Event Management System (HSEMS).

The feedback is based on the application and database tests conducted by Group 6, the eight findings recorded in the assessment tracker, and the explanations and proposed fixes provided by Group 2.

The purpose of this feedback is to recognize the Owner Group's responses and suggest improvements that may strengthen the system's data integrity, validation, authentication, and user experience.

## 2. Summary of Findings and Recommendations

### G02-INC1 – Student Number Format Is Not Enforced

**Owner Response:** Accepted

**Owner's Root Cause:** Validation covered length and uniqueness only.

**Owner's Proposed Fix:** Enforce one documented student number format in the form, API, and database through a CHECK constraint.

**Group 6 Recommendation:** Define the official student number format and apply consistent validation across the frontend, backend, and database to prevent invalid student numbers.

### G02-INC2 – Grade Level Does Not Enforce Expected Values

**Owner Response:** Accepted

**Owner's Root Cause:** No allowed-values rule existed in the form, API, or database.

**Owner's Proposed Fix:** Limit grade levels to those used by the school through a dropdown, API validation, and database CHECK constraint.

**Group 6 Recommendation:** Use a predefined list of valid grade levels and enforce the allowed values in both the application and database.

### G02-INC3 – Past Event Start Times Are Accepted

**Owner Response:** Accepted

**Owner's Root Cause:** Only the date was compared in the API and form.

**Owner's Proposed Fix:** When the event date is today, the start time must not be earlier than the current time. Apply the check during creation and relevant edits.

**Group 6 Recommendation:** Apply same-day time validation when creating or editing events to prevent events from being scheduled with a start time that has already passed.

### G02-INC4 – Login Accepts Uppercase Usernames

**Owner Response:** Accepted

**Owner's Root Cause:** Login ignores case, and a unique index on `lower(username)` prevents usernames that differ only by letter case.

**Owner's Proposed Fix:** Remove the `ignorecase` behavior and make authentication strictly case-sensitive.

**Group 6 Recommendation:** Follow the stated lowercase username requirement by enforcing lowercase usernames during account creation and updates and using case-sensitive authentication.

### G02-INC5 – Activity Overlap Warning Is Unclear

**Owner Response:** Partly accepted

**Owner's Root Cause:** The overlap label is informational, and there is no confirmation before saving.

**Owner's Proposed Fix:** Display a warning before saving, ask for confirmation, and reword the label to "Runs in parallel with another activity".

**Group 6 Recommendation:** Improve the warning message so users understand that overlapping activities may represent parallel workshops. A confirmation prompt may also help prevent confusion.

### G02-INC6 – Deactivated Account Displays a Generic Login Error

**Owner Response:** Partly accepted

**Owner's Root Cause:** The login failure handler returns one message for every failure.

**Owner's Proposed Fix:** When the username and password are correct but the account is deactivated, show "This account has been deactivated. Contact an administrator." Keep the generic message for other failures.

**Group 6 Recommendation:** Provide the deactivation message only after the correct credentials have been verified and the account is confirmed as deactivated. Retain generic error messages for other authentication failures to protect account information.

### G02-INC7 – Event Information Does Not Refresh After Updates

**Owner Response:** Could not reproduce as described

**Owner's Closest Identified Behavior:** Home and Dashboard lists do not refresh while left open.

**Owner's Proposed Improvement:** Refresh the lists when the browser tab regains focus and on a timer.

**Group 6 Recommendation:** Obtain exact reproduction steps and screenshots before treating the reported notification behavior as a confirmed defect. Consider automatic refreshing when the browser tab regains focus or after a set interval.

### G02-INC8 – Database Allows Activities Outside the Parent Event's Time Range

**Owner Response:** Accepted

**Owner's Root Cause:** The database had no cross-table constraint, and the runtime account can insert and update schedule rows and update event times.

**Owner's Proposed Fix:** Add a new migration with triggers that reject activity schedules outside their parent event's time range and prevent event time changes that would make existing activities invalid.

**Group 6 Recommendation:** Implement database-level validation to protect the relationship between event schedules and their activities. Test both activity insert/update operations and event time changes to ensure the triggers work correctly.

## 3. General Recommendations

Aside from the specific findings, Group 6 recommends the following:

1. **Consistent Validation:** Apply important validation rules in the frontend, backend, and database whenever appropriate.
2. **Database Integrity:** Use database constraints and triggers to protect important data relationships, even when application-level validation already exists.
3. **Authentication Security:** Maintain secure authentication behavior while providing appropriate account assistance to legitimate users.
4. **Clear User Feedback:** Use understandable validation messages and warnings so users know what actions they are performing.
5. **Updated Documentation:** Keep database roles, privileges, validation rules, migrations, and system behavior properly documented.
6. **Testing After Changes:** Test proposed fixes through both the application and direct database queries when applicable.

## 4. Overall Feedback

Group 6 acknowledges Group 2's responses and the explanations provided for the eight findings. The Owner Group identified the causes of several validation and database integrity gaps and provided specific proposed fixes.

Some findings were related to intended system behavior, particularly parallel activity scheduling and generic login error handling. The reported event notification issue also requires further reproduction before it can be confirmed.

Overall, the proposed improvements can help strengthen the system's data consistency, database integrity, authentication behavior, and usability. Group 6 recommends validating the fixes through appropriate application and database tests before considering the findings resolved.

**Prepared by:** Group 6 – Designer Group  
**Project Reviewed:** Group 2 – High School Event Management System