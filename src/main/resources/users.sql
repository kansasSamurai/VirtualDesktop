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

CREATE TABLE public.drivelog (
	drive VARCHAR(2),
	freespace DECIMAL(4,1),
	datecreated TIMESTAMP
);
