// Quem está logado. O token fica no sessionStorage: some quando a aba é fechada.
// O token é decodificado aqui SÓ para mostrar o nome e esconder botões. Quem barra de verdade é o backend.
const Sessao = {
    CHAVE: 'mercadofipp.sessao',

    salvar(usuario, token) {
        sessionStorage.setItem(this.CHAVE, JSON.stringify({ usuario, token }));
    },

    dados() {
        try {
            return JSON.parse(sessionStorage.getItem(this.CHAVE));
        } catch {
            return null;
        }
    },

    token() {
        return this.dados()?.token ?? null;
    },

    // O JWT é "cabeçalho.payload.assinatura", cada parte em base64url
    payload() {
        const token = this.token();
        if (!token) return null;
        try {
            const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
            const bytes = Uint8Array.from(atob(base64), c => c.charCodeAt(0));
            return JSON.parse(new TextDecoder().decode(bytes));
        } catch {
            return null;
        }
    },

    nome() {
        return this.payload()?.sub ?? this.dados()?.usuario ?? '';
    },

    // Identidade que o backend pôs no token. Só para a tela saber "este anúncio é meu": quem decide é o backend.
    usuarioId() {
        return this.payload()?.usr_id ?? null;
    },

    nivel() {
        return this.payload()?.nivel ?? '';
    },

    ehAdmin() {
        return this.nivel() === 'admin';
    },

    expirou() {
        const exp = this.payload()?.exp;
        return !exp || exp * 1000 <= Date.now();
    },

    logado() {
        return this.token() !== null && !this.expirou();
    },

    // Para páginas que exigem login: se não estiver logado, vai para o login e devolve false
    exigirLogin() {
        if (this.logado()) return true;
        this.sair(this.token() ? 'Sua sessão expirou. Entre novamente.' : '');
        return false;
    },

    // Chamado de dentro de paginas/. O motivo aparece na tela de login.
    sair(motivo) {
        sessionStorage.removeItem(this.CHAVE);
        location.replace('login.html' + (motivo ? '?motivo=' + encodeURIComponent(motivo) : ''));
    }
};
