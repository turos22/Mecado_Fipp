#!/usr/bin/env bash
# iniciar.sh: sobe o backend (porta 8080) e o front (porta 5500). Ctrl+C derruba os dois.
# Rode o ./adaptar.sh uma vez antes.
set -u
RAIZ="$(cd "$(dirname "$0")" && pwd)"
FERR="$RAIZ/.ferramentas"
PORTA_FRONT="${PORTA_FRONT:-5500}"
PORTA_BACK="${PORTA_BACK:-8080}"   # o front espera 8080; só mude para testes

erro() { printf '\nERRO: %s\n' "$*" >&2; exit 1; }
porta_ocupada() { (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null; }

[ -f "$FERR/banco.conf" ] || erro "rode o ./adaptar.sh antes (ele prepara o Java, as bibliotecas e o banco)."
while IFS='=' read -r chave valor; do
  case "$chave" in PG_HOST|PG_PORTA|PG_BANCO|PG_USER|PG_SENHA) printf -v "$chave" '%s' "$valor" ;; esac
done < "$FERR/banco.conf"

# mesmo Java que o adaptar escolheu: o JDK local, se existir; senão o do sistema
if   [ -x "$FERR/jdk/bin/java" ];               then JAVA_HOME="$FERR/jdk"
elif [ -x "$FERR/jdk/Contents/Home/bin/java" ]; then JAVA_HOME="$FERR/jdk/Contents/Home"
else
  command -v java >/dev/null 2>&1 || erro "Java não encontrado. Rode o ./adaptar.sh."
  JAVA_HOME="$(java -XshowSettings:properties -version 2>&1 | sed -n 's/^ *java.home = //p' | head -1)"
fi
export JAVA_HOME

porta_ocupada "$PORTA_BACK" && erro "a porta $PORTA_BACK já está em uso (outro backend rodando, talvez no IntelliJ). Feche-o e tente de novo."
porta_ocupada "$PORTA_FRONT" && erro "a porta $PORTA_FRONT já está em uso. Use outra: PORTA_FRONT=5600 ./iniciar.sh"

export SERVER_PORT="$PORTA_BACK"
export SPRING_DATASOURCE_URL="jdbc:postgresql://${PG_HOST:-localhost}:${PG_PORTA:-5432}/${PG_BANCO:-mercado_fipp}"
export SPRING_DATASOURCE_USERNAME="${PG_USER:-postgres}"
export SPRING_DATASOURCE_PASSWORD="${PG_SENHA-}"

( cd "$RAIZ/backend" && exec sh mvnw -B -ntp spring-boot:run ) &
BACK=$!
cleanup() { kill "$BACK" 2>/dev/null; pkill -P "$BACK" 2>/dev/null; wait "$BACK" 2>/dev/null; }
trap 'cleanup; exit 0' INT TERM
trap cleanup EXIT

printf '\nBackend subindo (a primeira vez demora um pouco)...\n'
for _ in $(seq 1 120); do
  porta_ocupada "$PORTA_BACK" && break
  kill -0 "$BACK" 2>/dev/null || erro "o backend caiu ao subir. Veja as mensagens acima."
  sleep 1
done
porta_ocupada "$PORTA_BACK" || erro "o backend não respondeu em 2 minutos."

printf '\n\033[1;32mTudo no ar.\033[0m  Abra:  http://localhost:%s/   (Ctrl+C para parar tudo)\n\n' "$PORTA_FRONT"
"$JAVA_HOME/bin/jwebserver" -d "$RAIZ/frontend" -p "$PORTA_FRONT"
