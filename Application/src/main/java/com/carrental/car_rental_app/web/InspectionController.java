package com.carrental.car_rental_app.web;

import com.carrental.car_rental_app.support.CurrentUser;
import com.carrental.car_rental_app.support.FileStorage;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Pickup and return inspections (created by StaffRentalController), their photos, and the customer's own notes. */
@RestController
public class InspectionController {
    private final JdbcTemplate db;
    private final CurrentUser users;
    private final FileStorage files;

    public InspectionController(JdbcTemplate db, CurrentUser users, FileStorage files) {
        this.db = db; this.users = users; this.files = files;
    }

    record PhotoDto(int id, String description, LocalDateTime uploadedAt) {}
    record DamageSummary(int id, String type, String description, String location, String severity, String status,
                         Integer feeId, BigDecimal feeAmount, String feeStatus, String disputeReason, String resolutionNotes, BigDecimal feePaid) {}
    record InspectionDto(int id, int rentalId, String type, LocalDateTime at, BigDecimal mileage, String fuelLevel,
                         String conditionNotes, String customerNotes, List<PhotoDto> photos, List<DamageSummary> damages) {}
    record NotesRequest(String notes) {}

    @GetMapping("/api/rentals/{rentalId}/inspections")
    public List<InspectionDto> forRental(@PathVariable int rentalId, Authentication a) {
        assertAccess(rentalId, a);
        return db.query("SELECT inspection_id, rental_id, inspection_type, inspection_datetime, mileage, fuel_level, condition_notes, customer_notes"
                + " FROM vehicle_inspection WHERE rental_id = ? ORDER BY inspection_id",
                (rs, i) -> {
                    int id = rs.getInt("inspection_id");
                    return new InspectionDto(id, rs.getInt("rental_id"), Rows.up(rs.getString("inspection_type")), Rows.ldt(rs, "inspection_datetime"),
                            rs.getBigDecimal("mileage"), rs.getString("fuel_level"), rs.getString("condition_notes"), rs.getString("customer_notes"),
                            photos(id), damages(id));
                }, rentalId);
    }

    /** The customer adds their own comments to an inspection of their rental (for example, scratches they noticed at pickup). */
    @PostMapping("/api/inspections/{id}/notes")
    @Transactional
    public Map<String, String> notes(@PathVariable int id, @RequestBody NotesRequest r, Authentication a) {
        int customerId = users.customerId(a);
        if (Rows.blank(r.notes()) || r.notes().length() > 2000) throw ApiError.bad("Enter your notes (up to 2000 characters).");
        int rentalId = rentalOf(id);
        boolean mine = Boolean.TRUE.equals(db.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM rental WHERE rental_id = ? AND customer_id = ?)", Boolean.class, rentalId, customerId));
        if (!mine) throw ApiError.notFound("Inspection not found.");
        db.update("UPDATE vehicle_inspection SET customer_notes = ? WHERE inspection_id = ?", r.notes().trim(), id);
        return Map.of("message", "Notes saved.");
    }

    /** Multipart form: photo (JPG or PNG), optional description. Staff, or the customer who owns the rental. */
    @PostMapping(value = "/api/inspections/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public PhotoDto addPhoto(@PathVariable int id, @RequestParam("photo") MultipartFile photo,
                             @RequestParam(value = "description", required = false) String description, Authentication a) {
        int rentalId = rentalOf(id);
        assertAccess(rentalId, a);
        Integer count = db.queryForObject("SELECT count(*) FROM inspection_photo WHERE inspection_id = ?", Integer.class, id);
        if (count != null && count >= 20) throw ApiError.bad("An inspection can have up to 20 photos.");
        boolean staff = users.isStaff(a);
        Integer staffId = staff ? users.staffId(a) : null;
        Integer customerId = staff ? null : users.customerId(a);
        String path = files.save(photo, "inspections/" + id);
        Integer photoId = db.queryForObject(
                "INSERT INTO inspection_photo (inspection_id, photo_path, photo_description, uploaded_by_customer_id, uploaded_by_staff_id)"
              + " VALUES (?,?,?,?,?) RETURNING photo_id", Integer.class, id, path, Rows.clean(description), customerId, staffId);
        return new PhotoDto(photoId, Rows.clean(description), LocalDateTime.now());
    }

    @GetMapping("/api/inspection-photos/{photoId}")
    public ResponseEntity<byte[]> photo(@PathVariable int photoId, Authentication a) {
        List<Map<String, Object>> rows = db.queryForList(
                "SELECT p.photo_path, i.rental_id FROM inspection_photo p JOIN vehicle_inspection i ON i.inspection_id = p.inspection_id"
              + " WHERE p.photo_id = ?", photoId);
        if (rows.isEmpty()) throw ApiError.notFound("Photo not found.");
        assertAccess((Integer) rows.get(0).get("rental_id"), a);
        String path = String.valueOf(rows.get(0).get("photo_path"));
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(FileStorage.contentType(path)))
                .header("Cache-Control", "private, no-store").body(files.read(path));
    }

    private List<PhotoDto> photos(int inspectionId) {
        return db.query("SELECT photo_id, photo_description, uploaded_at FROM inspection_photo WHERE inspection_id = ? ORDER BY photo_id",
                (rs, i) -> new PhotoDto(rs.getInt("photo_id"), rs.getString("photo_description"), Rows.ldt(rs, "uploaded_at")), inspectionId);
    }

    private List<DamageSummary> damages(int inspectionId) {
        return db.query("""
            SELECT d.damage_id, d.damage_type, d.description, d.location_on_vehicle, d.severity, d.status,
                   f.damage_fee_id, f.amount, f.fee_status, f.dispute_reason, f.resolution_notes,
                   (SELECT COALESCE(SUM(p.amount), 0) FROM payment p WHERE p.damage_fee_id = f.damage_fee_id AND p.payment_status = 'Paid') AS paid
            FROM damage_incident d LEFT JOIN damage_fee f ON f.damage_id = d.damage_id
            WHERE d.inspection_id = ? ORDER BY d.damage_id
            """, (rs, i) -> new DamageSummary(rs.getInt("damage_id"), rs.getString("damage_type"), rs.getString("description"),
                rs.getString("location_on_vehicle"), rs.getString("severity"), Rows.up(rs.getString("status")),
                Rows.intOrNull(rs, "damage_fee_id"), rs.getBigDecimal("amount"), Rows.up(rs.getString("fee_status")), rs.getString("dispute_reason"), rs.getString("resolution_notes"), rs.getBigDecimal("paid")), inspectionId);
    }

    private int rentalOf(int inspectionId) {
        List<Integer> r = db.queryForList("SELECT rental_id FROM vehicle_inspection WHERE inspection_id = ?", Integer.class, inspectionId);
        if (r.isEmpty()) throw ApiError.notFound("Inspection not found.");
        return r.get(0);
    }

    private void assertAccess(int rentalId, Authentication a) {
        if (users.isStaff(a)) return;
        int customerId = users.customerId(a);
        boolean mine = Boolean.TRUE.equals(db.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM rental WHERE rental_id = ? AND customer_id = ?)", Boolean.class, rentalId, customerId));
        if (!mine) throw ApiError.notFound("Rental not found.");
    }
}
