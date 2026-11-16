// Visualização de um anúncio (paginas/anuncio.html?id=N)
// Usa: GET get-id/{id} para mostrar e, só para o administrador, DELETE para excluir.
if (Sessao.exigirLogin()) {
    Cabecalho.montar();
    const conteudo = document.getElementById('conteudo');
    const id = new URLSearchParams(location.search).get('id');

    if (!id) {
        Util.alerta(conteudo, 'warning', 'Nenhum anúncio foi informado. Volte à lista e escolha um.');
    } else {
        carregar(id);
    }

    async function carregar(id) {
        conteudo.innerHTML = `<div class="text-center py-5">
            <div class="spinner-border text-primary" role="status"><span class="visually-hidden">Carregando…</span></div></div>`;
        try {
            desenhar(await AnuncioController.buscar(id));
        } catch (erro) {
            if (erro.status === 401) return; // a sessão já mandou para o login
            // 404 com "texto" é o sistema dizendo que o anúncio não existe. 404 sem "texto" é o Spring
            // dizendo que a ROTA não existe (o backend ainda não implementou o get-id).
            const anuncioInexistente = erro.status === 404 && erro.dados && erro.dados.texto;
            if (anuncioInexistente) Util.alerta(conteudo, 'warning', 'Anúncio não encontrado. Ele pode ter sido excluído.');
            else if (erro.status === 404) Util.alerta(conteudo, 'danger', `O servidor ainda não tem a rota GET ${AnuncioController.base()}/get-id/{id} (erro 404).`);
            else Util.alerta(conteudo, 'danger', erro.message);
        }
    }

    function desenhar(a) {
        document.title = `${a.titulo} · Mercado FIPP`;
        const fotos = a.fotos.length ? a.fotos : [null];

        const slides = fotos.map((arquivo, i) => `
            <div class="carousel-item ${i === 0 ? 'active' : ''}">
                <div class="ratio ratio-4x3 bg-light">
                    <img src="${Util.esc(Util.urlFoto(arquivo))}" class="d-block w-100 foto-detalhe" alt="Foto ${i + 1} de ${Util.esc(a.titulo)}">
                </div>
            </div>`).join('');
        const controles = fotos.length > 1 ? `
            <div class="carousel-indicators">${fotos.map((_, i) => `
                <button type="button" data-bs-target="#carrossel" data-bs-slide-to="${i}" ${i === 0 ? 'class="active" aria-current="true"' : ''} aria-label="Foto ${i + 1}"></button>`).join('')}
            </div>
            <button class="carousel-control-prev" type="button" data-bs-target="#carrossel" data-bs-slide="prev">
                <span class="carousel-control-prev-icon" aria-hidden="true"></span><span class="visually-hidden">Foto anterior</span></button>
            <button class="carousel-control-next" type="button" data-bs-target="#carrossel" data-bs-slide="next">
                <span class="carousel-control-next-icon" aria-hidden="true"></span><span class="visually-hidden">Próxima foto</span></button>` : '';

        const perguntas = a.perguntas.length ? a.perguntas.map(p => `
            <li class="list-group-item py-3">
                <div><i class="bi bi-chat-left-text me-2 text-primary"></i>${Util.esc(p.texto)}</div>
                ${p.resposta
                    ? `<div class="resposta mt-2"><i class="bi bi-arrow-return-right me-2"></i>${Util.esc(p.resposta)}</div>`
                    : '<div class="text-muted small mt-2 ms-4">Aguardando resposta do vendedor.</div>'}
            </li>`).join('') : '<li class="list-group-item text-muted py-3">Ninguém perguntou nada ainda.</li>';

        const botaoExcluir = Sessao.ehAdmin() ? `
            <button id="btn-excluir" type="button" class="btn btn-outline-danger w-100 mt-3" data-bs-toggle="modal" data-bs-target="#modal-excluir">
                <i class="bi bi-trash3 me-2"></i>Excluir anúncio
            </button>` : '';

        conteudo.innerHTML = `
            <nav aria-label="breadcrumb">
                <ol class="breadcrumb">
                    <li class="breadcrumb-item"><a href="anuncios.html">Anúncios</a></li>
                    <li class="breadcrumb-item">${Util.esc(a.categoria.nome ?? 'Sem categoria')}</li>
                    <li class="breadcrumb-item active" aria-current="page">${Util.esc(a.titulo)}</li>
                </ol>
            </nav>
            <div class="row g-4">
                <div class="col-lg-7">
                    <div id="carrossel" class="carousel slide card overflow-hidden">
                        <div class="carousel-inner">${slides}</div>${controles}
                    </div>
                </div>
                <div class="col-lg-5">
                    <div class="card painel-preco">
                        <div class="card-body">
                            <span class="badge badge-categoria mb-2">${Util.esc(a.categoria.nome ?? 'Sem categoria')}</span>
                            <h1 class="h3">${Util.esc(a.titulo)}</h1>
                            <p class="preco preco-grande my-3">${Util.esc(Util.preco(a.preco))}</p>
                            <ul class="list-unstyled text-muted mb-0">
                                <li><i class="bi bi-calendar3 me-2"></i>Publicado em ${Util.esc(Util.data(a.data))}</li>
                                ${a.vendedor ? `<li><i class="bi bi-person me-2"></i>${Util.esc(a.vendedor)}</li>` : ''}
                                <li><i class="bi bi-hash me-2"></i>Anúncio ${Util.esc(a.id)}</li>
                            </ul>
                            ${botaoExcluir}
                        </div>
                    </div>
                </div>
                <div class="col-lg-7">
                    <div class="card"><div class="card-body">
                        <h2 class="h5">Descrição</h2>
                        <p class="mb-0 descricao">${Util.esc(a.descricao) || '<span class="text-muted">Sem descrição.</span>'}</p>
                    </div></div>
                </div>
                <div class="col-12 col-lg-7">
                    <div class="card">
                        <div class="card-header bg-white"><h2 class="h5 mb-0">Perguntas e respostas</h2></div>
                        <ul class="list-group list-group-flush">${perguntas}</ul>
                    </div>
                </div>
            </div>`;

        conteudo.addEventListener('error', evento => {
            if (evento.target.tagName === 'IMG' && evento.target.src !== Util.SEM_FOTO) evento.target.src = Util.SEM_FOTO;
        }, true);

        const botaoConfirmar = document.getElementById('btn-confirmar-exclusao');
        botaoConfirmar.onclick = async () => {
            const aviso = document.getElementById('erro-exclusao');
            aviso.innerHTML = '';
            botaoConfirmar.disabled = true;
            botaoConfirmar.innerHTML = '<span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Excluindo…';
            try {
                await AnuncioController.excluir(a.id);
                location.replace('anuncios.html?excluido=' + encodeURIComponent(a.id));
            } catch (erro) {
                if (erro.status === 401) return;
                Util.alerta(aviso, 'danger', erro.message);
                botaoConfirmar.disabled = false;
                botaoConfirmar.textContent = 'Sim, excluir';
            }
        };
    }
}
