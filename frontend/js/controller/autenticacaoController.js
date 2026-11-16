// Conversa com apis/autenticacao
const AutenticacaoController = {
    // POST apis/autenticacao/login (parâmetros usuario e senha) -> { usuario, token } ou erro
    async entrar(usuario, senha) {
        const resposta = await Http.requisitar(Config.ROTA_LOGIN, {
            metodo: 'POST',
            form: { usuario, senha },
            auth: false
        });
        if (!resposta || !resposta.token) throw new ErroHttp(0, 'O servidor respondeu ao login de um jeito inesperado.');
        Sessao.salvar(resposta.usuario ?? usuario, resposta.token);
    }
};
