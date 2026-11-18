// Única porta de saída para o backend: todo fetch do sistema passa por aqui.
class ErroHttp extends Error {
    constructor(status, mensagem, dados = null) {
        super(mensagem);
        this.status = status;
        this.dados = dados;
    }
}

const Http = {
    async requisitar(rota, { metodo = 'GET', form = null, corpo = null, arquivos = null, auth = true } = {}) {
        const headers = {};
        let body;

        if (auth) {
            if (Sessao.expirou()) {
                Sessao.sair('Sua sessão expirou. Entre novamente.');
                throw new ErroHttp(401, 'Sessão expirada');
            }
            headers['Authorization'] = Sessao.token();
        }
        if (arquivos) {
            body = arquivos;
        } else if (form) {
            headers['Content-Type'] = 'application/x-www-form-urlencoded';
            body = new URLSearchParams(form);
        } else if (corpo) {
            headers['Content-Type'] = 'application/json';
            body = JSON.stringify(corpo);
        }

        let resposta;
        try {
            resposta = await fetch(Config.API + rota, { method: metodo, headers, body });
        } catch {
            throw new ErroHttp(0, 'Não foi possível conectar ao servidor. Ele está ligado?');
        }

        const texto = await resposta.text();
        let dados = texto;
        try {
            dados = JSON.parse(texto);
        } catch { }

        const naoAutorizado = resposta.status === 401 || resposta.status === 403 ||
            (resposta.status === 500 && typeof dados === 'string' && dados.includes('Não autorizado'));
        if (auth && naoAutorizado) {
            Sessao.sair('Sua sessão expirou ou você não tem permissão. Entre novamente.');
            throw new ErroHttp(resposta.status, 'Não autorizado');
        }
        if (!resposta.ok) throw new ErroHttp(resposta.status, this.mensagemDeErro(dados, resposta.status), dados);
        return dados;
    },

    mensagemDeErro(dados, status) {
        if (dados && typeof dados === 'object' && dados.texto) return dados.texto;
        if (typeof dados === 'string' && dados.trim() && !dados.trim().startsWith('<')) return dados.trim().slice(0, 200);
        return `O servidor respondeu com erro ${status}.`;
    }
};
