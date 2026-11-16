# Mercado FIPP

Sistema de classificados da FIPP: backend Spring Boot (REST + JWT + Spring Data JPA + PostgreSQL) e front em HTML/CSS/JS com Bootstrap.

```
adaptar.exe / adaptar.sh   prepara a máquina (Java, bibliotecas, banco)  -> rode UMA vez
iniciar.bat / iniciar.sh   sobe backend e front                          -> rode sempre que for usar
backend/                   Spring Boot (porta 8080) + mercadofipp.sql (banco)
frontend/                  páginas estáticas (porta 5500); detalhes em frontend/LEIAME.md
ferramentas/adaptar/       código-fonte do adaptar.exe (Go)
```

## O que você precisa ter

- **PostgreSQL instalado e ligado** (as máquinas da FIPP já têm). O `psql` precisa estar acessível; no Windows o `adaptar.exe` procura sozinho em `C:\Program Files\PostgreSQL\*\bin`.
- **Internet na primeira vez** (baixa o Java e as bibliotecas; nas próximas, não precisa).
- Nada mais. Java 25 e Maven **não** precisam estar instalados: se faltar o Java, o `adaptar` baixa um JDK portátil para a pasta `.ferramentas/` do projeto (não instala nada no sistema, não pede administrador).
- Linux/macOS: `curl`, `tar` e `unzip` (já vêm na maioria das distribuições).

## Passo a passo

### 1. Pegar o projeto

```bash
git clone <endereço-do-repositório>
cd <pasta-do-projeto>
```

### 2. Preparar (uma vez só)

**Windows:** dê dois cliques em `adaptar.exe` (ou rode no terminal).
**Linux/macOS:**

```bash
./adaptar.sh
```

Ele faz três coisas, nesta ordem, e avisa o que cada uma fez:

1. **Java 25** — usa o que já existe; se não houver, baixa o JDK (uns 300 MB) e confere o SHA-256.
2. **Bibliotecas do backend** — baixa tudo do Maven e compila (a primeira vez leva alguns minutos).
3. **Banco** — conecta no Postgres local (usuário `postgres`; se precisar de senha, ele pergunta — a senha **não** fica no código nem no git), cria o banco `mercado_fipp` e carrega `backend/mercadofipp.sql` com tabelas e dados de teste. Se o banco já existir, **não apaga nada**: só atualiza as senhas para BCrypt, se ainda estiverem em texto puro.

Pode rodar de novo quando quiser; o que já está pronto é pulado. A conexão fica em `.ferramentas/banco.conf` e em `backend/application-local.properties` (é de lá que o backend lê a senha). Os dois ficam **fora do git**.

### 3. Subir o sistema

**Windows:** dois cliques em `iniciar.bat` (abre duas janelas: backend e front).
**Linux/macOS:**

```bash
./iniciar.sh
```

Espere o backend terminar de subir (`Started BackendApplication` no log) e abra **http://localhost:5500/**. No Linux/macOS, `Ctrl+C` derruba os dois; no Windows, feche as duas janelas.

### 4. Entrar

Todas as senhas de teste estão no banco como hash BCrypt; para **entrar** use:

| Usuário | Senha | Nível |
|---|---|---|
| `adm` | `adm123` | administrador (vê a lista toda e pode excluir anúncios) |
| `jao` | `jao123` | usuário |
| `marina` | `mari123` | usuário |
| `rafael` | `rafa123` | usuário |
| `camila` | `cami123` | usuário |
| `thiago` | `thi123` | usuário |

A tela inicial é a vitrine de anúncios, para todos. Em **Meus anúncios** (cabeçalho) cada usuário vê e edita só os próprios anúncios.

## Rodar os testes do backend

Os testes usam o banco `mercado_fipp` e desfazem tudo que gravam (rollback). Rode o `adaptar` antes.

```bash
cd backend
./mvnw test          # Windows: mvnw.cmd test
```

Se o Java 25 não estiver instalado no sistema, aponte para o JDK do projeto antes:
`export JAVA_HOME="$PWD/../.ferramentas/jdk"` (Windows: `set JAVA_HOME=%CD%\..\.ferramentas\jdk`).

## Problemas comuns

| Sintoma | Causa e solução |
|---|---|
| `não consegui entrar no Postgres` | O Postgres não está ligado ou a senha informada está errada. Rode o `adaptar` num terminal normal: ele pergunta usuário e senha. Sem terminal, use as variáveis `ADAPTAR_PG_USER` e `ADAPTAR_PG_SENHA`. |
| `não achei o psql` | A pasta `bin` do Postgres não está no PATH (Linux/macOS) ou ele não está instalado. |
| Falha ao baixar o JDK / Maven | Sem internet ou rede com proxy. Defina `HTTPS_PROXY` (adaptar) e, para o Maven, o proxy em `~/.m2/settings.xml`. |
| `a porta 8080 já está em uso` | Outro backend está rodando (por exemplo, pelo IntelliJ). Feche-o. O front usa a 8080 fixa (`frontend/js/config.js`). |
| `a porta 5500 já está em uso` | Use outra: `PORTA_FRONT=5600 ./iniciar.sh`. No Windows, feche o que usa a 5500. |
| Windows avisa "app não reconhecido" ou o antivírus bloqueia o `adaptar.exe` | O `.exe` não é assinado digitalmente. Se preferir, recompile você mesmo (abaixo) ou use o `adaptar.sh` no Git Bash/WSL. |
| Login dá "Usuário ou senha inválidos" para um usuário seu | Senha em texto puro não funciona mais: grave o hash BCrypt (veja abaixo). |
| Anúncios aparecem como "Sem foto" | Esperado: o backend ainda não serve os arquivos de foto. |

## Segredos (o repositório é público)

- **Senha do banco:** não está em nenhum arquivo versionado. Vem de `backend/application-local.properties`, gerado pelo `adaptar` (ignorado pelo git). Se rodar o backend pelo IntelliJ, ele funciona sozinho depois do `adaptar`, desde que a pasta de trabalho seja `backend/` (o padrão do Maven).
- **Chave do JWT:** vem da variável de ambiente `JWT_SECRET` (mínimo 32 caracteres). Sem ela, o backend sorteia uma chave a cada subida; o efeito é só precisar entrar de novo depois de reiniciar.
- **Senhas dos usuários de teste:** aparecem em `backend/mercadofipp.sql` e na tabela acima porque são dados de demonstração. Não use essas senhas em nada real.

## Senhas (BCrypt)

`usuario.usr_pass` guarda o hash BCrypt (60 caracteres), nunca a senha. Para um usuário novo, grave `new BCryptPasswordEncoder().encode(senha)`. O bloco **SENHAS COM BCRYPT** no fim de `backend/mercadofipp.sql` mostra os hashes dos usuários de teste.

## Recompilar o adaptar.exe

Precisa do [Go](https://go.dev/dl/) (qualquer sistema):

```bash
cd ferramentas/adaptar
GOOS=windows GOARCH=amd64 go build -trimpath -ldflags "-s -w" -o ../../adaptar.exe .
```

## Rotas do backend (resumo)

| Rota | Quem |
|---|---|
| `POST apis/autenticacao/login` (`usuario`, `senha`) | todos |
| `GET apis/usuario/get-filter/{texto}` · `get-id/{id}` | logado |
| `GET apis/usuario/meus` · `PUT apis/usuario/meus/{id}` | dono do anúncio |
| `GET apis/adm/anuncio/get-filter/{texto}` · `get-id/{id}` · `DELETE ?id=` | administrador |

O token JWT vai no header `Authorization` (sem `Bearer`). Resposta 401 = token ausente/inválido/expirado; 403 = nível sem permissão.
