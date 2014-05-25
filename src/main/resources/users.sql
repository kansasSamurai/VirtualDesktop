/* -----------------------------
From HSQL Database Manager
jdbc:hsqldb:hsql://localhost:1234/sandbox
----------------------------- */
CREATE TABLE users (
  oid INTEGER PRIMARY KEY,
  lastname VARCHAR(20)
)

INSERT INTO users VALUES 1, 'Wellman'
INSERT INTO users VALUES 10, 'Zwiener'

SELECT * FROM users
SELECT * FROM "PUBLIC"."USERS"

