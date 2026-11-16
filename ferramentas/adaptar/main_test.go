package main

import (
	"archive/zip"
	"os"
	"path/filepath"
	"testing"
)

func TestExtrairZipTiraAPastaRaizEBarraZipSlip(t *testing.T) {
	dir := t.TempDir()
	arq := filepath.Join(dir, "jdk.zip")
	f, _ := os.Create(arq)
	z := zip.NewWriter(f)
	for nome, conteudo := range map[string]string{
		"jdk-25.0.5+8/bin/java.exe":   "exe",
		"jdk-25.0.5+8/lib/modules":    "mod",
		"jdk-25.0.5+8/../../fora.txt": "malicioso",
		"../fora2.txt":                "malicioso",
	} {
		w, _ := z.Create(nome)
		w.Write([]byte(conteudo))
	}
	z.Close()
	f.Close()

	destino := filepath.Join(dir, "jdk")
	if err := extrairZip(arq, destino); err != nil {
		t.Fatal(err)
	}
	for _, esperado := range []string{"bin/java.exe", "lib/modules"} {
		if _, err := os.Stat(filepath.Join(destino, filepath.FromSlash(esperado))); err != nil {
			t.Errorf("faltou %s: %v", esperado, err)
		}
	}
	for _, proibido := range []string{filepath.Join(dir, "fora.txt"), filepath.Join(dir, "fora2.txt"), filepath.Join(filepath.Dir(dir), "fora.txt")} {
		if _, err := os.Stat(proibido); err == nil {
			t.Errorf("escapou do destino: %s", proibido)
		}
	}
}
