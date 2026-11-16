#!/usr/bin/env bash
# adaptar.sh: prepara a máquina para rodar o Mercado FIPP (Linux e macOS).
#   1. Java 25: usa o que já existe; se não houver, baixa um JDK portátil para .ferramentas/jdk (não instala nada no sistema)
#   2. Maven: baixa as bibliotecas do backend (o mvnw baixa o próprio Maven)
#   3. Banco: cria o mercado_fipp no Postgres que já está instalado e carrega backend/mercadofipp.sql
# Pode rodar quantas vezes quiser: o que já está pronto é pulado.
# Variáveis opcionais: ADAPTAR_PG_HOST, ADAPTAR_PG_PORTA, ADAPTAR_PG_USER, ADAPTAR_PG_SENHA, ADAPTAR_PG_BANCO
set -u

RAIZ="$(cd "$(dirname "$0")" && pwd)"
FERR="$RAIZ/.ferramentas"
JAVA_MIN=25
PG_HOST="${ADAPTAR_PG_HOST:-localhost}"
PG_PORTA="${ADAPTAR_PG_PORTA:-5432}"
PG_USER="${ADAPTAR_PG_USER:-postgres}"
PG_SENHA="${ADAPTAR_PG_SENHA-}"   # sem senha escrita aqui: se precisar, ele pergunta
PG_BANCO="${ADAPTAR_PG_BANCO:-mercado_fipp}"

passo() { printf '\n\033[1;34m== %s\033[0m\n' "$*"; }
ok()    { printf '   \033[32mok\033[0m  %s\n' "$*"; }
aviso() { printf '   \033[33m!!\033[0m  %s\n' "$*"; }
erro()  { printf '\n\033[1;31mERRO:\033[0m %s\n' "$*" >&2; exit 1; }

[ -f "$RAIZ/backend/pom.xml" ] || erro "rode este script de dentro da pasta do projeto (não achei backend/pom.xml ao lado dele)."
printf '%s' "$PG_BANCO" | grep -Eq '^[A-Za-z0-9_]+$' || erro "nome de banco inválido: use só letras, números e _."

# ---------------------------------------------------------------- 1. Java
java_major() { "$1" -version 2>&1 | head -1 | sed -E 's/.*version "([0-9]+).*/\1/'; }
java_home_de() { "$1" -XshowSettings:properties -version 2>&1 | sed -n 's/^ *java.home = //p' | head -1; }

# devolve (em JAVA_HOME) a pasta de um Java >= 25, ou falha
achar_java() {
  local candidato maior
  for candidato in "$FERR/jdk/bin/java" "$FERR/jdk/Contents/Home/bin/java" "${JAVA_HOME:-/nao/existe}/bin/java" "$(command -v java 2>/dev/null || true)"; do
    [ -x "$candidato" ] || continue
    maior="$(java_major "$candidato")"
    case "$maior" in ''|*[!0-9]*) continue ;; esac
    if [ "$maior" -ge "$JAVA_MIN" ]; then
      JAVA_HOME="$(java_home_de "$candidato")"
      JAVA_VERSAO="$maior"
      return 0
    fi
  done
  return 1
}

baixar_jdk() {
  command -v curl >/dev/null 2>&1 || erro "preciso do 'curl' para baixar o Java. Instale-o e rode de novo."
  local os arch url tmp final esperado real
  case "$(uname -s)" in Linux) os=linux ;; Darwin) os=mac ;; *) erro "sistema não suportado por este script ($(uname -s)). No Windows use o adaptar.exe." ;; esac
  case "$(uname -m)" in x86_64|amd64) arch=x64 ;; aarch64|arm64) arch=aarch64 ;; *) erro "arquitetura não suportada: $(uname -m)" ;; esac
  url="https://api.adoptium.net/v3/binary/latest/$JAVA_MIN/ga/$os/$arch/jdk/hotspot/normal/eclipse"
  mkdir -p "$FERR"
  tmp="$FERR/jdk-download.tar.gz"
  echo "   baixando o JDK $JAVA_MIN (Temurin, uns 200 MB)..."
  # o primeiro redirect da API aponta para o arquivo na release do GitHub; ao lado dele fica o .sha256.txt
  final="$(curl -fsI -o /dev/null -w '%{redirect_url}' "$url")"
  [ -n "$final" ] || erro "não consegui consultar o Adoptium. Sem internet ou atrás de proxy? Veja a seção de problemas no README."
  curl -fL --retry 3 --progress-bar -o "$tmp" "$url" || { rm -f "$tmp"; erro "falha ao baixar o JDK. Sem internet ou atrás de proxy? Veja a seção de problemas no README."; }
  esperado="$(curl -fsL "$final.sha256.txt" | tr -d '\0' | awk '{print $1}')"
  if command -v sha256sum >/dev/null 2>&1; then real="$(sha256sum "$tmp" | awk '{print $1}')"; else real="$(shasum -a 256 "$tmp" | awk '{print $1}')"; fi
  if [ -z "$esperado" ] || [ "$esperado" != "$real" ]; then rm -f "$tmp"; erro "o arquivo baixado não bate com o SHA-256 publicado (esperado '$esperado', veio '$real'). Tente de novo."; fi
  ok "SHA-256 conferido"
  rm -rf "$FERR/jdk"; mkdir -p "$FERR/jdk"
  tar -xzf "$tmp" -C "$FERR/jdk" --strip-components=1 || erro "não consegui extrair o JDK."
  rm -f "$tmp"
}

passo "1/3  Java $JAVA_MIN ou mais novo"
if achar_java; then
  ok "Java $JAVA_VERSAO encontrado em $JAVA_HOME"
else
  aviso "nenhum Java $JAVA_MIN+ encontrado; baixando um JDK só para este projeto"
  baixar_jdk
  achar_java || erro "baixei o JDK mas não consegui executá-lo."
  ok "JDK $JAVA_VERSAO pronto em $JAVA_HOME"
fi
export JAVA_HOME

# ---------------------------------------------------------------- 2. Maven
passo "2/3  Bibliotecas do backend (Maven)"
echo "   a primeira vez baixa bastante coisa; as próximas são rápidas."
( cd "$RAIZ/backend" && sh mvnw -B -ntp dependency:go-offline && sh mvnw -B -ntp -q -DskipTests compile ) \
  || erro "o Maven falhou. Role a tela para cima: a causa costuma ser rede/proxy (veja o README)."
ok "dependências baixadas e o backend compila"

# ---------------------------------------------------------------- 3. Banco
passo "3/3  Banco de dados ($PG_BANCO no Postgres local)"
PSQL="$(command -v psql 2>/dev/null || true)"
[ -n "$PSQL" ] || erro "não achei o 'psql'. O Postgres está instalado? Coloque a pasta bin dele no PATH e rode de novo."

psql_cmd() { PGPASSWORD="$PG_SENHA" PGCLIENTENCODING=UTF8 "$PSQL" -w -h "$PG_HOST" -p "$PG_PORTA" -U "$PG_USER" -X -q -v ON_ERROR_STOP=1 "$@"; }

tentativas=0
until psql_cmd -d postgres -tAc 'select 1' >/dev/null 2>&1; do
  tentativas=$((tentativas + 1))
  [ -t 0 ] && [ "$tentativas" -le 3 ] || erro "não consegui entrar no Postgres em $PG_HOST:$PG_PORTA como '$PG_USER'. Ele está ligado? A senha confere? (ADAPTAR_PG_USER / ADAPTAR_PG_SENHA mudam isso)"
  aviso "não consegui conectar em $PG_HOST:$PG_PORTA como '$PG_USER'. Informe os dados (tentativa $tentativas de 3)."
  read -r -p "   usuário do Postgres [$PG_USER]: " resp; PG_USER="${resp:-$PG_USER}"
  read -r -s -p "   senha: " PG_SENHA; echo
done
ok "conectado como $PG_USER"

SQL="$RAIZ/backend/mercadofipp.sql"
existe="$(psql_cmd -d postgres -tAc "select 1 from pg_database where datname = '$PG_BANCO'")"
if [ "$existe" != "1" ]; then
  psql_cmd -d postgres -c "create database \"$PG_BANCO\"" || erro "não consegui criar o banco $PG_BANCO."
  if psql_cmd -d "$PG_BANCO" -f "$SQL" >/dev/null; then
    ok "banco $PG_BANCO criado e carregado com os dados de teste"
  else
    psql_cmd -d postgres -c "drop database \"$PG_BANCO\"" >/dev/null 2>&1
    erro "falha ao carregar $SQL; apaguei o banco pela metade. Veja a mensagem acima."
  fi
else
  tem_tabela="$(psql_cmd -d "$PG_BANCO" -tAc "select 1 from information_schema.tables where table_schema='public' and table_name='usuario'")"
  if [ "$tem_tabela" != "1" ]; then
    psql_cmd -d "$PG_BANCO" -f "$SQL" >/dev/null || erro "falha ao carregar $SQL no banco existente (vazio)."
    ok "banco $PG_BANCO já existia vazio; carreguei as tabelas e os dados de teste"
  else
    largura="$(psql_cmd -d "$PG_BANCO" -tAc "select coalesce(character_maximum_length, 0) from information_schema.columns where table_name='usuario' and column_name='usr_pass'")"
    if [ "${largura:-0}" -lt 60 ]; then
      # banco antigo, com senha em texto puro: aplica só o bloco "SENHAS COM BCRYPT" (idempotente) do fim do script
      inicio="$(grep -n -- '-- SENHAS COM BCRYPT' "$SQL" | head -1 | cut -d: -f1)"
      [ -n "$inicio" ] || erro "não achei o bloco de senhas em $SQL."
      sed -n "$((inicio - 1)),\$p" "$SQL" | psql_cmd -d "$PG_BANCO" -f - >/dev/null || erro "falha ao converter as senhas para BCrypt."
      ok "banco $PG_BANCO já existia: senhas convertidas para BCrypt (os anúncios e demais dados ficaram como estavam)"
    else
      ok "banco $PG_BANCO já existe e está atualizado; não mexi nos dados"
    fi
  fi
fi

# guarda como o backend deve conectar (lido pelo iniciar.sh). Fica fora do git (.gitignore).
mkdir -p "$FERR"
umask 077
{
  echo "PG_HOST=$PG_HOST"
  echo "PG_PORTA=$PG_PORTA"
  echo "PG_BANCO=$PG_BANCO"
  echo "PG_USER=$PG_USER"
  echo "PG_SENHA=$PG_SENHA"
} > "$FERR/banco.conf"
ok "configuração de conexão salva em .ferramentas/banco.conf"

# o backend lê a conexão deste arquivo (importado pelo application.properties); fica fora do git
propriedade() { printf '%s' "$1" | sed -e 's/\\/\\\\/g' -e 's/^ /\\ /'; }
{
  echo "spring.datasource.url=jdbc:postgresql://$PG_HOST:$PG_PORTA/$PG_BANCO"
  echo "spring.datasource.username=$(propriedade "$PG_USER")"
  echo "spring.datasource.password=$(propriedade "$PG_SENHA")"
} > "$RAIZ/backend/application-local.properties"
ok "conexão do backend gravada em backend/application-local.properties"

printf '\n\033[1;32mPronto.\033[0m Para subir o sistema:  ./iniciar.sh   (o README explica o resto)\n'
