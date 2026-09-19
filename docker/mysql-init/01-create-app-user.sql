-- Every service's datasource URL already includes createDatabaseIfNotExist=true
-- (see each service's application.yml), so MySQL creates each service's own
-- database automatically on that service's first connection — this script only
-- needs to make sure the shared application user exists with enough privilege
-- to do that, and to read/write freely afterward.
--
-- One shared user with global privileges is a deliberate local-dev
-- simplification, not the production design: in a real deployment each
-- service would get its own narrowly-scoped credential (grants on exactly
-- its own schema, nothing else), and likely its own physically separate
-- database instance rather than a shared MySQL container, for genuine
-- failure and blast-radius isolation between services. Locally, one MySQL
-- container with eight logically separate schemas keeps laptop resource
-- usage sane while every service still only ever touches its own schema —
-- enforced by application code and each service's own Flyway migrations,
-- never by a database-level restriction here.
CREATE USER IF NOT EXISTS 'banking_app'@'%' IDENTIFIED BY 'banking_app_pw';
GRANT ALL PRIVILEGES ON *.* TO 'banking_app'@'%';
FLUSH PRIVILEGES;
