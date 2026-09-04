package demo.tx;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** Named OrderRow because ORDER is a reserved word in SQL. */
@Entity
public class OrderRow {
    @Id
    private String id;
    private String note;

    protected OrderRow() {}
    public OrderRow(String id, String note) { this.id = id; this.note = note; }
    public String getId() { return id; }
    public String getNote() { return note; }
}
