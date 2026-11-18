// Categorias (paginas/categorias.html): CRUD de categorias, só para o administrador.
// Esconder a tela é só conforto: quem barra de verdade é o backend (AccessFilter responde 403 para não-admin).
if (Sessao.exigirLogin()) {
    // Usuário comum volta para a vitrine ANTES de chamar o backend: o 403 faria o Http deslogá-lo.
    if (!Sessao.ehAdmin()) {
        location.replace('anuncios.html');
    } else {
        const elLista = document.getElementById('lista');
        const elAviso = document.getElementById('aviso');
        const elErro = document.getElementById('erro-categoria');
        const elTitulo = document.getElementById('titulo-modal-categoria');
        const formulario = document.getElementById('form-categoria');
        const campoNome = document.getElementById('campo-nome');
        const botaoSalvar = document.getElementById('btn-salvar');
        const modal = new bootstrap.Modal(document.getElementById('modal-categoria'));

        // editando: null = criando uma nova; senão, a categoria { id, nome } que está sendo alterada
        const estado = { categorias: [], editando: null };

        Cabecalho.montar('', 'categorias');

        async function carregar() {
            elLista.innerHTML = `<tr><td colspan="3" class="text-center py-5">
                <div class="spinner-border text-primary" role="status"><span class="visually-hidden">Carregando…</span></div></td></tr>`;
            try {
                estado.categorias = await CategoriaController.listar();
                desenhar();
            } catch (erro) {
                if (erro.status === 401 || erro.status === 403) return; // a sessão já mandou para o login
                elLista.innerHTML = `<tr><td colspan="3"><div class="alert alert-danger mb-0" role="alert">${Util.esc(erro.message)}</div></td></tr>`;
            }
        }

        function desenhar() {
            if (estado.categorias.length === 0) {
                elLista.innerHTML = `<tr><td colspan="3" class="text-center text-muted py-5">
                    <i class="bi bi-tags fs-1 d-block mb-2"></i>Nenhuma categoria cadastrada.</td></tr>`;
                return;
            }
            elLista.innerHTML = estado.categorias.map(c => `
                <tr>
                    <td class="text-muted">${Util.esc(c.id)}</td>
                    <td><span class="badge badge-categoria fs-6">${Util.esc(c.nome)}</span></td>
                    <td class="text-end text-nowrap">
                        <button type="button" class="btn btn-sm btn-outline-secondary" data-editar="${Util.esc(c.id)}">
                            <i class="bi bi-pencil me-1"></i>Editar</button>
                        <button type="button" class="btn btn-sm btn-outline-danger ms-1" data-excluir="${Util.esc(c.id)}">
                            <i class="bi bi-trash me-1"></i>Excluir</button>
                    </td>
                </tr>`).join('');
        }

        function abrirModal(categoria) {
            estado.editando = categoria;
            elTitulo.textContent = categoria ? 'Editar categoria' : 'Nova categoria';
            campoNome.value = categoria ? categoria.nome : '';
            elErro.innerHTML = '';
            formulario.classList.remove('was-validated');
            modal.show();
        }

        async function excluir(categoria) {
            if (!confirm(`Excluir a categoria "${categoria.nome}"?`)) return;
            elAviso.innerHTML = '';
            try {
                await CategoriaController.excluir(categoria.id);
                estado.categorias = estado.categorias.filter(c => c.id !== categoria.id);
                desenhar();
                Util.alerta(elAviso, 'success', `Categoria "${categoria.nome}" excluída.`);
            } catch (erro) {
                if (erro.status === 401 || erro.status === 403) return;
                // 400 = tem anúncios (a mensagem vem do backend); 404 = alguém já excluiu
                Util.alerta(elAviso, 'danger', erro.message);
                if (erro.status === 404) carregar();
            }
        }

        document.getElementById('btn-nova').addEventListener('click', () => abrirModal(null));

        // O foco vai para o campo assim que o modal termina de abrir
        document.getElementById('modal-categoria').addEventListener('shown.bs.modal', () => campoNome.focus());

        elLista.addEventListener('click', evento => {
            const botaoEditar = evento.target.closest('[data-editar]');
            const botaoExcluir = evento.target.closest('[data-excluir]');
            const id = (botaoEditar ?? botaoExcluir)?.dataset;
            if (!id) return;
            const categoria = estado.categorias.find(c => String(c.id) === String(botaoEditar ? id.editar : id.excluir));
            if (!categoria) return;
            if (botaoEditar) abrirModal(categoria);
            else excluir(categoria);
        });

        formulario.addEventListener('submit', async evento => {
            evento.preventDefault();
            elErro.innerHTML = '';
            campoNome.value = campoNome.value.trim();
            if (!formulario.checkValidity()) {
                formulario.classList.add('was-validated');
                return;
            }
            botaoSalvar.disabled = true;
            botaoSalvar.innerHTML = '<span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Salvando…';
            try {
                const editando = estado.editando;
                const salva = editando
                    ? await CategoriaController.alterar(editando.id, campoNome.value)
                    : await CategoriaController.criar(campoNome.value);
                if (editando) estado.categorias = estado.categorias.map(c => c.id === salva.id ? salva : c);
                else estado.categorias.push(salva);
                estado.categorias.sort((x, y) => x.nome.localeCompare(y.nome, 'pt-BR'));
                desenhar();
                modal.hide();
                Util.alerta(elAviso, 'success', editando
                    ? `Categoria "${salva.nome}" atualizada.`
                    : `Categoria "${salva.nome}" criada.`);
            } catch (erro) {
                if (erro.status === 401 || erro.status === 403) return;
                // 400 = nome vazio, longo ou repetido (a mensagem vem do backend); 404 = categoria já não existe
                Util.alerta(elErro, 'danger', erro.message);
            } finally {
                botaoSalvar.disabled = false;
                botaoSalvar.textContent = 'Salvar';
            }
        });

        carregar();
    }
}
