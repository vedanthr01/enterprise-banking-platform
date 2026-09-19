-- Defense-in-depth for the audit log's core invariant. AuditEventRecorder
-- (the application's only write path into this table) never issues an
-- UPDATE or DELETE, by design — but that is only a promise the application
-- code keeps. These triggers make the same promise unconditional at the
-- database layer too, so even a direct SQL client authenticated as this
-- service's own database user cannot silently alter or remove a historical
-- record: MySQL rejects the statement outright, before any row is touched.
--
-- A hash *chain* (each row also folding in the previous row's hash) was
-- considered and deliberately not used here for the same reason
-- AuditHashUtil documents: it needs a single serialization point across
-- writers, which conflicts with this service's multi-partition, horizontally
-- scalable consumption model. These triggers plus the per-record hash in
-- AuditHashUtil are the two layers this project uses instead: the trigger
-- stops casual/accidental mutation outright, and the hash detects mutation
-- performed with elevated privileges that bypassed the trigger.
DELIMITER $$

CREATE TRIGGER trg_audit_events_no_update
BEFORE UPDATE ON audit_events
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'audit_events is append-only: UPDATE is not permitted';
END$$

CREATE TRIGGER trg_audit_events_no_delete
BEFORE DELETE ON audit_events
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'audit_events is append-only: DELETE is not permitted';
END$$

DELIMITER ;
