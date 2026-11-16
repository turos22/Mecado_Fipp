// Página inicial depois do login (paginas/anuncios.html): vitrine de anúncios.
// Busca por texto vai ao servidor (get-filter). Categoria e ordenação são feitas aqui, na lista já recebida.
if (Sessao.exigirLogin()) {
    const parametros = new URLSearchParams(location.search);
    const estado = { todos: [], categoria: null, ordem: 'recentes', texto: parametros.get('q') ?? '' };

    const elLista = document.getElementById('lista');
    const elCategorias = document.getElementById('categorias');
    const elContador = document.getElementById('contador');
    const elOrdem = document.getElementById('ordem');
    const elAviso = document.getElementById('aviso');

    Cabecalho.montar(estado.texto);
    window.aoBuscar = texto => {
        estado.texto = texto;
        estado.categoria = null;
        history.replaceState(null, '', texto.trim() ? '?q=' + encodeURIComponent(texto) : location.pathname);
        carregar();
    };

    if (parametros.get('excluido')) Util.alerta(elAviso, 'success', `Anúncio ${parametros.get('excluido')} excluído com sucesso.`);

    const ordenacoes = {
        recentes: (a, b) => (b.data ?? '').localeCompare(a.data ?? '') || (b.id - a.id),
        menor: (a, b) => (a.preco ?? Infinity) - (b.preco ?? Infinity),
        maior: (a, b) => (b.preco ?? -Infinity) - (a.preco ?? -Infinity),
        nome: (a, b) => a.titulo.localeCompare(b.titulo, 'pt-BR')
    };

    async function carregar() {
        elLista.innerHTML = `<div class="col-12 w-100 text-center py-5">
            <div class="spinner-border text-primary" role="status"><span class="visually-hidden">Carregando…</span></div></div>`;
        elContador.textContent = '';
        try {
            estado.todos = await AnuncioController.filtrar(estado.texto);
            desenharCategorias();
            desenharLista();
        } catch (erro) {
            if (erro.status === 401) return; // a sessão já mandou para o login
            elCategorias.innerHTML = '';
            elLista.innerHTML = `<div class="col-12 w-100"><div class="alert alert-danger" role="alert">${Util.esc(erro.message)}</div></div>`;
        }
    }

    function desenharCategorias() {
        const contagem = new Map();
        for (const a of estado.todos) {
            const nome = a.categoria.nome ?? 'Sem categoria';
            contagem.set(nome, (contagem.get(nome) ?? 0) + 1);
        }
        const botao = (nome, rotulo, ativo) =>
            `<button type="button" class="btn btn-sm chip ${ativo ? 'chip-ativo' : ''}" data-categoria="${Util.esc(nome ?? '')}">${Util.esc(rotulo)}</button>`;
        elCategorias.innerHTML = botao(null, `Todas (${estado.todos.length})`, estado.categoria === null) +
            [...contagem].sort((a, b) => a[0].localeCompare(b[0], 'pt-BR'))
                .map(([nome, qtd]) => botao(nome, `${nome} (${qtd})`, estado.categoria === nome)).join('');
    }

    function desenharLista() {
        const lista = estado.todos
            .filter(a => estado.categoria === null || (a.categoria.nome ?? 'Sem categoria') === estado.categoria)
            .sort(ordenacoes[estado.ordem]);

        elContador.textContent = lista.length === 1 ? '1 anúncio' : `${lista.length} anúncios`;
        if (lista.length === 0) {
            elLista.innerHTML = `<div class="col-12 w-100 text-center text-muted py-5">
                <i class="bi bi-search fs-1 d-block mb-2"></i>Nenhum anúncio encontrado.</div>`;
            return;
        }
        elLista.innerHTML = lista.map(a => `
            <div class="col">
                <article class="card h-100 cartao-anuncio">
                    <div class="ratio ratio-4x3 bg-light">
                        <img src="${Util.esc(Util.urlFoto(a.fotos[0]))}" class="card-img-top foto" alt="Foto de ${Util.esc(a.titulo)}" loading="lazy">
                    </div>
                    <div class="card-body d-flex flex-column">
                        <span class="badge badge-categoria align-self-start mb-2">${Util.esc(a.categoria.nome ?? 'Sem categoria')}</span>
                        <h2 class="h6 card-title">
                            <a class="stretched-link text-decoration-none text-dark" href="anuncio.html?id=${encodeURIComponent(a.id)}">${Util.esc(a.titulo)}</a>
                        </h2>
                        <p class="preco mt-auto mb-1">${Util.esc(Util.preco(a.preco))}</p>
                        <small class="text-muted">${Util.esc(Util.data(a.data))}${a.vendedor ? ' · ' + Util.esc(a.vendedor) : ''}</small>
                    </div>
                </article>
            </div>`).join('');
    }

    // Se a foto não carregar, troca pela imagem cinza (o evento "error" não borbulha, por isso a captura)
    elLista.addEventListener('error', evento => {
        if (evento.target.tagName === 'IMG' && evento.target.src !== Util.SEM_FOTO) evento.target.src = Util.SEM_FOTO;
    }, true);

    elCategorias.addEventListener('click', evento => {
        const botao = evento.target.closest('[data-categoria]');
        if (!botao) return;
        estado.categoria = botao.dataset.categoria === '' ? null : botao.dataset.categoria;
        desenharCategorias();
        desenharLista();
    });

    elOrdem.addEventListener('change', () => {
        estado.ordem = elOrdem.value;
        desenharLista();
    });

    carregar();
}
