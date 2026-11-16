# Front-end do Mercado FIPP

HTML5, CSS, JavaScript puro, Fetch API e Bootstrap 5 (pelo CDN, com a versão fixada). Sem build, sem npm.

## Como rodar

1. Ligue o backend (`http://localhost:8080`) e o PostgreSQL.
2. Sirva esta pasta por HTTP (abrir o arquivo com duplo clique não funciona bem). Dentro de `frontend/`:
   ```bash
   python3 -m http.server 5500
   ```
   (ou use a extensão Live Server do VS Code). Abra `http://localhost:5500`.
3. Se o backend estiver em outro endereço, mude `js/config.js`. É o único lugar.

O CORS do backend já aceita qualquer origem, então a porta do front não importa.

## Estrutura

```
frontend/
├── index.html            entrada: manda para o login, ou para os anúncios se já houver sessão
├── paginas/              login.html · anuncios.html (vitrine, para todos) · anuncio.html (um anúncio) · meus-anuncios.html (editar os próprios)
├── estilo/estilo.css     complementos ao Bootstrap
└── js/
    ├── config.js         endereço do backend e rotas
    ├── util.js           formatar preço/data, escapar HTML
    ├── sessao.js         token e quem está logado (sessionStorage)
    ├── http.js           ÚNICO lugar que chama fetch
    ├── cabecalho.js      barra do topo
    ├── controller/       conversa com o backend, uma rota por função
    │   ├── autenticacaoController.js
    │   └── anuncioController.js
    └── paginas/          código de cada tela (login.js, anuncios.js, anuncio.js, meus-anuncios.js)
```

Regra: a página chama o `controller`, o `controller` chama o `Http`. A página nunca usa `fetch` direto.

## Rotas usadas

| Tela | Chamada |
|---|---|
| Login | `POST apis/autenticacao/login` (usuario e senha no corpo) |
| Vitrine | `GET apis/adm/anuncio/get-filter/{texto}` (administrador) ou `apis/usuario/get-filter/{texto}` |
| Um anúncio | `GET .../get-id/{id}` |
| Meus anúncios | `GET apis/usuario/meus` (o dono vem do token) |
| Editar o meu anúncio | `PUT apis/usuario/meus/{id}` com `{anu_title, anu_desc, anu_price, cat_id}`; anúncio de outro = 404 |
| Excluir (só administrador) | `DELETE apis/adm/anuncio?id={id}` |

O token vai no cabeçalho `Authorization`, sem `Bearer` (é o que o `AccessFilter` do backend lê).

## O que o backend precisa devolver (combinar com o grupo)

- **Busca vazia:** o front chama `get-filter/todos`. O backend precisa devolver **todos** os anúncios nesse caso
  (o valor está em `Config.FILTRO_TODOS`).
- **`get-filter` e `get-id` devolvem JSON** (o backend já faz isso para o administrador), com estes campos (nomes das colunas do banco):
  ```json
  { "anu_id": 3, "anu_title": "...", "anu_date": "2026-10-22", "anu_desc": "...", "anu_price": 2450.0,
    "cat_id": 1, "cat_name": "informática", "usr_id": 4, "usr_name": "rafael",
    "fotos": [ { "fot_id": 3, "fot_file": "inspiron-1.jpg" } ],
    "perguntas": [ { "per_id": 5, "per_text": "...", "per_resp": "..." } ] }
  ```
  Se o backend usar outros nomes, ajuste só a função `normalizar` em `js/controller/anuncioController.js`.
- **Fotos:** o front baixa de `Config.URL_FOTOS + fot_file`. Sem a imagem, aparece uma imagem cinza "Sem foto".
- **Token:** claims `nivel` (`admin` ou `usuario`, vindo de `usr_level` 1/2 da tabela `usuario`) e `usr_id`. O backend responde 401 (sem token/inválido/expirado) e 403 (nível sem permissão).
- **Nível do token:** o claim `nivel` deve ser `admin` ou `usuario`. Só `admin` vê o botão de excluir.
- **Erros:** `{"tipo": "...", "texto": "..."}` (classe `Erro`). Falha de token pode ser 401/403 ou, como hoje, 500 com
  "Não autorizado": nos dois casos o front volta ao login.

## Segurança

Títulos, descrições e perguntas vêm de outros usuários. Todo texto do servidor passa por `Util.esc` antes de ir
para o HTML. Não coloque dados do servidor em `innerHTML` sem isso.
