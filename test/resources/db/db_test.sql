--
-- PostgreSQL database dump
--

-- Dumped from database version 11.2 (Debian 11.2-1.pgdg90+1)
-- Dumped by pg_dump version 11.2 (Debian 11.2-2)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: categories; Type: TABLE DATA; Schema: public; Owner: meuse
--

INSERT INTO public.categories VALUES ('96e54d4b-9c9d-4e19-9294-6f176717b60d', 'the email category', 'email');
INSERT INTO public.categories VALUES ('0cf2cf17-c837-4e69-a84b-27da259488ba', 'the system category', 'system');


--
-- Data for Name: crates; Type: TABLE DATA; Schema: public; Owner: meuse
--

INSERT INTO public.crates VALUES ('09a6ccc7-fec4-42fd-9d6a-ea3afcc13629', 'crate1');
INSERT INTO public.crates VALUES ('05edcb4b-6e1e-46bc-ad67-3ab3557b72e9', 'crate2');
INSERT INTO public.crates VALUES ('6931c9ab-795a-49cd-b7d5-0787fa498b7b', 'crate3');


--
-- Data for Name: crates_categories; Type: TABLE DATA; Schema: public; Owner: meuse
--

INSERT INTO public.crates_categories VALUES ('09a6ccc7-fec4-42fd-9d6a-ea3afcc13629', '96e54d4b-9c9d-4e19-9294-6f176717b60d');
INSERT INTO public.crates_categories VALUES ('09a6ccc7-fec4-42fd-9d6a-ea3afcc13629', '0cf2cf17-c837-4e69-a84b-27da259488ba');


--
-- Data for Name: roles; Type: TABLE DATA; Schema: public; Owner: meuse
--

INSERT INTO public.roles VALUES ('867428a0-69ba-11e9-a674-9f6c32022150', 'admin');
INSERT INTO public.roles VALUES ('a5435b66-69ba-11e9-8385-8b7c3810e186', 'tech');


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: meuse
--

INSERT INTO public.users VALUES ('a3790b22-4d0b-4470-98bc-a9b100fc6c5e', 1, 'user1', '$2a$11$bRrDDmEPJu.dODxF2h4ePe.k9L9qPmcfsb1ntrLtaRN4qFpF2WaVC', 'desc1', true, '867428a0-69ba-11e9-a674-9f6c32022150');
INSERT INTO public.users VALUES ('fa2abe9f-0ea8-4a82-ad09-709c86f4c254', 2, 'user2', '$2a$11$2p6US3nF8gjfXUHxYScLa.DgMY1/wxddOmTp5QaBhzKaB9jH7tDcS', 'desc2', true, 'a5435b66-69ba-11e9-8385-8b7c3810e186');
INSERT INTO public.users VALUES ('41611a2b-648e-447d-a7a4-4761c1a3756c', 3, 'user3', '$2a$11$aroBYyESe/mnDsgOI5Y3fOu0WGFULSeSDjQ6SpttjUK8E4Wb.CkdK', 'desc3', true, 'a5435b66-69ba-11e9-8385-8b7c3810e186');
INSERT INTO public.users VALUES ('cb8ad059-db1f-40d0-aea9-8e36154dbce2', 4, 'user4', '$2a$11$Y/n3oRkWGjeGFUfH2LIV2e/j2mhtIzPIQqb4aZvNksW5lGUqXJwSe', 'desc4', false, 'a5435b66-69ba-11e9-8385-8b7c3810e186');
INSERT INTO public.users VALUES ('a48d8cbd-ce8d-42b4-af62-9e5772e2258c', 5, 'user5', '$2a$11$5ENw9nCJbmAp/02HfmbpdeuZREaajouAeMw8husD1BaU/1dALi9Da', 'desc5', true, '867428a0-69ba-11e9-a674-9f6c32022150');


--
-- Data for Name: crates_users; Type: TABLE DATA; Schema: public; Owner: meuse
--

INSERT INTO public.crates_users VALUES ('09a6ccc7-fec4-42fd-9d6a-ea3afcc13629', 'a3790b22-4d0b-4470-98bc-a9b100fc6c5e');
INSERT INTO public.crates_users VALUES ('05edcb4b-6e1e-46bc-ad67-3ab3557b72e9', 'a3790b22-4d0b-4470-98bc-a9b100fc6c5e');
INSERT INTO public.crates_users VALUES ('6931c9ab-795a-49cd-b7d5-0787fa498b7b', 'a3790b22-4d0b-4470-98bc-a9b100fc6c5e');
INSERT INTO public.crates_users VALUES ('09a6ccc7-fec4-42fd-9d6a-ea3afcc13629', 'fa2abe9f-0ea8-4a82-ad09-709c86f4c254');
INSERT INTO public.crates_users VALUES ('09a6ccc7-fec4-42fd-9d6a-ea3afcc13629', '41611a2b-648e-447d-a7a4-4761c1a3756c');


--
-- Data for Name: crates_versions; Type: TABLE DATA; Schema: public; Owner: meuse
--

INSERT INTO public.crates_versions VALUES ('73737416-3a0a-4e91-8059-7ee3bdb3b7f6', '1.1.0', 'the crate1 description, this crate is for foobar', false, '2019-06-24 09:46:00.193', '2019-06-24 09:46:00.193', '''crate'':6 ''crate1'':1,3 ''descript'':4 ''foobar'':9', '09a6ccc7-fec4-42fd-9d6a-ea3afcc13629');
INSERT INTO public.crates_versions VALUES ('65d2a53f-f5f2-43a8-9d25-c38030404726', '1.1.4', 'the crate1 description, this crate is for foobar', false, '2019-06-24 09:46:00.205', '2019-06-24 09:46:00.205', '''crate'':6 ''crate1'':1,3 ''descript'':4 ''foobar'':9', '09a6ccc7-fec4-42fd-9d6a-ea3afcc13629');
INSERT INTO public.crates_versions VALUES ('41ca48f8-4f4c-4f38-81ff-0e3e4cbb1d4d', '1.1.5', 'the crate1 description, this crate is for foobar', true, '2019-06-24 09:46:00.213', '2019-06-24 09:46:00.213', '''crate'':6 ''crate1'':1,3 ''descript'':4 ''foobar'':9', '09a6ccc7-fec4-42fd-9d6a-ea3afcc13629');
INSERT INTO public.crates_versions VALUES ('addeb2ad-1043-4d99-b65b-0c76e421b608', '1.3.0', 'the crate2 description, this crate is for barbaz', false, '2019-06-24 09:46:00.219', '2019-06-24 09:46:00.219', '''barbaz'':9 ''crate'':6 ''crate2'':1,3 ''descript'':4', '05edcb4b-6e1e-46bc-ad67-3ab3557b72e9');
INSERT INTO public.crates_versions VALUES ('683c96f7-377d-4c9d-970b-040acde74ba4', '1.4.0', 'blablabla', false, '2019-06-24 09:46:00.224', '2019-06-24 09:46:00.224', '''blablabla'':2 ''crate3'':1', '6931c9ab-795a-49cd-b7d5-0787fa498b7b');


--
-- Data for Name: tokens; Type: TABLE DATA; Schema: public; Owner: meuse
--



--
-- Name: users_cargo_id_seq; Type: SEQUENCE SET; Schema: public; Owner: meuse
--

SELECT pg_catalog.setval('public.users_cargo_id_seq', 5, true);


--
-- PostgreSQL database dump complete
--

