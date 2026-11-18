// Meus anúncios (paginas/meus-anuncios.html): lista só os anúncios de quem está logado e deixa criar, editar, enviar fotos e excluir.
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
        descricao: document.getElementById('campo-descricao'),
        fotos: document.getElementById('campo-fotos')
    };
    const modalExcluir = new bootstrap.Modal(document.getElementById('modal-excluir'));
    const botaoConfirmarExclusao = document.getElementById('btn-confirmar-exclusao');
    const TIPOS_FOTO = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
    const TAMANHO_MAXIMO_FOTO = 5 * 1024 * 1024; // o mesmo limite do backend

    // editando: o anúncio aberto no formulário (null = anúncio novo); excluindo: o anúncio do modal de exclusão
    const estado = { meus: [], categorias: [], editando: null, excluindo: null };

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
                            ${a.perguntas.some(p => !p.resposta) ? `<span class="badge text-bg-warning ms-1">${a.perguntas.filter(p => !p.resposta).length} sem resposta</span>` : ''}
                        </div>
                    </div>
                    <div class="card-footer bg-white d-flex justify-content-between">
                        <a class="btn btn-sm btn-outline-secondary" href="anuncio.html?id=${encodeURIComponent(a.id)}">Ver anúncio e perguntas</a>
                        <div class="d-flex gap-2">
                            <button type="button" class="btn btn-sm btn-outline-danger" data-excluir="${Util.esc(a.id)}" aria-label="Excluir"><i class="bi bi-trash3"></i></button>
                            <button type="button" class="btn btn-sm btn-mf" data-editar="${Util.esc(a.id)}"><i class="bi bi-pencil me-1"></i>Editar</button>
                        </div>
                    </div>
                </article>
            </div>`).join('');
    }

    // Categorias vêm do banco (GET apis/adm/categoria/get-all, liberado para leitura a qualquer usuário logado).
    // Busca de novo a cada abertura do formulário, para já trazer as categorias que o administrador acabou de criar.
    async function carregarCategorias() {
        estado.categorias = await CategoriaController.listar();
    }

    // id = null abre o formulário vazio (anúncio novo); com id, abre o anúncio para editar
    async function abrirFormulario(id) {
        const anuncio = id === null ? null : estado.meus.find(a => String(a.id) === String(id));
        if (id !== null && !anuncio) return;
        estado.editando = anuncio;
        elErro.innerHTML = '';
        formulario.classList.remove('was-validated');
        formulario.reset(); // limpa também os arquivos escolhidos antes
        try {
            await carregarCategorias();
        } catch (erro) {
            if (erro.status === 401 || erro.status === 403) return;
            Util.alerta(elAviso, 'danger', erro.message);
            return;
        }
        if (!anuncio && estado.categorias.length === 0) {
            Util.alerta(elAviso, 'warning', 'Nenhuma categoria foi encontrada, então ainda não dá para criar anúncios.');
            return;
        }
        document.getElementById('titulo-modal-editar').textContent = anuncio ? 'Editar anúncio' : 'Novo anúncio';
        if (!anuncio) {
            campos.categoria.innerHTML = '<option value="" selected disabled>Escolha…</option>' +
                estado.categorias.map(c => `<option value="${Util.esc(c.id)}">${Util.esc(c.nome)}</option>`).join('');
            modal.show();
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

    // Confere as fotos antes de enviar (o backend confere de novo)
    function erroNasFotos(arquivos) {
        for (const arquivo of arquivos) {
            if (!TIPOS_FOTO.includes(arquivo.type)) return `"${arquivo.name}" não é JPG, PNG, GIF ou WEBP.`;
            if (arquivo.size > TAMANHO_MAXIMO_FOTO) return `"${arquivo.name}" passa de 5 MB.`;
        }
        return null;
    }

    // Põe na lista o anúncio devolvido pelo backend. substituir=true troca tudo (o retorno de criar/addFoto vem completo);
    // senão mantém fotos e perguntas que já estavam na tela (o retorno da edição)
    function guardar(salvo, substituir) {
        const posicao = estado.meus.findIndex(a => a.id === salvo.id);
        if (posicao < 0) estado.meus.unshift(salvo);
        else if (substituir) estado.meus[posicao] = salvo;
        else estado.meus[posicao] = { ...estado.meus[posicao], ...salvo, fotos: estado.meus[posicao].fotos, perguntas: estado.meus[posicao].perguntas };
    }

    elLista.addEventListener('click', evento => {
        const editar = evento.target.closest('[data-editar]');
        if (editar) abrirFormulario(editar.dataset.editar);
        const excluir = evento.target.closest('[data-excluir]');
        if (excluir) {
            estado.excluindo = estado.meus.find(a => String(a.id) === excluir.dataset.excluir);
            if (!estado.excluindo) return;
            document.getElementById('nome-excluir').textContent = estado.excluindo.titulo;
            document.getElementById('erro-exclusao').innerHTML = '';
            modalExcluir.show();
        }
    });

    document.getElementById('btn-novo').addEventListener('click', () => abrirFormulario(null));

    botaoConfirmarExclusao.addEventListener('click', async () => {
        const anuncio = estado.excluindo;
        const aviso = document.getElementById('erro-exclusao');
        aviso.innerHTML = '';
        botaoConfirmarExclusao.disabled = true;
        botaoConfirmarExclusao.innerHTML = '<span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Excluindo…';
        try {
            await AnuncioController.excluir(anuncio.id);
            estado.meus = estado.meus.filter(a => a.id !== anuncio.id);
            desenhar();
            modalExcluir.hide();
            Util.alerta(elAviso, 'success', `Anúncio "${anuncio.titulo}" excluído.`);
        } catch (erro) {
            if (erro.status === 401 || erro.status === 403) return;
            Util.alerta(aviso, 'danger', erro.status === 404 ? 'Este anúncio não existe mais ou não é seu.' : erro.message);
        } finally {
            botaoConfirmarExclusao.disabled = false;
            botaoConfirmarExclusao.textContent = 'Sim, excluir';
        }
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
        const fotos = [...campos.fotos.files];
        const erroFoto = erroNasFotos(fotos);
        if (erroFoto) {
            Util.alerta(elErro, 'danger', erroFoto);
            return;
        }
        const criando = estado.editando === null;
        botaoSalvar.disabled = true;
        botaoSalvar.innerHTML = '<span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Salvando…';
        try {
            const dados = {
                titulo: campos.titulo.value,
                descricao: campos.descricao.value,
                preco: Number(campos.preco.value),
                categoriaId: Number(campos.categoria.value)
            };
            let salvo = criando ? await AnuncioController.criar(dados) : await AnuncioController.editar(estado.editando.id, dados);
            guardar(salvo, criando);

            // O anúncio já está salvo: se só as fotos falharem, ele fica e o usuário é avisado
            let erroEnvio = null;
            if (fotos.length) {
                try {
                    salvo = await AnuncioController.adicionarFotos(salvo.id, fotos);
                    guardar(salvo, true);
                } catch (erro) {
                    if (erro.status === 401 || erro.status === 403) return;
                    erroEnvio = erro.message;
                }
            }
            desenhar();
            modal.hide();
            if (erroEnvio) Util.alerta(elAviso, 'warning', `Anúncio "${salvo.titulo}" salvo, mas as fotos não foram enviadas: ${erroEnvio} Edite o anúncio para tentar de novo.`);
            else Util.alerta(elAviso, 'success', `Anúncio "${salvo.titulo}" ${criando ? 'criado' : 'atualizado'}.`);
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
