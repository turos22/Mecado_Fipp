// Meus anúncios (paginas/meus-anuncios.html): lista só os anúncios de quem está logado e deixa editar.
// Quem decide o que é "meu" é o backend (pelo token); aqui só se mostra o que ele devolveu.
if (Sessao.exigirLogin()) {
    const elLista = document.getElementById('lista');
    const elAviso = document.getElementById('aviso');
    const elErro = document.getElementById('erro-edicao');
    const formulario = document.getElementById('form-editar');
    const botaoSalvar = document.getElementById('btn-salvar');
    const modal = new bootstrap.Modal(document.getElementById('modal-editar'));
    const campos = {
        titulo: document.getElementById('campo-titulo'),
        categoria: document.getElementById('campo-categoria'),
        preco: document.getElementById('campo-preco'),
        descricao: document.getElementById('campo-descricao')
    };

    const estado = { meus: [], categorias: [], editando: null };

    Cabecalho.montar('', 'meus');

    async function carregar() {
        elLista.innerHTML = `<div class="col-12 w-100 text-center py-5">
            <div class="spinner-border text-primary" role="status"><span class="visually-hidden">Carregando…</span></div></div>`;
        try {
            estado.meus = await AnuncioController.meus();
            desenhar();
        } catch (erro) {
            if (erro.status === 401 || erro.status === 403) return; // a sessão já mandou para o login
            elLista.innerHTML = `<div class="col-12 w-100"><div class="alert alert-danger" role="alert">${Util.esc(erro.message)}</div></div>`;
        }
    }

    function desenhar() {
        if (estado.meus.length === 0) {
            elLista.innerHTML = `<div class="col-12 w-100 text-center text-muted py-5">
                <i class="bi bi-bag fs-1 d-block mb-2"></i>Você ainda não tem anúncios.</div>`;
            return;
        }
        elLista.innerHTML = estado.meus.map(a => `
            <div class="col">
                <article class="card h-100 cartao-anuncio">
                    <div class="card-body d-flex gap-3">
                        <img src="${Util.esc(Util.urlFoto(a.fotos[0]))}" class="rounded foto" width="96" height="96" alt="" loading="lazy">
                        <div class="flex-grow-1">
                            <span class="badge badge-categoria mb-1">${Util.esc(a.categoria.nome ?? 'Sem categoria')}</span>
                            <h2 class="h6 mb-1">${Util.esc(a.titulo)}</h2>
                            <p class="preco mb-1">${Util.esc(Util.preco(a.preco))}</p>
                            <small class="text-muted">${Util.esc(Util.data(a.data))} · ${a.perguntas.length} pergunta(s)</small>
                        </div>
                    </div>
                    <div class="card-footer bg-white d-flex justify-content-between">
                        <a class="btn btn-sm btn-outline-secondary" href="anuncio.html?id=${encodeURIComponent(a.id)}">Ver como aparece</a>
                        <button type="button" class="btn btn-sm btn-mf" data-editar="${Util.esc(a.id)}"><i class="bi bi-pencil me-1"></i>Editar</button>
                    </div>
                </article>
            </div>`).join('');
    }

    // As categorias não têm rota própria no backend: saem dos anúncios que já existem
    async function carregarCategorias() {
        if (estado.categorias.length) return;
        const todos = await AnuncioController.filtrar('');
        const mapa = new Map();
        for (const a of todos) if (a.categoria.id !== null) mapa.set(a.categoria.id, a.categoria.nome ?? 'Categoria ' + a.categoria.id);
        estado.categorias = [...mapa].map(([id, nome]) => ({ id, nome })).sort((x, y) => x.nome.localeCompare(y.nome, 'pt-BR'));
    }

    async function abrirEdicao(id) {
        const anuncio = estado.meus.find(a => String(a.id) === String(id));
        if (!anuncio) return;
        estado.editando = anuncio;
        elErro.innerHTML = '';
        formulario.classList.remove('was-validated');
        try {
            await carregarCategorias();
        } catch (erro) {
            if (erro.status === 401 || erro.status === 403) return;
            Util.alerta(elAviso, 'danger', erro.message);
            return;
        }
        // O anúncio pode estar numa categoria que (por algum motivo) não veio da lista: garante que ela exista no select
        const opcoes = estado.categorias.some(c => c.id === anuncio.categoria.id)
            ? estado.categorias
            : [...estado.categorias, { id: anuncio.categoria.id, nome: anuncio.categoria.nome ?? 'Categoria ' + anuncio.categoria.id }];
        campos.categoria.innerHTML = opcoes.map(c => `<option value="${Util.esc(c.id)}">${Util.esc(c.nome)}</option>`).join('');
        campos.categoria.value = String(anuncio.categoria.id);
        campos.titulo.value = anuncio.titulo;
        campos.preco.value = anuncio.preco ?? '';
        campos.descricao.value = anuncio.descricao;
        modal.show();
    }

    elLista.addEventListener('click', evento => {
        const botao = evento.target.closest('[data-editar]');
        if (botao) abrirEdicao(botao.dataset.editar);
    });

    // Se a foto não carregar, troca pela imagem cinza
    elLista.addEventListener('error', evento => {
        if (evento.target.tagName === 'IMG' && evento.target.src !== Util.SEM_FOTO) evento.target.src = Util.SEM_FOTO;
    }, true);

    formulario.addEventListener('submit', async evento => {
        evento.preventDefault();
        elErro.innerHTML = '';
        campos.titulo.value = campos.titulo.value.trim();
        if (!formulario.checkValidity()) {
            formulario.classList.add('was-validated');
            return;
        }
        botaoSalvar.disabled = true;
        botaoSalvar.innerHTML = '<span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Salvando…';
        try {
            const salvo = await AnuncioController.editar(estado.editando.id, {
                titulo: campos.titulo.value,
                descricao: campos.descricao.value,
                preco: Number(campos.preco.value),
                categoriaId: Number(campos.categoria.value)
            });
            const posicao = estado.meus.findIndex(a => a.id === salvo.id);
            if (posicao >= 0) estado.meus[posicao] = { ...estado.meus[posicao], ...salvo, fotos: estado.meus[posicao].fotos, perguntas: estado.meus[posicao].perguntas };
            desenhar();
            modal.hide();
            Util.alerta(elAviso, 'success', `Anúncio "${salvo.titulo}" atualizado.`);
        } catch (erro) {
            if (erro.status === 401 || erro.status === 403) return;
            Util.alerta(elErro, 'danger', erro.status === 404 ? 'Este anúncio não existe mais ou não é seu.' : erro.message);
        } finally {
            botaoSalvar.disabled = false;
            botaoSalvar.textContent = 'Salvar';
        }
    });

    carregar();
}
