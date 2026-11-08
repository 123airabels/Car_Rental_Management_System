# Group 6 – Flaw Register

## Assessment Information

| Item | Details |
|---|---|
| Designer Group | Group 6 |
| Owner Group | Group 2 |
| Reviewed System | High School Event Management System (HSEMS) |
| Database | `grp02_event_management_db` |
| DBMS | PostgreSQL 18.6 |

## Flaw Register

This register documents the flaws identified by Group 6 during the application and database testing of Group 2's High School Event Management System.

| Flaw ID | Identified Flaw | Description | Related Incident | Owner Response | Status |
|---|---|---|---|---|---|
| G02-INC1 | Student Number Validation | The Student Number field accepts values that do not follow the expected student number format. | G02-INC1 | Accepted | For Verification |
| G02-INC2 | Grade Level Validation | The Grade Level field accepts values outside the expected grade-level format. | G02-INC2 | Accepted | For Verification |
| G02-INC3 | Event Start Time Validation | Event creation accepts a start time that has already passed when the event date is the current date. | G02-INC3 | Accepted | For Verification |
| G02-INC4 | Username Case Sensitivity | The login process accepts uppercase usernames even though the expected username format is lowercase. | G02-INC4 | Accepted | For Verification |
| G02-INC5 | Activity Schedule Overlap Warning | Overlapping activities can still be saved even when the system displays an overlap warning. | G02-INC5 | Partly Accepted | For Verification |
| G02-INC6 | Deactivated Account Login Message | Deactivated accounts display a generic invalid username or password message, which does not explain the account status. | G02-INC6 | Partly Accepted | For Verification |
| G02-INC7 | Event Information Refresh | Event information may remain outdated when event details are changed while the page is open. | G02-INC7 | Not Reproduced | Needs Further Investigation |
| G02-INC8 | Event Activity Time Range | The database allows an activity schedule outside the time range of its parent event through direct database insertion. | G02-INC8 | Accepted | For Verification |

## Summary

- Total identified flaws: 8
- Accepted by Owner Group: 6
- Partly accepted by Owner Group: 2
- Not reproduced by Owner Group: 1

*Note: The response categories above include the two partly accepted findings. Status refers to the verification of fixes, not simply the Owner Group's acceptance of the findings.*

## Notes

1. The findings are based on Group 6's application and database testing.
2. Owner acceptance does not automatically mean that a flaw has been fixed.
3. Findings should only be marked as resolved after the proposed changes are implemented and tested.
4. G02-INC7 requires additional reproduction steps or supporting evidence before it can be confirmed.
5. G02-INC8 requires database-level validation to ensure that the rule cannot be bypassed through direct SQL operations.

**Prepared by:** Group 6 – Designer Group  
**Reviewed System:** Group 2 – High School Event Management System