<?php
$db = new PDO("pgsql:dbname=geodata;host=132.230.21.8", "postgres", "postgres");
$sqlQuery = 'CREATE TABLE items (id_item integer NOT NULL, handle character varying(255) NOT NULL';
$sqlQuery .= 'handle_source character varying(255) NOT NULL,';
$sqlQuery .= 'handle_citation character varying(255) NOT NULL,';
$sqlQuery .=                                            'CONSTRAINT items_pkey PRIMARY KEY (id_item),';
$sqlQuery .=                                            'CONSTRAINT items_handle_key UNIQUE (handle)';
$sqlQuery .=                                            ')';'

$db->query($sqlQuery);
$db = null;
?>