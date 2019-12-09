--
-- PostgreSQL database dump
--

-- Dumped from database version 11.4 (Debian 11.4-1.pgdg90+1)
-- Dumped by pg_dump version 11.4 (Debian 11.4-1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: categories; Type: TABLE DATA; Schema: public; Owner: meuse
--

COPY public.categories (id, description, name) FROM stdin;
a5ea9a1a-cfd5-4dd3-850d-c01d18010380	the email category	email
b60e5fe7-0fcc-4cdd-ac32-dc2ee800b5f8	the system category	system
\.


--
-- Data for Name: crates; Type: TABLE DATA; Schema: public; Owner: meuse
--

COPY public.crates (id, name) FROM stdin;
ad8d109c-1816-4cc9-a5c8-0b9533789586	crate1
f2a206fd-7309-42de-a1dd-fd4509cac021	crate2
1a4fe345-107b-4596-88a4-3ea63854f471	crate3
\.


--
-- Data for Name: crates_categories; Type: TABLE DATA; Schema: public; Owner: meuse
--

COPY public.crates_categories (crate_id, category_id) FROM stdin;
ad8d109c-1816-4cc9-a5c8-0b9533789586	a5ea9a1a-cfd5-4dd3-850d-c01d18010380
ad8d109c-1816-4cc9-a5c8-0b9533789586	b60e5fe7-0fcc-4cdd-ac32-dc2ee800b5f8
\.


--
-- Data for Name: roles; Type: TABLE DATA; Schema: public; Owner: meuse
--

COPY public.roles (id, name) FROM stdin;
867428a0-69ba-11e9-a674-9f6c32022150	admin
a5435b66-69ba-11e9-8385-8b7c3810e186	tech
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: meuse
--

COPY public.users (id, cargo_id, name, password, description, active, role_id) FROM stdin;
750e2e2f-dd71-458c-bc86-b9e9dbf19d71	1	user1	$2a$11$gWw3Xpj7Y5H3d6v9yka5i.HyggBY2gJOGridHXJzsa2OO22ATta4u	desc1	t	867428a0-69ba-11e9-a674-9f6c32022150
fee1cb14-cbd1-4855-bdad-12c144d26930	2	user2	$2a$11$WQ11jT0sgsYmendAc4RAZuGlKAUFZUkOi6nAqDnTkRqORsL6whJmy	desc2	t	a5435b66-69ba-11e9-8385-8b7c3810e186
1dc4077f-5626-4c71-b4d5-194cb217e265	3	user3	$2a$11$mmAmewnT6t3PoA31mm5Dm.xplyhI7rIusWyHonEWuWssS.teuZd/6	desc3	t	a5435b66-69ba-11e9-8385-8b7c3810e186
b13a19e4-7e48-4b50-af77-d29dfba12ce8	4	user4	$2a$11$QJ.JFfCsRVCghgW3NG5cbejbVFkwOKPjG2CmFRwflNd/tdHgGRdLa	desc4	f	a5435b66-69ba-11e9-8385-8b7c3810e186
5aa7bd3c-9759-4a94-880a-1f23bfe66a4f	5	user5	$2a$11$rAdha/O3Hv5P33vAP52CnOHb/IysLtq9jCaXVmI8WJl4555giows6	desc5	t	867428a0-69ba-11e9-a674-9f6c32022150
\.


--
-- Data for Name: crates_users; Type: TABLE DATA; Schema: public; Owner: meuse
--

COPY public.crates_users (crate_id, user_id) FROM stdin;
ad8d109c-1816-4cc9-a5c8-0b9533789586	750e2e2f-dd71-458c-bc86-b9e9dbf19d71
f2a206fd-7309-42de-a1dd-fd4509cac021	750e2e2f-dd71-458c-bc86-b9e9dbf19d71
1a4fe345-107b-4596-88a4-3ea63854f471	750e2e2f-dd71-458c-bc86-b9e9dbf19d71
ad8d109c-1816-4cc9-a5c8-0b9533789586	fee1cb14-cbd1-4855-bdad-12c144d26930
ad8d109c-1816-4cc9-a5c8-0b9533789586	1dc4077f-5626-4c71-b4d5-194cb217e265
\.


--
-- Data for Name: crates_versions; Type: TABLE DATA; Schema: public; Owner: meuse
--

COPY public.crates_versions (id, version, description, yanked, created_at, updated_at, document_vectors, crate_id) FROM stdin;
081cd057-1fb0-4b5d-a331-93f128f3c643	1.1.0	the crate1 description, this crate is for foobar	f	2019-08-18 12:53:32.754	2019-08-18 12:53:32.754	'crate':6 'crate1':1,3 'descript':4 'email':10 'foobar':9 'system':11	ad8d109c-1816-4cc9-a5c8-0b9533789586
6c3251f5-9130-4435-bdf9-8b5c7a898bee	1.1.4	the crate1 description, this crate is for foobar	f	2019-08-18 12:53:32.765	2019-08-18 12:53:32.765	'crate':6 'crate1':1,3 'descript':4 'email':10 'foobar':9 'system':11	ad8d109c-1816-4cc9-a5c8-0b9533789586
3f030f13-a9f1-4ceb-9321-22218af96f85	1.1.5	the crate1 description, this crate is for foobar	t	2019-08-18 12:53:32.771	2019-08-18 12:53:32.771	'crate':6 'crate1':1,3 'descript':4 'email':10 'foobar':9 'system':11	ad8d109c-1816-4cc9-a5c8-0b9533789586
a03f049c-5eb8-4f12-b2e4-c3eacbb663ca	1.3.0	the crate2 description, this crate is for barbaz	f	2019-08-18 12:53:32.776	2019-08-18 12:53:32.776	'barbaz':9 'crate':6 'crate2':1,3 'descript':4	f2a206fd-7309-42de-a1dd-fd4509cac021
8565cac2-028b-4bfe-b16e-5d07c66c4bf2	1.4.0	blablabla	f	2019-08-18 12:53:32.78	2019-08-18 12:53:32.78	'blablabla':2 'crate3':1 'keyword1':3	1a4fe345-107b-4596-88a4-3ea63854f471
\.


--
-- Data for Name: tokens; Type: TABLE DATA; Schema: public; Owner: meuse
--

COPY public.tokens (id, name, identifier, token, created_at, expired_at, user_id) FROM stdin;
\.


--
-- Name: users_cargo_id_seq; Type: SEQUENCE SET; Schema: public; Owner: meuse
--

SELECT pg_catalog.setval('public.users_cargo_id_seq', 5, true);


--
-- PostgreSQL database dump complete
--
