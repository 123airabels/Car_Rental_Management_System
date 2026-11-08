# Group 6 – Incident Register

## Assessment Information

| Item | Details |
|---|---|
| Designer Group | Group 6 |
| Owner Group | Group 2 |
| Reviewed System | High School Event Management System (HSEMS) |
| Database | `grp02_event_management_db` |
| DBMS | PostgreSQL 18.6 |

## Incident Records

This register contains the incidents associated with the flaws identified by Group 6 and the corresponding investigation, responses, and proposed fixes provided by Group 2.

### G02-INC1 – Student Number Validation

**Incident Description:**  
The system accepted a participant's Student Number even when it contained letters only or did not follow the expected student number format.

**Owner Response:** Accepted

**Owner's Root Cause:**  
The form, API, and database only checked whether the Student Number was present, within the maximum length of 20 characters, and unique. No specific format validation was implemented.

**Owner's Proposed Fix:**  
Define one valid Student Number format and enforce it in the form, API, and database through a CHECK constraint.

**Group 6 Review:**  
Group 6 recommends consistent validation across all application and database layers to prevent invalid Student Numbers from being saved.

**Status:** For Verification

---

### G02-INC2 – Grade Level Validation

**Incident Description:**  
The system accepted a participant's Grade Level even when the value did not follow the expected grade-level format.

**Owner Response:** Accepted

**Owner's Root Cause:**  
The Grade Level field accepted any text value up to 10 characters without checking whether the value belonged to the school's allowed grade levels.

**Owner's Proposed Fix:**  
Use an allowed list of grade levels, provide a dropdown selection, validate the value in the API, and add a database CHECK constraint.

**Group 6 Review:**  
Group 6 recommends restricting Grade Level values to the actual grade levels supported by the school and enforcing the same rule in the database.

**Status:** For Verification

---

### G02-INC3 – Event Start Time Validation

**Incident Description:**  
The system accepted an event scheduled for the current date even though its start time had already passed.

**Owner Response:** Accepted

**Owner's Root Cause:**  
The application only checked whether the event date was earlier than the current date. It did not compare the event's start time with the current time when the date was today.

**Owner's Proposed Fix:**  
When the event date is the current date, the system should reject a start time earlier than the current time. The validation should also apply when editing the event date or start time.

**Group 6 Review:**  
Group 6 recommends validating both the event date and time during creation and editing to prevent events from being scheduled in the past.

**Status:** For Verification

---

### G02-INC4 – Username Case Sensitivity

**Incident Description:**  
The login process accepted an uppercase version of a username even though the expected username format was lowercase.

**Owner Response:** Accepted

**Owner's Root Cause:**  
The authentication process uses case-insensitive username matching. The unique index on `lower(username)` also treats uppercase and lowercase versions as the same username.

**Owner's Proposed Fix:**  
Remove case-insensitive username matching and use case-sensitive authentication.

**Group 6 Review:**  
Group 6 recommends enforcing the documented lowercase username format consistently during registration and login. The authentication behavior should follow the intended username policy.

**Status:** For Verification

---

### G02-INC5 – Activity Schedule Overlap Warning

**Incident Description:**  
The system allowed overlapping activity schedules to be saved even though an overlap warning appeared.

**Owner Response:** Partly Accepted

**Owner's Root Cause:**  
Parallel workshops within the same event are allowed by design. The system only prevents venue schedule conflicts between different events. The displayed warning is informational and does not require confirmation before saving.

**Owner's Proposed Fix:**  
Display the warning before saving, ask the user for confirmation, and change the warning label to make it clear that the activity will run in parallel with another activity.

**Group 6 Review:**  
Group 6 recommends improving the warning message and adding a confirmation prompt so users understand that overlapping activities are allowed and intentional only when appropriate.

**Status:** For Verification

---

### G02-INC6 – Deactivated Account Login Message

**Incident Description:**  
A deactivated account displayed the generic message "Invalid Password or Username" during login, even when the correct credentials were entered.

**Owner Response:** Partly Accepted

**Owner's Root Cause:**  
The login failure handler returns the same error message for all authentication failures. This behavior was intentionally used to avoid revealing whether a username exists or is deactivated.

**Owner's Proposed Fix:**  
After verifying that the username and password are correct, the system may display a message stating that the account has been deactivated and that the user should contact an administrator. Other authentication failures should continue using the generic error message.

**Group 6 Review:**  
Group 6 recommends keeping generic error messages for incorrect credentials while displaying the deactivation notice only after successful credential verification. This balances clear user feedback and account security.

**Status:** For Verification

---

### G02-INC7 – Event Information Refresh

**Incident Description:**  
Event information appeared to remain outdated after the event date or venue was changed.

**Owner Response:** Not Reproduced

**Owner's Root Cause:**  
The Owner Group could not reproduce the reported notification behavior. They explained that the Home and Dashboard pages reload when opened, but information may not automatically refresh while the page remains open.

**Owner's Proposed Fix:**  
Request the exact reproduction steps and affected screen. As a possible improvement, refresh event information when the browser tab regains focus and through periodic updates.

**Group 6 Review:**  
Group 6 recommends obtaining additional steps and screenshots to confirm the issue. Automatic refresh may also improve the consistency of event information displayed to users.

**Status:** Needs Further Investigation

---

### G02-INC8 – Event Activity Time Range

**Incident Description:**  
The database accepted an activity schedule outside the start and end time of its parent event when the record was inserted directly through the application database account.

**Owner Response:** Accepted

**Owner's Root Cause:**  
The time-range validation was implemented only in the application logic. No database-level mechanism prevented direct insertion of activity schedules outside the parent event's time range.

**Owner's Proposed Fix:**  
Create a new database migration that adds triggers to reject activity schedules outside their parent event's time range. The triggers should also prevent event time changes that would make existing activity schedules invalid.

**Group 6 Review:**  
Group 6 recommends enforcing this rule at the database level because application-only validation can be bypassed through direct SQL operations. The proposed triggers should be implemented and tested before the incident is considered resolved.

**Status:** For Verification

---

## Incident Summary

| Incident ID | Owner Response | Group 6 Assessment |
|---|---|---|
| G02-INC1 | Accepted | Fix should be verified |
| G02-INC2 | Accepted | Fix should be verified |
| G02-INC3 | Accepted | Fix should be verified |
| G02-INC4 | Accepted | Fix should be verified |
| G02-INC5 | Partly Accepted | Improve warning and confirmation |
| G02-INC6 | Partly Accepted | Balance clear feedback and security |
| G02-INC7 | Not Reproduced | Additional investigation needed |
| G02-INC8 | Accepted | Database-level fix should be verified |

## General Notes

1. The Owner Group's explanations and proposed fixes are documented based on their tracker responses.
2. Group 6's review provides recommendations based on the observed behavior and database testing.
3. A proposed fix should not be treated as completed until it has been implemented and tested.
4. Any additional evidence should be attached to the corresponding incident record.
5. Incident statuses should be updated when the Owner Group provides new findings or evidence of successful fixes.

**Prepared by:** Group 6 – Designer Group  
**Reviewed System:** Group 2 – High School Event Management System