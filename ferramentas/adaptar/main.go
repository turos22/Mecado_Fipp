// adaptar: prepara a máquina para rodar o Mercado FIPP (versão Windows do adaptar.sh; roda também em Linux/macOS).
//  1. Java 25: usa o que já existe; senão baixa um JDK portátil para .ferramentas/jdk
//  2. Maven: baixa as bibliotecas do backend (o mvnw baixa o próprio Maven)
//  3. Banco: cria o mercado_fipp no Postgres já instalado e carrega backend/mercadofipp.sql
//
// Pode rodar quantas vezes quiser. Opcionais: ADAPTAR_PG_HOST, ADAPTAR_PG_PORTA, ADAPTAR_PG_USER,
// ADAPTAR_PG_SENHA, ADAPTAR_PG_BANCO, ADAPTAR_SEM_PAUSA.
package main

import (
	"archive/tar"
	"archive/zip"
	"bufio"
	"compress/gzip"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"fmt"
	"io"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"runtime"
	"sort"
	"strconv"
	"strings"

	"golang.org/x/term"
)

const javaMin = 25

type config struct {
	raiz, ferr                       string
	pgHost, pgPorta, pgUser, pgSenha string
	pgBanco                          string
	javaHome                         string
	psql                             string
}

func main() {
	err := executar()
	code := 0
	if err != nil {
		fmt.Fprintf(os.Stderr, "\nERRO: %v\n", err)
		code = 1
	}
	// quem dá dois cliques no .exe veria a janela fechar antes de ler a mensagem
	if runtime.GOOS == "windows" && os.Getenv("ADAPTAR_SEM_PAUSA") == "" {
		fmt.Print("\nPressione Enter para fechar...")
		bufio.NewReader(os.Stdin).ReadString('\n')
	}
	os.Exit(code)
}

func passo(s string) { fmt.Printf("\n== %s\n", s) }
func ok(s string)    { fmt.Printf("   ok  %s\n", s) }
func aviso(s string) { fmt.Printf("   !!  %s\n", s) }

func env(nome, padrao string) string {
	if v, existe := os.LookupEnv(nome); existe {
		return v
	}
	return padrao
}

func executar() error {
	exe, err := os.Executable()
	if err != nil {
		return err
	}
	raiz := filepath.Dir(exe)
	// "go run" deixa o executável numa pasta temporária: aceita também a pasta atual
	if _, e := os.Stat(filepath.Join(raiz, "backend", "pom.xml")); e != nil {
		if cwd, e2 := os.Getwd(); e2 == nil {
			raiz = cwd
		}
	}
	if _, e := os.Stat(filepath.Join(raiz, "backend", "pom.xml")); e != nil {
		return errors.New("rode o adaptar de dentro da pasta do projeto (não achei backend\\pom.xml ao lado dele)")
	}
	c := &config{
		raiz:    raiz,
		ferr:    filepath.Join(raiz, ".ferramentas"),
		pgHost:  env("ADAPTAR_PG_HOST", "localhost"),
		pgPorta: env("ADAPTAR_PG_PORTA", "5432"),
		pgUser:  env("ADAPTAR_PG_USER", "postgres"),
		pgSenha: env("ADAPTAR_PG_SENHA", ""), // sem senha escrita aqui: se precisar, ele pergunta
		pgBanco: env("ADAPTAR_PG_BANCO", "mercado_fipp"),
	}
	if !regexp.MustCompile(`^[A-Za-z0-9_]+$`).MatchString(c.pgBanco) {
		return errors.New("nome de banco inválido: use só letras, números e _")
	}
	if err := c.passoJava(); err != nil {
		return err
	}
	if err := c.passoMaven(); err != nil {
		return err
	}
	if err := c.passoBanco(); err != nil {
		return err
	}
	fmt.Println("\nPronto. Para subir o sistema, dê dois cliques em iniciar.bat (o README explica o resto).")
	return nil
}

// ---------------------------------------------------------------- 1. Java

func exeJava(home string) string {
	n := "java"
	if runtime.GOOS == "windows" {
		n = "java.exe"
	}
	return filepath.Join(home, "bin", n)
}

// devolve a pasta (java.home) de um Java >= 25, ou "" se o executável não serve
func javaServe(caminho string) (string, int) {
	if _, err := os.Stat(caminho); err != nil {
		return "", 0
	}
	saida, _ := exec.Command(caminho, "-XshowSettings:properties", "-version").CombinedOutput()
	var home string
	var maior int
	for _, linha := range strings.Split(string(saida), "\n") {
		l := strings.TrimSpace(linha)
		if v, ok := strings.CutPrefix(l, "java.home = "); ok {
			home = v
		}
		if v, ok := strings.CutPrefix(l, "java.specification.version = "); ok {
			maior, _ = strconv.Atoi(strings.Split(v, ".")[0])
		}
	}
	if maior >= javaMin {
		return home, maior
	}
	return "", maior
}

func (c *config) acharJava() (int, bool) {
	var candidatos []string
	candidatos = append(candidatos, exeJava(filepath.Join(c.ferr, "jdk")), exeJava(filepath.Join(c.ferr, "jdk", "Contents", "Home")))
	if jh := os.Getenv("JAVA_HOME"); jh != "" {
		candidatos = append(candidatos, exeJava(jh))
	}
	if p, err := exec.LookPath("java"); err == nil {
		candidatos = append(candidatos, p)
	}
	for _, cand := range candidatos {
		if home, maior := javaServe(cand); home != "" {
			c.javaHome = home
			return maior, true
		}
	}
	return 0, false
}

func (c *config) passoJava() error {
	passo(fmt.Sprintf("1/3  Java %d ou mais novo", javaMin))
	if maior, achou := c.acharJava(); achou {
		ok(fmt.Sprintf("Java %d encontrado em %s", maior, c.javaHome))
		return nil
	}
	aviso(fmt.Sprintf("nenhum Java %d+ encontrado; baixando um JDK só para este projeto", javaMin))
	if err := c.baixarJDK(); err != nil {
		return err
	}
	maior, achou := c.acharJava()
	if !achou {
		return errors.New("baixei o JDK mas não consegui executá-lo")
	}
	ok(fmt.Sprintf("JDK %d pronto em %s", maior, c.javaHome))
	return nil
}

func (c *config) baixarJDK() error {
	so := map[string]string{"windows": "windows", "linux": "linux", "darwin": "mac"}[runtime.GOOS]
	arq := map[string]string{"amd64": "x64", "arm64": "aarch64"}[runtime.GOARCH]
	if so == "" || arq == "" {
		return fmt.Errorf("sistema/arquitetura não suportados: %s/%s", runtime.GOOS, runtime.GOARCH)
	}
	url := fmt.Sprintf("https://api.adoptium.net/v3/binary/latest/%d/ga/%s/%s/jdk/hotspot/normal/eclipse", javaMin, so, arq)

	// o primeiro redirect aponta para o arquivo na release do GitHub; ao lado dele fica o .sha256.txt
	semSeguir := &http.Client{CheckRedirect: func(*http.Request, []*http.Request) error { return http.ErrUseLastResponse }}
	r, err := semSeguir.Get(url)
	if err != nil {
		return fmt.Errorf("não consegui consultar o Adoptium (sem internet ou atrás de proxy? veja o README): %w", err)
	}
	r.Body.Close()
	final := r.Header.Get("Location")
	if final == "" {
		return fmt.Errorf("resposta inesperada do Adoptium (HTTP %d)", r.StatusCode)
	}

	if err := os.MkdirAll(c.ferr, 0o755); err != nil {
		return err
	}
	ext := ".tar.gz"
	if runtime.GOOS == "windows" {
		ext = ".zip"
	}
	tmp := filepath.Join(c.ferr, "jdk-download"+ext)
	fmt.Println("   baixando o JDK (uns 200 MB)...")
	hash, err := baixar(url, tmp)
	if err != nil {
		os.Remove(tmp)
		return fmt.Errorf("falha ao baixar o JDK: %w", err)
	}
	defer os.Remove(tmp)

	esperado, err := textoDe(final + ".sha256.txt")
	if err != nil {
		return fmt.Errorf("não consegui buscar o SHA-256 publicado: %w", err)
	}
	if f := strings.Fields(esperado); len(f) == 0 || !strings.EqualFold(f[0], hash) {
		return fmt.Errorf("o arquivo baixado não bate com o SHA-256 publicado (esperado %q, veio %q); tente de novo", esperado, hash)
	}
	ok("SHA-256 conferido")

	destino := filepath.Join(c.ferr, "jdk")
	os.RemoveAll(destino)
	if ext == ".zip" {
		err = extrairZip(tmp, destino)
	} else {
		err = extrairTarGz(tmp, destino)
	}
	if err != nil {
		return fmt.Errorf("não consegui extrair o JDK: %w", err)
	}
	return nil
}

// baixa para um arquivo e devolve o SHA-256 dele; mostra o andamento de 10 em 10%
func baixar(url, arquivo string) (string, error) {
	r, err := http.Get(url)
	if err != nil {
		return "", err
	}
	defer r.Body.Close()
	if r.StatusCode != 200 {
		return "", fmt.Errorf("HTTP %d", r.StatusCode)
	}
	f, err := os.Create(arquivo)
	if err != nil {
		return "", err
	}
	defer f.Close()
	h := sha256.New()
	buf := make([]byte, 256*1024)
	var lido int64
	proximo := int64(10)
	for {
		n, e := r.Body.Read(buf)
		if n > 0 {
			f.Write(buf[:n])
			h.Write(buf[:n])
			lido += int64(n)
			if r.ContentLength > 0 && lido*100/r.ContentLength >= proximo {
				fmt.Printf("   %d%%\n", proximo)
				proximo += 10
			}
		}
		if e == io.EOF {
			break
		}
		if e != nil {
			return "", e
		}
	}
	return hex.EncodeToString(h.Sum(nil)), nil
}

func textoDe(url string) (string, error) {
	r, err := http.Get(url)
	if err != nil {
		return "", err
	}
	defer r.Body.Close()
	if r.StatusCode != 200 {
		return "", fmt.Errorf("HTTP %d", r.StatusCode)
	}
	b, err := io.ReadAll(io.LimitReader(r.Body, 4096))
	return string(b), err
}

// tira a primeira pasta do caminho ("jdk-25.0.5+8/bin/java" -> "bin/java") e impede sair do destino
func caminhoSeguro(destino, nome string) (string, bool) {
	nome = strings.ReplaceAll(nome, "\\", "/")
	partes := strings.SplitN(nome, "/", 2)
	if len(partes) < 2 || partes[1] == "" {
		return "", false
	}
	alvo := filepath.Join(destino, filepath.FromSlash(partes[1]))
	if !strings.HasPrefix(alvo, filepath.Clean(destino)+string(os.PathSeparator)) {
		return "", false
	}
	return alvo, true
}

func extrairZip(arquivo, destino string) error {
	z, err := zip.OpenReader(arquivo)
	if err != nil {
		return err
	}
	defer z.Close()
	for _, f := range z.File {
		alvo, valido := caminhoSeguro(destino, f.Name)
		if !valido {
			continue
		}
		if f.FileInfo().IsDir() {
			os.MkdirAll(alvo, 0o755)
			continue
		}
		if err := os.MkdirAll(filepath.Dir(alvo), 0o755); err != nil {
			return err
		}
		src, err := f.Open()
		if err != nil {
			return err
		}
		dst, err := os.OpenFile(alvo, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, f.Mode()|0o600)
		if err != nil {
			src.Close()
			return err
		}
		_, err = io.Copy(dst, src)
		src.Close()
		dst.Close()
		if err != nil {
			return err
		}
	}
	return nil
}

func extrairTarGz(arquivo, destino string) error {
	f, err := os.Open(arquivo)
	if err != nil {
		return err
	}
	defer f.Close()
	gz, err := gzip.NewReader(f)
	if err != nil {
		return err
	}
	t := tar.NewReader(gz)
	for {
		h, err := t.Next()
		if err == io.EOF {
			return nil
		}
		if err != nil {
			return err
		}
		alvo, valido := caminhoSeguro(destino, h.Name)
		if !valido {
			continue
		}
		switch h.Typeflag {
		case tar.TypeDir:
			os.MkdirAll(alvo, 0o755)
		case tar.TypeSymlink:
			os.MkdirAll(filepath.Dir(alvo), 0o755)
			os.Symlink(h.Linkname, alvo)
		case tar.TypeReg:
			os.MkdirAll(filepath.Dir(alvo), 0o755)
			dst, err := os.OpenFile(alvo, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, os.FileMode(h.Mode)|0o600)
			if err != nil {
				return err
			}
			_, err = io.Copy(dst, t)
			dst.Close()
			if err != nil {
				return err
			}
		}
	}
}

// ---------------------------------------------------------------- 2. Maven

func (c *config) mvnw(args ...string) error {
	var cmd *exec.Cmd
	if runtime.GOOS == "windows" {
		cmd = exec.Command("cmd", append([]string{"/c", "mvnw.cmd"}, args...)...)
	} else {
		cmd = exec.Command("sh", append([]string{"mvnw"}, args...)...)
	}
	cmd.Dir = filepath.Join(c.raiz, "backend")
	cmd.Env = append(os.Environ(), "JAVA_HOME="+c.javaHome)
	cmd.Stdout, cmd.Stderr = os.Stdout, os.Stderr
	return cmd.Run()
}

func (c *config) passoMaven() error {
	passo("2/3  Bibliotecas do backend (Maven)")
	fmt.Println("   a primeira vez baixa bastante coisa; as próximas são rápidas.")
	if err := c.mvnw("-B", "-ntp", "dependency:go-offline"); err != nil {
		return fmt.Errorf("o Maven falhou (role a tela para cima: a causa costuma ser rede/proxy, veja o README): %w", err)
	}
	if err := c.mvnw("-B", "-ntp", "-q", "-DskipTests", "compile"); err != nil {
		return fmt.Errorf("o backend não compilou: %w", err)
	}
	ok("dependências baixadas e o backend compila")
	return nil
}

// ---------------------------------------------------------------- 3. Banco

func acharPsql() string {
	if p, err := exec.LookPath("psql"); err == nil {
		return p
	}
	if runtime.GOOS == "windows" {
		var achados []string
		for _, base := range []string{os.Getenv("ProgramFiles"), os.Getenv("ProgramFiles(x86)"), `C:\Program Files`} {
			if base == "" {
				continue
			}
			m, _ := filepath.Glob(filepath.Join(base, "PostgreSQL", "*", "bin", "psql.exe"))
			achados = append(achados, m...)
		}
		if len(achados) > 0 {
			sort.Slice(achados, func(i, j int) bool { return versaoDe(achados[i]) < versaoDe(achados[j]) })
			return achados[len(achados)-1] // a versão mais nova
		}
	}
	return ""
}

func versaoDe(caminho string) int {
	n, _ := strconv.Atoi(filepath.Base(filepath.Dir(filepath.Dir(caminho))))
	return n
}

// roda o psql; stdin opcional (script SQL). Devolve a saída e o erro.
func (c *config) sql(banco string, stdin string, args ...string) (string, error) {
	base := []string{"-w", "-h", c.pgHost, "-p", c.pgPorta, "-U", c.pgUser, "-X", "-q", "-v", "ON_ERROR_STOP=1", "-d", banco}
	cmd := exec.Command(c.psql, append(base, args...)...)
	cmd.Env = append(os.Environ(), "PGPASSWORD="+c.pgSenha, "PGCLIENTENCODING=UTF8")
	if stdin != "" {
		cmd.Stdin = strings.NewReader(stdin)
	}
	saida, err := cmd.CombinedOutput()
	if err != nil {
		return string(saida), fmt.Errorf("%w: %s", err, strings.TrimSpace(string(saida)))
	}
	return strings.TrimSpace(string(saida)), nil
}

func (c *config) consulta(banco, q string) (string, error) {
	return c.sql(banco, "", "-tA", "-c", q)
}

func perguntar(msg string) string {
	fmt.Print(msg)
	l, _ := bufio.NewReader(os.Stdin).ReadString('\n')
	return strings.TrimSpace(l)
}

func (c *config) passoBanco() error {
	passo(fmt.Sprintf("3/3  Banco de dados (%s no Postgres local)", c.pgBanco))
	c.psql = acharPsql()
	if c.psql == "" {
		return errors.New("não achei o psql. O Postgres está instalado? Coloque a pasta bin dele no PATH e rode de novo")
	}

	for tentativa := 1; ; tentativa++ {
		if _, err := c.consulta("postgres", "select 1"); err == nil {
			break
		}
		if tentativa > 3 || !term.IsTerminal(int(os.Stdin.Fd())) {
			return fmt.Errorf("não consegui entrar no Postgres em %s:%s como '%s'. Ele está ligado? A senha confere? (ADAPTAR_PG_USER / ADAPTAR_PG_SENHA mudam isso)", c.pgHost, c.pgPorta, c.pgUser)
		}
		aviso(fmt.Sprintf("não consegui conectar em %s:%s como '%s'. Informe os dados (tentativa %d de 3).", c.pgHost, c.pgPorta, c.pgUser, tentativa))
		if u := perguntar(fmt.Sprintf("   usuário do Postgres [%s]: ", c.pgUser)); u != "" {
			c.pgUser = u
		}
		fmt.Print("   senha: ")
		s, _ := term.ReadPassword(int(os.Stdin.Fd()))
		fmt.Println()
		c.pgSenha = string(s)
	}
	ok("conectado como " + c.pgUser)

	script, err := os.ReadFile(filepath.Join(c.raiz, "backend", "mercadofipp.sql"))
	if err != nil {
		return err
	}
	existe, err := c.consulta("postgres", fmt.Sprintf("select 1 from pg_database where datname = '%s'", c.pgBanco))
	if err != nil {
		return err
	}
	if existe != "1" {
		if _, err := c.sql("postgres", "", "-c", fmt.Sprintf(`create database "%s"`, c.pgBanco)); err != nil {
			return fmt.Errorf("não consegui criar o banco %s: %w", c.pgBanco, err)
		}
		if _, err := c.sql(c.pgBanco, string(script), "-f", "-"); err != nil {
			c.sql("postgres", "", "-c", fmt.Sprintf(`drop database "%s"`, c.pgBanco))
			return fmt.Errorf("falha ao carregar mercadofipp.sql (apaguei o banco pela metade): %w", err)
		}
		ok(fmt.Sprintf("banco %s criado e carregado com os dados de teste", c.pgBanco))
	} else if err := c.atualizarBancoExistente(string(script)); err != nil {
		return err
	}

	if err := os.MkdirAll(c.ferr, 0o755); err != nil {
		return err
	}
	conf := fmt.Sprintf("PG_HOST=%s\nPG_PORTA=%s\nPG_BANCO=%s\nPG_USER=%s\nPG_SENHA=%s\n", c.pgHost, c.pgPorta, c.pgBanco, c.pgUser, c.pgSenha)
	if err := os.WriteFile(filepath.Join(c.ferr, "banco.conf"), []byte(conf), 0o600); err != nil {
		return err
	}
	ok("configuração de conexão salva em .ferramentas/banco.conf")

	// o backend lê a conexão deste arquivo (importado pelo application.properties); fica fora do git
	props := fmt.Sprintf("spring.datasource.url=jdbc:postgresql://%s:%s/%s\nspring.datasource.username=%s\nspring.datasource.password=%s\n",
		c.pgHost, c.pgPorta, c.pgBanco, escaparPropriedade(c.pgUser), escaparPropriedade(c.pgSenha))
	if err := os.WriteFile(filepath.Join(c.raiz, "backend", "application-local.properties"), []byte(props), 0o600); err != nil {
		return err
	}
	ok("conexão do backend gravada em backend/application-local.properties")
	return nil
}

// formato .properties: a barra invertida precisa ser dobrada e o espaço inicial protegido
func escaparPropriedade(v string) string {
	v = strings.ReplaceAll(v, "\\", "\\\\")
	if strings.HasPrefix(v, " ") {
		v = "\\" + v
	}
	return v
}

func (c *config) atualizarBancoExistente(script string) error {
	tem, err := c.consulta(c.pgBanco, "select 1 from information_schema.tables where table_schema='public' and table_name='usuario'")
	if err != nil {
		return err
	}
	if tem != "1" {
		if _, err := c.sql(c.pgBanco, script, "-f", "-"); err != nil {
			return fmt.Errorf("falha ao carregar mercadofipp.sql no banco existente (vazio): %w", err)
		}
		ok(fmt.Sprintf("banco %s já existia vazio; carreguei as tabelas e os dados de teste", c.pgBanco))
		return nil
	}
	largura, err := c.consulta(c.pgBanco, "select coalesce(character_maximum_length, 0) from information_schema.columns where table_name='usuario' and column_name='usr_pass'")
	if err != nil {
		return err
	}
	if n, _ := strconv.Atoi(largura); n >= 60 {
		ok(fmt.Sprintf("banco %s já existe e está atualizado; não mexi nos dados", c.pgBanco))
		return nil
	}
	// banco antigo, com senha em texto puro: aplica só o bloco "SENHAS COM BCRYPT" (idempotente) do fim do script
	linhas := strings.Split(strings.ReplaceAll(script, "\n", "\n"), "\n")
	inicio := -1
	for i, l := range linhas {
		if strings.Contains(l, "-- SENHAS COM BCRYPT") {
			inicio = i - 1
			break
		}
	}
	if inicio < 0 {
		return errors.New("não achei o bloco de senhas em mercadofipp.sql")
	}
	if _, err := c.sql(c.pgBanco, strings.Join(linhas[inicio:], "\n"), "-f", "-"); err != nil {
		return fmt.Errorf("falha ao converter as senhas para BCrypt: %w", err)
	}
	ok(fmt.Sprintf("banco %s já existia: senhas convertidas para BCrypt (os anúncios e demais dados ficaram como estavam)", c.pgBanco))
	return nil
}
