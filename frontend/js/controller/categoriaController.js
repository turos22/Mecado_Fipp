// Conversa com apis/adm/categoria. Criar, alterar, excluir e buscar: só administrador.
// listar (get-all) também é liberado para usuário comum: o formulário de anúncio usa a lista.
//
// JSON de uma categoria no backend (nomes iguais às colunas do banco): { cat_id, cat_name }
// Aqui ele vira { id, nome }, para as páginas não dependerem dos nomes do banco.

function normalizarCategoria(c) {
    return { id: c?.cat_id ?? null, nome: c?.cat_name ?? '' };
}

const CategoriaController = {
    // GET apis/adm/categoria/get-all -> lista de categorias, em ordem alfabética
    async listar() {
        const dados = await Http.requisitar(`${Config.ROTA_ADM_CATEGORIA}/get-all`);
        if (!Array.isArray(dados)) throw new ErroHttp(0, 'O servidor devolveu a lista de categorias de um jeito inesperado.');
        return dados.map(normalizarCategoria).sort((x, y) => x.nome.localeCompare(y.nome, 'pt-BR'));
    },

    // GET apis/adm/categoria/get-id/{id} -> uma categoria
    async buscar(id) {
        const dados = await Http.requisitar(`${Config.ROTA_ADM_CATEGORIA}/get-id/${encodeURIComponent(id)}`);
        return normalizarCategoria(dados);
    },

    // POST apis/adm/categoria com {cat_name} -> categoria criada (com o id gerado pelo banco)
    async criar(nome) {
        const dados = await Http.requisitar(Config.ROTA_ADM_CATEGORIA, {
            metodo: 'POST',
            corpo: { cat_name: nome }
        });
        return normalizarCategoria(dados);
    },

    // PUT apis/adm/categoria com {cat_id, cat_name} -> categoria alterada
    async alterar(id, nome) {
        const dados = await Http.requisitar(Config.ROTA_ADM_CATEGORIA, {
            metodo: 'PUT',
            corpo: { cat_id: id, cat_name: nome }
        });
        return normalizarCategoria(dados);
    },

    // DELETE apis/adm/categoria?id=ID (o backend recusa com 400 se houver anúncios na categoria)
    async excluir(id) {
        await Http.requisitar(`${Config.ROTA_ADM_CATEGORIA}?id=${encodeURIComponent(id)}`, { metodo: 'DELETE' });
    }
};
