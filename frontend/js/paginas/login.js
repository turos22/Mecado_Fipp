// Página de login (paginas/login.html)
if (Sessao.logado()) {
    location.replace('anuncios.html');
} else {
    const formulario = document.getElementById('form-login');
    const mensagem = document.getElementById('mensagem');
    const botao = document.getElementById('btn-entrar');

    // Vindo de uma sessão que expirou, o motivo aparece aqui
    const motivo = new URLSearchParams(location.search).get('motivo');
    if (motivo) Util.alerta(mensagem, 'warning', motivo);

    formulario.addEventListener('submit', async evento => {
        evento.preventDefault();
        mensagem.innerHTML = '';
        if (!formulario.checkValidity()) {
            formulario.classList.add('was-validated');
            return;
        }

        botao.disabled = true;
        botao.innerHTML = '<span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Entrando…';
        try {
            await AutenticacaoController.entrar(
                document.getElementById('usuario').value.trim(),
                document.getElementById('senha').value);
            location.replace('anuncios.html');
        } catch (erro) {
            Util.alerta(mensagem, 'danger', erro.message);
            botao.disabled = false;
            botao.textContent = 'Entrar';
        }
    });
}
