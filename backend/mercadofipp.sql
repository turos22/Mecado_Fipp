--
-- PostgreSQL database dump
--

-- Dumped from database version 10.10
-- Dumped by pg_dump version 16.2

-- Started on 2024-10-23 11:21:57

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
-- TOC entry 6 (class 2615 OID 2200)
-- Name: public; Type: SCHEMA; Schema: -; Owner: postgres
--

-- *not* creating schema, since initdb creates it


ALTER SCHEMA public OWNER TO postgres;

SET default_tablespace = '';

--
-- TOC entry 196 (class 1259 OID 205115)
-- Name: anuncio; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.anuncio (
    anu_id integer NOT NULL,
    anu_title character varying(80),
    anu_date date,
    anu_desc text,
    anu_price numeric(10,1),
    cat_id integer,
    usr_id integer
);


ALTER TABLE public.anuncio OWNER TO postgres;

--
-- TOC entry 197 (class 1259 OID 205121)
-- Name: anuncio_anu_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.anuncio_anu_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.anuncio_anu_id_seq OWNER TO postgres;

--
-- TOC entry 2852 (class 0 OID 0)
-- Dependencies: 197
-- Name: anuncio_anu_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.anuncio_anu_id_seq OWNED BY public.anuncio.anu_id;


--
-- TOC entry 198 (class 1259 OID 205123)
-- Name: categoria; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.categoria (
    cat_id integer NOT NULL,
    cat_name character varying(20)
);


ALTER TABLE public.categoria OWNER TO postgres;

--
-- TOC entry 199 (class 1259 OID 205126)
-- Name: categoria_cat_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.categoria_cat_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.categoria_cat_id_seq OWNER TO postgres;

--
-- TOC entry 2853 (class 0 OID 0)
-- Dependencies: 199
-- Name: categoria_cat_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.categoria_cat_id_seq OWNED BY public.categoria.cat_id;


--
-- TOC entry 200 (class 1259 OID 205128)
-- Name: foto_anuncio; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.foto_anuncio (
    fot_id integer NOT NULL,
    fot_file character varying(20),
    anu_id integer
);


ALTER TABLE public.foto_anuncio OWNER TO postgres;

--
-- TOC entry 201 (class 1259 OID 205131)
-- Name: foto_anuncio_fot_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.foto_anuncio_fot_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.foto_anuncio_fot_id_seq OWNER TO postgres;

--
-- TOC entry 2854 (class 0 OID 0)
-- Dependencies: 201
-- Name: foto_anuncio_fot_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.foto_anuncio_fot_id_seq OWNED BY public.foto_anuncio.fot_id;


--
-- TOC entry 202 (class 1259 OID 205133)
-- Name: pergunta_anuncio; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.pergunta_anuncio (
    per_id integer NOT NULL,
    per_text text,
    anu_id integer,
    per_resp text
);


ALTER TABLE public.pergunta_anuncio OWNER TO postgres;

--
-- TOC entry 203 (class 1259 OID 205139)
-- Name: pergunta_anuncio_per_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.pergunta_anuncio_per_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.pergunta_anuncio_per_id_seq OWNER TO postgres;

--
-- TOC entry 2855 (class 0 OID 0)
-- Dependencies: 203
-- Name: pergunta_anuncio_per_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.pergunta_anuncio_per_id_seq OWNED BY public.pergunta_anuncio.per_id;


--
-- TOC entry 204 (class 1259 OID 205141)
-- Name: usuario; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.usuario (
    usr_id integer NOT NULL,
    usr_name character varying(20),
    usr_pass character varying(10),
    usr_level character varying(1)
);


ALTER TABLE public.usuario OWNER TO postgres;

--
-- TOC entry 205 (class 1259 OID 205144)
-- Name: usuario_usr_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.usuario_usr_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.usuario_usr_id_seq OWNER TO postgres;

--
-- TOC entry 2856 (class 0 OID 0)
-- Dependencies: 205
-- Name: usuario_usr_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.usuario_usr_id_seq OWNED BY public.usuario.usr_id;


--
-- TOC entry 2696 (class 2604 OID 205146)
-- Name: anuncio anu_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.anuncio ALTER COLUMN anu_id SET DEFAULT nextval('public.anuncio_anu_id_seq'::regclass);


--
-- TOC entry 2697 (class 2604 OID 205147)
-- Name: categoria cat_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.categoria ALTER COLUMN cat_id SET DEFAULT nextval('public.categoria_cat_id_seq'::regclass);


--
-- TOC entry 2698 (class 2604 OID 205148)
-- Name: foto_anuncio fot_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.foto_anuncio ALTER COLUMN fot_id SET DEFAULT nextval('public.foto_anuncio_fot_id_seq'::regclass);


--
-- TOC entry 2699 (class 2604 OID 205149)
-- Name: pergunta_anuncio per_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.pergunta_anuncio ALTER COLUMN per_id SET DEFAULT nextval('public.pergunta_anuncio_per_id_seq'::regclass);


--
-- TOC entry 2700 (class 2604 OID 205150)
-- Name: usuario usr_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuario ALTER COLUMN usr_id SET DEFAULT nextval('public.usuario_usr_id_seq'::regclass);


--
-- TOC entry 2836 (class 0 OID 205115)
-- Dependencies: 196
-- Data for Name: anuncio; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.anuncio VALUES (1, 'Fonte gamer PC', '2024-10-23', 'Vendo fonte usada para PC com 500W em ótimo estado, marca Corsair', 190.0, 1, 2);


--
-- TOC entry 2838 (class 0 OID 205123)
-- Dependencies: 198
-- Data for Name: categoria; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.categoria VALUES (1, 'informática');
INSERT INTO public.categoria VALUES (2, 'livros');
INSERT INTO public.categoria VALUES (4, 'acessorios');
INSERT INTO public.categoria VALUES (3, 'celulares');


--
-- TOC entry 2840 (class 0 OID 205128)
-- Dependencies: 200
-- Data for Name: foto_anuncio; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- TOC entry 2842 (class 0 OID 205133)
-- Dependencies: 202
-- Data for Name: pergunta_anuncio; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.pergunta_anuncio VALUES (1, 'Faz desconto à vista?', 1, 'só se for no PIX');
INSERT INTO public.pergunta_anuncio VALUES (2, 'Reserva ela para mim.', 1, 'Feito! Manda o PIX para 18-99987-1569');


--
-- TOC entry 2844 (class 0 OID 205141)
-- Dependencies: 204
-- Data for Name: usuario; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.usuario VALUES (1, 'adm', 'adm123', '1');
INSERT INTO public.usuario VALUES (2, 'jao', 'jao123', '2');


--
-- TOC entry 2857 (class 0 OID 0)
-- Dependencies: 197
-- Name: anuncio_anu_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.anuncio_anu_id_seq', 1, true);


--
-- TOC entry 2858 (class 0 OID 0)
-- Dependencies: 199
-- Name: categoria_cat_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.categoria_cat_id_seq', 4, true);


--
-- TOC entry 2859 (class 0 OID 0)
-- Dependencies: 201
-- Name: foto_anuncio_fot_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.foto_anuncio_fot_id_seq', 1, false);


--
-- TOC entry 2860 (class 0 OID 0)
-- Dependencies: 203
-- Name: pergunta_anuncio_per_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.pergunta_anuncio_per_id_seq', 2, true);


--
-- TOC entry 2861 (class 0 OID 0)
-- Dependencies: 205
-- Name: usuario_usr_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.usuario_usr_id_seq', 2, true);


--
-- TOC entry 2702 (class 2606 OID 205152)
-- Name: anuncio anuncio_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.anuncio
    ADD CONSTRAINT anuncio_pkey PRIMARY KEY (anu_id);


--
-- TOC entry 2704 (class 2606 OID 205154)
-- Name: categoria categoria_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.categoria
    ADD CONSTRAINT categoria_pkey PRIMARY KEY (cat_id);


--
-- TOC entry 2706 (class 2606 OID 205156)
-- Name: foto_anuncio foto_anuncio_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.foto_anuncio
    ADD CONSTRAINT foto_anuncio_pkey PRIMARY KEY (fot_id);


--
-- TOC entry 2708 (class 2606 OID 205158)
-- Name: pergunta_anuncio pergunta_anuncio_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.pergunta_anuncio
    ADD CONSTRAINT pergunta_anuncio_pkey PRIMARY KEY (per_id);


--
-- TOC entry 2710 (class 2606 OID 205160)
-- Name: usuario usuario_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT usuario_pkey PRIMARY KEY (usr_id);


--
-- TOC entry 2711 (class 2606 OID 205171)
-- Name: anuncio anuncio_cat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.anuncio
    ADD CONSTRAINT anuncio_cat_id_fkey FOREIGN KEY (cat_id) REFERENCES public.categoria(cat_id) NOT VALID;


--
-- TOC entry 2712 (class 2606 OID 205176)
-- Name: anuncio anuncio_usr_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.anuncio
    ADD CONSTRAINT anuncio_usr_id_fkey FOREIGN KEY (usr_id) REFERENCES public.usuario(usr_id) NOT VALID;


--
-- TOC entry 2713 (class 2606 OID 205161)
-- Name: foto_anuncio foto_anuncio_anu_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.foto_anuncio
    ADD CONSTRAINT foto_anuncio_anu_id_fkey FOREIGN KEY (anu_id) REFERENCES public.anuncio(anu_id);


--
-- TOC entry 2714 (class 2606 OID 205166)
-- Name: pergunta_anuncio pergunta_anuncio_anu_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.pergunta_anuncio
    ADD CONSTRAINT pergunta_anuncio_anu_id_fkey FOREIGN KEY (anu_id) REFERENCES public.anuncio(anu_id);


--
-- TOC entry 2851 (class 0 OID 0)
-- Dependencies: 6
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE USAGE ON SCHEMA public FROM PUBLIC;
GRANT ALL ON SCHEMA public TO PUBLIC;


-- Completed on 2024-10-23 11:21:57

--
-- PostgreSQL database dump complete
--



-- ============================================================================
-- DADOS DE TESTE (acrescentados ao dump acima; tudo fictício)
--
-- Tudo daqui para baixo respeita o schema do dump: senha de até 10 caracteres, nível '1' (administrador)
-- ou '2' (vendedor/comprador), preço com 1 casa decimal e nome de foto de até 20 caracteres.
-- Os ids foram escolhidos à mão para continuar depois dos dados que o dump já traz
-- (usuários 1-2, categorias 1-4, anúncio 1, perguntas 1-2).
--
-- Esta seção pode ser rodada de novo, sozinha, quantas vezes quiser: ON CONFLICT DO NOTHING
-- só insere o que ainda não existe e nunca altera nem apaga nada.
-- O nome das tabelas leva "public." porque o dump zera o search_path no começo.
--
-- Logins de teste (usuário / senha):  adm / adm123 (administrador),  jao / jao123,
--   marina / mari123,  rafael / rafa123,  camila / cami123,  thiago / thi123  (vendedores/compradores)
-- ============================================================================

-- Categorias (o dump já tem 1 informática, 2 livros, 3 celulares, 4 acessorios)
INSERT INTO public.categoria (cat_id, cat_name) VALUES
    (5, 'eletrônicos'),
    (6, 'móveis'),
    (7, 'vestuário')
ON CONFLICT DO NOTHING;

-- Usuários (o dump já tem 1 adm e 2 jao)
INSERT INTO public.usuario (usr_id, usr_name, usr_pass, usr_level) VALUES
    (3, 'marina', 'mari123', '2'),
    (4, 'rafael', 'rafa123', '2'),
    (5, 'camila', 'cami123', '2'),
    (6, 'thiago', 'thi123',  '2')
ON CONFLICT DO NOTHING;

-- Anúncios. Os ids crescem junto com a data, então "os últimos 5 inseridos" são os ids 9 a 13.
-- Preços com 1 casa decimal, que é o que a coluna guarda (numeric(10,1)).
INSERT INTO public.anuncio (anu_id, anu_title, anu_date, anu_desc, anu_price, cat_id, usr_id) VALUES
    (2,  'Cálculo, volume 1 (Stewart)',       '2026-10-20', 'Livro usado, 8ª edição, sem rasuras. Só uma orelha na capa. Retirada na FIPP.',                       85.0, 2, 3),
    (3,  'Notebook Dell Inspiron 15',          '2026-10-22', 'i5 de 11ª geração, 8 GB de RAM, SSD de 256 GB. Bateria dura cerca de 4 horas. Acompanha carregador.', 2450.0, 1, 4),
    (4,  'Mouse sem fio Logitech M170',        '2026-10-25', 'Novo na caixa, comprei dois por engano. Pilha inclusa.',                                              45.9, 4, 5),
    (5,  'iPhone 11 128 GB',                   '2026-10-28', 'Usado, bateria com 84% de saúde. Tela sem riscos, com película. Caixa e cabo originais.',            1890.0, 3, 6),
    (6,  'Banco de Dados, Elmasri e Navathe',  '2026-11-01', 'Edição de 2011, em bom estado, com marcações a lápis. Serve para a disciplina de Banco de Dados.',     60.0, 2, 4),
    (7,  'Fone de ouvido Bluetooth JBL',       '2026-11-03', 'Quase novo, usado por 2 meses. Bateria de 20 horas. Estojo de transporte incluso.',                  120.0, 5, 3),
    (8,  'Escrivaninha com gaveta',            '2026-11-05', 'Madeira clara, 120 cm. Pequeno risco no tampo. Você precisa buscar (cabe em carro pequeno).',        180.0, 6, 5),
    (9,  'Monitor LG 24 polegadas Full HD',    '2026-11-08', 'Monitor IPS de 24", HDMI e VGA. Sem pixels mortos. Cabo HDMI incluso.',                              520.0, 1, 6),
    (10, 'Camiseta oficial da Atlética FIPP',  '2026-11-10', 'Tamanho M, usada duas vezes. Estampa em perfeito estado.',                                            35.0, 7, 3),
    (11, 'Teclado mecânico Redragon',          '2026-11-12', 'Switch azul, ABNT2, iluminação RGB. Usado por 6 meses, funcionando perfeitamente.',                  210.0, 4, 4),
    (12, 'Clean Code (Robert C. Martin)',      '2026-11-13', 'Em português, capa mole, impecável. Indispensável para Engenharia de Software.',                      70.0, 2, 5),
    (13, 'Samsung Galaxy A54 128 GB',          '2026-11-14', 'Seminovo, comprado em março. Acompanha capa, película e carregador rápido. Nota fiscal disponível.', 1350.0, 3, 6)
ON CONFLICT DO NOTHING;

-- Fotos: de 0 a 3 por anúncio (o 1, o 4, o 6 e o 12 ficam sem foto de propósito).
-- fot_file aceita no máximo 20 caracteres. Os arquivos não vêm no script: para a imagem aparecer no front,
-- o arquivo com esse nome precisa existir na pasta de uploads do backend.
INSERT INTO public.foto_anuncio (fot_id, fot_file, anu_id) VALUES
    (1,  'calculo-1.jpg',      2),
    (2,  'calculo-2.jpg',      2),
    (3,  'inspiron-1.jpg',     3),
    (4,  'inspiron-2.jpg',     3),
    (5,  'inspiron-3.jpg',     3),
    (6,  'iphone11-1.jpg',     5),
    (7,  'iphone11-2.jpg',     5),
    (8,  'jbl-1.jpg',          7),
    (9,  'escrivaninha-1.jpg', 8),
    (10, 'monitor-lg-1.jpg',   9),
    (11, 'monitor-lg-2.jpg',   9),
    (12, 'camiseta-1.jpg',    10),
    (13, 'redragon-1.jpg',    11),
    (14, 'redragon-2.jpg',    11),
    (15, 'redragon-3.jpg',    11),
    (16, 'galaxy-a54-1.jpg',  13)
ON CONFLICT DO NOTHING;

-- Perguntas e respostas (per_resp vazio = ainda sem resposta; cada pergunta tem uma resposta só)
INSERT INTO public.pergunta_anuncio (per_id, per_text, per_resp, anu_id) VALUES
    (3,  'O livro tem anotações no miolo?',                      'Só uma ou duas marcações a lápis nos primeiros capítulos.', 2),
    (4,  'Aceita trocar por outro livro de cálculo?',             NULL,                                                        2),
    (5,  'Qual é a versão do Windows? Vem com licença?',          'Vem com Windows 11 Home, ativado.',                         3),
    (6,  'Faz por R$ 2.200 à vista?',                             'Consigo fazer por R$ 2.350 à vista.',                       3),
    (7,  'Tem garantia?',                                         NULL,                                                        5),
    (8,  'A bateria já foi trocada?',                             NULL,                                                        5),
    (9,  'Dá para retirar na quarta, na saída da aula da noite?', 'Dá sim, combinamos pelo portão principal.',                 9),
    (10, 'O teclado é ABNT2 mesmo?',                              'Sim, ABNT2 com ç.',                                         11),
    (11, 'Ainda está disponível?',                                NULL,                                                        13)
ON CONFLICT DO NOTHING;

-- Sequências: os dados acima entraram com ids escolhidos à mão. Isto leva cada sequência ao próximo id livre,
-- senão o primeiro cadastro de verdade pelo sistema tentaria usar um id que já existe.
SELECT pg_catalog.setval('public.usuario_usr_id_seq',                COALESCE(MAX(usr_id), 0) + 1, false) FROM public.usuario;
SELECT pg_catalog.setval('public.categoria_cat_id_seq',              COALESCE(MAX(cat_id), 0) + 1, false) FROM public.categoria;
SELECT pg_catalog.setval('public.anuncio_anu_id_seq',                COALESCE(MAX(anu_id), 0) + 1, false) FROM public.anuncio;
SELECT pg_catalog.setval('public.foto_anuncio_fot_id_seq',           COALESCE(MAX(fot_id), 0) + 1, false) FROM public.foto_anuncio;
SELECT pg_catalog.setval('public.pergunta_anuncio_per_id_seq',       COALESCE(MAX(per_id), 0) + 1, false) FROM public.pergunta_anuncio;


-- =====================================================================
-- SENHAS COM BCRYPT
-- O login confere o hash BCrypt (60 caracteres), então usr_pass passa de varchar(10) para varchar(60)
-- e as senhas de teste acima viram hash. Pode ser colado em banco que já tem os dados: só troca
-- a senha que ainda estiver em texto puro, e rodar de novo não faz nada.
-- As senhas para LOGAR continuam as mesmas (adm/adm123, jao/jao123, marina/mari123, rafael/rafa123,
-- camila/cami123, thiago/thi123). Usuário criado por fora deste script com senha em texto puro
-- NÃO consegue mais logar: grave o hash BCrypt dele.
-- =====================================================================
ALTER TABLE public.usuario ALTER COLUMN usr_pass TYPE character varying(60);

UPDATE public.usuario u
   SET usr_pass = s.hash
  FROM (VALUES
    ('adm', 'adm123', '$2a$10$IY1YXEWgxNvvKcqifsLHau4BXzthArQ0yUWg0P7JXgWHiOdtmNMaO'),
    ('jao', 'jao123', '$2a$10$OSByNm4EZe5DdB4BTYD/V.x9SItDOewc552C5JkYDgFL1Y9dfnBRK'),
    ('marina', 'mari123', '$2a$10$BEPYpVT9TxPoqKpS26dU7eiTWrGiFXPR/3W93CgLSW9Ee1ScyxsFC'),
    ('rafael', 'rafa123', '$2a$10$Fi9AeZz5yxSClKWoltpTlu3EN3K7Lv/boZadG/LIa.4oWO1SbO5GK'),
    ('camila', 'cami123', '$2a$10$ZV7pB6CDhSj4oxLkQk5E/eHG29GEYVda2lxQJw42CeeskwvIq2PRO'),
    ('thiago', 'thi123', '$2a$10$AE3XUdIAo8DQXFPg/O6Y1uopRrtXUmAspU5uzDI91WHbhojNFI7.2')
  ) AS s(nome, texto, hash)
 WHERE u.usr_name = s.nome AND u.usr_pass = s.texto;
