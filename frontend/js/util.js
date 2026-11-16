// Funções de apoio, sem relação com tela ou servidor.
const Util = {
    // Dados dos anúncios vêm de outros usuários: NUNCA colocar direto no HTML sem passar por aqui.
    esc(texto) {
        return String(texto ?? '')
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    },

    // 1890.5 -> "R$ 1.890,50"
    preco(valor) {
        if (valor === null || valor === undefined || Number.isNaN(valor)) return 'Preço não informado';
        return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(valor);
    },

    // "2026-11-14" -> "14/11/2026" (sem usar Date, para o fuso horário não mudar o dia)
    data(texto) {
        const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(texto ?? '');
        return m ? `${m[3]}/${m[2]}/${m[1]}` : (texto ?? '');
    },

    // Imagem cinza usada quando o anúncio não tem foto ou o arquivo não carrega
    SEM_FOTO: 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
        '<svg xmlns="http://www.w3.org/2000/svg" width="400" height="300" viewBox="0 0 400 300">' +
        '<rect width="400" height="300" fill="#e9edf5"/>' +
        '<g fill="none" stroke="#9aa6bf" stroke-width="8" stroke-linecap="round" stroke-linejoin="round">' +
        '<rect x="140" y="100" width="120" height="90" rx="10"/><circle cx="175" cy="132" r="11"/>' +
        '<path d="M150 180l32-30 24 22 18-14 36 22"/></g>' +
        '<text x="200" y="228" text-anchor="middle" font-family="sans-serif" font-size="20" fill="#7c89a6">Sem foto</text></svg>'),

    urlFoto(arquivo) {
        return arquivo ? Config.URL_FOTOS + encodeURIComponent(arquivo) : Util.SEM_FOTO;
    },

    // Mostra uma mensagem (Bootstrap alert) dentro de um elemento
    alerta(elemento, tipo, texto) {
        elemento.innerHTML = `<div class="alert alert-${tipo} mb-0" role="alert">${Util.esc(texto)}</div>`;
    }
};
